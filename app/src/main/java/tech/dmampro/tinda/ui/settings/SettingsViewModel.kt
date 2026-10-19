package tech.dmampro.tinda.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.AppLanguage
import tech.dmampro.tinda.data.BlockRepository
import tech.dmampro.tinda.data.PrefsRepository
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.StatsRepository
import tech.dmampro.tinda.data.VaultRepository
import tech.dmampro.tinda.sms.ScheduledSmsManager
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.SwipeAction
import tech.dmampro.tinda.util.LocaleHelper
import javax.inject.Inject

/** Fil archivé affiché dans les paramètres, avec nom résolu. */
data class ArchivedItem(
    val threadId: Long,
    val displayName: String,
)

/** Paramètres : thèmes, gestes, son, langue, liste noire (lot 2). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PrefsRepository,
    private val smsRepository: SmsRepository,
    private val blockRepository: BlockRepository,
    private val vaultRepository: VaultRepository,
    private val simRepository: SimRepository,
    private val scheduledManager: ScheduledSmsManager,
    private val statsRepository: StatsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val themeId: Flow<String> = prefs.appThemeId
    val themeMode: Flow<String> = prefs.themeMode
    val dynamicColor: Flow<Boolean> = prefs.dynamicColor
    val swipeLeft: Flow<String> = prefs.swipeLeftAction
    val swipeRight: Flow<String> = prefs.swipeRightAction
    val soundEnabled: Flow<Boolean> = prefs.sendSoundEnabled
    val soundVolume: Flow<Int> = prefs.sendSoundVolume
    val appLanguage: Flow<String> = prefs.appLanguage
    val blockedCount: Flow<Int> = blockRepository.observeBlocked()
        .map { it.size }

    /** SIM actives (carte Double SIM de l'en-tête). */
    private val _sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val sims: StateFlow<List<SimInfo>> = _sims.asStateFlow()

    /** Nombre de SMS programmés en attente (badge). */
    val pendingScheduledCount: StateFlow<Int> = scheduledManager.observeAll()
        .map { list -> list.count { it.status.name == "PENDING" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Statistiques SMS (maquette Stitch : compteurs par SIM, total, bloqués, délivrance). */
    private val _stats = MutableStateFlow(SmsStats())
    val stats: StateFlow<SmsStats> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            _sims.value = simRepository.getActiveSims()
        }
        viewModelScope.launch {
            val sims = simRepository.getActiveSims()
            val perSim = sims.associate { it.subscriptionId to statsRepository.sentCountForSim(it.subscriptionId) }
            _stats.value = SmsStats(
                total = statsRepository.totalCount(),
                blocked = statsRepository.blockedCount(),
                deliveryRate = statsRepository.deliveryRate(),
                perSim = perSim,
            )
        }
    }

    val archivedItems: StateFlow<List<ArchivedItem>> =
        smsRepository.observeArchivedThreads()
            .map { list ->
                list.map { thread ->
                    val name = smsRepository.resolveDisplayName(thread.address)
                    ArchivedItem(thread.threadId, name.ifBlank { thread.address })
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(id: String) {
        viewModelScope.launch {
            prefs.setAppThemeId(id)
            EventLog.log("theme.set", id)
        }
    }

    /** Les options avancées sont verrouillées par le code du coffre (si créé). */
    fun isAdvancedLocked(): Boolean = vaultRepository.isSetup()

    fun isBiometricAvailable(): Boolean = vaultRepository.isBiometricAvailable()

    fun verifyAdvancedPin(pin: String): Boolean {
        val ok = vaultRepository.verifyPin(pin)
        EventLog.log(if (ok) "settings.advanced_unlock_pin_ok" else "settings.advanced_unlock_pin_fail")
        return ok
    }

    fun unlockAdvancedWithBiometric(): Boolean {
        val ok = vaultRepository.unlockWithBiometric()
        EventLog.log(if (ok) "settings.advanced_unlock_bio_ok" else "settings.advanced_unlock_bio_fail")
        return ok
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun setSwipeLeft(action: SwipeAction) {
        viewModelScope.launch { prefs.setSwipeLeftAction(action) }
    }

    fun setSwipeRight(action: SwipeAction) {
        viewModelScope.launch { prefs.setSwipeRightAction(action) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSendSoundEnabled(enabled) }
    }

    fun setSoundVolume(volume: Int) {
        viewModelScope.launch { prefs.setSendSoundVolume(volume) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setAppLanguage(language)
            LocaleHelper.apply(context, language)
        }
    }

    fun unarchive(threadId: Long) {
        viewModelScope.launch { smsRepository.setThreadArchived(threadId, false) }
    }
}

/** Statistiques SMS affichées dans l'en-tête des paramètres (maquette Stitch). */
data class SmsStats(
    val total: Int = 0,
    val blocked: Int = 0,
    val deliveryRate: Int = 100,
    val perSim: Map<Int, Int> = emptyMap(),
)
