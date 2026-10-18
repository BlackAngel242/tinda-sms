package tech.dmampro.tinda.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import tech.dmampro.tinda.data.AppLanguage

/**
 * Langue de l'interface (F8). Sur Android 13+, la langue par application est
 * gérée par le système (LocaleManager). En dessous, l'app suit la langue du
 * système (les ressources FR/EN existent déjà).
 */
object LocaleHelper {

    /** Vrai si le choix de langue par application est supporté. */
    fun isPerAppLanguageSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun apply(context: Context, language: AppLanguage) {
        if (!isPerAppLanguageSupported()) return
        val tags = when (language) {
            AppLanguage.FRENCH -> "fr"
            AppLanguage.ENGLISH -> "en"
            AppLanguage.SYSTEM -> ""
        }
        context.getSystemService(LocaleManager::class.java)
            ?.applicationLocales = LocaleList.forLanguageTags(tags)
    }
}
