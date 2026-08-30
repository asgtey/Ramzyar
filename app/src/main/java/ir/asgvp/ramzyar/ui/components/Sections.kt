package ir.asgvp.ramzyar.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.asgvp.ramzyar.settings.HistoryEntry
import ir.asgvp.ramzyar.ui.theme.Amber
import ir.asgvp.ramzyar.ui.theme.AmberDeep
import ir.asgvp.ramzyar.ui.theme.Emerald
import ir.asgvp.ramzyar.ui.theme.SkyBlue
import ir.asgvp.ramzyar.ui.theme.VioletSoft

/**
 * نشان تزئینی گرادیانی؛ برای سرصفحه ردیف‌ها و بخش‌ها
 */
@Composable
fun AccentBadge(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    corner: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(colors))
    )
}

/**
 * برچسب دسته‌بندی به شکل چیپ رنگی بین گروه‌های کارت‌ها
 */
@Composable
fun CategoryLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * کارت استاندارد بخش‌ها؛ گوشه گرد با حاشیه گرادیانی ظریف
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                        )
                    ),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/**
 * کارت با سرصفحه‌ی تاشو؛ برای کم‌کردن شلوغی صفحه
 */
@Composable
fun CollapsibleCard(
    title: String,
    badge: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                AccentBadge(
                    colors = listOf(accent, MaterialTheme.colorScheme.primary),
                    size = 10.dp,
                    corner = 5.dp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            badge?.let { value ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
            ) {
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/**
 * دکمه گرادیانی اصلی برنامه
 */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(Emerald, SkyBlue))
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * بخش قابل استفاده‌مجدد برای مدیریت یک لیست (افزودن/حذف مورد)
 */
@Composable
fun ListEditorSection(
    items: Set<String>,
    placeholder: String,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    title: String? = null,
    extraAction: (@Composable () -> Unit)? = null
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(text = placeholder, style = MaterialTheme.typography.bodySmall)
                }
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = text.isNotBlank()) {
                        if (text.isNotBlank()) {
                            onAdd(text.trim())
                            text = ""
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "افزودن",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f)
                    }
                )
            }
        }

        extraAction?.invoke()

        if (items.isEmpty()) {
            Text(
                text = "موردی ثبت نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            items.sorted().forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onRemove(item) }) {
                            Text(
                                text = "حذف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * دیالوگ انتخاب فرستنده از پیامک‌های اخیر گوشی.
 * برخی فرستنده‌ها به‌جای شماره نام دارند؛ همان نام نشان داده و اضافه می‌شود.
 */
@Composable
fun SenderPickerDialog(
    items: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "بستن") }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccentBadge(colors = listOf(Emerald, SkyBlue), size = 12.dp, corner = 6.dp)
                Text(
                    text = "انتخاب فرستنده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(text = "جستجو...", style = MaterialTheme.typography.bodySmall)
                    }
                )

                val filtered = items.filter {
                    it.contains(query.trim(), ignoreCase = true)
                }

                when {
                    items.isEmpty() -> {
                        Text(
                            text = "پیامکی در صندوق ورودی یافت نشد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    filtered.isEmpty() -> {
                        Text(
                            text = "موردی مطابق جستجو پیدا نشد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered) { sender ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(sender) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                        .copy(alpha = 0.05f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp, vertical = 10.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = sender,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "افزودن +",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "برخی فرستنده‌ها به‌جای شماره، نام دارند؛ همان نام ذخیره می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    )
}

/** تبدیل ارقام انگلیسی به فارسی برای نمایش */
private fun toPersianDigits(value: String): String {
    val persian = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return buildString {
        value.forEach { c ->
            append(if (c in '0'..'9') persian[c - '0'] else c)
        }
    }
}

/**
 * تبدیل میلادی به جلالی با الگوریتم استاندارد (بدون وابستگی خارجی)
 */
private fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var jy = if (gy <= 1600) 0 else 979
    val gy2 = if (gy <= 1600) gy - 621 else gy - 1600
    val gy3 = if (gm > 2) gy2 + 1 else gy2
    var days = 365L * gy2 + (gy3 + 3) / 4 - (gy3 + 99) / 100 +
        (gy3 + 399) / 400 - 80 + gd + gDaysInMonth[gm - 1]
    jy += 33 * (days / 12053).toInt()
    days %= 12053
    jy += 4 * (days / 1461).toInt()
    days %= 1461
    jy += ((days - 1) / 365).toInt()
    if (days > 366) days = (days - 1) % 365
    val jm: Long
    val jd: Long
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return Triple(jy, jm.toInt(), jd.toInt())
}

/** تاریخ و ساعت شمسی بر اساس زمان میلی‌ثانیه‌ای */
private fun formatJalali(timestamp: Long): String {
    return runCatching {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        val (jy, jm, jd) = toJalali(
            gy = cal.get(java.util.Calendar.YEAR),
            gm = cal.get(java.util.Calendar.MONTH) + 1,
            gd = cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        val date = "%04d/%02d/%02d".format(
            java.util.Locale.US, jy, jm, jd
        )
        val time = "%02d:%02d".format(
            java.util.Locale.US,
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE)
        )
        toPersianDigits("$date - $time")
    }.getOrDefault("")
}

/**
 * محتوای تاریخچه رمزهای دریافتی؛ لمس هر مورد آن را کپی می‌کند.
 * هر ردیف نقطه رنگی چرخشی، رمز، برچسب، فرستنده و تاریخ/ساعت شمسی دارد.
 */
@Composable
fun HistoryContent(
    history: List<HistoryEntry>,
    onClear: () -> Unit,
    onCopyEntry: (String) -> Unit
) {
    val accents = listOf(Emerald, VioletSoft, SkyBlue, Amber)

    when {
        history.isEmpty() -> {
            Text(
                text = "هنوز رمزی دریافت نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "لمس هر مورد آن را کپی می‌کند",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                TextButton(onClick = onClear) {
                    Text(
                        text = "پاک کردن همه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            history.forEachIndexed { index, entry ->
                val accent = accents[index % accents.size]
                val metaParts = listOf(
                    entry.sender.ifBlank { null },
                    formatJalali(entry.timestamp).ifBlank { null }
                ).filterNotNull()

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyEntry(entry.code) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccentBadge(
                            colors = listOf(accent, MaterialTheme.colorScheme.primary),
                            size = 38.dp,
                            corner = 13.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.code,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                        .copy(alpha = 0.45f)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "کپی",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * محتوای راهنمای جلوگیری از بسته شدن اپ توسط بهینه‌سازی باتری
 */
@Composable
fun BatteryGuideContent(onOpenBatterySettings: () -> Unit) {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brandTip = when {
        listOf("xiaomi", "redmi", "poco").any { manufacturer.contains(it) } ->
            "شیائومی / ردمی / پوکو: برنامه «امنیت» ← Permissions ← Autostart را برای رمزیار روشن کنید و در تنظیمات باتری حالت «بدون محدودیت» را انتخاب کنید."
        manufacturer.contains("samsung") ->
            "سامسونگ: تنظیمات ← Battery ← Background usage limits ← رمزیار را از فهرست برنامه‌های خواب حذف کنید."
        manufacturer.contains("huawei") || manufacturer.contains("honor") ->
            "هوآوی / آنر: تنظیمات ← Battery ← App launch ← رمزیار ← Manage manually و هر سه کلید را روشن کنید."
        else -> null
    }

    Text(
        text = "برای اینکه رمزیار همیشه پیامک‌ها را دریافت کند، در تنظیمات اندروید مسیر «برنامه‌ها ← رمزیار ← باتری» بروید و «بدون محدودیت» را انتخاب کنید.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )

    brandTip?.let {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Amber.copy(alpha = 0.12f)
        ) {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = AmberDeep,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    Spacer(modifier = Modifier.width(0.dp))

    TextButton(onClick = onOpenBatterySettings) {
        Text(text = "باز کردن تنظیمات باتری", style = MaterialTheme.typography.labelLarge)
    }
}
