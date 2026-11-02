package com.mevlutcelik.kelimeler.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.StudyStats
import com.mevlutcelik.kelimeler.presentation.common.ContentCard
import com.mevlutcelik.kelimeler.presentation.common.ScreenHeader
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
import com.mevlutcelik.kelimeler.presentation.common.shortDuration
import com.mevlutcelik.kelimeler.presentation.home.DayBarChart
import com.mevlutcelik.kelimeler.presentation.home.Meter
import com.mevlutcelik.kelimeler.presentation.theme.NewBlue
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val turkish = Locale.forLanguageTag("tr")
private val dayFormat = DateTimeFormatter.ofPattern("d MMMM EEEE", turkish)
private val shortDay = DateTimeFormatter.ofPattern("d MMMM", turkish)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", turkish)
private const val DAY_MS = 24 * 60 * 60 * 1000L

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        ScreenHeader(stringResource(R.string.nav_stats), stringResource(R.string.stats_subtitle))
        val current = stats
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryMint) }
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CalendarCard(current)
            ForecastCard(current)
            FinishCard(current)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CalendarCard(stats: StudyStats) {
    var selected by remember(stats.today) { mutableStateOf(stats.today) }
    ContentCard {
        SectionLabel(stringResource(R.string.stats_calendar_title))
        Spacer(Modifier.height(12.dp))
        ActivityHeatmap(stats.activity, stats.today, selected) { selected = it }
        Spacer(Modifier.height(10.dp))
        val date = LocalDate.ofEpochDay(selected).format(dayFormat)
        val day = stats.activity[selected]
        Text(
            if (day == null) stringResource(R.string.stats_day_empty, date)
            else stringResource(R.string.stats_day_detail, date, day.reviews, day.newWords, (day.correct * 100f / day.reviews).roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.stats_days, stats.currentStreak), stringResource(R.string.stats_current_streak), Modifier.weight(1f))
            StatTile(stringResource(R.string.stats_days, stats.longestStreak), stringResource(R.string.stats_longest_streak), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stats.activeDays.toString(), stringResource(R.string.stats_active_days), Modifier.weight(1f))
            StatTile(stats.totalReviews.toString(), stringResource(R.string.stats_total_reviews), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ForecastCard(stats: StudyStats) {
    ContentCard {
        SectionLabel(stringResource(R.string.stats_forecast_title))
        Spacer(Modifier.height(12.dp))
        DayBarChart(
            days = stats.forecast,
            summary = { total -> stringResource(R.string.stats_forecast_total, total) },
            description = { total, peak -> stringResource(R.string.stats_forecast_description, total, peak) },
            startLabel = stringResource(R.string.home_activity_today),
            endLabel = LocalDate.ofEpochDay(stats.forecast.last().day).format(shortDay),
            colors = listOf(NewBlue, NewBlue.copy(alpha = 0.55f)),
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.stats_forecast_note), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
    }
}

@Composable
private fun FinishCard(stats: StudyStats) {
    val context = LocalContext.current
    ContentCard {
        SectionLabel(stringResource(R.string.stats_finish_title))
        Spacer(Modifier.height(10.dp))
        val days = stats.finishDays
        Text(
            when {
                days == null -> stringResource(R.string.stats_finish_none)
                days == 0 -> stringResource(R.string.stats_finish_done)
                else -> stringResource(
                    R.string.stats_finish_body,
                    stats.newPerDay,
                    stats.unseenWords,
                    context.shortDuration(days * DAY_MS),
                    LocalDate.ofEpochDay(stats.today + days).format(monthFormat),
                )
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.stats_mature_title))
        Spacer(Modifier.height(10.dp))
        val ratio = if (stats.totalWords == 0) 0f else stats.matureWords.toFloat() / stats.totalWords
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stats.matureWords.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(" / ${stats.totalWords}", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceMuted)
        }
        Spacer(Modifier.height(8.dp))
        Meter(ratio)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.stats_mature_body, stats.matureWords), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
    }
}
