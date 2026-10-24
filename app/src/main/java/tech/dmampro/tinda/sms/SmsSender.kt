package tech.dmampro.tinda.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.TindaLogger
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envoi de SMS (F2) : double SIM, messages longs découpés, accusés d'envoi
 * et de livraison (DEC-008 : livraison activée par défaut).
 *
 * Séquence : ligne Room (SENDING) -> appel radio -> écriture provider Sent.
 * Les statuts sont affinés par [SmsStatusReceiver] via le jeton de corrélation.
 * Jamais de contenu ni de numéro dans les logs.
 */
@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TindaDatabase,
    private val repository: SmsRepository,
) {
    data class SendResult(val token: String, val partCount: Int)

    suspend fun send(
        address: String,
        body: String,
        subscriptionId: Int,
        threadId: Long,
        deliveryReport: Boolean = true,
    ): SendResult = withContext(Dispatchers.IO) {
        val (isDefault, source) = tech.dmampro.tinda.util.isDefaultSmsAppDetail(context)
        SendJournal.log("send() : rôle par défaut=$isDefault (source=$source)")
        if (!isDefault) {
            SendJournal.log("send() : REFUSÉ, pas l'appli SMS par défaut")
            throw SecurityException("Envoi refusé : Tinda n'est pas la default SMS app")
        }
        val token = UUID.randomUUID().toString()
        val smsManager = smsManagerFor(subscriptionId)
        val parts = smsManager.divideMessage(body)

        // Emplacement de la SIM à cet instant précis, conservé avec
        // le message : si la carte change de logement plus tard,
        // l'historique de ce message reste vrai. Illisible (permission
        // ou carte absente) = null, donc « SIM inconnue », jamais une
        // valeur déduite après coup.
        val simSlot = runCatching {
            if (subscriptionId >= 0) {
                context.getSystemService(SubscriptionManager::class.java)
                    ?.getActiveSubscriptionInfo(subscriptionId)
                    ?.simSlotIndex
            } else {
                null
            }
        }.getOrNull()

        // 1. Ligne locale immédiate (l'UI affiche "envoi en cours").
        val rowId = db.messageDao().insert(
            Message(
                threadId = threadId,
                address = address,
                body = body,
                date = System.currentTimeMillis(),
                read = true,
                incoming = false,
                status = MessageStatus.SENDING,
                token = token,
                subscriptionId = subscriptionId,
                simSlot = simSlot,
            ),
        )
        repository.notifyChanged()

        // 1 bis. Suivi des segments (L3) : les accusés seront agrégés
        // par segment, jamais appliqués un par un au statut du message.
        db.messagePartDao().insertAll(
            parts.indices.map { index ->
                tech.dmampro.tinda.data.local.MessagePart(token = token, partIndex = index)
            },
        )

        // 2 et 3. Appel radio, puis écriture dans le provider Sent.
        // La frontière compte (voir SendFlow) : une exception de
        // l'appel radio vaut échec de soumission ; une exception
        // après son retour (écriture fournisseur, association du
        // providerId) ne prouve pas que le SMS n'est pas parti. Dans
        // ce second cas, le message reste SENDING jusqu'à l'accusé ou
        // à la réconciliation, et rien ne propose de renvoi.
        val outcome = SendFlow.execute(
            submitToRadio = {
                if (parts.size <= 1) {
                    smsManager.sendTextMessage(
                        address, null, body,
                        statusIntent(ACTION_SMS_SENT, token, 0),
                        if (deliveryReport) {
                            statusIntent(ACTION_SMS_DELIVERED, token, 0)
                        } else {
                            null
                        },
                    )
                } else {
                    val sent = ArrayList<PendingIntent>(parts.size)
                    val delivered = ArrayList<PendingIntent>(parts.size)
                    parts.forEachIndexed { index, _ ->
                        sent.add(statusIntent(ACTION_SMS_SENT, token, index))
                        if (deliveryReport) {
                            delivered.add(statusIntent(ACTION_SMS_DELIVERED, token, index))
                        }
                    }
                    smsManager.sendMultipartTextMessage(
                        address, null, parts, sent,
                        delivered.takeIf { deliveryReport },
                    )
                }
            },
            writeToProvider = {
                val providerId = writeToSentProvider(address, body, subscriptionId)
                SendJournal.log("send() : provider Sent id=$providerId")
                providerId?.let { db.messageDao().updateProviderId(rowId, it) }
                providerId
            },
            onProviderFailure = { e ->
                TindaLogger.e(TAG, "échec après l'appel radio", e)
                SendJournal.log(
                    "send() : écriture fournisseur en échec après l'appel radio, " +
                        "état laissé en attente des accusés",
                )
            },
        )
        when (outcome) {
            is SendFlow.Outcome.RadioFailed -> {
                val e = outcome.error
                TindaLogger.e(TAG, "exception pendant l'appel radio", e)
                SendJournal.log("send() : EXCEPTION radio ${e.javaClass.simpleName}")
                // La synchronisation du dépôt protège un accusé déjà
                // persisté en terminal et aligne le fournisseur sur
                // l'état réellement conservé, même pour cette exception.
                repository.updateMessageStatus(
                    token, MessageStatus.FAILED, FAILURE_EXCEPTION
                )
                throw e
            }

            is SendFlow.Outcome.Submitted -> {
                SendJournal.log("send() : appel radio OK, en attente des accusés")
                SendResult(token, parts.size)
            }
        }
    }

    private fun smsManagerFor(subscriptionId: Int): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)!!.createForSubscriptionId(subscriptionId)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        }

    /**
     * Envoi depuis le coffre secret : appel radio uniquement, JAMAIS écrit
     * dans le provider système (RB-01) ni dans la base principale. Pas de
     * suivi de statut (aucun PendingIntent) : un appel radio qui ne lève
     * pas d'exception vaut envoi. La trace reste dans la base chiffrée.
     */
    suspend fun sendVault(address: String, body: String): Unit = withContext(Dispatchers.IO) {
        val (isDefault, _) = tech.dmampro.tinda.util.isDefaultSmsAppDetail(context)
        if (!isDefault) throw SecurityException("Envoi refusé : Tinda n'est pas la default SMS app")
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(body)
        if (parts.size <= 1) {
            smsManager.sendTextMessage(address, null, body, null, null)
        } else {
            smsManager.sendMultipartTextMessage(address, null, parts, null, null)
        }
        SendJournal.log("sendVault() : appel radio OK (sans accusés, sans provider)")
    }

    /**
     * PendingIntent IMMUTABLE (obligatoire depuis API 31) vers notre receiver
     * interne (exported=false) : le système ne peut pas le détourner.
     */
    private fun statusIntent(action: String, token: String, partIndex: Int): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TOKEN, token)
            putExtra(EXTRA_PART_INDEX, partIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun writeToSentProvider(address: String, body: String, subscriptionId: Int): Long? =
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_NONE)
                if (subscriptionId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
            }
            context.contentResolver
                .insert(Telephony.Sms.Sent.CONTENT_URI, values)
                ?.lastPathSegment?.toLongOrNull()
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "écriture provider Sent sans permission")
            null
        }

    companion object {
        const val ACTION_SMS_SENT = "tech.dmampro.tinda.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "tech.dmampro.tinda.SMS_DELIVERED"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_PART_INDEX = "part_index"
        /**
         * failureCode stocké quand l'appel radio lui-même échoue par
         * exception, distinct des resultCode radio (1 à 4). Un échec
         * après l'appel radio ne reçoit pas ce code : il ne marque
         * pas FAILED (voir SendFlow).
         */
        const val FAILURE_EXCEPTION = -1
        private val requestCodeCounter = AtomicInteger(0)
        private const val TAG = "SmsSender"
    }
}
