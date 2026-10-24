package tech.dmampro.tinda.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.sms.MultipartStatus
import tech.dmampro.tinda.sms.PartOutcome
import tech.dmampro.tinda.sms.PartSentState

/**
 * Tests de comportement de la synchronisation des statuts.
 *
 * Le faux magasin reproduit le contrat de transition conditionnelle
 * du DAO : un statut terminal déjà persisté ne peut pas être remplacé
 * par un autre statut. Le faux fournisseur enregistre ce qui lui
 * serait réellement écrit, ce qui permet de vérifier l'alignement.
 */
class MessageStatusSyncTest {

    private data class ProviderWrite(val providerId: Long, val status: MessageStatus)

    private class FakeLocalStore(
        initial: StoredMessageStatus?,
    ) : LocalMessageStatusStore {
        var stored: StoredMessageStatus? = initial

        override suspend fun find(token: String): StoredMessageStatus? = stored

        override suspend fun tryTransition(
            token: String,
            status: MessageStatus,
            failureCode: Int?,
        ): Int {
            val current = stored ?: return 0
            val terminal = current.status == MessageStatus.DELIVERED ||
                current.status == MessageStatus.FAILED
            if (terminal && current.status != status) return 0
            stored = current.copy(status = status, failureCode = failureCode)
            return 1
        }
    }

    private class FakeProviderWriter : ProviderMessageStatusWriter {
        val writes = mutableListOf<ProviderWrite>()

        override suspend fun write(providerId: Long, status: MessageStatus) {
            writes += ProviderWrite(providerId, status)
        }
    }

    private fun fixture(
        initial: StoredMessageStatus?,
    ): Triple<FakeLocalStore, FakeProviderWriter, MessageStatusSync> {
        val local = FakeLocalStore(initial)
        val provider = FakeProviderWriter()
        return Triple(local, provider, MessageStatusSync(local, provider))
    }

    @Test
    fun `un delivre terminal refuse un sent tardif et le fournisseur recoit delivre`() =
        runBlocking {
            val (local, provider, sync) = fixture(
                StoredMessageStatus(MessageStatus.DELIVERED, providerId = 42),
            )

            val result = sync.update("token", MessageStatus.SENT)

            assertEquals(
                MessageStatusSyncResult.RefusedAligned(MessageStatus.DELIVERED),
                result,
            )
            assertEquals(MessageStatus.DELIVERED, local.stored?.status)
            assertEquals(
                listOf(ProviderWrite(42, MessageStatus.DELIVERED)),
                provider.writes,
            )
        }

    @Test
    fun `un echec terminal refuse un sent tardif et le fournisseur recoit l echec`() =
        runBlocking {
            val (local, provider, sync) = fixture(
                StoredMessageStatus(MessageStatus.FAILED, providerId = 43, failureCode = 2),
            )

            val result = sync.update("token", MessageStatus.SENT)

            assertEquals(
                MessageStatusSyncResult.RefusedAligned(MessageStatus.FAILED),
                result,
            )
            assertEquals(MessageStatus.FAILED, local.stored?.status)
            assertEquals(listOf(ProviderWrite(43, MessageStatus.FAILED)), provider.writes)
        }

    @Test
    fun `une transition normale est appliquee puis ecrite au fournisseur`() = runBlocking {
        val (local, provider, sync) = fixture(
            StoredMessageStatus(MessageStatus.SENDING, providerId = 44),
        )

        val result = sync.update("token", MessageStatus.SENT)

        assertEquals(MessageStatusSyncResult.Applied(MessageStatus.SENT), result)
        assertEquals(MessageStatus.SENT, local.stored?.status)
        assertEquals(listOf(ProviderWrite(44, MessageStatus.SENT)), provider.writes)
    }

    @Test
    fun `les callbacks inverses tardifs et en double ne font pas regresser delivre`() =
        runBlocking {
            val (local, provider, sync) = fixture(
                StoredMessageStatus(MessageStatus.SENDING, providerId = 45),
            )

            sync.update("token", MessageStatus.DELIVERED)
            val late = sync.update("token", MessageStatus.SENT)
            val duplicate = sync.update("token", MessageStatus.DELIVERED)

            assertEquals(
                MessageStatusSyncResult.RefusedAligned(MessageStatus.DELIVERED),
                late,
            )
            assertEquals(MessageStatusSyncResult.Applied(MessageStatus.DELIVERED), duplicate)
            assertEquals(MessageStatus.DELIVERED, local.stored?.status)
            assertTrue(provider.writes.all { it.status == MessageStatus.DELIVERED })
            assertEquals(3, provider.writes.size)
        }

    @Test
    fun `un segment multipart en echec donne un echec terminal non regressif`() = runBlocking {
        val (local, provider, sync) = fixture(
            StoredMessageStatus(MessageStatus.SENDING, providerId = 46),
        )
        val failedAggregate = MultipartStatus.aggregate(
            parts = listOf(
                PartOutcome(sent = PartSentState.CONFIRMED),
                PartOutcome(sent = PartSentState.FAILED),
            ),
            current = local.stored!!.status,
        )
        assertEquals(MessageStatus.FAILED, failedAggregate)
        sync.update("token", failedAggregate, failureCode = 1)

        // Un calcul tardif fondé sur des accusés confirmés ne peut pas
        // transformer l'échec agrégé en envoi réussi.
        val staleResult = sync.update("token", MessageStatus.SENT)
        val stillFailed = MultipartStatus.aggregate(
            parts = listOf(
                PartOutcome(sent = PartSentState.CONFIRMED),
                PartOutcome(sent = PartSentState.CONFIRMED),
            ),
            current = local.stored!!.status,
        )

        assertEquals(
            MessageStatusSyncResult.RefusedAligned(MessageStatus.FAILED),
            staleResult,
        )
        assertEquals(MessageStatus.FAILED, stillFailed)
        assertEquals(MessageStatus.FAILED, local.stored?.status)
        assertTrue(provider.writes.all { it.status == MessageStatus.FAILED })
    }

    @Test
    fun `un message absent ne produit aucune ecriture fournisseur`() = runBlocking {
        val (local, provider, sync) = fixture(initial = null)

        val result = sync.update("missing", MessageStatus.SENT)

        assertEquals(MessageStatusSyncResult.NotFound, result)
        assertEquals(null, local.stored)
        assertTrue(provider.writes.isEmpty())
    }
}
