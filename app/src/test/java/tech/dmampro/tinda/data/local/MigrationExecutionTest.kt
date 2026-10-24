package tech.dmampro.tinda.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exécution réelle du code des migrations en JVM (revue PR nº 2,
 * point 5) : chaque migration de ALL_MIGRATIONS est invoquée contre
 * une base factice qui enregistre le SQL émis, et la cohérence des
 * schémas exportés 10 et 11 est vérifiée.
 *
 * Limite honnête : ce test exécute le code Kotlin des migrations et
 * contrôle le SQL produit, il n'exécute pas SQLite et ne valide pas
 * le schéma résultant contre une vraie base. Cette validation exige
 * MigrationTestHelper (androidx.room:room-testing) et un contexte
 * Android (androidx.test), absents du cache Gradle hors ligne, et un
 * appareil ou émulateur, absent de cet environnement. Les schémas
 * exportés ne couvrent que les versions 10 et 11 : aucune migration
 * instrumentée depuis la version 3 ne peut donc être construite ici.
 * Voir docs/validation/revue-codex-pr2-correctifs.md.
 */
class MigrationExecutionTest {

    /** Base factice : enregistre les execSQL, réponses neutres sinon. */
    private class RecordingDb(val sql: MutableList<String>) : InvocationHandler {
        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<Any?>?): Any? {
            if (method.name == "execSQL" && !args.isNullOrEmpty()) {
                sql.add(args[0] as String)
                return null
            }
            return when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                else -> null
            }
        }
    }

    private fun runAllMigrations(): List<String> {
        val sql = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            RecordingDb(sql),
        ) as SupportSQLiteDatabase
        ALL_MIGRATIONS.sortedBy { it.startVersion }.forEach { it.migrate(db) }
        return sql
    }

    @Test
    fun `chaque migration emet son sql et la chaine complete en emet 17`() {
        val sql = runAllMigrations()
        assertEquals(17, sql.size)
        assertTrue(sql.any { it.contains("ALTER TABLE messages ADD COLUMN simSlot") })
        assertTrue(sql.any { it.contains("ALTER TABLE messages ADD COLUMN failureCode") })
        assertTrue(sql.any { it.contains("CREATE TABLE IF NOT EXISTS secret_rules") })
        assertTrue(sql.any { it.contains("CREATE TABLE IF NOT EXISTS scheduled_messages") })
        assertTrue(sql.any { it.contains("CREATE TABLE IF NOT EXISTS auto_reply_rules") })
        assertTrue(sql.any { it.contains("CREATE TABLE IF NOT EXISTS forward_rules") })
        assertTrue(sql.any { it.contains("CREATE TABLE IF NOT EXISTS auto_delete_rules") })
        assertTrue(sql.any { it.contains("CREATE TABLE IF NOT EXISTS sim_assignments") })
    }

    @Test
    fun `la migration 10 vers 11 cree le suivi de segments et les tentatives`() {
        val sql = runAllMigrations()
        val parts = sql.firstOrNull { it.contains("message_parts") }
        assertTrue(parts != null && parts.contains("PRIMARY KEY(token, partIndex)"))
        assertTrue(sql.any { it.contains("ALTER TABLE scheduled_messages ADD COLUMN attemptToken") })
        assertTrue(sql.any { it.contains("ALTER TABLE scheduled_messages ADD COLUMN attemptStartedAt") })
        assertTrue(
            sql.any {
                it.contains("ALTER TABLE forward_rules ADD COLUMN allowVerificationCodes") &&
                    it.contains("DEFAULT 0")
            },
        )
        assertTrue(
            sql.any {
                it.contains("ALTER TABLE forward_log ADD COLUMN heldVerificationCode") &&
                    it.contains("DEFAULT 0")
            },
        )
    }

    @Test
    fun `les schemas exportes 10 et 11 refletent la migration 10 vers 11`() {
        val dir = locateSchemasDir()
        val v10 = File(dir, "10.json").readText()
        val v11 = File(dir, "11.json").readText()
        assertFalse(v10.contains("\"tableName\": \"message_parts\""))
        assertFalse(v10.contains("attemptToken"))
        assertTrue(v11.contains("\"tableName\": \"message_parts\""))
        assertTrue(v11.contains("attemptToken"))
        assertTrue(v11.contains("attemptStartedAt"))
        assertTrue(v11.contains("allowVerificationCodes"))
        assertTrue(v11.contains("heldVerificationCode"))
    }

    private fun locateSchemasDir(): File {
        val relative = "schemas/tech.dmampro.tinda.data.local.TindaDatabase"
        var dir = File(System.getProperty("user.dir")).absoluteFile
        val candidates = generateSequence(dir) { it.parentFile }
            .take(5)
            .flatMap { base -> listOf(File(base, relative), File(base, "app/$relative")) }
            .toList()
        return candidates.firstOrNull { File(it, "11.json").isFile }
            ?: throw AssertionError(
                "Schémas exportés introuvables depuis ${System.getProperty("user.dir")}",
            )
    }
}
