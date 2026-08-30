package ir.asgvp.ramzyar.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val DATA_STORE_NAME = "sms_settings"

val Context.smsSettingsDataStore by preferencesDataStore(
    name = DATA_STORE_NAME
)

data class SmsSettings(
    val copyEnabled: Boolean,
    val appEnabled: Boolean,
    val timerSeconds: Int,
    val customKeywords: Set<String> = emptySet(),
    val blacklistWords: Set<String> = emptySet(),
    val blacklistNumbers: Set<String> = emptySet(),
    val whitelistSenders: Set<String> = emptySet(),
    val whitelistEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val hideSmsNotification: Boolean = true
)

data class OverlayPosition(
    val x: Int,
    val y: Int
)

data class HistoryEntry(
    val code: String,
    val label: String,
    val sender: String = "",
    val timestamp: Long
)

object SmsSettingsRepository {

    private val KEY_COPY_ENABLED = booleanPreferencesKey("copy_enabled")
    private val KEY_APP_ENABLED = booleanPreferencesKey("app_enabled")
    private val KEY_OVERLAY_X = intPreferencesKey("overlay_x")
    private val KEY_OVERLAY_Y = intPreferencesKey("overlay_y")
    private val KEY_TIMER_SECONDS = intPreferencesKey("timer_seconds")
    private val KEY_CUSTOM_KEYWORDS = stringSetPreferencesKey("custom_keywords")
    private val KEY_BLACKLIST_WORDS = stringSetPreferencesKey("blacklist_words")
    private val KEY_BLACKLIST_NUMBERS = stringSetPreferencesKey("blacklist_numbers")
    private val KEY_WHITELIST_SENDERS = stringSetPreferencesKey("whitelist_senders")
    private val KEY_WHITELIST_ENABLED = booleanPreferencesKey("whitelist_enabled")
    private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val KEY_HIDE_SMS_NOTIFICATION = booleanPreferencesKey("hide_sms_notification")
    private val KEY_HISTORY = stringPreferencesKey("history_json")

    const val DEFAULT_COPY_ENABLED = true
    const val DEFAULT_APP_ENABLED = true
    const val DEFAULT_TIMER_SECONDS = 110
    const val DEFAULT_WHITELIST_ENABLED = false
    const val DEFAULT_VIBRATION_ENABLED = true
    const val DEFAULT_HIDE_SMS_NOTIFICATION = true
    const val MAX_HISTORY = 30

    fun observeSettings(context: Context): Flow<SmsSettings> {
        return context.smsSettingsDataStore.data.map { prefs ->
            SmsSettings(
                copyEnabled = prefs[KEY_COPY_ENABLED] ?: DEFAULT_COPY_ENABLED,
                appEnabled = prefs[KEY_APP_ENABLED] ?: DEFAULT_APP_ENABLED,
                timerSeconds = prefs[KEY_TIMER_SECONDS] ?: DEFAULT_TIMER_SECONDS,
                customKeywords = prefs[KEY_CUSTOM_KEYWORDS] ?: emptySet(),
                blacklistWords = prefs[KEY_BLACKLIST_WORDS] ?: emptySet(),
                blacklistNumbers = prefs[KEY_BLACKLIST_NUMBERS] ?: emptySet(),
                whitelistSenders = prefs[KEY_WHITELIST_SENDERS] ?: emptySet(),
                whitelistEnabled = prefs[KEY_WHITELIST_ENABLED] ?: DEFAULT_WHITELIST_ENABLED,
                vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: DEFAULT_VIBRATION_ENABLED,
                hideSmsNotification = prefs[KEY_HIDE_SMS_NOTIFICATION]
                    ?: DEFAULT_HIDE_SMS_NOTIFICATION
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

    suspend fun updateVibrationEnabled(context: Context, enabled: Boolean) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun updateHideSmsNotification(context: Context, enabled: Boolean) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_HIDE_SMS_NOTIFICATION] = enabled
        }
    }

    suspend fun updateWhitelistEnabled(context: Context, enabled: Boolean) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_WHITELIST_ENABLED] = enabled
        }
    }

    suspend fun updateTimerSeconds(context: Context, value: Int) {
        val safe = value.coerceIn(10, 600) // بین ۱۰ تا ۶۰۰ ثانیه
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[KEY_TIMER_SECONDS] = safe
        }
    }

    suspend fun updateCustomKeywords(context: Context, values: Set<String>) =
        updateStringSet(context, KEY_CUSTOM_KEYWORDS, values)

    suspend fun updateBlacklistWords(context: Context, values: Set<String>) =
        updateStringSet(context, KEY_BLACKLIST_WORDS, values)

    suspend fun updateBlacklistNumbers(context: Context, values: Set<String>) =
        updateStringSet(context, KEY_BLACKLIST_NUMBERS, values)

    suspend fun updateWhitelistSenders(context: Context, values: Set<String>) =
        updateStringSet(context, KEY_WHITELIST_SENDERS, values)

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

    // ---------- تاریخچه رمزها ----------

    fun observeHistory(context: Context): Flow<List<HistoryEntry>> {
        return context.smsSettingsDataStore.data.map { prefs ->
            parseHistory(prefs[KEY_HISTORY])
        }
    }

    suspend fun addHistoryEntry(context: Context, code: String, label: String, sender: String = "") {
        context.smsSettingsDataStore.edit { prefs ->
            val list = parseHistory(prefs[KEY_HISTORY]).toMutableList()
            list.add(0, HistoryEntry(code, label, sender, System.currentTimeMillis()))
            while (list.size > MAX_HISTORY) {
                list.removeAt(list.size - 1)
            }
            val arr = JSONArray()
            list.forEach { entry ->
                arr.put(
                    JSONObject()
                        .put("code", entry.code)
                        .put("label", entry.label)
                        .put("sender", entry.sender)
                        .put("time", entry.timestamp)
                )
            }
            prefs[KEY_HISTORY] = arr.toString()
        }
    }

    suspend fun clearHistory(context: Context) {
        context.smsSettingsDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }

    private fun parseHistory(json: String?): List<HistoryEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                HistoryEntry(
                    code = obj.getString("code"),
                    label = obj.optString("label", "رمز"),
                    sender = obj.optString("sender", ""),
                    timestamp = obj.getLong("time")
                )
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun updateStringSet(
        context: Context,
        key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>,
        values: Set<String>
    ) {
        updatePreferences(context) { mutablePrefs ->
            mutablePrefs[key] = values.filter { it.isNotBlank() }.toSet()
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
