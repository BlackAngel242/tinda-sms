package tech.dmampro.tinda.notifications

/**
 * Identités explicites des notifications et de leurs PendingIntent.
 * Un fil et le coffre ne doivent jamais partager un identifiant :
 * ils se remplaceraient et leurs intentions se confondraient.
 */
object NotificationIds {

    // Espace négatif réservé au coffre : les identifiants de fils du
    // provider sont positifs, aucune collision n'est possible (le fil
    // 9012 écrasait la notification déguisée avec l'ancien schéma).
    const val VAULT_NOTIFICATION_ID: Int = -9012
    const val VAULT_REQUEST_CODE: Int = -9012

    const val ACTION_OPEN_THREAD = "tech.dmampro.tinda.action.OPEN_THREAD"
    const val ACTION_OPEN_VAULT = "tech.dmampro.tinda.action.OPEN_VAULT"

    fun messageNotificationId(threadId: Long): Int = threadId.toInt()

    fun messageRequestCode(threadId: Long): Int = threadId.toInt()
}
