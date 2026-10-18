package tech.dmampro.tinda.util

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.provider.Telephony

/**
 * Vrai si Tinda détient le rôle "application SMS par défaut".
 *
 * Sur certaines ROM (constaté sur Tecno/HiOS), Telephony.Sms.getDefaultSmsPackage()
 * renvoie null alors que le rôle est bien détenu (les réglages système
 * l'affichent). On interroge donc d'abord directement le RoleManager
 * (isRoleHeld, API 29+), plus fiable, avec l'ancienne API en repli.
 */
fun isDefaultSmsApp(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) return true
    }
    return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
}
