package tech.dmampro.tinda.sms

import tech.dmampro.tinda.util.OtpDetection

/**
 * Garde-fou du transfert automatique (F11, point P0-8 du brief).
 *
 * Un message qui contient un code de vérification détecté n'est
 * transféré que si la règle l'autorise explicitement. La détection
 * repose sur [OtpDetection] : elle n'est pas exhaustive (faux négatifs
 * possibles) et peut produire des faux positifs ; dans le doute, le
 * transfert est suspendu et journalisé, jamais silencieux.
 */
object ForwardGuard {

    /** Vrai si ce message peut être transféré par une règle. */
    fun shouldForward(body: String, allowVerificationCodes: Boolean): Boolean =
        allowVerificationCodes || OtpDetection.detectCode(body) == null

    /** Vrai si le message contient un code de vérification détecté. */
    fun containsVerificationCode(body: String): Boolean =
        OtpDetection.detectCode(body) != null
}
