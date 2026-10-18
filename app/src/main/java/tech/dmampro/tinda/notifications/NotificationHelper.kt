package tech.dmampro.tinda.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import tech.dmampro.tinda.MainActivity
import tech.dmampro.tinda.R

/**
 * Notifications des SMS reçus : un canal "Messages", style conversation.
 * Vérifie POST_NOTIFICATIONS (API 33+) avant tout envoi : sans l'autorisation,
 * on se tait au lieu de planter.
 */
object NotificationHelper {

    const val CHANNEL_MESSAGES = "tinda_messages"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_MESSAGES,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /** Une notification par conversation (l'ID = threadId tronqué). */
    fun showMessage(context: Context, threadId: Long, displayName: String, snippet: String) {
        if (!canNotify(context)) return
        val person = Person.Builder().setName(displayName).build()
        val style = NotificationCompat.MessagingStyle(person)
            .addMessage(snippet, System.currentTimeMillis(), person)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_THREAD, threadId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            threadId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_sms)
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(threadId.toInt(), notification)
    }

    fun cancelThread(context: Context, threadId: Long) {
        context.getSystemService(NotificationManager::class.java)?.cancel(threadId.toInt())
    }
}
