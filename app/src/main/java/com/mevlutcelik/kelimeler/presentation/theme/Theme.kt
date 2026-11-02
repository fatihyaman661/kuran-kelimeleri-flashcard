package com.mevlutcelik.kelimeler.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mevlutcelik.kelimeler.R

/** Kelimeler ve çekimler için Amiri; ayetler için Kur'an işaretlerini düzgün çizen Amiri Quran. */
val AmiriFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal), Font(R.font.amiri_bold, FontWeight.Bold))
val AmiriQuranFamily = FontFamily(Font(R.font.amiri_quran))

private val Rtl = TextStyle(textDirection = TextDirection.Rtl)

/** Ayarlar → Görünüm'deki yazı boyutu çarpanları. */
data class TextScales(val arabic: Float = 1f, val latin: Float = 1f)

/**
 * [KelimelerTheme] Latin çarpanını yazı ölçeğine (fontScale) uygular; böylece bütün yazılar büyür,
 * dp ölçüleri değişmez. Arapça stiller bunu geri alıp kendi çarpanını uygular: iki ayar bağımsızdır.
 */
val LocalTextScales = staticCompositionLocalOf { TextScales() }

@Composable
@ReadOnlyComposable
private fun arabicFactor(): Float = LocalTextScales.current.let { it.arabic / it.latin }

@Composable
@ReadOnlyComposable
private fun TextStyle.arabic(): TextStyle {
    val factor = arabicFactor()
    return copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)
}

private val HeadwordBase = Rtl.copy(fontFamily = AmiriFamily, fontSize = 56.sp, lineHeight = 96.sp, textAlign = TextAlign.Center)
private val FormBase = Rtl.copy(fontFamily = AmiriFamily, fontSize = 24.sp, lineHeight = 38.sp)
private val InlineBase = Rtl.copy(fontFamily = AmiriFamily, fontSize = 18.sp, lineHeight = 28.sp)

/** Arapça yazı stilleri; boyutları kullanıcının Arapça yazı çarpanıyla ölçeklenir. */
object ArabicStyles {
    /** Kelime sayfasındaki ve kart arkasındaki başlık kelime. */
    val Headword: TextStyle @Composable @ReadOnlyComposable get() = HeadwordBase.arabic()

    /** Çalışma kartının önündeki kelime. */
    val CardWord: TextStyle @Composable @ReadOnlyComposable get() = HeadwordBase.copy(fontSize = 64.sp, lineHeight = 110.sp).arabic()

    val Form: TextStyle @Composable @ReadOnlyComposable get() = FormBase.arabic()

    /** Çekim tablosu hücresi. */
    val TableCell: TextStyle @Composable @ReadOnlyComposable get() =
        FormBase.copy(fontSize = 21.sp, lineHeight = 34.sp, textAlign = TextAlign.Center).arabic()

    val Inline: TextStyle @Composable @ReadOnlyComposable get() = InlineBase.arabic()

    /** Ana sayfa kartındaki ayet imzası. */
    val Signature: TextStyle @Composable @ReadOnlyComposable get() = InlineBase.copy(fontSize = 20.sp).arabic()

    val ListItem: TextStyle @Composable @ReadOnlyComposable get() =
        Rtl.copy(fontFamily = AmiriFamily, fontSize = 26.sp, lineHeight = 40.sp).arabic()

    val Verse: TextStyle @Composable @ReadOnlyComposable get() =
        Rtl.copy(fontFamily = AmiriQuranFamily, fontSize = 24.sp, lineHeight = 52.sp, textAlign = TextAlign.Start).arabic()

    val Prose: TextStyle @Composable @ReadOnlyComposable get() =
        Rtl.copy(fontFamily = AmiriFamily, fontSize = 20.sp, lineHeight = 36.sp, textAlign = TextAlign.Start).arabic()

    /** Türkçe metnin içindeki Arapça (kök, çekim) için. */
    @Composable
    @ReadOnlyComposable
    fun span(size: TextUnit, color: Color = Color.Unspecified) = SpanStyle(fontFamily = AmiriFamily, fontSize = size * arabicFactor(), color = color)
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
fun KelimelerTheme(scales: TextScales = TextScales(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextScales provides scales) {
        WithLatinScale(scales.latin) {
            MaterialTheme(
                colorScheme = KelimelerColorScheme,
                typography = KelimelerTypography,
                content = content,
            )
        }
    }
}

/**
 * Sistem yazı ölçeğinin üstüne [latin] çarpanını uygular. Ayarlardaki önizleme de bunu kaydırıcının
 * anlık değeriyle kullanır; başlangıç noktası her zaman sistem ölçeğidir, iç içe çarpılmaz.
 */
@Composable
fun WithLatinScale(latin: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    val system = LocalConfiguration.current.fontScale
    val scaled = remember(density, system, latin) { Density(density, system * latin) }
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}
