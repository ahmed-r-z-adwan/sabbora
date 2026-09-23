package com.sabbora.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// A fixed palette rather than Material You. Attendance colours have to mean the same
// thing on every phone: a teacher glancing at a screen of chips is reading colour, and
// wallpaper-derived theming would repaint "absent" differently on each device.
private val Teal = Color(0xFF00696E)
private val TealLight = Color(0xFF9CF1F6)
private val Slate = Color(0xFF4A6365)
private val Sand = Color(0xFF4C6A45)

private val LightScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = Color(0xFF002022),
    secondary = Slate,
    onSecondary = Color.White,
    tertiary = Sand,
    background = Color(0xFFF5FAFB),
    onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF5FAFB),
    onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4849),
    error = Color(0xFFBA1A1A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF80D4DA),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = TealLight,
    secondary = Color(0xFFB1CBCD),
    onSecondary = Color(0xFF1C3438),
    tertiary = Color(0xFFB2CFA8),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDEE3E4),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDEE3E4),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8C9),
    error = Color(0xFFFFB4AB),
)

/** Status colours, resolved against the active scheme so they stay legible in dark mode. */
object AttendanceColors {
    val present: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7ED09A) else Color(0xFF1B6B3A)
    val late: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE8C468) else Color(0xFF8A6100)
    val absent: Color @Composable get() = MaterialTheme.colorScheme.error
    val excused: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

// Slightly larger body text than the Material default: the app is used standing up, in a
// classroom, at arm's length.
private val SabboraTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun SabboraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // enableEdgeToEdge() already makes the bars transparent, so only the icon
            // contrast is set here; assigning statusBarColor is deprecated and a no-op
            // from API 35 on.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = SabboraTypography, content = content)
}
