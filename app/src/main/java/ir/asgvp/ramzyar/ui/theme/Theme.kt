package ir.asgvp.ramzyar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection

// پالت رنگی روشن رمزیار
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00A170),
    secondary = Color(0xFF00897B),
    tertiary = Pink40,
    background = Color(0xFFF4FFF9),
    surface = Color(0xFFF4FFF9)
)

// پالت رنگی تیره رمزیار (بک‌گراند سرمه‌ای تیره و متن روشن)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),      // آبی روشن برای دکمه‌ها
    secondary = Color(0xFF22C55E),    // سبز روشن برای تاکید
    tertiary = Pink80,
    background = Color(0xFF020617),   // سرمه‌ای خیلی تیره (near black navy)
    surface = Color(0xFF020617),
    onBackground = Color(0xFFE5E7EB), // خاکستری خیلی روشن برای متن
    onSurface = Color(0xFFE5E7EB)
)

@Composable
fun RamzyarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}