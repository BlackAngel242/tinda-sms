package tech.dmampro.tinda.util

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Journal d'événements : enregistrement en mémoire et dans un fichier,
 * sans dépendance Android.
 */
class EventLogTest {

    @Test
    fun log_keepsEventsInMemory() {
        val dir = Files.createTempDirectory("eventlog").toFile()
        EventLog.init(dir)
        EventLog.log("tap.test_button", "detail_technique")
        val events = EventLog.snapshot()
        assertTrue(events.any { it.name == "tap.test_button" && it.detail == "detail_technique" })
    }

    @Test
    fun log_writesToFileWithoutPii() {
        val dir = Files.createTempDirectory("eventlog2").toFile()
        EventLog.init(dir)
        EventLog.log("screen", "settings")
        val file = File(dir, "tinda_events.log")
        assertTrue(file.exists())
        val content = file.readText()
        assertTrue(content.contains("screen"))
    }

    @Test
    fun log_truncatesLongDetail() {
        val dir = Files.createTempDirectory("eventlog3").toFile()
        EventLog.init(dir)
        EventLog.log("test", "x".repeat(500))
        val event = EventLog.snapshot().last { it.name == "test" }
        assertEquals(160, event.detail.length)
    }
}
