package com.mevlutcelik.kelimeler.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.DayActivity
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.common.hijriMonthShort
import com.mevlutcelik.kelimeler.presentation.common.toHijri
import com.mevlutcelik.kelimeler.presentation.theme.AccentGold
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerPalette
import com.mevlutcelik.kelimeler.presentation.theme.LocalPalette
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

private val CellSize = 13.dp
private val CellGap = 3.dp
private val MonthRow = 16.dp

/** Son haftaya düşen ay adı kesilmesin diye takvimin sağında bırakılan pay. */
private val LabelOverflow = 36.dp
private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("tr"))

/** Boş gün + dört yoğunluk; en yoğun güne göre çeyrekler. */
private fun KelimelerPalette.levelColor(count: Int, peak: Int): Color {
    if (count <= 0 || peak <= 0) return emptyDay
    val level = ceil(4.0 * count / peak).toInt().coerceIn(1, 4)
    return primaryMint.copy(alpha = listOf(0.25f, 0.45f, 0.7f, 1f)[level - 1])
}

/**
 * GitHub'ın katkı grafiği gibi çalışma takvimi: son 53 hafta, her sütun bir hafta (Pazartesi üstte).
 * Miladi aylar üstte, hicrî aylar altta yazılır. Yatay kaydırılır, açılışta bugünün olduğu uca gelir;
 * bir kareye dokununca [onSelect] o günü verir.
 */
@Composable
fun ActivityHeatmap(activity: Map<Long, DayActivity>, today: Long, selected: Long?, onSelect: (Long) -> Unit) {
    val todayDate = LocalDate.ofEpochDay(today)
    val firstMonday = todayDate.minusWeeks(52).with(DayOfWeek.MONDAY)
    val start = firstMonday.toEpochDay()
    val weeks = (ChronoUnit.WEEKS.between(firstMonday, todayDate.with(DayOfWeek.MONDAY)) + 1).toInt()
    val peak = activity.values.filter { it.day >= start }.maxOfOrNull { it.reviews } ?: 0
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = OnSurfaceMuted)
    val hijriStyle = labelStyle.copy(color = AccentGold)
    val palette = LocalPalette.current
    // Hicrî ayın 1'i hangi haftaya düşüyorsa ay adı o sütunun altına.
    val hijriLabels = remember(start, weeks) {
        (0 until weeks).mapNotNull { column ->
            (0..6).firstNotNullOfOrNull { row ->
                val hijri = LocalDate.ofEpochDay(start + column * 7L + row).toHijri()
                if (hijri.get(ChronoField.DAY_OF_MONTH) == 1) column to hijriMonthShort(hijri.get(ChronoField.MONTH_OF_YEAR)) else null
            }
        }
    }
    val description = stringResource(R.string.stats_calendar_description)

    Column {
        Row {
            // Hafta günü etiketleri sabit kalır; yalnızca takvim kayar.
            Column(Modifier.padding(top = MonthRow, end = 6.dp), verticalArrangement = Arrangement.spacedBy(CellGap)) {
                listOf("Pzt", "", "Çar", "", "Cum", "", "").forEach {
                    Box(Modifier.height(CellSize), contentAlignment = Alignment.CenterStart) {
                        Text(it, style = labelStyle)
                    }
                }
            }
            Box(Modifier.horizontalScroll(scroll)) {
                Canvas(
                    Modifier
                        .size(width = (CellSize + CellGap) * weeks + LabelOverflow, height = MonthRow * 2 + (CellSize + CellGap) * 7)
                        .semantics { contentDescription = description }
                        .pointerInput(start, weeks) {
                            detectTapGestures { offset ->
                                val step = (CellSize + CellGap).toPx()
                                val column = (offset.x / step).toInt()
                                val row = ((offset.y - MonthRow.toPx()) / step).toInt()
                                if (row !in 0..6 || column !in 0 until weeks) return@detectTapGestures
                                val day = start + column * 7L + row
                                if (day <= today) onSelect(day)
                            }
                        },
                ) {
                    val step = (CellSize + CellGap).toPx()
                    val cell = CellSize.toPx()
                    val top = MonthRow.toPx()
                    val radius = CornerRadius(3.dp.toPx())
                    var lastMonth = -1
                    for (column in 0 until weeks) {
                        val weekStart = LocalDate.ofEpochDay(start + column * 7L)
                        // Ay etiketi, ayın ilk tam haftasının üstüne.
                        if (weekStart.monthValue != lastMonth && weekStart.dayOfMonth <= 7) {
                            drawText(measurer, weekStart.format(monthFormat), Offset(column * step, 0f), labelStyle)
                            lastMonth = weekStart.monthValue
                        }
                        for (row in 0..6) {
                            val day = start + column * 7L + row
                            if (day > today) continue
                            val origin = Offset(column * step, top + row * step)
                            drawRoundRect(palette.levelColor(activity[day]?.reviews ?: 0, peak), origin, Size(cell, cell), radius)
                            if (day == selected) {
                                drawRoundRect(palette.onSurface, origin, Size(cell, cell), radius, style = Stroke(1.5.dp.toPx()))
                            }
                        }
                    }
                    val hijriTop = top + 7 * step + 2.dp.toPx()
                    hijriLabels.forEach { (column, name) ->
                        drawText(measurer, name, Offset(column * step, hijriTop), hijriStyle)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.stats_less), style = labelStyle)
            Spacer(Modifier.width(6.dp))
            listOf(0, 1, 2, 3, 4).forEach { level ->
                Box(
                    Modifier
                        .padding(horizontal = 1.5.dp)
                        .size(11.dp)
                        .background(palette.levelColor(level, 4), RoundedCornerShape(2.5.dp)),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.stats_more), style = labelStyle)
        }
    }
}
