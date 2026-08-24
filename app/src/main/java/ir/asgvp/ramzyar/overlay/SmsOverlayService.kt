package ir.asgvp.ramzyar.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import ir.asgvp.ramzyar.R
import ir.asgvp.ramzyar.settings.SmsSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class SmsOverlayService : Service() {

    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_TITLE = "extra_title"
        private const val TAG = "SmsOverlayService"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var countDownTimer: CountDownTimer? = null

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    // برای خواندن تنظیمات بدون قفل‌کردن ترد اصلی
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        val text = intent?.getStringExtra(EXTRA_TEXT)
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "رمز پویا"
        if (text != null) {
            serviceScope.launch { showOverlay(text, title) }
        } else {
            Log.w(TAG, "No EXTRA_TEXT provided, stopping service")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        serviceScope.cancel()
        countDownTimer?.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private suspend fun showOverlay(text: String, title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted, stopping service.")
            stopSelf()
            return
        }

        // در صورت وجود لایه قبلی، ابتدا آن را حذف می‌کنیم
        removeOverlay()

        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // نوار: عرض کمی بیشتر، راست‌چین (عنوان سمت راست، رمز وسط، بستن سمت چپ)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val dp = resources.displayMetrics.density
        val desiredWidth = (screenWidth * 0.70f).toInt().coerceAtLeast((220 * dp).toInt())

        // موقعیت ذخیره‌شده قبلی؛ همیشه داخل محدوده صفحه محدود می‌شود
        val savedPosition = SmsSettingsRepository.getOverlayPosition(this@SmsOverlayService)
        val minBarHeightPx = (60 * dp).toInt()
        val clampedX = (savedPosition?.x ?: 0).coerceIn(0, maxOf(0, screenWidth - desiredWidth))
        val clampedY = (savedPosition?.y ?: 150).coerceIn(0, maxOf(0, screenHeight - minBarHeightPx))

        val params = WindowManager.LayoutParams(
            desiredWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampedX
            y = clampedY
        }

        overlayLayoutParams = params

        // نوار کم‌عرض: سه بخش در یک ردیف — رنگ مطابق بک‌گراند اپ
        val context = this
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val backgroundColor = if (isDark) 0xFF232A37.toInt() else 0xFFF4FFF9.toInt()
        val closeSideColor = if (isDark) 0xFF2E3542.toInt() else 0xFFC8E6D4.toInt()
        val textColor = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
        val textColorSecondary = if (isDark) 0xB3E5E7EB.toInt() else 0xB31F2937.toInt()
        val dividerColor = if (isDark) 0x33FFFFFF.toInt() else 0x33000000.toInt()
        val borderColor = if (isDark) 0x28FFFFFF.toInt() else 0x18000000.toInt()

        val appFont = ResourcesCompat.getFont(context, R.font.vazirmatn_wght)
        val padH = (10 * dp).toInt()
        val padV = (6 * dp).toInt()
        val progressBarHeight = (2 * dp).toInt().coerceAtLeast(1)
        val dividerW = (1 * dp).toInt()
        val contentHeight = (32 * dp).toInt()
        val barRowTotalHeight = contentHeight + padV + padV

        val cornerRadius = 20 * dp
        val borderWidthPx = (1 * dp).toInt().coerceAtLeast(1)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, 0, 0, 0)
            setBackgroundColor(backgroundColor)
            elevation = 24f
        }

        val barRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padH, padV, padH, padV)
        }

        val titleView = TextView(context).apply {
            this.text = title
            textSize = 11f
            typeface = Typeface.create(appFont, Typeface.NORMAL)
            setTextColor(textColorSecondary)
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
        }
        barRow.addView(titleView, LinearLayout.LayoutParams(0, contentHeight).apply {
            weight = 0.25f
            marginEnd = 0
        })

        val div1 = View(context).apply { setBackgroundColor(dividerColor) }
        barRow.addView(div1, LinearLayout.LayoutParams(dividerW, contentHeight))

        val codeView = TextView(context).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.create(appFont, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
        }
        barRow.addView(codeView, LinearLayout.LayoutParams(0, contentHeight).apply {
            weight = 0.5f
            gravity = Gravity.CENTER
            marginStart = padH
            marginEnd = padH
        })

        val closeButton = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(textColor))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(padH, padV, padH, padV)
            setOnClickListener {
                removeOverlay()
                stopSelf()
            }
        }
        barRow.addView(closeButton, LinearLayout.LayoutParams(0, contentHeight).apply {
            weight = 0.25f
            gravity = Gravity.CENTER
        })

        // نوار پیشرفت: چسبیده به لبه پایین، راست‌به‌چپ، کاهش نرم
        val headerProgress = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            isIndeterminate = false
            max = 100
            progress = 100
            progressDrawable = context.getDrawable(R.drawable.progress_bar_track)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(barRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            barRowTotalHeight
        ))
        // نوار پیشرفت با حاشیه افقی تا فقط روی قسمت صاف (بین دو گردی پایین) باشد
        val progressMarginH = (cornerRadius).toInt()
        root.addView(
            headerProgress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                progressBarHeight
            ).apply {
                marginStart = progressMarginH
                marginEnd = progressMarginH
                bottomMargin = 0
            }
        )

        // جابجایی نوار با کشیدن؛ بستن فقط با یک ضربه‌ی کامل (بدون کشیدن) در ۲۵٪ سمت چپ
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var touchDownViewX = 0f
        var touchDownViewY = 0f
        root.setOnTouchListener { view, event ->
            val layoutParams = overlayLayoutParams ?: return@setOnTouchListener false
            val wmInstance = windowManager ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownViewX = event.x
                    touchDownViewY = event.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    wmInstance.updateViewLayout(root, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val closeZoneWidth = view.width * 0.25f
                    val isTap = abs(event.x - touchDownViewX) <= touchSlop &&
                        abs(event.y - touchDownViewY) <= touchSlop
                    if (isTap && event.x < closeZoneWidth) {
                        removeOverlay()
                        stopSelf()
                        return@setOnTouchListener true
                    }
                    serviceScope.launch {
                        SmsSettingsRepository.saveOverlayPosition(
                            this@SmsOverlayService,
                            layoutParams.x,
                            layoutParams.y
                        )
                    }
                    false
                }
                else -> false
            }
        }

        overlayView = root
        root.scaleX = 0f
        root.scaleY = 0f
        root.visibility = View.INVISIBLE
        wm.addView(root, params)
        root.post {
            val w = root.width
            val closeSideWidth = (w * 0.25f).toInt()
            val closeSide = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    cornerRadius, cornerRadius, 0f, 0f,
                    0f, 0f, cornerRadius, cornerRadius
                )
                setColor(closeSideColor)
            }
            val restOfBar = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    0f, 0f, cornerRadius, cornerRadius,
                    cornerRadius, cornerRadius, 0f, 0f
                )
                setColor(backgroundColor)
            }
            val borderFrame = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = cornerRadius
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(borderWidthPx, borderColor)
            }
            val layerDrawable = LayerDrawable(arrayOf(closeSide, restOfBar, borderFrame))
            layerDrawable.setLayerInset(0, 0, 0, w - closeSideWidth, 0)
            layerDrawable.setLayerInset(1, closeSideWidth, 0, 0, 0)
            layerDrawable.setLayerInset(2, 0, 0, 0, 0)
            root.background = layerDrawable
            val cornerRadiusPx = cornerRadius.toInt().coerceAtLeast(0)
            root.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                }
            }
            root.clipToOutline = false

            root.pivotX = root.width / 2f
            root.pivotY = root.height / 2f
            root.visibility = View.VISIBLE
            root.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        Log.d(TAG, "Overlay added")

        // خواندن مدت زمان تایمر از تنظیمات (پیش‌فرض ۱۱۰ ثانیه)
        val durationSeconds = SmsSettingsRepository.getTimerSeconds(this@SmsOverlayService)
            .coerceIn(10, 600)

        // رزولوشن بالا برای کاهش پیوسته و نرم (۲۰ به‌روزرسانی در ثانیه)
        val tickMs = 100L
        val totalTicks = (durationSeconds * 1000L) / tickMs
        headerProgress.max = totalTicks.toInt()
        headerProgress.progress = totalTicks.toInt()

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationSeconds * 1000L, tickMs) {
            override fun onTick(millisUntilFinished: Long) {
                val ticksLeft = (millisUntilFinished / tickMs).toInt()
                headerProgress.progress = ticksLeft
            }

            override fun onFinish() {
                removeOverlay()
                stopSelf()
            }
        }.start()
    }

    private fun removeOverlay() {
        val wm = windowManager
        val view = overlayView
        if (wm != null && view != null) {
            Log.d(TAG, "Removing overlay")
            wm.removeView(view)
            overlayView = null
        }
    }
}

