package tech.dmampro.tinda.sms

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.dmampro.tinda.data.local.ScheduledMessage
import tech.dmampro.tinda.data.local.ScheduledStatus

/**
 * Entrelacements du worker de SMS programmés, rejoués sur JVM.
 *
 * Le faux magasin applique les mêmes conditions que le DAO Room
 * (transitions seulement depuis l'état et l'identité de tentative
 * attendus) ; les tests instrumentés vérifient le SQL réel. Chaque
 * test nomme l'entrelacement qui produisait le défaut : une écriture
 * FAILED inconditionnelle après une simple lecture PENDING.
 */
class ScheduledSendRunnerTest {

    private class FakeStore(
        initial: ScheduledMessage,
    ) : ScheduledSendRunner.Store {
        var row: ScheduledMessage = initial

        /** Rejoue un événement concurrent juste après la lecture du worker. */
        var afterRead: (() -> Unit)? = null

        override suspend fun getById(id: Long): ScheduledMessage? {
            val snapshot = row.takeIf { it.id == id }
            afterRead?.invoke()
            return snapshot
        }

        override suspend fun failIfPending(id: Long): Int {
            if (row.id != id || row.status != ScheduledStatus.PENDING) return 0
            row = row.copy(status = ScheduledStatus.FAILED)
            return 1
        }

        override suspend fun claim(id: Long, startedAt: Long): Int {
            if (row.id != id || row.status != ScheduledStatus.PENDING) return 0
            row = row.copy(
                status = ScheduledStatus.SENDING,
                attemptStartedAt = startedAt,
                attemptToken = null,
            )
            return 1
        }

        override suspend fun attachToken(id: Long, token: String): Int {
            if (row.id != id) return 0
            if (row.status != ScheduledStatus.SENDING &&
                row.status != ScheduledStatus.AMBIGUOUS
            ) {
                return 0
            }
            row = row.copy(attemptToken = token)
            return 1
        }

        override suspend fun failAttemptIfStillSending(id: Long, startedAt: Long): Int {
            if (row.id != id || row.status != ScheduledStatus.SENDING) return 0
            if (row.attemptStartedAt != startedAt) return 0
            row = row.copy(status = ScheduledStatus.FAILED)
            return 1
        }

        /** Simule le verdict d'émission qui résout la tentative par son jeton. */
        fun resolveByVerdict(status: ScheduledStatus) {
            if (row.status == ScheduledStatus.SENDING ||
                row.status == ScheduledStatus.AMBIGUOUS
            ) {
                row = row.copy(status = status)
            }
        }

        /** Simule l'annulation utilisateur (possible seulement depuis PENDING). */
        fun cancel() {
            if (row.status == ScheduledStatus.PENDING) {
                row = row.copy(status = ScheduledStatus.CANCELLED)
            }
        }
    }

    private fun pendingRow() = ScheduledMessage(
        id = 7L,
        address = "TEST",
        body = "Message de test",
        scheduledAt = 10_000L,
        subscriptionId = -1,
        status = ScheduledStatus.PENDING,
        createdAt = 500L,
    )

    private fun runner(
        store: FakeStore,
        isDefault: Boolean,
        send: suspend (ScheduledMessage, Long) -> SmsSender.SendResult = { _, _ ->
            SmsSender.SendResult("attempt-token", 1)
        },
    ) = ScheduledSendRunner(
        store = store,
        isDefaultSmsApp = { isDefault },
        nowMillis = { 2_000L },
        resolveThreadId = { 99L },
        send = send,
        resolveByToken = { },
    )

    @Test
    fun cancellationBetweenReadAndRoleLossStaysCancelled() = runBlocking {
        val store = FakeStore(pendingRow())
        // L'utilisateur annule après la lecture du worker, avant son
        // écriture d'échec pour perte du rôle.
        store.afterRead = { store.cancel() }

        val outcome = runner(store, isDefault = false).run(7L)

        assertEquals(ScheduledSendRunner.Outcome.ALREADY_HANDLED, outcome)
        assertEquals(ScheduledStatus.CANCELLED, store.row.status)
    }

    @Test
    fun roleLossWhileStillPendingMarksFailed() = runBlocking {
        val store = FakeStore(pendingRow())

        val outcome = runner(store, isDefault = false).run(7L)

        assertEquals(ScheduledSendRunner.Outcome.FAILED_NO_ROLE, outcome)
        assertEquals(ScheduledStatus.FAILED, store.row.status)
    }

    @Test
    fun verdictArrivingDuringSendThenExceptionKeepsVerdict() = runBlocking {
        val store = FakeStore(pendingRow())
        // Le verdict SENT arrive pendant l'appel d'envoi, puis
        // l'émetteur lève : le catch ne doit pas écraser le verdict.
        val outcome = runner(
            store,
            isDefault = true,
            send = { _, _ ->
                store.resolveByVerdict(ScheduledStatus.SENT)
                throw RuntimeException("panne après le verdict")
            },
        ).run(7L)

        assertEquals(ScheduledSendRunner.Outcome.ALREADY_HANDLED, outcome)
        assertEquals(ScheduledStatus.SENT, store.row.status)
    }

    @Test
    fun exceptionWithoutInterleavingMarksAttemptFailed() = runBlocking {
        val store = FakeStore(pendingRow())

        val outcome = runner(
            store,
            isDefault = true,
            send = { _, _ -> throw RuntimeException("panne franche") },
        ).run(7L)

        assertEquals(ScheduledSendRunner.Outcome.FAILED_SEND, outcome)
        assertEquals(ScheduledStatus.FAILED, store.row.status)
    }

    @Test
    fun alreadyHandledRowIsNeverSent() = runBlocking {
        val store = FakeStore(pendingRow().copy(status = ScheduledStatus.SENT))
        var sendCalls = 0

        val outcome = runner(
            store,
            isDefault = true,
            send = { _, _ ->
                sendCalls++
                SmsSender.SendResult("attempt-token", 1)
            },
        ).run(7L)

        assertEquals(ScheduledSendRunner.Outcome.ALREADY_HANDLED, outcome)
        assertEquals(0, sendCalls)
    }

    @Test
    fun happyPathClaimsAttachesTokenAndWaitsForVerdict() = runBlocking {
        val store = FakeStore(pendingRow())
        var resolvedToken: String? = null
        val runner = ScheduledSendRunner(
            store = store,
            isDefaultSmsApp = { true },
            nowMillis = { 2_000L },
            resolveThreadId = { 99L },
            send = { _, _ -> SmsSender.SendResult("attempt-token", 1) },
            resolveByToken = { token -> resolvedToken = token },
        )

        val outcome = runner.run(7L)

        assertEquals(ScheduledSendRunner.Outcome.LAUNCHED, outcome)
        assertEquals(ScheduledStatus.SENDING, store.row.status)
        assertEquals("attempt-token", store.row.attemptToken)
        assertEquals("attempt-token", resolvedToken)
    }
}
