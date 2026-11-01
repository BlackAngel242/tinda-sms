package tech.dmampro.tinda.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrations Room réelles vérifiées une à une (10 vers 11, puis
 * 11 vers 12, puis 12 vers 13).
 *
 * Les schémas exportés dans app/schemas couvrent les versions 10,
 * 11, 12 et 13 : ces tests ne prétendent donc pas valider les
 * migrations 3 vers 10. Ils exigent un appareil ou un émulateur et
 * sont exécutés par la CI.
 */
@RunWith(AndroidJUnit4::class)
class TindaMigrationInstrumentedTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TindaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate10To11PreservesScheduledMessage() {
        val databaseName = "tinda-migration-10-11"
        helper.createDatabase(databaseName, 10).apply {
            execSQL(
                "INSERT INTO scheduled_messages " +
                    "(id, address, body, scheduledAt, subscriptionId, status, createdAt) " +
                    "VALUES (7, 'TEST', 'Message programme conserve', " +
                    "123456, -1, 'PENDING', 999)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            11,
            true,
            MIGRATION_10_11,
        )

        migrated.query(
            "SELECT body, status, attemptToken, attemptStartedAt " +
                "FROM scheduled_messages WHERE id = 7",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Message programme conserve", cursor.getString(0))
            assertEquals("PENDING", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'message_parts'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("message_parts", cursor.getString(0))
        }
        migrated.close()
    }

    @Test
    fun migrate11To12AddsSimSlotAndKeepsItNullForOldMessages() {
        val databaseName = "tinda-migration-11-12"
        helper.createDatabase(databaseName, 11).apply {
            execSQL(
                "INSERT INTO messages " +
                    "(id, threadId, address, body, date, read, incoming, status, " +
                    "subscriptionId, simulated) " +
                    "VALUES (5, 3, 'TEST', 'Ancien message sans slot conserve', " +
                    "123456, 1, 0, 'SENT', 2, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            12,
            true,
            MIGRATION_11_12,
        )

        // L'ancien message survit, et son emplacement historique
        // reste NULL : la migration ne déduit rien de l'emplacement
        // actuel de sa carte.
        migrated.query(
            "SELECT body, subscriptionId, simSlot FROM messages WHERE id = 5",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ancien message sans slot conserve", cursor.getString(0))
            assertEquals(2, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
        }
        migrated.close()
    }

    @Test
    fun migrate12To13AddsDeliveredAtAndKeepsItNullForOldMessages() {
        val databaseName = "tinda-migration-12-13"
        helper.createDatabase(databaseName, 12).apply {
            execSQL(
                "INSERT INTO messages " +
                    "(id, threadId, address, body, date, read, incoming, status, " +
                    "subscriptionId, simulated, simSlot) " +
                    "VALUES (9, 4, 'TEST', 'Message livre sans heure conservee', " +
                    "123456, 1, 0, 'DELIVERED', 1, 0, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            13,
            true,
            MIGRATION_12_13,
        )

        // Le message survit avec son emplacement historique, et son
        // heure de livraison reste NULL : la migration ne fabrique pas
        // une heure que l'accusé n'a jamais établie.
        migrated.query(
            "SELECT body, status, simSlot, deliveredAt FROM messages WHERE id = 9",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Message livre sans heure conservee", cursor.getString(0))
            assertEquals("DELIVERED", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertTrue(cursor.isNull(3))
        }
        migrated.close()
    }
}
