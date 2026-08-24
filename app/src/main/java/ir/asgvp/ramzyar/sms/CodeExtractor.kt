package ir.asgvp.ramzyar.sms

object CodeExtractor {

    /**
     * کلمات کلیدی از پیش‌تعریف‌شده برای تشخیص پیامک‌های حاوی رمز
     */
    val KEYWORDS: List<String> = listOf(
        "رمز پویا",
        "رمز یکبار مصرف",
        "رمز یک‌بارمصرف",
        "رمز",
        "کد تایید",
        "کد تأیید",
        "کد فعالسازی",
        "کد فعال‌سازی",
        "کد ورود",
        "کد امنیتی"
    )

    data class SmartCode(
        val code: String,
        val label: String
    )

    /**
     * از متن پیام، با استفاده از الگوی Regex داده‌شده، مقدار موردنظر (مثلاً کد عددی) را استخراج می‌کند.
     * اگر الگو نامعتبر باشد یا چیزی پیدا نشود، null برمی‌گرداند.
     */
    fun extract(message: String, regexPattern: String): String? {
        return runCatching {
            val regex = Regex(regexPattern)
            val match = regex.find(message) ?: return null

            // اگر در الگو گروه ( ) تعریف شده باشد، مقدار گروه اول را برگردان
            if (match.groupValues.size > 1) {
                match.groupValues[1]
            } else {
                match.value
            }
        }.getOrNull()
    }

    /**
     * استخراج هوشمند کد از متن، بدون نیاز به تنظیم Regex توسط کاربر.
     *
     * منطق ساده:
     *  - همه دنباله‌های عددی با طول حداقل ۴ رقم را پیدا می‌کند.
     *  - اگر کلماتی مثل «رمز»، «کد»، «رمز پویا»، «کد تایید» در متن باشند،
     *    نزدیک‌ترین دنباله عددی به این کلمات به‌عنوان کد انتخاب می‌شود.
     *  - در غیر این صورت، طولانی‌ترین دنباله عددی انتخاب می‌شود.
     */
    fun extractSmart(message: String): String? {
        return extractSmartWithLabel(message)?.code
    }

    /**
     * نسخه‌ای که علاوه بر کد، برچسب (label) برگرفته از کلمه‌ی کلیدی نزدیک را هم برمی‌گرداند.
     */
    fun extractSmartWithLabel(message: String): SmartCode? {
        val text = message.replace("\n", " ")

        // همه دنباله‌های عددی ۴ تا ۱۰ رقمی
        val digitRegex = Regex("(\\d{4,10})")
        val matches = digitRegex.findAll(text).toList()
        if (matches.isEmpty()) return null

        val lowerText = text.lowercase()

        // پیدا کردن همه کلمات کلیدی و موقعیت آن‌ها
        val keywordHits = mutableListOf<Pair<Int, String>>()
        for (keyword in KEYWORDS) {
            val idx = lowerText.indexOf(keyword.lowercase())
            if (idx >= 0) {
                keywordHits += idx to keyword
            }
        }

        // اگر هیچ کلمه کلیدی پیدا نشد، پیام را بی‌ارتباط با رمز در نظر بگیر
        if (keywordHits.isEmpty()) return null

        // نزدیک‌ترین دنباله عددی به کلمات کلیدی
        val bestMatch = matches.minByOrNull { match ->
            val matchCenter = match.range.first + match.value.length / 2
            keywordHits.minOf { (pos, _) -> kotlin.math.abs(matchCenter - pos) }
        } ?: return null

        // برچسب را از نزدیک‌ترین کلمه کلیدی نسبت به این دنباله انتخاب کن
        val matchCenter = bestMatch.range.first + bestMatch.value.length / 2
        val bestKeyword = keywordHits.minByOrNull { (pos, _) ->
            kotlin.math.abs(matchCenter - pos)
        }?.second ?: "رمز پویا"

        return SmartCode(
            code = bestMatch.value,
            label = bestKeyword
        )
    }
}

