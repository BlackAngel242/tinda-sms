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
 * Migration Room réelle de la version 10 vers la version 11.
 *
 * Les schémas 10 et 11 sont les seuls exportés dans app/schemas :
 * ce test ne prétend donc pas valider les migrations 3 vers 10.
 * Il exige un appareil ou un émulateur et est exécuté par la CI.
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
}
