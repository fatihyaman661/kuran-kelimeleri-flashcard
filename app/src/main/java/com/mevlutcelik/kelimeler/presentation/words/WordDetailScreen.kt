package com.mevlutcelik.kelimeler.presentation.words

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.fsrs.Phase
import com.mevlutcelik.fsrs.Rating
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.local.CardStateEntity
import com.mevlutcelik.kelimeler.data.local.ReviewEntity
import com.mevlutcelik.kelimeler.presentation.common.ContentCard
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
import com.mevlutcelik.kelimeler.presentation.common.HeroBackground
import com.mevlutcelik.kelimeler.presentation.common.WordBack
import com.mevlutcelik.kelimeler.presentation.common.metaLine
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.common.label
import com.mevlutcelik.kelimeler.presentation.common.percent
import com.mevlutcelik.kelimeler.presentation.common.relativeTime
import com.mevlutcelik.kelimeler.presentation.theme.HardAmber
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.NewBlue
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WordDetailScreen(
    onBack: () -> Unit,
    viewModel: WordDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            state.word?.let { Text("#${it.rank}", style = MaterialTheme.typography.titleMedium, color = OnSurfaceMuted) }
            Spacer(Modifier.weight(1f))
            if (state.word != null) {
                TextButton(onClick = viewModel::toggleSuspended) {
                    Icon(
                        if (state.suspended) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                        contentDescription = null,
                        tint = if (state.suspended) PrimaryMint else OnSurfaceMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(if (state.suspended) R.string.word_unsuspend else R.string.word_suspend),
                        color = if (state.suspended) PrimaryMint else OnSurfaceMuted,
                    )
                }
            }
        }
        val word = state.word
        val details = state.details
        if (word == null || details == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryMint) }
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Alttaki boşluk camiye ayrıldı; silüet yazının arkasına girmesin.
                HeroBackground(RoundedCornerShape(28.dp), Modifier.fillMaxWidth(), skylineMaxHeight = 46.dp) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 62.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(word.vocalized, style = ArabicStyles.Headword, color = PrimaryMint)
                        Text(word.category, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            word.meaning,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 28.sp),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(metaLine(word), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, textAlign = TextAlign.Center)
                    }
                }
                ContentCard {
                    WordBack(word, details, initialExamples = 5, mufredatExpanded = true, showHeader = false)
                }
                ProgressCard(state.state, state.retrievability, state.reviews, state.suspended)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProgressCard(cardState: CardStateEntity?, retrievability: Double?, reviews: List<ReviewEntity>, suspended: Boolean) {
    ContentCard {
        SectionLabel(stringResource(R.string.word_progress_title))
        Spacer(Modifier.height(10.dp))
        if (suspended) {
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PauseCircle, contentDescription = null, tint = KelimelerColors.Suspended, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.word_suspended_note), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
            }
        }
        val turkish = Locale.forLanguageTag("tr")
        if (cardState == null) {
            if (!suspended) Text(stringResource(R.string.word_never), color = OnSurfaceMuted)
        } else {
            val context = LocalContext.current
            InfoRow(stringResource(R.string.word_state), stringResource(Phase.of(cardState.phase).label()))
            // Askıdaki kelimenin tekrarı gelmez; tarih yerine durum yazılır.
            InfoRow(stringResource(R.string.word_next), if (suspended) stringResource(R.string.filter_suspended) else context.relativeTime(cardState.due))
            InfoRow(stringResource(R.string.word_stability), String.format(turkish, "%.1f g", cardState.stability))
            InfoRow(stringResource(R.string.word_difficulty), String.format(turkish, "%.1f / 10", cardState.difficulty))
            retrievability?.let { InfoRow(stringResource(R.string.word_retrievability), percent(it.toFloat())) }
            InfoRow(stringResource(R.string.word_reps), "${cardState.reps} / ${cardState.lapses}")
        }

        if (reviews.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.word_history))
            val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, turkish)
            reviews.forEach { review ->
                val (label, color) = when {
                    ReviewEntity.isAnswer(review.rating) -> Rating.of(review.rating).let { it.label() to it.color() }
                    review.rating == ReviewEntity.SUSPEND -> R.string.history_suspended to KelimelerColors.Suspended
                    else -> R.string.history_unsuspended to KelimelerColors.Suspended
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(format.format(Date(review.reviewedAt)), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Rating.label() = when (this) {
    Rating.Again -> R.string.rating_again
    Rating.Hard -> R.string.rating_hard
    Rating.Good -> R.string.rating_good
    Rating.Easy -> R.string.rating_easy
}

private fun Rating.color(): Color = when (this) {
    Rating.Again -> LearnCoral
    Rating.Hard -> HardAmber
    Rating.Good -> PrimaryMint
    Rating.Easy -> NewBlue
}
