package tech.dmampro.tinda.sms

import java.util.Calendar
import tech.dmampro.tinda.data.local.AutoReplyRule

/**
 * Logique pure du répondeur automatique (F10), sans dépendance Android :
 * fenêtre horaire, jours actifs, adresse répondable.
 */
fun matchesWindow(rule: AutoReplyRule, now: Calendar = Calendar.getInstance()): Boolean {
    if (rule.startHour >= 0 && rule.endHour >= 0 && rule.startHour != rule.endHour) {
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val inWindow = if (rule.startHour < rule.endHour) {
            hour in rule.startHour until rule.endHour
        } else {
            hour >= rule.startHour || hour < rule.endHour
        }
        if (!inWindow) return false
    }
    val dayBit = 1 shl (now.get(Calendar.DAY_OF_WEEK) - 1)
    return rule.days and dayBit != 0
}

/** Uniquement les numéros longs composables par la radio. */
fun isReplyableAddress(address: String): Boolean {
    val digits = address.filter { it.isDigit() }
    val others = address.filter { !it.isDigit() && it != '+' && it != ' ' }
    return others.isEmpty() && digits.length > 6
}
