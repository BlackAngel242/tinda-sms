package tech.dmampro.tinda.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Format court d'une date de message : heure aujourd'hui, "Hier HH:mm" hier,
 * sinon jj/MM/aa HH:mm. Pur Kotlin (hors contexte) pour rester testable.
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
        val dateTimeFmt = SimpleDateFormat("dd/MM/yy HH:mm", locale)
        return when {
            isSameDay(now, then) -> timeFmt.format(Date(epochMillis))
            isYesterday(now, then) -> "$yesterdayLabel ${timeFmt.format(Date(epochMillis))}"
            else -> dateTimeFmt.format(Date(epochMillis))
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
