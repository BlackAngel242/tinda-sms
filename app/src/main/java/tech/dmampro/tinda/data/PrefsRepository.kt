package tech.dmampro.tinda.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tindaPrefs: DataStore<Preferences> by preferencesDataStore(name = "tinda_prefs")

/**
 * Préférences applicatives (DataStore) : onboarding, SIM par conversation.
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

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
