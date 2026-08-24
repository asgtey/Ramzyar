package ir.asgvp.ramzyar.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DATA_STORE_NAME = "sms_settings"

val Context.smsSettingsDataStore by preferencesDataStore(
    name = DATA_STORE_NAME
)

data class SmsSettings(
    val copyEnabled: Boolean,
    val appEnabled: Boolean,
    val timerSeconds: Int
)

data class OverlayPosition(
    val x: Int,
    val y: Int
)

object SmsSettingsRepository {

    private val KEY_COPY_ENABLED = booleanPreferencesKey("copy_enabled")
    private val KEY_APP_ENABLED = booleanPreferencesKey("app_enabled")
    private val KEY_OVERLAY_X = intPreferencesKey("overlay_x")
    private val KEY_OVERLAY_Y = intPreferencesKey("overlay_y")
    private val KEY_TIMER_SECONDS = intPreferencesKey("timer_seconds")

    const val DEFAULT_COPY_ENABLED = true
    const val DEFAULT_APP_ENABLED = true
    const val DEFAULT_TIMER_SECONDS = 110

    fun observeSettings(context: Context): Flow<SmsSettings> {
        return context.smsSettingsDataStore.data.map { prefs ->
            SmsSettings(
                copyEnabled = prefs[KEY_COPY_ENABLED] ?: DEFAULT_COPY_ENABLED,
                appEnabled = prefs[KEY_APP_ENABLED] ?: DEFAULT_APP_ENABLED,
                timerSeconds = prefs[KEY_TIMER_SECONDS] ?: DEFAULT_TIMER_SECONDS
            )
        }
    }

    suspend fun updateCopyEnabled(context: Context, enabled: Boolean) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_COPY_ENABLED] = enabled
        }
    }

    suspend fun updateAppEnabled(context: Context, enabled: Boolean) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_APP_ENABLED] = enabled
        }
    }

    suspend fun updateTimerSeconds(context: Context, value: Int) {
        val safe = value.coerceIn(10, 600) // بین ۱۰ تا ۶۰۰ ثانیه
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_TIMER_SECONDS] = safe
        }
    }

    suspend fun isCopyEnabled(context: Context): Boolean {
        return observeSettings(context).first().copyEnabled
    }

    suspend fun isAppEnabled(context: Context): Boolean {
        return observeSettings(context).first().appEnabled
    }

    suspend fun getTimerSeconds(context: Context): Int {
        return observeSettings(context).first().timerSeconds
    }

    suspend fun getOverlayPosition(context: Context): OverlayPosition? {
        val prefs = context.smsSettingsDataStore.data.first()
        val x = prefs[KEY_OVERLAY_X]
        val y = prefs[KEY_OVERLAY_Y]
        return if (x != null && y != null) {
            OverlayPosition(x, y)
        } else {
            null
        }
    }

    suspend fun saveOverlayPosition(context: Context, x: Int, y: Int) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_OVERLAY_X] = x
            mutablePrefs[KEY_OVERLAY_Y] = y
        }
    }

    private suspend fun updatePreferences(
        context: Context,
        block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        context.smsSettingsDataStore.edit { prefs ->
            block(prefs)
        }
    }
}

