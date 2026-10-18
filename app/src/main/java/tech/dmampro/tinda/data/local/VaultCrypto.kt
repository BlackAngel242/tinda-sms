package tech.dmampro.tinda.data.local

import net.sqlcipher.database.SQLiteDatabase
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Chiffrement du coffre-fort (lot 4 : UI du PIN et câblage).
 *
 * Principe (DEC-004) : la clé est dérivée du PIN de l'utilisateur (PBKDF2),
 * aucun séquestre, PIN oublié = données perdues. Base SQLCipher séparée,
 * jamais mélangée au provider SMS du système (RB-01).
 */
object VaultCrypto {

    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    /** Charge les bibliothèques natives SQLCipher (à appeler une fois au démarrage). */
    fun loadLibraries(context: android.content.Context) {
        SQLiteDatabase.loadLibs(context)
    }

    fun generateSalt(): ByteArray =
        ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    /** Dérive une clé de 256 bits depuis le PIN + sel (PBKDF2-HMAC-SHA256). */
    fun deriveKey(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Convertit la clé en passphrase hexadécimale pour SQLCipher. */
    fun toPassphrase(key: ByteArray): String =
        key.joinToString("") { "%02x".format(it) }
}
