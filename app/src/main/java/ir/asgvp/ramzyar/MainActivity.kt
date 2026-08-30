package ir.asgvp.ramzyar

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.asgvp.ramzyar.notify.OtpNotificationListener
import ir.asgvp.ramzyar.overlay.SmsOverlayService
import ir.asgvp.ramzyar.settings.HistoryEntry
import ir.asgvp.ramzyar.settings.SmsSettings
import ir.asgvp.ramzyar.settings.SmsSettingsRepository
import ir.asgvp.ramzyar.sms.CodeExtractor
import ir.asgvp.ramzyar.sms.SmsInboxReader
import ir.asgvp.ramzyar.ui.components.BatteryGuideContent
import ir.asgvp.ramzyar.ui.components.CategoryLabel
import ir.asgvp.ramzyar.ui.components.CollapsibleCard
import ir.asgvp.ramzyar.ui.components.GradientButton
import ir.asgvp.ramzyar.ui.components.HistoryContent
import ir.asgvp.ramzyar.ui.components.ListEditorSection
import ir.asgvp.ramzyar.ui.components.SectionCard
import ir.asgvp.ramzyar.ui.components.SenderPickerDialog
import ir.asgvp.ramzyar.ui.theme.Amber
import ir.asgvp.ramzyar.ui.theme.Emerald
import ir.asgvp.ramzyar.ui.theme.RamzyarTheme
import ir.asgvp.ramzyar.ui.theme.SkyBlue
import ir.asgvp.ramzyar.ui.theme.Success
import ir.asgvp.ramzyar.ui.theme.VioletSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RamzyarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val smsSettingsState by SmsSettingsRepository.observeSettings(context)
        .collectAsState(
            initial = SmsSettings(
                copyEnabled = SmsSettingsRepository.DEFAULT_COPY_ENABLED,
                appEnabled = SmsSettingsRepository.DEFAULT_APP_ENABLED,
                timerSeconds = SmsSettingsRepository.DEFAULT_TIMER_SECONDS,
                vibrationEnabled = SmsSettingsRepository.DEFAULT_VIBRATION_ENABLED
            )
        )

    var copyEnabled by remember { mutableStateOf(smsSettingsState.copyEnabled) }
    var appEnabled by remember { mutableStateOf(smsSettingsState.appEnabled) }
    var vibrationEnabled by remember { mutableStateOf(smsSettingsState.vibrationEnabled) }
    var hideSmsNotification by remember { mutableStateOf(smsSettingsState.hideSmsNotification) }
    var timerSecondsText by remember { mutableStateOf(smsSettingsState.timerSeconds.toString()) }
    var timerFieldFocused by remember { mutableStateOf(false) }
    var timerSaveJob by remember { mutableStateOf<Job?>(null) }

    val notificationListenerGranted = remember {
        mutableStateOf(OtpNotificationListener.isEnabled(context))
    }

    val smsReadGranted = remember {
        mutableStateOf(SmsInboxReader.hasPermission(context))
    }

    // ---------- انتخاب فرستنده از پیامک‌های اخیر ----------
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    var pickerSenders by remember { mutableStateOf<List<String>>(emptyList()) }
    var pickHandler by remember { mutableStateOf<(String) -> Unit>({}) }
    var awaitingReadPermission by rememberSaveable { mutableStateOf(false) }

    val readSmsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsReadGranted.value = granted
        if (granted && awaitingReadPermission) {
            awaitingReadPermission = false
            scope.launch {
                pickerSenders = withContext(Dispatchers.IO) {
                    SmsInboxReader.fetchRecentSenders(context)
                }
                pickerVisible = true
            }
        }
    }

    fun openSenderPicker(onPick: (String) -> Unit) {
        pickHandler = onPick
        if (SmsInboxReader.hasPermission(context)) {
            scope.launch {
                pickerSenders = withContext(Dispatchers.IO) {
                    SmsInboxReader.fetchRecentSenders(context)
                }
                pickerVisible = true
            }
        } else {
            awaitingReadPermission = true
            readSmsLauncher.launch(android.Manifest.permission.READ_SMS)
        }
    }

    val history by SmsSettingsRepository.observeHistory(context)
        .collectAsState(initial = emptyList<HistoryEntry>())

    LaunchedEffect(smsSettingsState) {
        copyEnabled = smsSettingsState.copyEnabled
        appEnabled = smsSettingsState.appEnabled
        vibrationEnabled = smsSettingsState.vibrationEnabled
        hideSmsNotification = smsSettingsState.hideSmsNotification
        // در حین تایپ کاربر، متن فیلد از تنظیمات بازنویسی نشود
        if (!timerFieldFocused) {
            timerSecondsText = smsSettingsState.timerSeconds.toString()
        }
    }

    val smsPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val overlayGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    // هر بار که صفحه دوباره Resume شود، وضعیت دسترسی‌های سیستمی به‌روزرسانی می‌شود
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted.value =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(context)
                    } else {
                        true
                    }
                notificationListenerGranted.value = OtpNotificationListener.isEnabled(context)
                smsReadGranted.value = SmsInboxReader.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted.value = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeroHeader(isActive = appEnabled)

        CategoryLabel(text = "دسترسی‌ها", color = SkyBlue)

        // دسترسی‌ها
        SectionCard {
            PermissionRow(
                label = "دریافت پیامک‌ها",
                granted = smsPermissionGranted.value,
                actionText = "فعال‌سازی",
                accent = Emerald,
                onAction = { smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS) }
            )
            PermissionRow(
                label = "نمایش روی برنامه‌ها (Overlay)",
                granted = overlayGranted.value,
                actionText = "اجازه نمایش",
                accent = VioletSoft,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            )
            PermissionRow(
                label = "خواندن پیامک‌ها (برای انتخاب فرستنده)",
                granted = smsReadGranted.value,
                actionText = "فعال‌سازی",
                accent = Amber,
                onAction = { readSmsLauncher.launch(Manifest.permission.READ_SMS) }
            )
        }

        CategoryLabel(text = "تنظیمات", color = Emerald)

        // تنظیمات اصلی
        SectionCard {
            Text(
                text = "تنظیمات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            SwitchRow(
                label = "رمزیار فعال باشد",
                checked = appEnabled,
                onChange = {
                    appEnabled = !appEnabled
                    scope.launch {
                        SmsSettingsRepository.updateAppEnabled(context, appEnabled)
                    }
                }
            )
            SwitchRow(
                label = "کپی خودکار پس از دریافت",
                checked = copyEnabled,
                onChange = { enabled ->
                    copyEnabled = enabled
                    scope.launch {
                        SmsSettingsRepository.updateCopyEnabled(context, enabled)
                    }
                }
            )
            SwitchRow(
                label = "لرزش هنگام رسیدن رمز",
                checked = vibrationEnabled,
                onChange = { enabled ->
                    vibrationEnabled = enabled
                    scope.launch {
                        SmsSettingsRepository.updateVibrationEnabled(context, enabled)
                    }
                }
            )
            SwitchRow(
                label = "اعلان پیامک رمز خودکار بسته شود",
                checked = hideSmsNotification,
                onChange = { enabled ->
                    hideSmsNotification = enabled
                    scope.launch {
                        SmsSettingsRepository.updateHideSmsNotification(context, enabled)
                    }
                }
            )
            if (hideSmsNotification && !notificationListenerGranted.value) {
                PermissionRow(
                    label = "دسترسی مدیریت اعلان‌ها (برای بستن اعلان پیامک)",
                    granted = false,
                    actionText = "دادن دسترسی",
                    accent = SkyBlue,
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    }
                )
                Text(
                    text = "اندروید اجازه تغییر وضعیت «خوانده‌شده» را فقط به اپ پیامک پیش‌فرض می‌دهد؛ با این دسترسی، رمزیار اعلان آن پیامک را بسته و نشان خوانده‌نشده را حذف می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            // زمان تایمر: لیبل و فیلد در یک ردیف
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "مدت نمایش نوار (ثانیه)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = timerSecondsText,
                        onValueChange = { newText ->
                            val digits = newText.filter { it.isDigit() }.take(3)
                            timerSecondsText = digits
                            if (digits.isNotEmpty()) {
                                timerSaveJob?.cancel()
                                timerSaveJob = scope.launch {
                                    delay(800)
                                    SmsSettingsRepository.updateTimerSeconds(context, digits.toInt())
                                }
                            }
                        },
                        modifier = Modifier
                            .width(84.dp)
                            .onFocusChanged { state ->
                                timerFieldFocused = state.isFocused
                                if (!state.isFocused) {
                                    val value = timerSecondsText.toIntOrNull()
                                    if (value != null) {
                                        timerSaveJob?.cancel()
                                        timerSaveJob = scope.launch {
                                            SmsSettingsRepository.updateTimerSeconds(context, value)
                                        }
                                    } else {
                                        timerSecondsText =
                                            smsSettingsState.timerSeconds.toString()
                                    }
                                }
                            },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // نمایش تستی رمز
        GradientButton(text = "نمایش تستی رمز") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context)
            ) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } else {
                val sampleMessage = "رمز 1234"
                val extracted = CodeExtractor.extractSmart(sampleMessage) ?: sampleMessage
                context.startService(
                    Intent(context, SmsOverlayService::class.java).apply {
                        putExtra(SmsOverlayService.EXTRA_TEXT, extracted)
                    }
                )
            }
        }
        Text(
            text = "هر پیامکی که حاوی کلماتی مانند «رمز پویا»، «کد تایید» و ... باشد شناسایی شده و رمز آن روی صفحه نمایش داده و کپی می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        CategoryLabel(text = "تشخیص رمز و فیلترها", color = VioletSoft)

        // کلمات تشخیص رمز
        CollapsibleCard(
            title = "کلمات تشخیص رمز",
            badge = "${smsSettingsState.customKeywords.size} دلخواه"
        ) {
            Text(
                text = "پیش‌فرض: " + CodeExtractor.KEYWORDS.joinToString("، "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            ListEditorSection(
                title = "کلمات دلخواه شما (علاوه بر پیش‌فرض‌ها شناسایی می‌شوند)",
                items = smsSettingsState.customKeywords,
                placeholder = "کلمه جدید، مثلاً: کد ورود دوم",
                onAdd = { keyword ->
                    scope.launch {
                        SmsSettingsRepository.updateCustomKeywords(
                            context,
                            smsSettingsState.customKeywords + keyword
                        )
                    }
                },
                onRemove = { keyword ->
                    scope.launch {
                        SmsSettingsRepository.updateCustomKeywords(
                            context,
                            smsSettingsState.customKeywords - keyword
                        )
                    }
                }
            )
        }

        // لیست سیاه و فیلتر فرستنده
        CollapsibleCard(
            title = "لیست سیاه و فیلتر فرستنده",
            badge = "${smsSettingsState.blacklistWords.size + smsSettingsState.blacklistNumbers.size}"
        ) {
            ListEditorSection(
                title = "کلمات ممنوع (پیامکِ دارای این کلمات کاملاً نادیده می‌شود)",
                items = smsSettingsState.blacklistWords,
                placeholder = "مثلاً: قرعه‌کشی",
                onAdd = { word ->
                    scope.launch {
                        SmsSettingsRepository.updateBlacklistWords(
                            context,
                            smsSettingsState.blacklistWords + word
                        )
                    }
                },
                onRemove = { word ->
                    scope.launch {
                        SmsSettingsRepository.updateBlacklistWords(
                            context,
                            smsSettingsState.blacklistWords - word
                        )
                    }
                }
            )
            ListEditorSection(
                title = "شماره‌های ممنوع (بخشی از شماره کافی است)",
                items = smsSettingsState.blacklistNumbers,
                placeholder = "مثلاً: 981000",
                onAdd = { number ->
                    scope.launch {
                        SmsSettingsRepository.updateBlacklistNumbers(
                            context,
                            smsSettingsState.blacklistNumbers + number
                        )
                    }
                },
                onRemove = { number ->
                    scope.launch {
                        SmsSettingsRepository.updateBlacklistNumbers(
                            context,
                            smsSettingsState.blacklistNumbers - number
                        )
                    }
                },
                extraAction = {
                    TextButton(onClick = {
                        openSenderPicker { sender ->
                            scope.launch {
                                SmsSettingsRepository.updateBlacklistNumbers(
                                    context,
                                    smsSettingsState.blacklistNumbers + sender
                                )
                            }
                        }
                    }) {
                        Text(
                            text = "انتخاب از پیامک‌های اخیر",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            SwitchRow(
                label = "فیلتر فرستنده (لیست سفید)",
                checked = smsSettingsState.whitelistEnabled,
                onChange = { enabled ->
                    scope.launch {
                        SmsSettingsRepository.updateWhitelistEnabled(context, enabled)
                    }
                }
            )
            if (smsSettingsState.whitelistEnabled) {
                ListEditorSection(
                    title = "فقط پیامک این شماره‌ها پردازش می‌شود",
                    items = smsSettingsState.whitelistSenders,
                    placeholder = "بخشی از شماره یا نام فرستنده",
                    onAdd = { sender ->
                        scope.launch {
                            SmsSettingsRepository.updateWhitelistSenders(
                                context,
                                smsSettingsState.whitelistSenders + sender
                            )
                        }
                    },
                    onRemove = { sender ->
                        scope.launch {
                            SmsSettingsRepository.updateWhitelistSenders(
                                context,
                                smsSettingsState.whitelistSenders - sender
                            )
                        }
                    },
                    extraAction = {
                        TextButton(onClick = {
                            openSenderPicker { sender ->
                                scope.launch {
                                    SmsSettingsRepository.updateWhitelistSenders(
                                        context,
                                        smsSettingsState.whitelistSenders + sender
                                    )
                                }
                            }
                        }) {
                            Text(
                                text = "انتخاب از پیامک‌های اخیر",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            } else {
                Text(
                    text = "با فعال کردن این گزینه فقط شماره‌های مشخص‌شده بررسی می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // تاریخچه رمزها
        CollapsibleCard(
            title = "تاریخچه رمزها",
            badge = "${history.size}"
        ) {
            HistoryContent(
                history = history,
                onClear = {
                    scope.launch { SmsSettingsRepository.clearHistory(context) }
                },
                onCopyEntry = { code ->
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("sms_code", code))
                    Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                }
            )
        }

        CategoryLabel(text = "کمک و نگهداری", color = Amber)

        // راهنمای باتری
        CollapsibleCard(title = "راهنمای باتری") {
            BatteryGuideContent(
                onOpenBatterySettings = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                }
            )
        }

        // دیالوگ انتخاب فرستنده از پیامک‌های اخیر
        if (pickerVisible) {
            SenderPickerDialog(
                items = pickerSenders,
                onDismiss = { pickerVisible = false },
                onPick = { sender ->
                    pickHandler(sender)
                    pickerVisible = false
                    Toast.makeText(context, "به لیست اضافه شد", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = toPersianDigitsCompat("رمزیار  نسخه 1.0"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** تبدیل ارقام انگلیسی به فارسی برای نمایش */
private fun toPersianDigitsCompat(value: String): String {
    val persian = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return buildString {
        value.forEach { c ->
            append(if (c in '0'..'9') persian[c - '0'] else c)
        }
    }
}

@Composable
private fun HeroHeader(isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF047857), Color(0xFF0D9488), Color(0xFF6366F1))
                )
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        // حلقه‌های تزئینی پس‌زمینه
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-46).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-32).dp, y = 34.dp)
                .size(110.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "رمزیار",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مدیریت هوشمند رمزهای پیامکی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFF86EFAC) else Color.White.copy(alpha = 0.6f)
                            )
                    )
                    Text(
                        text = if (isActive) "فعال" else "خاموش",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    actionText: String,
    accent: Color,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (granted) Success else accent.copy(alpha = 0.55f))
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (granted) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Success.copy(alpha = 0.13f)
            ) {
                Text(
                    text = "فعال",
                    style = MaterialTheme.typography.bodySmall,
                    color = Success,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(50),
                color = Amber.copy(alpha = 0.16f),
                modifier = Modifier.clickable(onClick = onAction)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberDeepCompat
                    )
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberDeepCompat
                    )
                }
            }
        }
    }
}

private val AmberDeepCompat = Color(0xFFB45309)

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
