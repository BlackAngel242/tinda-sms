package tech.dmampro.tinda

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.AppLanguage
import tech.dmampro.tinda.data.PrefsRepository
import tech.dmampro.tinda.notifications.NotificationHelper
import tech.dmampro.tinda.util.LocaleHelper
import tech.dmampro.tinda.util.TindaLogger
import javax.inject.Inject

@HiltAndroidApp
class TindaApp : Application() {

    @Inject
    lateinit var prefs: PrefsRepository

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
        // Langue par application choisie dans les paramètres (lot 2, F8).
        CoroutineScope(Dispatchers.IO).launch {
            val language = prefs.appLanguage.first()
            LocaleHelper.apply(
                this@TindaApp,
                runCatching { AppLanguage.valueOf(language) }.getOrDefault(AppLanguage.SYSTEM),
            )
        }
        TindaLogger.i(TAG, "Application démarrée")
    }

    companion object {
        private const val TAG = "TindaApp"
    }
}
