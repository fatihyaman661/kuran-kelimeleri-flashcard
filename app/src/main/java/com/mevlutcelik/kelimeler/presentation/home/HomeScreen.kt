package com.mevlutcelik.kelimeler.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.deck.DeckChange
import com.mevlutcelik.kelimeler.data.study.StudyOverview
import com.mevlutcelik.kelimeler.presentation.common.ContentCard
import com.mevlutcelik.kelimeler.presentation.common.HeroBackground
import com.mevlutcelik.kelimeler.presentation.common.IconBadge
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
import com.mevlutcelik.kelimeler.presentation.common.StatusDot
import com.mevlutcelik.kelimeler.presentation.common.percent
import com.mevlutcelik.kelimeler.presentation.common.shortDuration
import com.mevlutcelik.kelimeler.presentation.navigation.NavigationDrawerMenuButton
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private val turkish = Locale.forLanguageTag("tr")
private val dateFormat = DateTimeFormatter.ofPattern("d MMMM EEEE", turkish)

@Composable
fun HomeScreen(
    onStudy: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overview = state.overview

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Header(overview)
        if (!state.ready || overview == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryMint)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.home_preparing), color = OnSurfaceMuted)
                }
            }
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimatedVisibility(state.deckChange != null, exit = fadeOut() + shrinkVertically()) {
                state.deckChange?.let { DeckChangeBanner(it, viewModel::dismissDeckChange) }
            }
            TodayHero(overview, onStudy)
            ProgressCard(overview)
            ActivityCard(overview)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Uygulama adı yerine güne göre selam, tarih ve çalışma serisi. */
@Composable
private fun Header(overview: StudyOverview?) {
    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> R.string.greeting_morning
        in 11..16 -> R.string.greeting_day
        in 17..21 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationDrawerMenuButton()
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                LocalDate.now().format(dateFormat).replaceFirstChar { it.titlecase(turkish) },
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMuted,
            )
            Text(stringResource(greeting), style = MaterialTheme.typography.titleLarge)
        }
        overview?.let { StreakChip(streak(it)) }
    }
}

/** Bugünden (bugün henüz çalışılmadıysa dünden) geriye kesintisiz çalışılan gün sayısı. */
private fun streak(overview: StudyOverview): Int {
    val days = overview.daily.map { it.reviews > 0 }.reversed()
    val start = if (days.firstOrNull() == true) 0 else 1
    return days.drop(start).takeWhile { it }.size
}

@Composable
private fun StreakChip(days: Int) {
    val active = days > 0
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) HardAmber.copy(alpha = 0.14f) else KelimelerColors.CardBg,
        border = BorderStroke(1.dp, if (active) HardAmber.copy(alpha = 0.4f) else KelimelerColors.CardBorder),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = if (active) HardAmber else OnSurfaceMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.home_streak, days), style = MaterialTheme.typography.labelLarge, color = OnSurface)
        }
    }
}

@Composable
private fun TodayHero(overview: StudyOverview, onStudy: () -> Unit) {
    val context = LocalContext.current
    HeroBackground(RoundedCornerShape(28.dp), Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.home_today_session), color = PrimaryMint)
                Spacer(Modifier.weight(1f))
                if (overview.hasWork) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_estimate, estimateMinutes(overview)), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CounterTile(overview.newAvailable, stringResource(R.string.counter_new), NewBlue, Modifier.weight(1f))
                CounterTile(overview.learningDue, stringResource(R.string.counter_learning), LearnCoral, Modifier.weight(1f))
                CounterTile(overview.reviewDue, stringResource(R.string.counter_review), PrimaryMint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            if (overview.hasWork) {
                Button(
                    onClick = onStudy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
                ) {
                    Text(stringResource(R.string.home_start), style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.home_done_title), style = MaterialTheme.typography.titleMedium)
                        val next = overview.nextLearningDue
                        Text(
                            if (next != null) stringResource(R.string.home_next_learning, context.shortDuration(next - System.currentTimeMillis()))
                            else stringResource(R.string.home_done_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Süs: inen ilk ayetin başı, desenin üstünde sessiz bir imza gibi.
            Text(
                stringResource(R.string.home_hero_verse),
                style = ArabicStyles.Inline.copy(fontSize = 20.sp),
                color = PrimaryMint.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** Tekrar ~8 sn, öğrenme adımı ~10 sn, yeni kelime ~25 sn varsayımıyla kaba süre. */
private fun estimateMinutes(o: StudyOverview): Int =
    ceil((o.reviewDue * 8 + o.learningDue * 10 + o.newAvailable * 25) / 60.0).toInt().coerceAtLeast(1)

@Composable
private fun CounterTile(value: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(value, tween(600), label = "counter")
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(animated.toString(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 34.sp), fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        }
    }
}

@Composable
private fun ProgressCard(overview: StudyOverview) {
    val learnedRatio = if (overview.totalWords == 0) 0f else overview.learnedWords.toFloat() / overview.totalWords
    val coverage = if (overview.totalFrequency == 0L) 0f else overview.learnedFrequency.toFloat() / overview.totalFrequency
    val newWords = (overview.totalWords - overview.learnedWords - overview.learningWords).coerceAtLeast(0)
    ContentCard {
        SectionLabel(stringResource(R.string.home_progress_title))
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LearnedRing(learnedRatio)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StateRow(PrimaryMint, stringResource(R.string.filter_learned), overview.learnedWords)
                StateRow(LearnCoral, stringResource(R.string.filter_learning), overview.learningWords)
                StateRow(NewBlue, stringResource(R.string.filter_new), newWords)
            }
        }
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = 0.2f), contentColor = OnSurface) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Update, size = 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.home_coverage_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(percent(coverage), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Meter(coverage)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.home_coverage_body), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
            }
        }
    }
}

/** Renk kimliği noktada; sayı ve etiket metin renginde kalır. */
@Composable
private fun StateRow(color: Color, label: String, value: Int) {
    val animated by animateIntAsState(value, tween(600), label = "state")
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
        Text(animated.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActivityCard(overview: StudyOverview) {
    ContentCard {
        SectionLabel(stringResource(R.string.home_activity_title))
        Spacer(Modifier.height(12.dp))
        DailyReviewsChart(overview.daily)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(overview.reviewsToday.toString(), stringResource(R.string.home_today_reviews_label), Modifier.weight(1f))
            MiniStat(overview.newToday.toString(), stringResource(R.string.home_today_new_label), Modifier.weight(1f))
            MiniStat(
                if (overview.reviewsToday > 0) "%${(overview.correctToday * 100f / overview.reviewsToday).roundToInt()}" else "–",
                stringResource(R.string.home_today_accuracy_label),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
    }
}

@Composable
private fun DeckChangeBanner(change: DeckChange, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = KelimelerColors.CardBg,
        contentColor = OnSurface,
        border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(PrimaryMint, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                if (change.removed > 0) stringResource(R.string.home_deck_updated_removed, change.added, change.removed)
                else stringResource(R.string.home_deck_updated, change.added),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dismiss), tint = OnSurfaceMuted)
            }
        }
    }
}
