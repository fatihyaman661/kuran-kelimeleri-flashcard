package com.mevlutcelik.kelimeler.presentation.guide

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mevlutcelik.fsrs.Card
import com.mevlutcelik.fsrs.FsrsParameters
import com.mevlutcelik.fsrs.Rating
import com.mevlutcelik.fsrs.Scheduler
import com.mevlutcelik.fsrs.ZonedDayClock
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.presentation.common.MihrabFrame
import com.mevlutcelik.kelimeler.presentation.common.StatusDot
import com.mevlutcelik.kelimeler.presentation.common.shortDuration
import com.mevlutcelik.kelimeler.presentation.theme.AccentGold
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.HardAmber
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.NewBlue
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Örnek zamanlama: rehberdeki süreler uydurma değil, uygulamanın kendi FSRS hesabından gelir.
// ---------------------------------------------------------------------------------------------

internal object GuideSchedule {
    private val zone = ZoneId.systemDefault()
    private val clock = ZonedDayClock(zone)

    /** Sabit bir başlangıç anı; böylece rehberdeki sayılar her açılışta aynı çıkar. */
    private val start = LocalDate.of(2026, 1, 5).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    /** Rehberde fuzz kapalı: örnekte süreler rastgele oynamasın. */
    fun newScheduler(retention: Double = FsrsParameters().desiredRetention) =
        Scheduler(FsrsParameters(desiredRetention = retention, enableFuzz = false), clock)

    /** Hiç görülmemiş kelime ve onu gördüğün an. */
    fun newCard(): Pair<Card, Long> = Card() to start

    /** Üç kez "İyi" denerek öğrenilmiş, tekrar vakti gelmiş bir kelime. */
    fun reviewCard(scheduler: Scheduler = newScheduler()): Pair<Card, Long> {
        var card = Card()
        var now = start
        repeat(3) {
            card = scheduler.review(card, Rating.Good, now)
            now = card.due
        }
        return card to now
    }

    /** Dört butonun her biri için "kaç milisaniye sonra tekrar gelir". */
    fun intervals(card: Card, now: Long, scheduler: Scheduler = newScheduler()): Map<Rating, Long> =
        scheduler.preview(card, now).mapValues { (_, next) -> next.due - now }

    /** Yeni bir kelimeye her seferinde "İyi" denirse aradaki süreler. */
    fun journey(steps: Int): List<Long> {
        val scheduler = newScheduler()
        var card = Card()
        var now = start
        return List(steps) {
            card = scheduler.review(card, Rating.Good, now)
            val wait = card.due - now
            now = card.due
            wait
        }
    }
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** Düz yazı içindeki süre: "10 dakika", "9 gün", "2 ay". */
internal fun Context.longDuration(millis: Long): String {
    val ms = millis.coerceAtLeast(MINUTE)
    return when {
        ms < HOUR -> getString(R.string.guide_long_minutes, (ms / MINUTE).toInt())
        ms < DAY -> getString(R.string.guide_long_hours, (ms.toDouble() / HOUR).roundToInt())
        ms < 30 * DAY -> getString(R.string.guide_long_days, (ms.toDouble() / DAY).roundToInt())
        ms < 365 * DAY -> getString(R.string.guide_long_months, (ms.toDouble() / (30 * DAY)).roundToInt())
        else -> getString(R.string.guide_long_years, String.format(Locale.forLanguageTag("tr"), "%.1f", ms.toDouble() / (365 * DAY)))
    }
}

internal fun groupedNumber(value: Int): String = String.format(Locale.forLanguageTag("tr"), "%,d", value)

// ---------------------------------------------------------------------------------------------
// Ortak görsel parçalar
// ---------------------------------------------------------------------------------------------

internal data class RatingStyle(val rating: Rating, val label: Int, val color: Color)

@Composable
internal fun ratingStyles(): List<RatingStyle> = listOf(
    RatingStyle(Rating.Again, R.string.rating_again, LearnCoral),
    RatingStyle(Rating.Hard, R.string.rating_hard, HardAmber),
    RatingStyle(Rating.Good, R.string.rating_good, PrimaryMint),
    RatingStyle(Rating.Easy, R.string.rating_easy, NewBlue),
)

/** Çalışma ekranındaki dört cevap butonunun birebir görünümü; [selected] olan vurgulanır. */
@Composable
internal fun MockRatingBar(
    intervals: Map<Rating, Long>,
    selected: Rating?,
    onSelect: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ratingStyles().forEach { style ->
            val active = selected == style.rating
            Surface(
                onClick = { onSelect(style.rating) },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = style.color.copy(alpha = if (active) 0.30f else 0.14f),
                contentColor = OnSurface,
                border = BorderStroke(if (active) 2.dp else 1.dp, style.color.copy(alpha = if (active) 0.9f else 0.4f)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stringResource(style.label), style = MaterialTheme.typography.labelLarge, color = style.color)
                    Text(
                        intervals[style.rating]?.let { context.shortDuration(it) }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                    )
                }
            }
        }
    }
}

/** Kartın türünü gösteren yuvarlak etiket (kartın önündekiyle aynı). */
@Composable
internal fun KindChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f), contentColor = OnSurface) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Görsel üzerindeki numaralı işaret; seçiliyse dolu. */
@Composable
internal fun NumberBadge(number: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier
            .size(24.dp)
            .background(if (selected) PrimaryMint else PrimaryMint.copy(alpha = 0.18f), CircleShape)
            .border(1.dp, PrimaryMint, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = if (selected) OnMint else PrimaryMint,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Bir parçayı açıklamalı göstermek için: seçiliyse nane renkli çerçeve, başka bir parça seçiliyse
 * soluk. Hiçbiri seçili değilse ([anySelected] false) olduğu gibi kalır.
 */
@Composable
internal fun Modifier.guideHighlight(selected: Boolean, anySelected: Boolean = true, shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier {
    val fade by animateFloatAsState(if (!anySelected || selected) 1f else 0.3f, tween(250), label = "fade")
    val border = if (selected) PrimaryMint else Color.Transparent
    return this
        .alpha(fade)
        .border(2.dp, border, shape)
        .padding(4.dp)
}

/** Açıklama kutusu: renkli başlık ve metin. */
@Composable
internal fun ExplainBox(title: String, body: String, color: Color = PrimaryMint, modifier: Modifier = Modifier, footer: @Composable (() -> Unit)? = null) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.08f),
        contentColor = OnSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            footer?.invoke()
        }
    }
}

/** Çalışma kartının önü, küçültülmüş. */
@Composable
internal fun MockCardFront(
    word: String,
    type: String,
    chip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = KelimelerColors.CardBg,
        contentColor = OnSurface,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(PrimaryMint.copy(alpha = 0.35f), KelimelerColors.CardBorder))),
        shadowElevation = 6.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            MihrabFrame(Modifier.align(Alignment.Center).fillMaxWidth(0.8f).fillMaxHeight(0.74f))
            Box(Modifier.align(Alignment.TopCenter).padding(top = 18.dp)) { chip() }
            Column(Modifier.align(Alignment.Center).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(word, style = ArabicStyles.Headword)
                Text(type, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, textAlign = TextAlign.Center)
                extra()
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TouchApp, contentDescription = null, tint = KelimelerColors.NavInactive, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.study_tap_to_reveal), style = MaterialTheme.typography.bodySmall, color = KelimelerColors.NavInactive)
            }
        }
    }
}

/** Dokununca dönen küçük kart: önde harekesiz kelime, arkada harekeli hâli ve anlamı. */
@Composable
internal fun MiniFlipCard(revealed: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, height: Dp = 260.dp) {
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(520, easing = FastOutSlowInEasing), label = "flip")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
    ) {
        if (rotation <= 90f) {
            MockCardFront(
                word = GuideWord.word,
                type = GuideWord.type,
                chip = { KindChip(stringResource(R.string.kind_new), NewBlue) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                shape = RoundedCornerShape(28.dp),
                color = KelimelerColors.CardBg,
                contentColor = OnSurface,
                border = BorderStroke(1.dp, KelimelerColors.CardBorder),
                shadowElevation = 6.dp,
            ) {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(GuideWord.vocalized, style = ArabicStyles.Headword, color = PrimaryMint)
                    Text(GuideWord.category, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        GuideWord.meaning,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Unutma eğrisi
// ---------------------------------------------------------------------------------------------

/** Eğrinin yatay ekseni: gün. */
internal const val CurveDays = 40f

/** Tekrar günleri ve her tekrardan sonraki kararlılık (hatırlamanın %90'a inmesi için geçen gün). */
internal val CurveReviews = listOf(0f to 1f, 1f to 3f, 4f to 8f, 12f to 20f, 32f to 50f)

private fun retention(t: Float, since: Float, stability: Float) = 0.9f.pow((t - since) / stability)

/**
 * Hiç tekrar edilmeyen kelimenin hızla düşen eğrisi ile, hatırlama %90'a inince tekrar edilen
 * kelimenin giderek yavaşlayan eğrisi. Açılınca soldan sağa çizilir; [replayKey] değişince baştan.
 */
@Composable
internal fun ForgettingCurve(replayKey: Int, modifier: Modifier = Modifier) {
    val progress = remember(replayKey) { Animatable(0f) }
    LaunchedEffect(replayKey) { progress.animateTo(1f, tween(2600, easing = LinearEasing)) }
    val mint = PrimaryMint
    val coral = LearnCoral
    val gold = AccentGold
    val grid = KelimelerColors.CardBorder
    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        val w = size.width
        val h = size.height
        fun x(t: Float) = t / CurveDays * w
        fun y(r: Float) = (1f - r) * h

        // Izgara: %100, %50 ve taban.
        listOf(1f, 0.5f, 0f).forEach { r -> drawLine(grid, Offset(0f, y(r)), Offset(w, y(r)), strokeWidth = 1.dp.toPx()) }
        // Hedef: %90.
        drawLine(
            gold,
            Offset(0f, y(0.9f)),
            Offset(w, y(0.9f)),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )

        val steps = 200
        val forget = Path().apply {
            moveTo(x(0f), y(1f))
            for (i in 1..steps) {
                val t = CurveDays * i / steps
                lineTo(x(t), y(retention(t, 0f, 1f)))
            }
        }
        // Her tekrar arası ayrı bir parça; tekrar anında eğri dikine %100'e sıçrar.
        val spaced = Path().apply {
            moveTo(x(0f), y(1f))
            CurveReviews.forEachIndexed { index, (since, stability) ->
                if (since > CurveDays) return@forEachIndexed
                val end = (CurveReviews.getOrNull(index + 1)?.first ?: CurveDays).coerceAtMost(CurveDays)
                lineTo(x(since), y(1f))
                val segmentSteps = ((end - since) / CurveDays * steps).roundToInt().coerceAtLeast(2)
                for (i in 1..segmentSteps) {
                    val t = since + (end - since) * i / segmentSteps
                    lineTo(x(t), y(retention(t, since, stability)))
                }
            }
        }
        clipRect(right = w * progress.value) {
            drawPath(forget, coral.copy(alpha = 0.8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
            drawPath(spaced, mint, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            CurveReviews.drop(1).forEach { (t, _) ->
                if (t <= CurveDays) drawCircle(mint, radius = 5.dp.toPx(), center = Offset(x(t), y(1f)))
            }
        }
    }
}
