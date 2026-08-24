package ir.asgvp.ramzyar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.asgvp.ramzyar.overlay.SmsOverlayService
import ir.asgvp.ramzyar.settings.SmsSettings
import ir.asgvp.ramzyar.settings.SmsSettingsRepository
import ir.asgvp.ramzyar.sms.CodeExtractor
import ir.asgvp.ramzyar.ui.theme.RamzyarTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RamzyarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        activity = this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val smsSettingsState by SmsSettingsRepository.observeSettings(context)
        .collectAsState(
            initial = SmsSettings(
                copyEnabled = SmsSettingsRepository.DEFAULT_COPY_ENABLED,
                appEnabled = SmsSettingsRepository.DEFAULT_APP_ENABLED,
                timerSeconds = SmsSettingsRepository.DEFAULT_TIMER_SECONDS
            )
        )

    var copyEnabled by remember { mutableStateOf(smsSettingsState.copyEnabled) }
    var appEnabled by remember { mutableStateOf(smsSettingsState.appEnabled) }
    var timerSecondsText by remember { mutableStateOf(smsSettingsState.timerSeconds.toString()) }
    var timerFieldFocused by remember { mutableStateOf(false) }
    var timerSaveJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(smsSettingsState) {
        copyEnabled = smsSettingsState.copyEnabled
        appEnabled = smsSettingsState.appEnabled
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

    // هر بار که صفحه (Activity) دوباره Resume شود، وضعیت دسترسی Overlay را به‌روزرسانی کن
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
            .padding(12.dp)
    ) {
        // هدر فشرده
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "رمزیار",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "مدیریت رمزهای پیامکی",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        // بخش دسترسی‌ها
        PermissionsSection(
            smsPermissionGranted = smsPermissionGranted.value,
            overlayGranted = overlayGranted.value,
            onRequestSms = { smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
            onOpenOverlaySettings = {
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

        // بخش تنظیمات
        SettingsSection(
            copyEnabled = copyEnabled,
            appEnabled = appEnabled,
            timerSecondsText = timerSecondsText,
            onCopyEnabledChange = { enabled ->
                copyEnabled = enabled
                scope.launch {
                    SmsSettingsRepository.updateCopyEnabled(context, enabled)
                }
            },
            onTimerSecondsChange = { newText ->
                // فقط رقم مجاز است؛ ذخیره‌سازی با تاخیر تا تایپ کاربر تمام شود
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
            onTimerFocusChange = { focused ->
                timerFieldFocused = focused
                if (!focused) {
                    val value = timerSecondsText.toIntOrNull()
                    if (value != null) {
                        timerSaveJob?.cancel()
                        timerSaveJob = scope.launch {
                            SmsSettingsRepository.updateTimerSeconds(context, value)
                        }
                    } else {
                        timerSecondsText = smsSettingsState.timerSeconds.toString()
                    }
                }
            },
            onAppEnabledToggle = {
                appEnabled = !appEnabled
                scope.launch {
                    SmsSettingsRepository.updateAppEnabled(context, appEnabled)
                }
            }
        )

        // بخش تست
        TestSection(context = context)
    }
}

@Composable
private fun PermissionsSection(
    smsPermissionGranted: Boolean,
    overlayGranted: Boolean,
    onRequestSms: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "پیامک: " + if (smsPermissionGranted) "✓" else "✗",
            style = MaterialTheme.typography.bodyMedium
        )
        PrimaryButton(
            text = "درخواست دسترسی پیامک",
            enabled = !smsPermissionGranted,
            onClick = onRequestSms
        )
        Text(
            text = "Overlay: " + if (overlayGranted) "✓" else "✗",
            style = MaterialTheme.typography.bodyMedium
        )
        PrimaryButton(
            text = "تنظیمات Overlay",
            enabled = !overlayGranted,
            onClick = onOpenOverlaySettings
        )
    }
}

@Composable
private fun SettingsSection(
    copyEnabled: Boolean,
    appEnabled: Boolean,
    timerSecondsText: String,
    onCopyEnabledChange: (Boolean) -> Unit,
    onTimerSecondsChange: (String) -> Unit,
    onTimerFocusChange: (Boolean) -> Unit,
    onAppEnabledToggle: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // سوییچ فعال/غیرفعال بودن اپ
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("فعال بودن رمزیار", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = appEnabled, onCheckedChange = { onAppEnabledToggle() })
        }

        // سوییچ کپی خودکار
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("کپی خودکار", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = copyEnabled, onCheckedChange = onCopyEnabledChange)
        }

        // زمان تایمر: لیبل و فیلد در یک ردیف
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "مدت نمایش (ثانیه)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = timerSecondsText,
                    onValueChange = onTimerSecondsChange,
                    modifier = Modifier
                        .width(72.dp)
                        .onFocusChanged { onTimerFocusChange(it.isFocused) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun TestSection(
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PrimaryButton(
            text = "نمایش تستی رمز",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    context.startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ))
                } else {
                    val sampleMessage = "رمز 1234"
                    val extracted = CodeExtractor.extractSmart(sampleMessage) ?: sampleMessage
                    context.startService(Intent(context, SmsOverlayService::class.java).apply {
                        putExtra(SmsOverlayService.EXTRA_TEXT, extracted)
                    })
                }
            }
        )
        Text(
            text = "هر پیامکی که حاوی کلماتی مانند «رمز پویا»، «رمز یکبار مصرف»، «کد تایید»، «کد فعالسازی» و ... باشد، به‌صورت خودکار شناسایی شده و رمز آن نمایش داده می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    RamzyarTheme {
        // پیش‌نمایش ساده؛ Activity واقعی پاس داده نمی‌شود
        MainScreen(activity = Activity())
    }
}