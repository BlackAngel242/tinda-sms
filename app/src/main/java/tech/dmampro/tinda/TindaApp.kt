package tech.dmampro.tinda

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import tech.dmampro.tinda.notifications.NotificationHelper
import tech.dmampro.tinda.util.TindaLogger

@HiltAndroidApp
class TindaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // StrictMode UNIQUEMENT en debug : jamais en production (coût perf).
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
        // Canal de notification des SMS reçus (lot 1).
        NotificationHelper.createChannel(this)
        TindaLogger.i(TAG, "Application démarrée")
    }

    companion object {
        private const val TAG = "TindaApp"
    }
}
