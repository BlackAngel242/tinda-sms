package tech.dmampro.tinda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Détection de codes OTP/2FA : mot-clé + suite de chiffres requis,
 * faux positifs écartés, mise en forme d'affichage.
 */
class OtpDetectionTest {

    @Test
    fun frenchOtp_detected() {
        assertEquals(
            "849201",
            OtpDetection.detectCode("Votre code de vérification à usage unique : 849201"),
        )
    }

    @Test
    fun englishOtp_detected() {
        assertEquals(
            "492018",
            OtpDetection.detectCode("Your OTP code for login is 492018. Do not share it."),
        )
    }

    @Test
    fun twoFaKeyword_detected() {
        assertEquals(
            "1234",
            OtpDetection.detectCode("2FA : utilisez le code 1234 pour valider"),
        )
    }

    @Test
    fun digitsWithoutKeyword_ignored() {
        assertNull(OtpDetection.detectCode("Rendez-vous demain à 18h30 précises"))
    }

    @Test
    fun keywordWithoutCode_ignored() {
        assertNull(OtpDetection.detectCode("Votre code de vérification arrive bientôt"))
    }

    @Test
    fun shortNumber_ignored() {
        assertNull(OtpDetection.detectCode("Code : appelez le 123 pour info"))
    }

    @Test
    fun formatCode_sixDigits() {
        assertEquals("492-018", OtpDetection.formatCode("492018"))
    }

    @Test
    fun formatCode_fourDigitsUnchanged() {
        assertEquals("1234", OtpDetection.formatCode("1234"))
    }
}
