package tech.dmampro.tinda.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.PrefsRepository
import javax.inject.Inject

/** État d'onboarding : rôle SMS par défaut + autorisations runtime. */
data class OnboardingState(
    val isDefaultSmsApp: Boolean = false,
    val smsGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val phoneGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
) {
    val telephonyReady: Boolean get() = isDefaultSmsApp && smsGranted
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** À rappeler à chaque retour au premier plan (le dialogue système est externe). */
    fun refresh() {
        _state.update {
            OnboardingState(
                isDefaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName,
                smsGranted = hasAll(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                ),
                contactsGranted = hasAll(Manifest.permission.READ_CONTACTS),
                phoneGranted = hasAll(Manifest.permission.READ_PHONE_STATE),
                notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    hasAll(Manifest.permission.POST_NOTIFICATIONS),
            )
        }
    }

    fun markDone() {
        viewModelScope.launch { prefs.setOnboardingDone() }
    }

    private fun hasAll(vararg permissions: String): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        /** Permissions runtime demandées par l'onboarding. */
        fun requiredPermissions(): Array<String> = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
}
