package tech.dmampro.tinda.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.dmampro.tinda.theme.DEFAULT_THEME_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tindaPrefs: DataStore<Preferences> by preferencesDataStore(name = "tinda_prefs")

/**
 * Résolution Material You (lot 1) : sans préférence enregistrée, les
 * couleurs dynamiques sont désactivées pour que la palette fixe
 * choisie s'applique. Une valeur explicitement enregistrée (true ou
 * false) est toujours respectée. Fonction pure, testée unitairement.
 */
fun resolveDynamicColor(stored: Boolean?): Boolean = stored ?: false

/**
 * Préférences applicatives (DataStore) : onboarding, SIM par conversation,
 * thèmes, gestes, son, langue (lot 2).
 * Aucune donnée sensible ici (le PIN du coffre n'y figurera jamais).
 */
@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store: DataStore<Preferences> = context.tindaPrefs

    suspend fun isOnboardingDone(): Boolean =
        store.data.map { it[KEY_ONBOARDING_DONE] == true }.first()

    suspend fun setOnboardingDone() {
        store.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    /** SIM choisie pour une conversation (null = SIM par défaut du système). */
    suspend fun getThreadSim(threadId: Long): Int? =
        store.data.map { it[intPreferencesKey(threadSimKey(threadId))] }.first()

    suspend fun setThreadSim(threadId: Long, subscriptionId: Int) {
        store.edit { it[intPreferencesKey(threadSimKey(threadId))] = subscriptionId }
    }

    private fun threadSimKey(threadId: Long) = "sim_thread_$threadId"

    // ---- Thème (F4) ----

    /** Identifiant du thème choisi ([DEFAULT_THEME_ID] par défaut). */
    val appThemeId: Flow<String> =
        store.data.map { it[KEY_THEME_ID] ?: DEFAULT_THEME_ID }

    suspend fun setAppThemeId(id: String) {
        store.edit { it[KEY_THEME_ID] = id }
    }

    val themeMode: Flow<String> =
        store.data.map { it[KEY_THEME_MODE] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        store.edit { it[KEY_THEME_MODE] = mode }
    }

    /** Couleurs dynamiques Material You (Android 12+), désactivées par défaut. */
    val dynamicColor: Flow<Boolean> =
        store.data.map { resolveDynamicColor(it[KEY_DYNAMIC_COLOR]) }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    // ---- Gestes (F5) ----

    val swipeLeftAction: Flow<String> =
        store.data.map { it[KEY_SWIPE_LEFT] ?: SwipeAction.ARCHIVE.name }

    suspend fun setSwipeLeftAction(action: SwipeAction) {
        store.edit { it[KEY_SWIPE_LEFT] = action.name }
    }

    val swipeRightAction: Flow<String> =
        store.data.map { it[KEY_SWIPE_RIGHT] ?: SwipeAction.REPLY.name }

    suspend fun setSwipeRightAction(action: SwipeAction) {
        store.edit { it[KEY_SWIPE_RIGHT] = action.name }
    }

    // ---- Son d'envoi (F6) ----

    val sendSoundEnabled: Flow<Boolean> =
        store.data.map { it[KEY_SOUND_ENABLED] != false }

    suspend fun setSendSoundEnabled(enabled: Boolean) {
        store.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    /** Volume du son d'envoi, 0 à 100. */
    val sendSoundVolume: Flow<Int> =
        store.data.map { (it[KEY_SOUND_VOLUME] ?: 70).coerceIn(0, 100) }

    suspend fun setSendSoundVolume(volume: Int) {
        store.edit { it[KEY_SOUND_VOLUME] = volume.coerceIn(0, 100) }
    }

    // ---- Langue (F8) ----

    val appLanguage: Flow<String> =
        store.data.map { it[KEY_LANGUAGE] ?: AppLanguage.SYSTEM.name }

    suspend fun setAppLanguage(language: AppLanguage) {
        store.edit { it[KEY_LANGUAGE] = language.name }
    }

    // ---- Recherche (F3) ----

    /** Dernier remplissage complet de l'index FTS (0 = jamais). */
    suspend fun getLastFtsBackfill(): Long =
        store.data.map { it[KEY_FTS_BACKFILL] ?: 0L }.first()

    suspend fun setLastFtsBackfill(timeMillis: Long) {
        store.edit { it[KEY_FTS_BACKFILL] = timeMillis }
    }

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_THEME_ID = stringPreferencesKey("theme_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_SWIPE_LEFT = stringPreferencesKey("swipe_left")
        private val KEY_SWIPE_RIGHT = stringPreferencesKey("swipe_right")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_SOUND_VOLUME = intPreferencesKey("sound_volume")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_FTS_BACKFILL = longPreferencesKey("fts_backfill")
    }
}
