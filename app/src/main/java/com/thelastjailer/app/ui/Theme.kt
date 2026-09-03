package com.thelastjailer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The dark cinematic bronze/gold palette used throughout the app — the single source of color roles. */
object JailerColors {
    val Night = Color(0xFF07090D)
    val NightGradientTop = Color(0xFF151A21)
    val Panel = Color(0xFF11151B)
    val Gold = Color(0xFFC79A4A)
    val GoldSoft = Color(0xFF8D6A32)
    val TextPrimary = Color(0xFFE8DFC9)
    val HealthRed = Color(0xFFC25B5B)
    val HonourBlue = Color(0xFF8AA7C2)
}

/**
 * [labelLarge] deliberately has no hardcoded `color`: Material3's `Button`/`OutlinedButton`/
 * `TextButton` all apply this style to their content by default, and a `TextStyle`'s own color
 * always wins over the button's `contentColor` (which is what gives filled, outlined and text
 * buttons their correct, different, contrast-safe text colors). Hardcoding gold here previously
 * made every button's label render gold regardless of its background - invisible on the many
 * buttons whose container is also gold. Any non-button usage that wants labelLarge in gold
 * (section headers, etc.) passes `color = JailerColors.Gold` explicitly at that call site.
 */
private val JailerTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = JailerColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        color = JailerColors.Gold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        color = JailerColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = JailerColors.TextPrimary
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        color = JailerColors.TextPrimary
    )
)

@Composable
fun JailerTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = JailerColors.Gold,
        onPrimary = Color.Black,
        secondary = JailerColors.GoldSoft,
        onSecondary = JailerColors.TextPrimary,
        background = JailerColors.Night,
        onBackground = JailerColors.TextPrimary,
        surface = JailerColors.Panel,
        onSurface = JailerColors.TextPrimary,
        error = JailerColors.HealthRed
    )
    MaterialTheme(colorScheme = colorScheme, typography = JailerTypography, content = content)
}

/** A reusable ornate bordered panel — the one place the app's "gold border on dark panel" look is defined. */
@Composable
fun OrnatePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(JailerColors.Panel, RoundedCornerShape(8.dp))
            .border(1.dp, JailerColors.GoldSoft.copy(alpha = .55f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        content()
    }
}
