package com.mevlutcelik.kelimeler.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Uygulamanın bütün renkleri. Koyu tema mescit gecesi; açık tema aynı nane vurgusunu krem kâğıt
 * zemine taşır. Nane ve durum renkleri açık zeminde okunsun diye orada koyulaştırılmıştır.
 */
data class KelimelerPalette(
    val isDark: Boolean,
    val primaryMint: Color,
    val primaryMintDark: Color,
    val onMint: Color,
    val background: Color,
    val surface: Color,
    val surfaceDim: Color,
    val surfaceMintDeep: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accentGold: Color,
    val newBlue: Color,
    val learnCoral: Color,
    val hardAmber: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val navInactive: Color,
    val progressTrack: Color,
    val suspended: Color,
    /** Kart içindeki küçük sayı kutuları. */
    val tile: Color,
    /** Halka ve ince ölçerin boş kısmı. */
    val ringTrack: Color,
    /** Meal ve el-Müfredât çevirisi: gövde metninden bir ton sakin. */
    val translation: Color,
    /** Takvimde çalışılmamış gün. */
    val emptyDay: Color,
    val outline: Color,
    val heroGradient: List<Color>,
)

val DarkPalette = KelimelerPalette(
    isDark = true,
    primaryMint = Color(0xFF3DDC97),
    primaryMintDark = Color(0xFF1FA874),
    onMint = Color(0xFF00140D),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1E1E1E),
    surfaceDim = Color(0xFF121212),
    surfaceMintDeep = Color(0xFF002A1C),
    onSurface = Color(0xFFEEEEEE),
    onSurfaceMuted = Color(0xFF888888),
    // Destedeki altın başlık renginin (#9a7b3c) koyu zeminde okunan hali.
    accentGold = Color(0xFFC9A55C),
    newBlue = Color(0xFF7CB7FF),
    learnCoral = Color(0xFFFF8A80),
    hardAmber = Color(0xFFFFC266),
    cardBg = Color(0xFF181818),
    cardBorder = Color(0xFF262626),
    navInactive = Color(0xFF666666),
    progressTrack = Color(0xFF333333),
    suspended = Color(0xFF9AA3AD),
    tile = Color.Black.copy(alpha = 0.2f),
    ringTrack = Color(0xFF0E3B2A),
    translation = Color(0xFFB5B2AB),
    emptyDay = Color(0xFF232323),
    outline = Color(0xFF333333),
    heroGradient = listOf(Color(0xFF0F4F38), Color(0xFF082E21), Color(0xFF051A12)),
)

val LightPalette = KelimelerPalette(
    isDark = false,
    primaryMint = Color(0xFF0E8A5F),
    primaryMintDark = Color(0xFF4CC495),
    onMint = Color.White,
    background = Color(0xFFF6F4EE),
    surface = Color(0xFFECE8DF),
    surfaceDim = Color(0xFFF1EEE6),
    surfaceMintDeep = Color(0xFFD3EFE1),
    onSurface = Color(0xFF1C1B19),
    onSurfaceMuted = Color(0xFF6E6A62),
    accentGold = Color(0xFF9A7B3C),
    newBlue = Color(0xFF2F6FC4),
    learnCoral = Color(0xFFCC4A3E),
    hardAmber = Color(0xFFB07400),
    cardBg = Color.White,
    cardBorder = Color(0xFFE4DFD3),
    navInactive = Color(0xFF9C978D),
    progressTrack = Color(0xFFE2DDD1),
    suspended = Color(0xFF77818C),
    tile = Color(0xFF3B3428).copy(alpha = 0.05f),
    ringTrack = Color(0xFFD6EEE2),
    translation = Color(0xFF55524C),
    emptyDay = Color(0xFFECE8DE),
    outline = Color(0xFFD6D1C5),
    heroGradient = listOf(Color(0xFFCDEEDD), Color(0xFFE2F4EA), Color(0xFFF3FAF6)),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val PrimaryMint: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primaryMint
val PrimaryMintDark: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primaryMintDark
val OnMint: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onMint
val Background: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
val SurfaceDim: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceDim
val OnSurface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onSurface
val OnSurfaceMuted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onSurfaceMuted

/** Bölüm başlıkları, ayet kaynakları. */
val AccentGold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accentGold

// Kart türleri ve cevap butonları
val NewBlue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.newBlue
val LearnCoral: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.learnCoral
val HardAmber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hardAmber
