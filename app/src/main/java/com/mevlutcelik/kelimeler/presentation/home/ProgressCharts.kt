package com.mevlutcelik.kelimeler.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.DayCount
import com.mevlutcelik.kelimeler.presentation.common.percent
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMintDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Halkanın boş kısmı: aynı nane tonunun koyu basamağı, böylece halkanın tamamı tek ölçek gibi okunur. */
private val RingTrack = Color(0xFF0E3B2A)

/** Öğrenilen kelime oranı: tek değerli halka ölçer, ortada büyük yüzde. */
@Composable
fun LearnedRing(fraction: Float, modifier: Modifier = Modifier, diameter: Dp = 136.dp, stroke: Dp = 14.dp) {
    val animated by animateFloatAsState(fraction, tween(900, easing = FastOutSlowInEasing), label = "ring")

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val width = stroke.toPx()
            val arcSize = Size(size.width - width, size.height - width)
            val topLeft = Offset(width / 2, width / 2)
            drawArc(RingTrack, -90f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width))
            if (animated > 0f) {
                drawArc(
                    Brush.sweepGradient(listOf(PrimaryMintDark, PrimaryMint, PrimaryMintDark)), -90f, 360f * animated.coerceAtMost(1f), useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                percent(fraction),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.home_learned_percent_label), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        }
    }
}

/** Tek değer için ince yatay ölçer (metin kapsamı). */
@Composable
fun Meter(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(RingTrack, cornerRadius = r)
        if (fraction > 0f) {
            drawRoundRect(PrimaryMint, size = Size(maxOf(size.height, size.width * fraction.coerceAtMost(1f)), size.height), cornerRadius = r)
        }
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("tr"))

/**
 * Günlük tekrar sayıları için sütun grafik: tek seri (açıklama kutusu yok, başlık söylüyor),
 * ince sütunlar, yalnızca üst köşesi yuvarlak, ince ızgara. Sütuna dokununca o günün değeri
 * grafiğin üstündeki satırda görünür.
 */
@Composable
fun DailyReviewsChart(days: List<DayCount>, modifier: Modifier = Modifier) {
    DayBarChart(
        days = days,
        summary = { total -> stringResource(R.string.home_activity_total, total) },
        description = { total, peak -> stringResource(R.string.home_activity_description, total, peak) },
        startLabel = LocalDate.ofEpochDay(days.first().day).format(dayFormat),
        endLabel = stringResource(R.string.home_activity_today),
        modifier = modifier,
    )
}

/**
 * Günlük sütun grafiği: bir sütuna dokununca o günün sayısı üstteki satırda görünür. Son 30 gün
 * (ana sayfa) ve gelecek 30 günün tekrar tahmini (istatistik) bunu kullanır.
 */
@Composable
fun DayBarChart(
    days: List<DayCount>,
    summary: @Composable (total: Int) -> String,
    description: @Composable (total: Int, peak: Int) -> String,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(PrimaryMint, PrimaryMintDark),
) {
    var selected by remember(days) { mutableStateOf<Int?>(null) }
    val total = days.sumOf { it.reviews }
    val peak = days.maxOfOrNull { it.reviews } ?: 0
    val scaleMax = niceCeiling(peak)
    val descriptionText = description(total, peak)

    Column(modifier) {
        val pick = selected
        Text(
            if (pick == null) summary(total)
            else stringResource(R.string.home_activity_day, LocalDate.ofEpochDay(days[pick].day).format(dayFormat), days[pick].reviews),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Canvas(
                Modifier
                    .weight(1f)
                    .height(112.dp)
                    .semantics { contentDescription = descriptionText }
                    .pointerInput(days) {
                        detectTapGestures { offset ->
                            val index = (offset.x / (size.width.toFloat() / days.size)).toInt().coerceIn(days.indices)
                            selected = if (selected == index) null else index
                        }
                    },
            ) {
                val grid = KelimelerColors.CardBorder
                val hairline = 1.dp.toPx()
                drawLine(grid, Offset(0f, 0f), Offset(size.width, 0f), hairline)
                drawLine(grid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), hairline)
                drawLine(grid, Offset(0f, size.height - hairline / 2), Offset(size.width, size.height - hairline / 2), hairline)

                val slot = size.width / days.size
                val barWidth = min(24.dp.toPx(), slot - 2.dp.toPx())
                val radius = min(4.dp.toPx(), barWidth / 2)
                days.forEachIndexed { i, day ->
                    if (day.reviews == 0) return@forEachIndexed
                    val height = size.height * day.reviews / scaleMax
                    val left = i * slot + (slot - barWidth) / 2
                    val bar = RoundRect(
                        left = left, top = size.height - height, right = left + barWidth, bottom = size.height,
                        topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                    )
                    val dimmed = selected != null && selected != i
                    val brush = Brush.verticalGradient(colors, startY = bar.top, endY = bar.bottom)
                    drawPath(Path().apply { addRoundRect(bar) }, brush, alpha = if (dimmed) 0.35f else 1f)
                }
            }
            // Y ekseni: yalnızca üst ve orta ızgara çizgisinin değeri, sessiz metinle.
            Column(Modifier.height(112.dp).padding(start = 6.dp)) {
                AxisLabel(compact(scaleMax))
                Spacer(Modifier.weight(1f))
                AxisLabel(compact(scaleMax / 2))
                Spacer(Modifier.weight(1f))
                AxisLabel("0")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            AxisLabel(startLabel)
            Spacer(Modifier.weight(1f))
            AxisLabel(endLabel)
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp), color = OnSurfaceMuted)
}

/** Ekseni temiz bir sayıya yuvarlar: 1, 2, 5 × 10ⁿ (en az 4, ortası tam sayı olsun diye çift). */
private fun niceCeiling(value: Int): Int {
    if (value <= 4) return 4
    val magnitude = 10.0.pow(kotlin.math.floor(log10(value.toDouble())))
    val step = listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= value }
    val nice = ceil(step * magnitude).toInt()
    return if (nice % 2 == 1) nice + 1 else nice
}

private fun compact(value: Int) = if (value >= 1000) "${value / 1000}B" else value.toString()
