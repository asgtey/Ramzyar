package ir.asgvp.ramzyar.sms

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import ir.asgvp.ramzyar.overlay.SmsOverlayService
import ir.asgvp.ramzyar.settings.SmsSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        // برای انجام کارهای async و مطمئن شدن از اتمام، از goAsync استفاده می‌کنیم
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // اگر کاربر اپ را غیرفعال کرده باشد، هیچ کاری انجام نده
                if (!SmsSettingsRepository.isAppEnabled(context)) {
                    return@launch
                }

                val smart = CodeExtractor.extractSmartWithLabel(fullMessageBody) ?: return@launch

                // کپی خودکار در کلیپ‌بورد در صورت فعال بودن
                if (SmsSettingsRepository.isCopyEnabled(context)) {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("sms_code", smart.code)
                    clipboard.setPrimaryClip(clip)
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
}

