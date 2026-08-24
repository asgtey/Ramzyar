package ir.asgvp.ramzyar

import ir.asgvp.ramzyar.sms.CodeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeExtractorTest {

    @Test
    fun `رمز پویا با مبلغ قبل از رمز - عدد درست انتخاب میشود`() {
        val message = "مبلغ 1000000 ریال از حساب شما بابت انتقال وجه\nرمز: 8196579"
        val result = CodeExtractor.extractSmartWithLabel(message)
        assertEquals("8196579", result?.code)
    }

    @Test
    fun `برچسب از نزدیکترین کلمه کلیدی گرفته میشود`() {
        val message = "بانک آزمایشی\nرمز پویا: 483920\nجهت انجام تراکنش استفاده کنید"
        val result = CodeExtractor.extractSmartWithLabel(message)
        assertEquals("483920", result?.code)
        assertEquals("رمز پویا", result?.label)
    }

    @Test
    fun `کد تایید شناسایی میشود`() {
        val message = "کد تایید شما: 45321"
        val result = CodeExtractor.extractSmartWithLabel(message)
        assertEquals("45321", result?.code)
        assertEquals("کد تایید", result?.label)
    }

    @Test
    fun `پیامک بدون کلمه کلیدی نادیده گرفته میشود`() {
        val message = "فاکتور شماره 123456 صادر شد"
        assertNull(CodeExtractor.extractSmartWithLabel(message))
    }

    @Test
    fun `پیامک با کلمه کلیدی ولی بدون عدد - null برمیگرداند`() {
        val message = "رمز شما به زودی ارسال می‌شود"
        assertNull(CodeExtractor.extractSmartWithLabel(message))
    }

    @Test
    fun `عدد کمتر از چهار رقم رمز محسوب نمیشود`() {
        assertNull(CodeExtractor.extractSmartWithLabel("رمز 123"))
        val result = CodeExtractor.extractSmartWithLabel("رمز 1234")
        assertEquals("1234", result?.code)
    }

    @Test
    fun `پیامک چند خطی به درستی پردازش میشود`() {
        val message = "بانک ملت\nرمز پویا:\n555012\nمرکز ارتباط: ۰۲۱"
        val result = CodeExtractor.extractSmartWithLabel(message)
        assertEquals("555012", result?.code)
    }

    @Test
    fun `extractSmart فقط خود کد را برمیگرداند`() {
        val code = CodeExtractor.extractSmart("کد فعالسازی شما 984513 است")
        assertEquals("984513", code)
    }

    @Test
    fun `نزدیکترین عدد به کلمه کلیدی انتخاب میشود`() {
        // دو عدد ۵ رقمی داریم؛ عدد نزدیک‌تر به «رمز» باید برگردد
        val message = "رسید 11111 برای خرید\nرمز 22222 پایان پیامک"
        val result = CodeExtractor.extractSmartWithLabel(message)
        assertEquals("22222", result?.code)
    }
}
