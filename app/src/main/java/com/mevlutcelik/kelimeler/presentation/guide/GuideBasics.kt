package com.mevlutcelik.kelimeler.presentation.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mevlutcelik.fsrs.Rating
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.settings.StudySettings
import com.mevlutcelik.kelimeler.presentation.common.ContentCard
import com.mevlutcelik.kelimeler.presentation.common.HeroBackground
import com.mevlutcelik.kelimeler.presentation.common.IconBadge
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
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
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// 1 ─ Uygulamanın amacı ------------------------------------------------------------------------

@Composable
internal fun WelcomePage() {
    GuidePageColumn {
        HeroBackground(RoundedCornerShape(24.dp), Modifier.fillMaxWidth(), skylineBias = 1f) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.home_hero_verse), style = ArabicStyles.Signature, color = OnSurface)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.guide_welcome_title), style = MaterialTheme.typography.titleLarge, color = OnSurface)
            }
        }
        GuideBody(stringResource(R.string.guide_welcome_body, groupedNumber(GuideTotalWords)))
        ContentCard {
            SectionLabel(stringResource(R.string.guide_coverage_label))
            Spacer(Modifier.height(10.dp))
            GuideCoverage.forEachIndexed { index, (count, percent) ->
                CoverageRow(count, percent, delayMillis = 250L * index)
            }
        }
        GuideNote(stringResource(R.string.guide_coverage_note, GuideCoverage.first { it.first == 100 }.second.toTenths()))
    }
}

/** %71 → "7": "her 10 kelimeden yaklaşık 7'si". */
private fun Int.toTenths() = (this / 10f).roundToInt()

@Composable
private fun CoverageRow(count: Int, percent: Int, delayMillis: Long) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        fill.animateTo(percent / 100f, tween(900, easing = FastOutSlowInEasing))
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.guide_coverage_first, groupedNumber(count)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("%${(fill.value * 100).roundToInt()}", style = MaterialTheme.typography.titleMedium, color = PrimaryMint, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).background(KelimelerColors.ProgressTrack, RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth(fill.value).fillMaxHeight().background(PrimaryMint, RoundedCornerShape(5.dp)))
        }
    }
}

// 2 ─ Sıklık sırası ----------------------------------------------------------------------------

@Composable
internal fun FrequencyPage() {
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_frequency_title))
        GuideBody(stringResource(R.string.guide_frequency_body))
        ContentCard {
            val max = GuideTopWords.maxOf { it.count }.toFloat()
            GuideTopWords.forEachIndexed { index, word ->
                FrequentRow(word, word.count / max, delayMillis = 120L * index)
            }
            Spacer(Modifier.height(4.dp))
            Text("⋮", style = MaterialTheme.typography.titleMedium, color = OnSurfaceMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
            FrequentRow(GuideFrequent(GuideWord.rank, GuideWord.vocalized, GuideWord.type, GuideWord.frequency), GuideWord.frequency / max, delayMillis = 900L)
        }
        GuideNote(stringResource(R.string.guide_frequency_note))
    }
}

@Composable
private fun FrequentRow(word: GuideFrequent, fraction: Float, delayMillis: Long) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        fill.animateTo(fraction, tween(700, easing = FastOutSlowInEasing))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#${word.rank}", style = MaterialTheme.typography.labelLarge, color = AccentGold, modifier = Modifier.width(40.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word.type, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.guide_times, groupedNumber(word.count)), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
            }
            Box(Modifier.fillMaxWidth().padding(top = 3.dp).height(6.dp).background(KelimelerColors.ProgressTrack, RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxWidth(fill.value).fillMaxHeight().background(PrimaryMint.copy(alpha = 0.8f), RoundedCornerShape(3.dp)))
            }
        }
        Text(word.ar, style = ArabicStyles.Inline.copy(fontSize = 24.sp, lineHeight = 36.sp), modifier = Modifier.widthIn(min = 72.dp).padding(start = 12.dp), textAlign = TextAlign.End)
    }
}

// 3 ─ Aralıklı tekrar ---------------------------------------------------------------------------

@Composable
internal fun SpacingPage() {
    var replay by remember { mutableIntStateOf(0) }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_spacing_title))
        GuideBody(stringResource(R.string.guide_spacing_body))
        ContentCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.guide_curve_label), Modifier.weight(1f))
                TextButton(onClick = { replay++ }) {
                    Text(stringResource(R.string.guide_replay), color = PrimaryMint)
                }
            }
            Row {
                Column(Modifier.height(190.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf("%100", "%50", "%0").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted) }
                }
                ForgettingCurve(replay, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(start = 34.dp, top = 4.dp)) {
                Text(stringResource(R.string.guide_curve_today), style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.guide_curve_days, CurveDays.roundToInt()), style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
            }
            Spacer(Modifier.height(10.dp))
            LegendRow(LearnCoral, stringResource(R.string.guide_curve_forget), dashed = true)
            LegendRow(PrimaryMint, stringResource(R.string.guide_curve_spaced), dashed = false)
            LegendRow(AccentGold, stringResource(R.string.guide_curve_target), dashed = true)
            Spacer(Modifier.height(10.dp))
            val context = LocalContext.current
            val gaps = CurveReviews.zipWithNext { a, b -> (b.first - a.first).roundToInt() }
            Text(
                stringResource(R.string.guide_curve_gaps, gaps.joinToString(" → ") { context.getString(R.string.guide_long_days, it) }),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMuted,
            )
        }
        GuideBody(stringResource(R.string.guide_spacing_body2))
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, text: String, dashed: Boolean) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.width(28.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(if (dashed) 3 else 1) {
                Box(Modifier.weight(1f).height(3.dp).background(color, RoundedCornerShape(2.dp)))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = OnSurface)
    }
}

// 4 ─ Bir kartın akışı ---------------------------------------------------------------------------

@Composable
internal fun FlowPage() {
    var revealed by rememberSaveable { mutableStateOf(false) }
    var answered by rememberSaveable { mutableStateOf(false) }
    val (card, now) = remember { GuideSchedule.newCard() }
    val intervals = remember { GuideSchedule.intervals(card, now) }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_flow_title))
        StepRow(1, Icons.Filled.Visibility, stringResource(R.string.guide_flow_step1))
        StepRow(2, Icons.Filled.TouchApp, stringResource(R.string.guide_flow_step2))
        StepRow(3, Icons.Filled.Psychology, stringResource(R.string.guide_flow_step3))
        StepRow(4, Icons.Filled.CheckCircle, stringResource(R.string.guide_flow_step4))
        SectionLabel(stringResource(R.string.guide_try_it))
        MiniFlipCard(revealed, onToggle = { revealed = !revealed; answered = false })
        AnimatedVisibility(visible = revealed, enter = fadeIn() + slideInVertically { it / 2 }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GuideNote(stringResource(R.string.guide_flow_rate_now))
                MockRatingBar(intervals, selected = null, onSelect = { answered = true; revealed = false })
            }
        }
        if (answered) {
            ExplainBox(stringResource(R.string.guide_flow_done_title), stringResource(R.string.guide_flow_done_body))
        } else if (!revealed) {
            GuideNote(stringResource(R.string.guide_flow_tap_card), Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun StepRow(number: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberBadge(number, selected = true)
        Spacer(Modifier.width(10.dp))
        IconBadge(icon, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

// 5 ─ Dört buton ---------------------------------------------------------------------------------

@Composable
internal fun ButtonsPage() {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(Rating.Good) }
    val (card, now) = remember { GuideSchedule.reviewCard() }
    val intervals = remember { GuideSchedule.intervals(card, now) }
    val style = ratingStyles().first { it.rating == selected }
    val (title, body) = when (selected) {
        Rating.Again -> R.string.guide_again_title to R.string.guide_again_body
        Rating.Hard -> R.string.guide_hard_title to R.string.guide_hard_body
        Rating.Good -> R.string.guide_good_title to R.string.guide_good_body
        Rating.Easy -> R.string.guide_easy_title to R.string.guide_easy_body
    }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_buttons_title))
        GuideBody(stringResource(R.string.guide_buttons_body))
        SectionLabel(stringResource(R.string.guide_tap_a_button))
        MockRatingBar(intervals, selected, onSelect = { selected = it })
        ExplainBox(
            title = "${stringResource(style.label)} · ${stringResource(title)}",
            body = stringResource(body),
            color = style.color,
            footer = {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.guide_button_result, intervals[selected]?.let { context.longDuration(it) }.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = style.color,
                )
            },
        )
        TipBox(stringResource(R.string.guide_buttons_tip))
    }
}

@Composable
internal fun TipBox(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AccentGold.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)),
        contentColor = OnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.Info, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// 6 ─ Butonların altındaki süreler --------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DurationsPage() {
    val context = LocalContext.current
    val (card, now) = remember { GuideSchedule.reviewCard() }
    val good = remember { GuideSchedule.intervals(card, now)[Rating.Good] ?: 0L }
    val journey = remember { GuideSchedule.journey(7) }
    var replay by remember { mutableIntStateOf(0) }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_durations_title))
        GuideBody(stringResource(R.string.guide_durations_body, context.shortDuration(good), context.longDuration(good)))
        // Büyütülmüş tek buton ve açıklaması.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.width(110.dp).height(76.dp),
                shape = RoundedCornerShape(20.dp),
                color = PrimaryMint.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.4f)),
                contentColor = OnSurface,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.rating_good), style = MaterialTheme.typography.titleMedium, color = PrimaryMint)
                    Text(
                        context.shortDuration(good),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(HardAmber.copy(alpha = 0.25f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp),
                    )
                }
            }
            Text("  ←  ", style = MaterialTheme.typography.titleLarge, color = HardAmber)
            Text(
                stringResource(R.string.guide_durations_arrow, context.longDuration(good)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
        SectionLabel(stringResource(R.string.guide_units_label))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.guide_unit_min,
                R.string.guide_unit_hour,
                R.string.guide_unit_day,
                R.string.guide_unit_month,
                R.string.guide_unit_year,
            ).forEach { unit ->
                Surface(shape = RoundedCornerShape(10.dp), color = KelimelerColors.CardBg, border = BorderStroke(1.dp, KelimelerColors.CardBorder), contentColor = OnSurface) {
                    Text(stringResource(unit), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
        ContentCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.guide_journey_label), Modifier.weight(1f))
                TextButton(onClick = { replay++ }) { Text(stringResource(R.string.guide_replay), color = PrimaryMint) }
            }
            GuideNote(stringResource(R.string.guide_journey_body))
            Spacer(Modifier.height(6.dp))
            journey.forEachIndexed { index, wait ->
                JourneyRow(index, context.shortDuration(wait), replay)
            }
        }
        GuideNote(stringResource(R.string.guide_durations_note))
    }
}

@Composable
private fun JourneyRow(index: Int, wait: String, replay: Int) {
    var visible by remember(replay) { mutableStateOf(false) }
    LaunchedEffect(replay) {
        delay(350L * index)
        visible = true
    }
    AnimatedVisibility(visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.guide_journey_step, index + 1), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.rating_good), style = MaterialTheme.typography.labelLarge, color = PrimaryMint)
            Text("  →  ", color = OnSurfaceMuted)
            Surface(shape = RoundedCornerShape(8.dp), color = PrimaryMint.copy(alpha = 0.14f), contentColor = OnSurface) {
                Text(wait, style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 3.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

// 7 ─ FSRS ve hedef hatırlama oranı -----------------------------------------------------------------

@Composable
internal fun FsrsPage() {
    val context = LocalContext.current
    var retention by rememberSaveable { mutableFloatStateOf(StudySettings.DEFAULT_RETENTION) }
    val percent = (retention * 100).roundToInt()
    val good = remember(percent) {
        val scheduler = GuideSchedule.newScheduler(percent / 100.0)
        val (card, now) = GuideSchedule.reviewCard(scheduler)
        GuideSchedule.intervals(card, now, scheduler)[Rating.Good] ?: 0L
    }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_fsrs_title))
        GuideBody(stringResource(R.string.guide_fsrs_body))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FactTile(stringResource(R.string.guide_fsrs_difficulty_title), stringResource(R.string.guide_fsrs_difficulty_body), HardAmber, Modifier.weight(1f))
            FactTile(stringResource(R.string.guide_fsrs_stability_title), stringResource(R.string.guide_fsrs_stability_body), PrimaryMint, Modifier.weight(1f))
        }
        ContentCard {
            SectionLabel(stringResource(R.string.retention_title))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%$percent", style = MaterialTheme.typography.titleLarge, color = PrimaryMint, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.guide_fsrs_slider_result, context.longDuration(good)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Slider(
                value = retention,
                onValueChange = { retention = it },
                valueRange = StudySettings.MIN_RETENTION..StudySettings.MAX_RETENTION,
                steps = ((StudySettings.MAX_RETENTION - StudySettings.MIN_RETENTION) * 100).roundToInt() - 1,
                colors = SliderDefaults.colors(thumbColor = PrimaryMint, activeTrackColor = PrimaryMint),
            )
            GuideNote(stringResource(R.string.guide_fsrs_slider_note))
        }
        TipBox(stringResource(R.string.guide_fsrs_tip))
    }
}

@Composable
private fun FactTile(title: String, body: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        contentColor = OnSurface,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = OnSurface)
        }
    }
}

// 8 ─ Günlük düzen -------------------------------------------------------------------------------

@Composable
internal fun DailyPage(onStart: () -> Unit, onMore: () -> Unit, onPlacement: () -> Unit) {
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_daily_title))
        DailyRow(Icons.Filled.Today, stringResource(R.string.guide_daily_start))
        DailyRow(Icons.Filled.AddCircleOutline, stringResource(R.string.guide_daily_new, StudySettings.DEFAULT_NEW_PER_DAY))
        DailyRow(Icons.Filled.Schedule, stringResource(R.string.guide_daily_every_day))
        DailyRow(Icons.Filled.Replay, stringResource(R.string.guide_daily_learning))
        DailyRow(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.guide_daily_words))
        DailyRow(Icons.Filled.Sync, stringResource(R.string.guide_daily_sync))
        HeroBackground(RoundedCornerShape(24.dp), Modifier.fillMaxWidth(), skylineBias = 1f) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.guide_basics_done_title), style = MaterialTheme.typography.titleLarge, color = OnSurface)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.guide_basics_done_body), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.guide_start)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onMore,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.guide_more_screens), color = PrimaryMint) }
            }
        }
        // Daha önce Arapça çalışmış olanlar baştaki bildikleri kelimelerle uğraşmasın.
        ContentCard {
            Text(stringResource(R.string.guide_placement_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.guide_placement_body), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onPlacement,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.guide_placement_button), color = PrimaryMint) }
        }
    }
}

@Composable
private fun DailyRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(top = 6.dp))
    }
}
