package tech.dmampro.tinda.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.dmampro.tinda.sms.PartSentState

/**
 * Tests Room réels des transitions utilisées par les accusés.
 *
 * Ces tests exigent un appareil ou un émulateur Android et les
 * artefacts androidx.test / room-testing : ils sont exécutés par la
 * CI GitHub, pas par l'environnement local hors ligne.
 */
@RunWith(AndroidJUnit4::class)
class ScheduledAndMessageDaoInstrumentedTest {

    private lateinit var db: TindaDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TindaDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    private suspend fun insertScheduled(
        status: ScheduledStatus = ScheduledStatus.PENDING,
    ): Long = db.scheduledMessageDao().insert(
        ScheduledMessage(
            address = "TEST",
            body = "Message de test",
            scheduledAt = 10_000L,
            subscriptionId = -1,
            status = status,
            createdAt = 500L,
        ),
    )

    @Test
    fun lateReceiptResolvesAmbiguousAttemptWithSameToken() = runBlocking {
        val dao = db.scheduledMessageDao()
        val id = insertScheduled()
        assertEquals(1, dao.claim(id, startedAt = 1_000L))
        dao.attachToken(id, "attempt-token")
        assertEquals(1, dao.markAmbiguousIfStillSending(id, 1_000L, "attempt-token"))

        assertEquals(1, dao.resolveByToken("attempt-token", ScheduledStatus.SENT))
        assertEquals(ScheduledStatus.SENT, dao.getById(id)?.status)
    }

    @Test
    fun replacedAttemptCannotBeResolvedByOldToken() = runBlocking {
        val dao = db.scheduledMessageDao()
        val id = insertScheduled()
        assertEquals(1, dao.claim(id, startedAt = 1_000L))
        dao.attachToken(id, "old-token")
        assertEquals(1, dao.markAmbiguousIfStillSending(id, 1_000L, "old-token"))
        assertEquals(1, dao.requeueAmbiguous(id, at = 2_000L))
        assertEquals(1, dao.claim(id, startedAt = 3_000L))
        dao.attachToken(id, "new-token")

        assertEquals(0, dao.resolveByToken("old-token", ScheduledStatus.SENT))
        assertEquals(ScheduledStatus.SENDING, dao.getById(id)?.status)
        assertEquals(1, dao.resolveByToken("new-token", ScheduledStatus.SENT))
        assertEquals(ScheduledStatus.SENT, dao.getById(id)?.status)
    }

    @Test
    fun ambiguousSweepDoesNotOverwriteReceiptThatArrivedAfterStaleRead() = runBlocking {
        val dao = db.scheduledMessageDao()
        val id = insertScheduled()
        assertEquals(1, dao.claim(id, startedAt = 4_000L))
        dao.attachToken(id, "raced-token")
        val staleStartedAt = dao.getById(id)?.attemptStartedAt
        val staleToken = dao.getById(id)?.attemptToken

        // Entrelacement de la course signalée : le verdict arrive
        // après la lecture des lignes périmées, avant la transition.
        assertEquals(1, dao.resolveByToken("raced-token", ScheduledStatus.SENT))
        assertEquals(
            0,
            dao.markAmbiguousIfStillSending(id, staleStartedAt, staleToken),
        )
        assertEquals(ScheduledStatus.SENT, dao.getById(id)?.status)

        // Une identité de tentative différente ne transitionne pas non plus.
        val otherId = insertScheduled()
        assertEquals(1, dao.claim(otherId, startedAt = 5_000L))
        dao.attachToken(otherId, "other-token")
        assertEquals(
            0,
            dao.markAmbiguousIfStillSending(otherId, 4_999L, "other-token"),
        )
        assertEquals(ScheduledStatus.SENDING, dao.getById(otherId)?.status)
    }

    @Test
    fun messageTerminalStatesCannotRegressAtDaoLevel() = runBlocking {
        val dao = db.messageDao()
        dao.insert(
            Message(
                threadId = 1L,
                address = "TEST",
                body = "Message de test",
                date = 1_000L,
                read = true,
                incoming = false,
                status = MessageStatus.SENDING,
                token = "message-token",
            ),
        )

        assertEquals(
            1,
            dao.updateStatusAndFailureByTokenIfNotTerminal(
                "message-token",
                MessageStatus.DELIVERED,
                null,
            ),
        )
        assertEquals(
            0,
            dao.updateStatusAndFailureByTokenIfNotTerminal(
                "message-token",
                MessageStatus.SENT,
                null,
            ),
        )
        assertEquals(MessageStatus.DELIVERED, dao.findByToken("message-token")?.status)
        assertEquals(
            1,
            dao.updateStatusAndFailureByTokenIfNotTerminal(
                "message-token",
                MessageStatus.DELIVERED,
                null,
            ),
        )
    }

    @Test
    fun messagePartsCanBeInsertedAndUpdated() = runBlocking {
        val dao = db.messagePartDao()
        dao.insertAll(
            listOf(
                MessagePart(token = "multipart-token", partIndex = 0),
                MessagePart(token = "multipart-token", partIndex = 1),
            ),
        )

        assertEquals(
            1,
            dao.updateSent(
                "multipart-token",
                partIndex = 1,
                state = PartSentState.FAILED,
                failureCode = 2,
            ),
        )
        val parts = dao.getByToken("multipart-token")
        assertEquals(PartSentState.PENDING, parts[0].sentState)
        assertEquals(PartSentState.FAILED, parts[1].sentState)
        assertEquals(2, parts[1].failureCode)
    }
}
