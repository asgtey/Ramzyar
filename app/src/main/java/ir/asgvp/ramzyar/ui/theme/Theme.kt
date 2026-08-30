package ir.asgvp.ramzyar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// پالت روشن: پس‌زمینه نعنایی ملایم با تاکید زمردی و بنفش
private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F3DF),
    secondary = Violet,
    onSecondary = Color.White,
    tertiary = Amber,
    background = MintBg,
    onBackground = InkOnMint,
    surface = MintBg,
    onSurface = InkOnMint,
    surfaceVariant = Color(0xFFDFF3EA),
    onSurfaceVariant = Color(0xFF37544B),
    outline = Color(0xFF9DBFB3),
    error = Rose
)

// پالت تیره: سرمه‌ای عمیق با زمردی درخشان و بنفش روشن
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF39D98A),
    onPrimary = Color(0xFF04291B),
    primaryContainer = Color(0xFF0E4433),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF171233),
    tertiary = Color(0xFFFBBF24),
    background = NavyInk,
    onBackground = InkOnNavy,
    surface = NavyInk,
    onSurface = InkOnNavy,
    surfaceVariant = Color(0xFF151D31),
    onSurfaceVariant = Color(0xFFB7C3D6),
    outline = Color(0xFF3D4A63),
    error = Color(0xFFFF7085)
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
