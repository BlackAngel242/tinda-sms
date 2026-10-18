package tech.dmampro.tinda.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import tech.dmampro.tinda.data.local.SecretRule
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.data.local.VaultCrypto
import tech.dmampro.tinda.data.local.VaultDatabase
import tech.dmampro.tinda.data.local.VaultMessage
import tech.dmampro.tinda.data.local.VaultThread

/**
 * Coffre secret : stockage chiffré (SQLCipher), PIN + biométrie.
 *
 * Modèle de clés :
 * - `dbKey` : 32 octets aléatoires, la vraie clé du chiffrement SQLCipher.
 *   Jamais stockée en clair : enveloppée (AES/GCM) par une clé du Keystore
 *   Android liée à l'appareil (TEE). Le fichier de base est donc illisible
 *   hors de l'appareil, même extrait.
 * - Le PIN et la biométrie sont des contrôles d'accès pour l'interface :
 *   le PIN est vérifié contre un vérifieur (SHA-256(sel + PIN)), la
 *   biométrie via BiometricPrompt. Sans eux, pas d'ouverture pour l'UI.
 * - Le récepteur SMS peut ouvrir la base en tâche de fond via la clé
 *   appareil (le téléphone est déverrouillé quand un SMS arrive), ce qui
 *   permet le routage de la liste jaune même coffre "verrouillé".
 *
 * PIN oublié = données perdues (DEC-004) : le vérifieur est irréversible
 * et la clé n'existe que dans le TEE.
 *
 * Règle d'or (AGENTS.md) : aucun contenu SMS, numéro ou PIN dans les logs.
 */
@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainDb: TindaDatabase,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var db: VaultDatabase? = null

    /** Vrai si un PIN a été configuré (coffre initialisé). */
    fun isSetup(): Boolean = prefs.contains(KEY_VERIFIER)

    /** Vrai si la base chiffrée est ouverte en mémoire. */
    fun isUnlocked(): Boolean = db != null

    /** Verrouille : oublie la base ouverte (la clé ne survit pas en mémoire). */
    fun lock() {
        try {
            db?.close()
        } catch (_: Exception) {
        }
        db = null
    }

    /**
     * Initialise le coffre avec un PIN (4 à 12 chiffres). Génère la clé
     * de base et l'enveloppe avec la clé appareil du Keystore.
     */
    fun setupPin(pin: String) {
        require(pin.length in 4..12 && pin.all { it.isDigit() }) {
            "PIN invalide"
        }
        VaultCrypto.loadLibraries(context)
        val salt = VaultCrypto.generateSalt()
        val dbKey = randomBytes(32)
        val deviceKey = getOrCreateDeviceKey()
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey)
        val ct = cipher.doFinal(dbKey)
        prefs.edit {
            putString(KEY_SALT, b64(salt))
            putString(KEY_VERIFIER, b64(pinVerifier(pin, salt)))
            putString(KEY_IV, b64(cipher.iv))
            putString(KEY_CT, b64(ct))
        }
        openDb(dbKey)
        wipe(dbKey)
    }

    /** Vérifie le PIN sans ouvrir la base. */
    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val expectB64 = prefs.getString(KEY_VERIFIER, null) ?: return false
        val salt = unb64(saltB64)
        return MessageDigest.isEqual(pinVerifier(pin, salt), unb64(expectB64))
    }

    /** Déverrouille l'UI par PIN. Faux si le PIN est incorrect. */
    fun unlockWithPin(pin: String): Boolean {
        if (!verifyPin(pin)) return false
        return openWithDeviceKey()
    }

    /** Déverrouille l'UI après un succès biométrique. */
    fun unlockWithBiometric(): Boolean = openWithDeviceKey()

    /**
     * Ouvre la base en tâche de fond (récepteur SMS) : sans contrôle
     * d'accès utilisateur, via la clé appareil. Ne fait rien si le
     * coffre n'est pas initialisé.
     */
    fun ensureOpenForBackground(): Boolean {
        if (db != null) return true
        if (!isSetup()) return false
        return openWithDeviceKey()
    }

    /** La biométrie est proposée si le capteur est disponible et configuré. */
    fun isBiometricAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        if (!isSetup()) return false
        val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            ?: return false
        val r = bm.canAuthenticate(
            android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG,
        )
        return r == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
    }

    // ------------------------------------------------------------------
    // Données du coffre (base ouverte requise)
    // ------------------------------------------------------------------

    private fun requireDb(): VaultDatabase =
        db ?: throw IllegalStateException("Coffre verrouillé")

    fun observeThreads(): Flow<List<VaultThread>> = requireDb().vaultMessageDao().observeThreads()

    fun observeForAddress(address: String): Flow<List<VaultMessage>> =
        requireDb().vaultMessageDao().observeForAddress(address)

    suspend fun storeIncoming(address: String, body: String, date: Long) {
        requireDb().vaultMessageDao().insert(
            VaultMessage(address = address, body = body, date = date, incoming = true),
        )
    }

    suspend fun storeOutgoing(address: String, body: String, date: Long) {
        requireDb().vaultMessageDao().insert(
            VaultMessage(address = address, body = body, date = date, incoming = false, read = true),
        )
    }

    suspend fun markAddressRead(address: String) {
        requireDb().vaultMessageDao().markAddressRead(address)
    }

    suspend fun deleteAddress(address: String) {
        requireDb().vaultMessageDao().deleteAddress(address)
    }

    // Règles de la liste jaune : base principale (routage possible coffre
    // verrouillé), jamais affichées hors du coffre.
    fun observeRules(): Flow<List<SecretRule>> = mainDb.secretRuleDao().observeAll()

    suspend fun addNumberRule(number: String) {
        val norm = number.filter { it.isDigit() || it == '+' }
        if (norm.length >= 4) {
            mainDb.secretRuleDao().insert(
                SecretRule(type = RULE_NUMBER, value = norm, addedAt = System.currentTimeMillis()),
            )
        }
    }

    suspend fun addKeywordRule(keyword: String) {
        val k = keyword.trim().lowercase()
        if (k.length >= 2) {
            mainDb.secretRuleDao().insert(
                SecretRule(type = RULE_KEYWORD, value = k, addedAt = System.currentTimeMillis()),
            )
        }
    }

    suspend fun deleteRule(id: Long) {
        mainDb.secretRuleDao().delete(id)
    }

    /**
     * Vrai si ce message entrant doit aller au coffre : numéro de la
     * liste jaune (comparaison sur chiffres) ou mot-clé dans le contenu.
     * Fonctionne coffre verrouillé (règles en base principale).
     */
    suspend fun matchesSecretRule(address: String, body: String): Boolean {
        if (!isSetup()) return false
        val rules = mainDb.secretRuleDao().getAll()
        if (rules.isEmpty()) return false
        val normAddr = address.filter { it.isDigit() || it == '+' }
        val lowerBody = body.lowercase()
        return rules.any { rule ->
            when (rule.type) {
                RULE_NUMBER -> normAddr.endsWith(rule.value) || rule.value.endsWith(normAddr)
                RULE_KEYWORD -> lowerBody.contains(rule.value)
                else -> false
            }
        }
    }

    // ------------------------------------------------------------------
    // Phrase de reconnaissance (notification déguisée), configurable.
    // ------------------------------------------------------------------

    fun getDisguisePhrase(): String =
        prefs.getString(KEY_DISGUISE, DEFAULT_DISGUISE) ?: DEFAULT_DISGUISE

    fun setDisguisePhrase(phrase: String) {
        val p = phrase.trim().ifEmpty { DEFAULT_DISGUISE }
        prefs.edit { putString(KEY_DISGUISE, p.take(80)) }
    }

    // ------------------------------------------------------------------
    // Internes
    // ------------------------------------------------------------------

    private fun openDb(dbKey: ByteArray) {
        val passphrase = VaultCrypto.toPassphrase(dbKey)
        val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
        db = Room.databaseBuilder(context, VaultDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .build()
    }

    /** Ouvre la base via la clé appareil (après contrôle d'accès ou en fond). */
    private fun openWithDeviceKey(): Boolean {
        return try {
            VaultCrypto.loadLibraries(context)
            val deviceKey = getDeviceKey() ?: return false
            val cipher = Cipher.getInstance(AES_GCM)
            val iv = unb64(prefs.getString(KEY_IV, null) ?: return false)
            val ct = unb64(prefs.getString(KEY_CT, null) ?: return false)
            cipher.init(Cipher.DECRYPT_MODE, deviceKey, GCMParameterSpec(128, iv))
            val dbKey = cipher.doFinal(ct)
            openDb(dbKey)
            wipe(dbKey)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun pinVerifier(pin: String, salt: ByteArray): ByteArray {
        val d = MessageDigest.getInstance("SHA-256")
        d.update(salt)
        d.update(pin.toByteArray(Charsets.UTF_8))
        return d.digest()
    }

    /** Clé appareil du Keystore : sans biométrie requise, liée au TEE. */
    private fun getOrCreateDeviceKey(): SecretKey {
        getDeviceKey()?.let { return it }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            DEVICE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setUserAuthenticationRequired(false)
            setRandomizedEncryptionRequired(true)
        }.build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun getDeviceKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            ks.getKey(DEVICE_ALIAS, null) as? SecretKey
        } catch (_: Exception) {
            null
        }
    }

    private fun randomBytes(n: Int): ByteArray =
        ByteArray(n).also { SecureRandom().nextBytes(it) }

    private fun wipe(b: ByteArray) {
        b.fill(0)
    }

    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)

    private fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    companion object {
        private const val PREFS = "vault_prefs"
        private const val DB_NAME = "tinda_vault.db"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEVICE_ALIAS = "tinda_vault_device"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "pin_verifier"
        private const val KEY_IV = "key_iv"
        private const val KEY_CT = "key_ct"
        private const val KEY_DISGUISE = "disguise_phrase"
        const val RULE_NUMBER = "NUMBER"
        const val RULE_KEYWORD = "KEYWORD"
        const val DEFAULT_DISGUISE = "Rappel : pense à boire de l'eau"
    }
}
