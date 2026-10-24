package tech.dmampro.tinda.sms

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
 * Le code 0 reste non concluant tant qu'une empreinte Build exacte
 * n'a pas séparément prouvé sa signification à l'émission et à la
 * livraison. La seule empreinte capturée à ce jour a ses deux
 * drapeaux à faux. Voir [RomPolicy] et [sentVerdictForResultCode].
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
                val fingerprint = BuildFingerprint(
                    manufacturer = Build.MANUFACTURER,
                    brand = Build.BRAND,
                    model = Build.MODEL,
                    display = Build.DISPLAY,
                    versionIncremental = Build.VERSION.INCREMENTAL,
                )
                val provenDevice = provenZeroResultDeviceFor(fingerprint)
                when (intent.action) {
                    SmsSender.ACTION_SMS_SENT -> {
                        val (state, failureCode) = sentVerdictForResultCode(
                            rc,
                            zeroProvesSentForExactDevice =
                                provenDevice?.zeroProvesSent == true,
                        )
                        repository.recordPartSent(token, partIndex, state, failureCode)
                        SendJournal.log(
                            "accusé envoi segment $partIndex : $state (resultCode=$rc)",
                        )
                    }
                    SmsSender.ACTION_SMS_DELIVERED -> {
                        // Politique distincte de l'émission : le code 0
                        // ne vaut livraison que si ce drapeau précis a
                        // été prouvé pour cette empreinte exacte.
                        if (
                            isDeliveryConfirmed(
                                rc,
                                provenDevice?.zeroProvesDelivered == true,
                            )
                        ) {
                            repository.recordPartDelivered(token, partIndex)
                            SendJournal.log(
                                "accusé livraison segment $partIndex (resultCode=$rc)",
                            )
                        } else {
                            SendJournal.log(
                                "accusé livraison segment $partIndex : " +
                                    "code $rc non confirmé, livraison non enregistrée",
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

    private companion object {
        const val TAG = "SmsStatusReceiver"
    }
}
