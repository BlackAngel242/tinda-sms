package tech.dmampro.tinda.sms

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.dmampro.tinda.data.local.AutoReplyRule

/**
 * Logique pure du répondeur (F10) : fenêtre horaire, jours, adresse
 * répondable. Aucune dépendance Android.
 */
class AutoReplyMatchingTest {

    private fun calendarAt(dayOfWeek: Int, hour: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
        }

    private fun rule(
        startHour: Int = -1,
        endHour: Int = -1,
        days: Int = 127,
    ) = AutoReplyRule(name = "test", message = "réponse", startHour = startHour, endHour = endHour, days = days)

    @Test
    fun alwaysActive_matchesAnytime() {
        assertTrue(matchesWindow(rule(), calendarAt(Calendar.MONDAY, 3)))
        assertTrue(matchesWindow(rule(), calendarAt(Calendar.SUNDAY, 23)))
    }

    @Test
    fun simpleWindow_respectsBounds() {
        val night = rule(startHour = 22, endHour = 7)
        assertTrue(matchesWindow(night, calendarAt(Calendar.MONDAY, 23)))
        assertTrue(matchesWindow(night, calendarAt(Calendar.TUESDAY, 2)))
        assertFalse(matchesWindow(night, calendarAt(Calendar.MONDAY, 12)))
        assertFalse(matchesWindow(night, calendarAt(Calendar.MONDAY, 7)))
    }

    @Test
    fun dayWindow_matchesOvernight() {
        val night = rule(startHour = 22, endHour = 7)
        assertTrue(matchesWindow(night, calendarAt(Calendar.SATURDAY, 6)))
        assertFalse(matchesWindow(night, calendarAt(Calendar.SATURDAY, 8)))
    }

    @Test
    fun dayMask_filtersDays() {
        // Lundi uniquement : bit (MONDAY=2) -> 1 shl 1 = 2.
        val mondayOnly = rule(days = 1 shl (Calendar.MONDAY - 1))
        assertTrue(matchesWindow(mondayOnly, calendarAt(Calendar.MONDAY, 10)))
        assertFalse(matchesWindow(mondayOnly, calendarAt(Calendar.TUESDAY, 10)))
        assertFalse(matchesWindow(mondayOnly, calendarAt(Calendar.SUNDAY, 10)))
    }

    @Test
    fun replyableAddress_acceptsLongNumbers() {
        assertTrue(isReplyableAddress("+242068123456"))
        assertTrue(isReplyableAddress("068 123 456"))
        assertTrue(isReplyableAddress("242068123456"))
    }

    @Test
    fun replyableAddress_rejectsShortcodesAndAlphanumeric() {
        assertFalse(isReplyableAddress("1234")) // numéro court
        assertFalse(isReplyableAddress("123456")) // 6 chiffres, trop court
        assertFalse(isReplyableAddress("MTN")) // alphanumérique
        assertFalse(isReplyableAddress("+242-MTN")) // lettres
        assertFalse(isReplyableAddress("")) // vide
    }
}
