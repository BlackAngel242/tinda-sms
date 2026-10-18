package tech.dmampro.tinda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.dmampro.tinda.util.SmsEncoding.Encoding

/**
 * Cas limites de l'encodage SMS : GSM-7 contre UCS-2, table d'extension,
 * découpage en segments. Aucune dépendance Android.
 */
class SmsEncodingTest {

    @Test
    fun emptyText_zeroSegments() {
        val info = SmsEncoding.segmentInfo("")
        assertEquals(Encoding.GSM_7, info.encoding)
        assertEquals(0, info.parts)
        assertEquals(160, info.remaining)
    }

    @Test
    fun shortAsciiText_oneSegment() {
        val info = SmsEncoding.segmentInfo("Bonjour")
        assertEquals(Encoding.GSM_7, info.encoding)
        assertEquals(1, info.parts)
        assertEquals(153, info.remaining)
    }

    @Test
    fun exactly160Chars_oneSegmentFull() {
        val info = SmsEncoding.segmentInfo("a".repeat(160))
        assertEquals(1, info.parts)
        assertEquals(0, info.remaining)
    }

    @Test
    fun one161Chars_twoSegments() {
        val info = SmsEncoding.segmentInfo("a".repeat(161))
        assertEquals(2, info.parts)
        assertEquals(2 * 153 - 161, info.remaining)
    }

    @Test
    fun extensionTableChar_countsDouble() {
        // "€" est dans la table d'extension : 2 septets.
        assertEquals(2, SmsEncoding.septetCount("€"))
        val info = SmsEncoding.segmentInfo("€".repeat(80))
        assertEquals(Encoding.GSM_7, info.encoding)
        assertEquals(1, info.parts)
        assertEquals(0, info.remaining)
    }

    @Test
    fun emoji_forcesUcs2() {
        assertNull(SmsEncoding.septetCount("Salut \uD83D\uDE0A"))
        val info = SmsEncoding.segmentInfo("Salut \uD83D\uDE0A")
        assertEquals(Encoding.UCS_2, info.encoding)
        assertEquals(1, info.parts)
    }

    @Test
    fun ucs2_71Chars_twoSegments() {
        // 71 unités UTF-16 hors GSM-7 ("中") : 2 segments de 67.
        val info = SmsEncoding.segmentInfo("中".repeat(71))
        assertEquals(Encoding.UCS_2, info.encoding)
        assertEquals(2, info.parts)
        assertEquals(2 * 67 - 71, info.remaining)
    }

    @Test
    fun frenchAccents_stayGsm7() {
        // Seuls des accents présents en GSM-7 (É è é à Ç €) : le "ç"
        // minuscule et le "û" n'existent pas dans la table.
        val info = SmsEncoding.segmentInfo("Élève à l'école : Ça coute 5€.")
        assertEquals(Encoding.GSM_7, info.encoding)
        assertEquals(1, info.parts)
    }

    @Test
    fun unsupportedAccents_forceUcs2() {
        // ë, û, ç minuscule : absents du GSM-7, bascule réelle en UCS-2.
        val info = SmsEncoding.segmentInfo("Noël : ça coûte cher.")
        assertEquals(Encoding.UCS_2, info.encoding)
        assertEquals(1, info.parts)
    }
}
