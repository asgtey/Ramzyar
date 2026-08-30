package ir.asgvp.ramzyar.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

/**
 * خواندن فهرست فرستندگان اخیر از صندوق پیامک برای انتخاب راحت در لیست سفید/سیاه.
 * برخی فرستنده‌ها شماره ندارند و نام دارند (مثل MelatBank)؛ همان نام برگردانده می‌شود.
 */
object SmsInboxReader {

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * فرستندگان متمایز پیامک‌های ورودی، مرتب بر اساس تازگی.
     */
    fun fetchRecentSenders(context: Context, limit: Int = 100): List<String> {
        val senders = linkedMapOf<String, Unit>()
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext() && senders.size < limit) {
                    val address = cursor.getString(0)?.trim().orEmpty()
                    if (address.isNotBlank()) {
                        senders.putIfAbsent(address, Unit)
                    }
                }
            }
        }
        return senders.keys.toList()
    }
}
