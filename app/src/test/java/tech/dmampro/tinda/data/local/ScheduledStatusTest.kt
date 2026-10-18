package tech.dmampro.tinda.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Convertisseurs des statuts de SMS programmés : l'aller-retour doit être
 * stable, car Room stocke le nom de l'enum en base (table scheduled_messages).
 */
class ScheduledStatusTest {

    private val converters = Converters()

    @Test
    fun status_roundTrip() {
        for (status in ScheduledStatus.entries) {
            val stored = converters.scheduledStatusToString(status)
            assertEquals(status, converters.stringToScheduledStatus(stored))
        }
    }

    @Test
    fun status_storedAsName() {
        assertEquals("PENDING", converters.scheduledStatusToString(ScheduledStatus.PENDING))
        assertEquals("SENT", converters.scheduledStatusToString(ScheduledStatus.SENT))
        assertEquals("CANCELLED", converters.scheduledStatusToString(ScheduledStatus.CANCELLED))
        assertEquals("FAILED", converters.scheduledStatusToString(ScheduledStatus.FAILED))
    }
}
