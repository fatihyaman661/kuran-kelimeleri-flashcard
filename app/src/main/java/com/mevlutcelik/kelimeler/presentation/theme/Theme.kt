package com.mevlutcelik.kelimeler.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.mevlutcelik.kelimeler.R

/** Kelimeler ve çekimler için Amiri; ayetler için Kur'an işaretlerini düzgün çizen Amiri Quran. */
val AmiriFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal), Font(R.font.amiri_bold, FontWeight.Bold))
val AmiriQuranFamily = FontFamily(Font(R.font.amiri_quran))

private val Rtl = TextStyle(textDirection = TextDirection.Rtl)

object ArabicStyles {
    /** Kartın ön yüzündeki kelime. */
    val Headword = Rtl.copy(fontFamily = AmiriFamily, fontSize = 56.sp, lineHeight = 96.sp, textAlign = TextAlign.Center)
    val Form = Rtl.copy(fontFamily = AmiriFamily, fontSize = 24.sp, lineHeight = 38.sp)
    val Inline = Rtl.copy(fontFamily = AmiriFamily, fontSize = 18.sp, lineHeight = 28.sp)
    val ListItem = Rtl.copy(fontFamily = AmiriFamily, fontSize = 26.sp, lineHeight = 40.sp)
    val Verse = Rtl.copy(fontFamily = AmiriQuranFamily, fontSize = 24.sp, lineHeight = 52.sp, textAlign = TextAlign.Start)
    val Prose = Rtl.copy(fontFamily = AmiriFamily, fontSize = 20.sp, lineHeight = 36.sp, textAlign = TextAlign.Start)
}

val KelimelerColorScheme = darkColorScheme(
    primary          = PrimaryMint,
    onPrimary        = OnMint,
    primaryContainer = SurfaceMintDeep,
    onPrimaryContainer = PrimaryMint,
    secondary        = AccentGold,
    onSecondary      = Color.Black,
    surface          = SurfaceDim,
    surfaceVariant   = Surface,
    surfaceContainer = SurfaceDim,
    surfaceContainerHigh = Surface,
    background       = Background,
    onBackground     = OnSurface,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceMuted,
    outline          = Color(0xFF333333),
    outlineVariant   = Color(0xFF262626),
)

val KelimelerTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    ),
)

object KelimelerColors {
    val CardBg = Color(0xFF181818)
    val CardBorder = Color(0xFF262626)
    val NavBarBg = Color(0xFF111111)
    val NavBarBorder = Color(0xFF222222)
    val NavInactive = Color(0xFF666666)
    val ProgressTrack = Color(0xFF333333)
    val HeroBorder = Color(0xFF0E4A33)
    val Highlight = PrimaryMint

    /** Askıdaki kelime: diğer durum renklerinden ayrı, sakin bir gri. */
    val Suspended = Color(0xFF9AA3AD)
}

@Composable
fun KelimelerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KelimelerColorScheme,
        typography = KelimelerTypography,
        content = content,
    )
}
