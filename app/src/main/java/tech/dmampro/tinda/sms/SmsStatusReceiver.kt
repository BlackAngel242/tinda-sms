package tech.dmampro.tinda.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.TindaLogger

/**
 * Accusés d'envoi et de livraison (L3) : chaque accusé vaut pour UN
 * segment (jeton + index). Le statut du message est recalculé par
 * agrégation des segments dans le dépôt ; ce récepteur ne décide
 * jamais seul du statut d'un message multipart.
 *
 * [resultCode] est capturé pendant [onReceive], avant tout traitement
 * asynchrone : sa valeur n'est garantie qu'à cet instant.
 *
 * Sur son Redmi Note 14 (HyperOS), le système renvoie 0 alors que le
 * SMS est réellement parti (prouvé le 18 octobre 2026) ; ailleurs, 0
 * reste non concluant. Voir [sentVerdictForResultCode].
 */
class SmsStatusReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun smsRepository(): SmsRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val rc = resultCode
        val token = intent.getStringExtra(SmsSender.EXTRA_TOKEN) ?: return
        val partIndex = intent.getIntExtra(SmsSender.EXTRA_PART_INDEX, 0)
        val pending = goAsync()
        scope.launch {
            try {
                val repository = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java,
                ).smsRepository()
                when (intent.action) {
                    SmsSender.ACTION_SMS_SENT -> {
                        val (state, failureCode) = sentVerdictForResultCode(
                            rc,
                            zeroMeansSentOnThisRom = isZeroSuccessRom(),
                        )
                        repository.recordPartSent(token, partIndex, state, failureCode)
                        SendJournal.log(
                            "accusé envoi segment $partIndex : $state (resultCode=$rc)",
                        )
                    }
                    SmsSender.ACTION_SMS_DELIVERED -> {
                        if (rc == Activity.RESULT_OK || rc == Activity.RESULT_CANCELED) {
                            repository.recordPartDelivered(token, partIndex)
                            SendJournal.log(
                                "accusé livraison segment $partIndex (resultCode=$rc)",
                            )
                        } else {
                            SendJournal.log(
                                "accusé livraison segment $partIndex : code $rc ignoré",
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                TindaLogger.e(TAG, "traitement d'un accusé", e)
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * ROM où le code 0 accompagnant un envoi réel est prouvé par le
     * terrain : fabricant Xiaomi (Redmi Note 14, HyperOS). Toute autre
     * ROM est non prouvée : 0 y reste non concluant.
     */
    private fun isZeroSuccessRom(): Boolean =
        Build.MANUFACTURER.contains("Xiaomi", ignoreCase = true)

    private companion object {
        const val TAG = "SmsStatusReceiver"
    }
}
