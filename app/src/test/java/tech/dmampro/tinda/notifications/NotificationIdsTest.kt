package tech.dmampro.tinda.notifications

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Identités des notifications (M3) : la notification d'un fil et la
 * notification déguisée du coffre ne doivent jamais se remplacer ni
 * partager un PendingIntent, y compris pour le fil dont l'identifiant
 * vaudrait celui du coffre.
 */
class NotificationIdsTest {

    @Test
    fun `le fil 9012 ne collisionne ni avec la notification ni avec le PendingIntent du coffre`() {
        assertNotEquals(
            NotificationIds.VAULT_NOTIFICATION_ID,
            NotificationIds.messageNotificationId(9012L),
        )
        assertNotEquals(
            NotificationIds.VAULT_REQUEST_CODE,
            NotificationIds.messageRequestCode(9012L),
        )
    }

    @Test
    fun `des fils differents ont des identites differentes`() {
        val ids = listOf(1L, 2L, 9012L, 9013L, 100_000L, 2_000_000L)
            .map { NotificationIds.messageNotificationId(it) }
        assertTrue(ids.distinct().size == ids.size)
    }

    @Test
    fun `les actions d intention distinguent le fil et le coffre`() {
        assertNotEquals(NotificationIds.ACTION_OPEN_THREAD, NotificationIds.ACTION_OPEN_VAULT)
    }
}
