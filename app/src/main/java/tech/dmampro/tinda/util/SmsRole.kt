package tech.dmampro.tinda.util

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.provider.Telephony

/**
 * Vrai si Tinda détient le rôle "application SMS par défaut".
 *
 * Sur certaines ROM (constaté sur Tecno/HiOS), les API 1 et 2 mentent :
 * Telephony.Sms.getDefaultSmsPackage() renvoie null et
 * RoleManager.isRoleHeld() renvoie false alors que le rôle est bien détenu
 * (Tinda reçoit les SMS_DELIVER et les réglages affichent "Tinda SMS").
 * On lit donc aussi le réglage système brut, celui-là même qu'affiche
 * l'écran "Applications par défaut". Dès qu'une source dit oui, c'est oui.
 */
fun isDefaultSmsApp(context: Context): Boolean {
    // 1. RoleManager direct (API 29+) : source de vérité en AOSP.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) return true
    }
    // 2. API Téléphonie classique.
    if (Telephony.Sms.getDefaultSmsPackage(context) == context.packageName) return true
    // 3. Réglage système brut (lecture autorisée sans permission).
    //    Constante Settings.Secure.SMS_DEFAULT_APPLICATION cachée du SDK
    //    public : on utilise sa valeur littérale ("sms_default_application").
    return try {
        Settings.Secure.getString(context.contentResolver, "sms_default_application") ==
            context.packageName
    } catch (e: Exception) {
        false
    }
}
