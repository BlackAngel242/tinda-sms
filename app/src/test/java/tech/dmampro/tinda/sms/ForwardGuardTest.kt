package tech.dmampro.tinda.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fou OTP du transfert automatique (P0-8) : sans autorisation
 * explicite de la règle, un code de vérification n'est pas transféré.
 */
class ForwardGuardTest {

    private val otpBody = "Votre code de vérification à usage unique : 849201"
    private val normalBody = "La réunion est déplacée à demain 10h, merci."

    @Test
    fun `un code de verification est suspendu sans autorisation explicite`() {
        assertFalse(ForwardGuard.shouldForward(otpBody, allowVerificationCodes = false))
        assertTrue(ForwardGuard.containsVerificationCode(otpBody))
    }

    @Test
    fun `un code de verification est transfere si la regle l autorise`() {
        assertTrue(ForwardGuard.shouldForward(otpBody, allowVerificationCodes = true))
    }

    @Test
    fun `un message ordinaire est toujours transfere`() {
        assertTrue(ForwardGuard.shouldForward(normalBody, allowVerificationCodes = false))
        assertTrue(ForwardGuard.shouldForward(normalBody, allowVerificationCodes = true))
        assertFalse(ForwardGuard.containsVerificationCode(normalBody))
    }
}
