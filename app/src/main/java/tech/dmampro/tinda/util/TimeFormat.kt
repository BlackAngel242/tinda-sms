package tech.dmampro.tinda.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Format court d'une date de message : heure aujourd'hui, "Hier" hier,
 * sinon jj/MM/aa. Pur Kotlin (hors contexte) pour rester testable.
 */
object TimeFormat {

    fun shortDate(
        epochMillis: Long,
        yesterdayLabel: String = "Hier",
        locale: Locale = Locale.getDefault(),
    ): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val timeFmt = SimpleDateFormat("HH:mm", locale)
        val dateFmt = SimpleDateFormat("dd/MM/yy", locale)
        return when {
            isSameDay(now, then) -> timeFmt.format(Date(epochMillis))
            isYesterday(now, then) -> yesterdayLabel
            else -> dateFmt.format(Date(epochMillis))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(now: Calendar, then: Calendar): Boolean {
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, then)
    }
}
