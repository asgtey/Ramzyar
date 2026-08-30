package ir.asgvp.ramzyar.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import ir.asgvp.ramzyar.sms.CodeExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * سرویس شنود اعلان‌ها؛ برای بستن خودکار اعلان پیامکی که رمز از آن استخراج شده.
 * نیازمند دسترسی ویژه «مدیریت اعلان‌ها» از تنظیمات سیستم است.
 */
class OtpNotificationListener : NotificationListenerService() {

    companion object {
        private var instance: OtpNotificationListener? = null

        /** آیا کاربر دسترسی مدیریت اعلان‌ها را داده است؟ */
        fun isEnabled(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        /**
         * اعلان حاوی این کد را با چند بار تلاش می‌بندد؛ چون اعلان ممکن است
         * کمی بعد از رسیدن پیامک توسط اپ پیامک منتشر شود.
         */
        fun dismissNotificationsWithCode(code: String) {
            val service = instance ?: return
            service.serviceScope.launch {
                service.dismissLoop(code)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun dismissLoop(code: String) {
        var attempts = 0
        while (attempts < 16) { // حداکثر ۸ ثانیه تلاش
            if (dismissOnce(code)) return
            delay(500)
            attempts++
        }
    }

    private fun dismissOnce(code: String): Boolean {
        var removedAny = false
        val notifications = activeNotifications ?: return false
        for (sbn in notifications) {
            if (containsCode(sbn, code)) {
                runCatching { cancelNotification(sbn.key) }
                removedAny = true
            }
        }
        return removedAny
    }

    private fun containsCode(sbn: StatusBarNotification, code: String): Boolean {
        // اعلان خودِ رمزیار نباشد
        if (sbn.packageName == packageName) return false

        val extras = sbn.notification.extras
        val texts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { texts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { texts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { texts.add(it.toString()) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
            texts.add(line.toString())
        }

        // مقایسه با احتساب ارقام فارسی
        return texts.any { CodeExtractor.normalizeDigits(it).contains(code) }
    }
}
