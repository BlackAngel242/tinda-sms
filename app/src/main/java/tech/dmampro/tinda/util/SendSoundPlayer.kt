package tech.dmampro.tinda.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.PrefsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Son d'envoi signature (F6) : petit carillon original généré par
 * tools/generate_send_sound.py, joué à chaque envoi. Volume réglable,
 * désactivable dans les paramètres.
 */
@Singleton
class SendSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository,
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    @Volatile
    private var soundId: Int = 0

    init {
        soundId = soundPool.load(context, R.raw.tinda_send, 1)
    }

    /** Joue le son si activé ; lit les préférences (appel suspendu). */
    suspend fun playIfEnabled() {
        if (!prefs.sendSoundEnabled.first()) return
        val volume = prefs.sendSoundVolume.first() / 100f
        if (volume <= 0f || soundId == 0) return
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }
}
