package tech.dmampro.tinda.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.data.local.TindaDatabase
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
        if (!repository.isDefaultSmsApp()) {
            throw SecurityException("Envoi refusé : Tinda n'est pas la default SMS app")
        }
        val token = UUID.randomUUID().toString()
        val smsManager = smsManagerFor(subscriptionId)
        val parts = smsManager.divideMessage(body)

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
            ),
        )
        repository.notifyChanged()

        try {
            // 2. Appel radio (lève une exception en cas d'échec immédiat).
            if (parts.size <= 1) {
                smsManager.sendTextMessage(
                    address, null, body,
                    statusIntent(ACTION_SMS_SENT, token, 0),
                    if (deliveryReport) statusIntent(ACTION_SMS_DELIVERED, token, 0) else null,
                )
            } else {
                val sent = ArrayList<PendingIntent>(parts.size)
                val delivered = ArrayList<PendingIntent>(parts.size)
                parts.forEachIndexed { index, _ ->
                    sent.add(statusIntent(ACTION_SMS_SENT, token, index))
                    if (deliveryReport) delivered.add(statusIntent(ACTION_SMS_DELIVERED, token, index))
                }
                smsManager.sendMultipartTextMessage(
                    address, null, parts, sent,
                    delivered.takeIf { deliveryReport },
                )
            }

            // 3. Écriture dans le provider Sent (rôle default handler).
            writeToSentProvider(address, body, subscriptionId)?.let { providerId ->
                db.messageDao().updateProviderId(rowId, providerId)
            }
        } catch (e: Exception) {
            TindaLogger.e(TAG, "exception pendant l'envoi", e)
            db.messageDao().updateStatusAndFailureByToken(
                token, MessageStatus.FAILED, FAILURE_EXCEPTION
            )
            repository.notifyChanged()
            throw e
        }
        SendResult(token, parts.size)
    }

    private fun smsManagerFor(subscriptionId: Int): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)!!.createForSubscriptionId(subscriptionId)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
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
         * failureCode stocké quand l'envoi échoue par exception dans
         * l'application (avant ou après l'appel radio), distinct des
         * resultCode radio (1 à 4).
         */
        const val FAILURE_EXCEPTION = -1
        private val requestCodeCounter = AtomicInteger(0)
        private const val TAG = "SmsSender"
    }
}
