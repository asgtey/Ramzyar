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

    /**
     * تبدیل ارقام فارسی و عربی به انگلیسی
     * (بسیاری از بانک‌ها رمز را با ارقام فارسی می‌فرستند)
     */
    fun normalizeDigits(text: String): String {
        val sb = StringBuilder(text.length)
        text.forEach { c ->
            when {
                c in '۰'..'۹' -> sb.append(('0'.code + (c - '۰')).toChar())
                c in '٠'..'٩' -> sb.append(('0'.code + (c - '٠')).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    data class SmartCode(
        val code: String,
        val label: String
    )

    /**
     * استخراج هوشمند کد از متن، بدون نیاز به تنظیم Regex توسط کاربر.
     *
     * منطق ساده:
     *  - اگر کلمه‌ای از کلمات لیست سیاه در متن باشد، کل پیامک نادیده گرفته می‌شود.
     *  - همه دنباله‌های عددی با طول حداقل ۴ رقم پیدا می‌شوند.
     *  - نزدیک‌ترین دنباله عددی به کلمات کلیدی (پیش‌فرض + دلخواه کاربر)
     *    به‌عنوان کد انتخاب می‌شود.
     */
    fun extractSmart(message: String): String? {
        return extractSmartWithLabel(message)?.code
    }

    /**
     * نسخه‌ای که علاوه بر کد، برچسب (label) برگرفته از کلمه‌ی کلیدی نزدیک را هم برمی‌گرداند.
     *
     * @param customKeywords کلمات کلیدی اضافه‌شده توسط کاربر در تنظیمات
     * @param blacklistWords کلماتی که اگر در متن باشند، پیامک به‌کل رد می‌شود
     */
    fun extractSmartWithLabel(
        message: String,
        customKeywords: Collection<String> = emptyList(),
        blacklistWords: Collection<String> = emptySet()
    ): SmartCode? {
        val text = normalizeDigits(message.replace("\n", " "))
        val lowerText = text.lowercase()

        // کلمات لیست سیاه → کل پیامک نادیده شود (حتی اگر «رمز» هم داشته باشد)
        if (blacklistWords.any { it.isNotBlank() && lowerText.contains(it.trim().lowercase()) }) {
            return null
        }

        // همه دنباله‌های عددی ۴ تا ۱۰ رقمی
        val digitRegex = Regex("(\\d{4,10})")
        val matches = digitRegex.findAll(text).toList()
        if (matches.isEmpty()) return null

        // ترکیب کلمات پیش‌فرض و دلخواه کاربر
        val allKeywords = (KEYWORDS + customKeywords.map { it.trim() })
            .filter { it.isNotBlank() }
            .distinct()

        // پیدا کردن «همه» وقوع کلمات کلیدی و موقعیت آن‌ها
        val keywordHits = mutableListOf<Pair<Int, String>>()
        for (keyword in allKeywords) {
            var idx = lowerText.indexOf(keyword.lowercase())
            while (idx >= 0) {
                keywordHits += idx to keyword
                idx = lowerText.indexOf(keyword.lowercase(), idx + 1)
            }
        }

        // اگر هیچ کلمه کلیدی پیدا نشد، پیام را بی‌ارتباط با رمز در نظر بگیر
        if (keywordHits.isEmpty()) return null

        // آخرین وقوعِ کلمه کلیدی مبناست؛ چون در پیامک‌های واقعی رمز بلافاصله
        // بعد از آن می‌آید (مثلاً «رمز قبلی ۹۹۹ باطل شد. رمز جدید شما ۱۲۳»)
        val anchorPos = keywordHits.maxOf { (pos, _) -> pos }

        // نزدیک‌ترین دنباله عددی به کلمه کلیدی؛ عددِ بعد از کلمه ترجیح دارد
        val bestMatch = matches.minByOrNull { match ->
            val matchCenter = match.range.first + match.value.length / 2
            val distance = kotlin.math.abs(matchCenter - anchorPos)
            if (matchCenter >= anchorPos) distance else distance * 2
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
