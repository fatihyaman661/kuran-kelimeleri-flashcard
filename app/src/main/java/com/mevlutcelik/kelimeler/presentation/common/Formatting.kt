package com.mevlutcelik.kelimeler.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.mevlutcelik.fsrs.Phase
import com.mevlutcelik.kelimeler.R
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.roundToInt

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** Butonların üstündeki ve listedeki kısa süreler: "10 dk", "3 g", "2 ay", "1,5 yıl". */
fun Context.shortDuration(millis: Long): String {
    val ms = millis.coerceAtLeast(MINUTE)
    return when {
        ms < HOUR -> getString(R.string.duration_minutes, (ms / MINUTE).toInt())
        ms < DAY -> getString(R.string.duration_hours, (ms.toDouble() / HOUR).roundToInt())
        ms < 30 * DAY -> getString(R.string.duration_days, (ms.toDouble() / DAY).roundToInt())
        ms < 365 * DAY -> getString(R.string.duration_months, (ms.toDouble() / (30 * DAY)).roundToInt())
        else -> getString(R.string.duration_years, String.format(Locale.forLanguageTag("tr"), "%.1f", ms.toDouble() / (365 * DAY)))
    }
}

/** "14 dk önce", "9 g sonra"; uygulama Türkçe olduğu için cihaz dilinden bağımsız. */
fun Context.relativeTime(time: Long, now: Long = System.currentTimeMillis()): String {
    val diff = time - now
    return when {
        kotlin.math.abs(diff) < MINUTE -> getString(R.string.time_now)
        diff > 0 -> getString(R.string.time_in, shortDuration(diff))
        else -> getString(R.string.time_ago, shortDuration(-diff))
    }
}

/** Yüzde: %10'un altında bir ondalıkla ("%0,1"), üstünde tam sayı. */
fun percent(fraction: Float): String {
    val value = fraction * 100
    return if (value > 0f && value < 10f) "%" + String.format(Locale.forLanguageTag("tr"), "%.1f", value) else "%${value.roundToInt()}"
}

private val hijriMonths = listOf(
    "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir",
    "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce",
)

/** Takvimin dar ay satırı için; uzun adlar dört haftalık genişliğe sığmıyor. */
private val hijriMonthsShort = listOf(
    "Muharrem", "Safer", "R.evvel", "R.ahir", "C.evvel", "C.ahir",
    "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce",
)

/**
 * Hicrî tarih Ümmü'l-Kurâ takvimiyle hesaplanır (java.time'ın HijrahDate'i); Diyanet'in ilan ettiği
 * ay başıyla bir gün farklı olabilir.
 */
fun LocalDate.toHijri(): HijrahDate = HijrahDate.from(this)

/** "11 Cemaziyelevvel 1448" */
fun hijriDate(date: LocalDate): String {
    val h = date.toHijri()
    return "${h.get(ChronoField.DAY_OF_MONTH)} ${hijriMonths[h.get(ChronoField.MONTH_OF_YEAR) - 1]} ${h.get(ChronoField.YEAR)}"
}

/** Hicrî ayın kısa adı; [month] 1..12. */
fun hijriMonthShort(month: Int): String = hijriMonthsShort[month - 1]

@StringRes
fun Phase.label(): Int = when (this) {
    Phase.New -> R.string.phase_new
    Phase.Learning -> R.string.phase_learning
    Phase.Review -> R.string.phase_review
    Phase.Relearning -> R.string.phase_relearning
}
