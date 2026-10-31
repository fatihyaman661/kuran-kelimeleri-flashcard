package com.mevlutcelik.kelimeler.presentation.study

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.fsrs.Rating
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.CardKind
import com.mevlutcelik.kelimeler.data.study.StudyCard
import com.mevlutcelik.kelimeler.presentation.common.MihrabFrame
import com.mevlutcelik.kelimeler.presentation.common.MoreNewWordsDialog
import com.mevlutcelik.kelimeler.presentation.common.MosqueSkyline
import com.mevlutcelik.kelimeler.presentation.common.StatusDot
import com.mevlutcelik.kelimeler.presentation.common.WordBack
import com.mevlutcelik.kelimeler.presentation.common.shortDuration
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.Background
import com.mevlutcelik.kelimeler.presentation.theme.HardAmber
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.NewBlue
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint

private val CardShape = RoundedCornerShape(32.dp)

@Composable
fun StudyScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding(),
    ) {
        TopBar(state, onBack, viewModel::undo, viewModel::suspendCurrent)
        val card = state.card
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.loading -> CircularProgressIndicator(color = PrimaryMint)
                card == null -> Finished(state, onBack, viewModel::addNewToday)
                else -> key(card.word.id, card.state?.reps) {
                    FlipCard(card, state.revealed, viewModel::reveal)
                }
            }
        }
        if (card != null && !state.loading) {
            if (state.revealed) {
                RatingBar(state.intervals, viewModel::answer)
            } else {
                Button(
                    onClick = viewModel::reveal,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
                ) {
                    Text(stringResource(R.string.study_show_answer), style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
                }
            }
        }
    }
}

@Composable
private fun TopBar(state: StudyUiState, onBack: () -> Unit, onUndo: () -> Unit, onSuspend: () -> Unit) {
    val remaining = state.counts.new + state.counts.learning + state.counts.review
    val total = state.reviewedInSession + remaining
    val progress by animateFloatAsState(
        if (total == 0) 1f else state.reviewedInSession.toFloat() / total,
        tween(400),
        label = "session",
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.back))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = PrimaryMint,
                trackColor = KelimelerColors.ProgressTrack,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val kind = state.card?.kind
                CountChip(state.counts.new, NewBlue, kind == CardKind.New)
                CountChip(state.counts.learning, LearnCoral, kind == CardKind.Learning)
                CountChip(state.counts.review, PrimaryMint, kind == CardKind.Review)
            }
        }
        IconButton(onClick = onSuspend, enabled = state.card != null && !state.loading) {
            Icon(Icons.Outlined.PauseCircle, contentDescription = stringResource(R.string.study_suspend))
        }
        IconButton(onClick = onUndo, enabled = state.undoable > 0) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.study_undo))
        }
    }
}

@Composable
private fun CountChip(value: Int, color: Color, current: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color)
        Spacer(Modifier.width(4.dp))
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (current) OnSurface else OnSurfaceMuted,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** Dokununca 3B dönerek arka yüzü gösteren kart. */
@Composable
private fun FlipCard(card: StudyCard, revealed: Boolean, onReveal: () -> Unit) {
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(520, easing = FastOutSlowInEasing), label = "flip")
    Box(
        Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            },
    ) {
        if (rotation <= 90f) {
            CardFace(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onReveal)) {
                Front(card)
            }
        } else {
            CardFace(Modifier.graphicsLayer { rotationY = 180f }) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WordBack(card.word, card.details)
                }
            }
        }
    }
}

@Composable
private fun CardFace(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = CardShape,
        color = KelimelerColors.CardBg,
        contentColor = OnSurface,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(PrimaryMint.copy(alpha = 0.35f), KelimelerColors.CardBorder))),
        shadowElevation = 8.dp,
    ) {
        content()
    }
}

@Composable
private fun Front(card: StudyCard) {
    val (label, color) = when (card.kind) {
        CardKind.New -> R.string.kind_new to NewBlue
        CardKind.Learning -> R.string.kind_learning to LearnCoral
        CardKind.Review -> R.string.kind_review to PrimaryMint
    }
    Box(Modifier.fillMaxSize()) {
        MihrabFrame(Modifier.align(Alignment.Center).fillMaxWidth(0.82f).fillMaxHeight(0.74f))
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
            shape = RoundedCornerShape(50),
            color = color.copy(alpha = 0.14f),
            contentColor = OnSurface,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.word.word, style = ArabicStyles.CardWord)
            Text(card.word.type, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, textAlign = TextAlign.Center)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = KelimelerColors.NavInactive, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.study_tap_to_reveal), style = MaterialTheme.typography.bodySmall, color = KelimelerColors.NavInactive)
        }
    }
}

@Composable
private fun RatingBar(intervals: Map<Rating, Long>, onAnswer: (Rating) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            Triple(Rating.Again, R.string.rating_again, LearnCoral),
            Triple(Rating.Hard, R.string.rating_hard, HardAmber),
            Triple(Rating.Good, R.string.rating_good, PrimaryMint),
            Triple(Rating.Easy, R.string.rating_easy, NewBlue),
        ).forEach { (rating, label, color) ->
            Surface(
                onClick = {
                    haptics.performHapticFeedback(if (rating == Rating.Again) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
                    onAnswer(rating)
                },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = color.copy(alpha = 0.14f),
                contentColor = OnSurface,
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = color)
                    Text(
                        intervals[rating]?.let { context.shortDuration(it) }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun Finished(state: StudyUiState, onBack: () -> Unit, onMoreNew: (Int) -> Unit) {
    val context = LocalContext.current
    var askMoreNew by remember { mutableStateOf(false) }
    if (askMoreNew) {
        MoreNewWordsDialog(
            newPerDay = state.newPerDay,
            bonusToday = state.bonusNewToday,
            onPick = { askMoreNew = false; onMoreNew(it) },
            onDismiss = { askMoreNew = false },
        )
    }
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    Box(Modifier.fillMaxSize()) {
        MosqueSkyline(Modifier.matchParentSize(), color = PrimaryMint.copy(alpha = 0.06f), maxHeightFraction = 0.3f)
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .scale(pop.value)
                    .size(96.dp)
                    .background(PrimaryMint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(64.dp).background(PrimaryMint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = OnMint, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.study_finished_title), style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.study_finished_body), color = OnSurfaceMuted, textAlign = TextAlign.Center)
            if (state.reviewedInSession > 0) {
                Text(stringResource(R.string.study_session_count, state.reviewedInSession), color = OnSurfaceMuted, textAlign = TextAlign.Center)
            }
            state.nextLearningDue?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.home_next_learning, context.shortDuration(it - System.currentTimeMillis())),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
            if (state.canAddNew) {
                Button(
                    onClick = { askMoreNew = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
                ) {
                    Text(stringResource(R.string.more_new_button))
                }
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.6f)),
            ) {
                Text(stringResource(R.string.study_back_home), color = PrimaryMint)
            }
        }
    }
}
