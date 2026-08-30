package ir.asgvp.ramzyar.sms

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Telephony
import android.telephony.SmsMessage
import ir.asgvp.ramzyar.notify.OtpNotificationListener
import ir.asgvp.ramzyar.overlay.SmsOverlayService
import ir.asgvp.ramzyar.settings.SmsSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val extras: Bundle = intent.extras ?: return
        val pdus = extras["pdus"] as? Array<*> ?: return
        val format = extras.getString("format")

        val messages = pdus.mapNotNull { pdu ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && format != null ->
                    SmsMessage.createFromPdu(pdu as ByteArray, format)

                else -> SmsMessage.createFromPdu(pdu as ByteArray)
            }
        }

        if (messages.isEmpty()) return

        val fullMessageBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val sender = messages.firstNotNullOfOrNull { it.originatingAddress } ?: ""

        // برای انجام کارهای async و مطمئن شدن از اتمام، از goAsync استفاده می‌کنیم
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // همه تنظیمات یک‌جا خوانده می‌شوند
                val settings = SmsSettingsRepository.observeSettings(context).first()

                // اگر کاربر اپ را غیرفعال کرده باشد، هیچ کاری انجام نده
                if (!settings.appEnabled) {
                    return@launch
                }

                // لیست سیاه شماره‌ها: این فرستنده‌ها هرگز پردازش نمی‌شوند
                if (matchesAny(sender, settings.blacklistNumbers)) {
                    return@launch
                }

                // اگر فیلتر فرستنده فعال باشد، فقط شماره‌های مجاز پردازش می‌شوند
                if (settings.whitelistEnabled && !matchesAny(sender, settings.whitelistSenders)) {
                    return@launch
                }

                val smart = CodeExtractor.extractSmartWithLabel(
                    fullMessageBody,
                    settings.customKeywords,
                    settings.blacklistWords
                ) ?: return@launch

                // کپی خودکار در کلیپ‌بورد در صورت فعال بودن
                if (settings.copyEnabled) {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("sms_code", smart.code)
                    clipboard.setPrimaryClip(clip)
                }

                // لرزش کوتاه برای اطلاع از رسیدن رمز
                if (settings.vibrationEnabled) {
                    vibrate(context)
                }

                // ذخیره در تاریخچه (با فرستنده برای نمایش در اپ)
                SmsSettingsRepository.addHistoryEntry(context, smart.code, smart.label, sender)

                // بستن اعلان پیامکِ حاوی رمز + تلاش برای «خوانده‌شده» کردن آن
                if (settings.hideSmsNotification) {
                    tryMarkSmsRead(context, smart.code)
                    OtpNotificationListener.dismissNotificationsWithCode(smart.code)
                }

                val serviceIntent = Intent(context, SmsOverlayService::class.java).apply {
                    putExtra(SmsOverlayService.EXTRA_TEXT, smart.code)
                    putExtra(SmsOverlayService.EXTRA_TITLE, smart.label)
                }
                // شروع سرویس برای نمایش لایه‌ی شناور
                context.startService(serviceIntent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * تطبیق فرستنده با لیست: اگر بخشی از شماره با الگو مطابق باشد، true برمی‌گردد.
     * مثلاً الگوی «50004» با فرستنده «+985000401» مطابق است.
     */
    private fun matchesAny(sender: String, patterns: Set<String>): Boolean {
        return patterns.any { pattern ->
            pattern.isNotBlank() && sender.contains(pattern.trim(), ignoreCase = true)
        }
    }

    /**
     * تلاش برای علامت‌گذاری پیامک حاوی رمز به‌عنوان «خوانده‌شده».
     * توجه: اندروید نوشتن در دیتابیس پیامک را فقط به اپ پیامک پیش‌فرض
     * می‌دهد؛ در بیشتر گوشی‌ها این تلاش بی‌اثر است و بی‌صدا رد می‌شود.
     */
    private fun tryMarkSmsRead(context: Context, code: String) {
        runCatching {
            context.contentResolver.update(
                Telephony.Sms.Inbox.CONTENT_URI,
                ContentValues().apply { put(Telephony.Sms.READ, 1) },
                "${Telephony.Sms.BODY} LIKE ?",
                arrayOf("%$code%")
            )
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
}
