package tech.dmampro.tinda.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony

/**
 * Diagnostic du rôle "application SMS par défaut".
 *
 * Exécute les mêmes requêtes PackageManager que le RoleController d'Android
 * pour qualifier les candidates (définition exacte dans roles.xml d'AOSP :
 * 2 receivers avec BROADCAST_SMS / BROADCAST_WAP_PUSH, 1 service avec
 * SEND_RESPOND_VIA_MESSAGE, 1 activité SENDTO).
 *
 * Si les 4 voyants sont verts ici mais que Tinda n'apparaît pas dans
 * Réglages → Applis par défaut → SMS, le blocage vient de la ROM
 * (pas du manifest). Jamais de numéro ni de contenu dans les logs.
 */
data class RoleCheck(
    val label: String,
    val ok: Boolean,
    val detail: String,
)

object RoleDiagnostic {

    private const val MMS_MIME = "application/vnd.wap.mms-message"
    private const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"
    private const val PERM_SEND_RESPOND = "android.permission.SEND_RESPOND_VIA_MESSAGE"

    fun run(context: Context): List<RoleCheck> {
        val pm = context.packageManager
        val pkg = context.packageName
        val out = mutableListOf<RoleCheck>()

        // 1. Récepteur SMS_DELIVER, permission BROADCAST_SMS.
        val smsReceivers = pm.queryBroadcastReceivers(
            Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION), 0
        )
        out += RoleCheck(
            label = "SMS_DELIVER",
            ok = smsReceivers.any {
                it.activityInfo?.packageName == pkg &&
                    it.activityInfo?.permission == Manifest.permission.BROADCAST_SMS
            },
            detail = "Récepteur SMS trouvé dans ${smsReceivers.size} candidat(s)",
        )

        // 2. Récepteur WAP_PUSH_DELIVER, permission BROADCAST_WAP_PUSH, type MMS.
        val wapIntent = Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION)
            .setDataAndType(Uri.EMPTY, MMS_MIME)
        val wapReceivers = pm.queryBroadcastReceivers(wapIntent, 0)
        out += RoleCheck(
            label = "WAP_PUSH_DELIVER",
            ok = wapReceivers.any {
                it.activityInfo?.packageName == pkg &&
                    it.activityInfo?.permission == Manifest.permission.BROADCAST_WAP_PUSH
            },
            detail = "Récepteur MMS trouvé dans ${wapReceivers.size} candidat(s)",
        )

        // 3. Service RESPOND_VIA_MESSAGE, permission SEND_RESPOND_VIA_MESSAGE.
        val respondIntent = Intent(ACTION_RESPOND_VIA_MESSAGE, Uri.fromParts("smsto", "", null))
        val services = pm.queryIntentServices(respondIntent, 0)
        out += RoleCheck(
            label = "RESPOND_VIA_MESSAGE",
            ok = services.any {
                it.serviceInfo?.packageName == pkg &&
                    it.serviceInfo?.permission == PERM_SEND_RESPOND
            },
            detail = "Service trouvé dans ${services.size} candidat(s)",
        )

        // 4. Activité SENDTO.
        val sendtoIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", "", null))
        val activities = pm.queryIntentActivities(sendtoIntent, 0)
        out += RoleCheck(
            label = "SENDTO",
            ok = activities.any { it.activityInfo?.packageName == pkg },
            detail = "Activité trouvée dans ${activities.size} candidat(s)",
        )

        // 5. Statut actuel du rôle (vérification robuste : isRoleHeld d'abord,
        //    car getDefaultSmsPackage() renvoie null sur certaines ROM).
        val held = isDefaultSmsApp(context)
        val currentDefault = Telephony.Sms.getDefaultSmsPackage(context)
        out += RoleCheck(
            label = "Rôle détenu",
            ok = held,
            detail = if (held) {
                "Tinda est l'application SMS par défaut"
            } else {
                "Application par défaut vue par le système : $currentDefault"
            },
        )

        return out
    }
}
