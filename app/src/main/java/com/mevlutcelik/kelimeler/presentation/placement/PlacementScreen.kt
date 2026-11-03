package com.mevlutcelik.kelimeler.presentation.placement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.PlacementSummary
import com.mevlutcelik.kelimeler.data.study.StudyRepository
import com.mevlutcelik.kelimeler.presentation.common.ContentCard
import com.mevlutcelik.kelimeler.presentation.common.MihrabFrame
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.Background
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Seviye tespiti: kısa, uyarlanabilir bir çoktan seçmeli test. Sonucu kullanıcı onaylar; bilinen
 * sayılan kelimeler askıya alınmaz, günde en fazla birkaç tanesi tekrar olarak gelir (bkz.
 * StudyRepository.applyPlacement).
 */
@Composable
fun PlacementScreen(onClose: () -> Unit, viewModel: PlacementViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.placement_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            val step = state.step
            if (step is PlacementStep.Question) {
                Text(
                    stringResource(R.string.placement_question_number, step.number),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceMuted,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            when (val step = state.step) {
                PlacementStep.Loading, PlacementStep.Applying -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryMint)
                }
                PlacementStep.Intro -> Intro(state, onStart = viewModel::start, onUndo = viewModel::undo, onClose = onClose)
                PlacementStep.Unavailable -> Message(stringResource(R.string.placement_unavailable), stringResource(R.string.placement_close), onClose)
                is PlacementStep.Basics -> BasicsView(step, viewModel::answerBasics)
                is PlacementStep.Question -> QuestionView(step, viewModel::choose)
                is PlacementStep.Result -> ResultView(state, step, viewModel::setCutoff, viewModel::apply, onSkip = onClose)
                is PlacementStep.Done -> DoneView(step, onClose)
            }
        }
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text(text) }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(text, color = PrimaryMint) }
}

// Giriş -------------------------------------------------------------------------------------------

@Composable
private fun Intro(state: PlacementUiState, onStart: () -> Unit, onUndo: () -> Unit, onClose: () -> Unit) {
    var confirmUndo by remember { mutableStateOf(false) }
    val summary = state.summary
    if (confirmUndo && summary != null) {
        AlertDialog(
            onDismissRequest = { confirmUndo = false },
            title = { Text(stringResource(R.string.placement_undo_title)) },
            text = { Text(stringResource(R.string.placement_undo_body, summary.pending)) },
            confirmButton = { TextButton(onClick = { confirmUndo = false; onUndo() }) { Text(stringResource(R.string.placement_undo_confirm), color = LearnCoral) } },
            dismissButton = { TextButton(onClick = { confirmUndo = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    PageColumn {
        Text(stringResource(R.string.placement_intro_body), style = MaterialTheme.typography.bodyLarge)
        ContentCard {
            SectionLabel(stringResource(R.string.placement_how_label))
            Spacer(Modifier.height(8.dp))
            listOf(R.string.placement_how_1, R.string.placement_how_2, R.string.placement_how_3).forEach {
                Bullet(stringResource(it))
            }
        }
        ContentCard {
            SectionLabel(stringResource(R.string.placement_after_label))
            Spacer(Modifier.height(8.dp))
            Bullet(stringResource(R.string.placement_after_1))
            Bullet(stringResource(R.string.placement_after_2, StudyRepository.PLACEMENT_PER_DAY))
            Bullet(stringResource(R.string.placement_after_3))
        }
        if (summary != null && summary.placed > 0) SummaryCard(summary, onUndo = { confirmUndo = true })
        state.undone?.let {
            Text(stringResource(R.string.placement_undone, it), style = MaterialTheme.typography.bodyMedium, color = PrimaryMint)
        }
        PrimaryAction(stringResource(R.string.placement_start), onStart)
        SecondaryAction(stringResource(R.string.placement_cancel), onClose)
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = PrimaryMint, modifier = Modifier.width(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(summary: PlacementSummary, onUndo: () -> Unit) {
    ContentCard {
        SectionLabel(stringResource(R.string.placement_summary_label))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.placement_summary_placed, summary.placed, summary.pending), style = MaterialTheme.typography.bodyMedium)
        if (summary.checked > 0) {
            val percent = (summary.forgotten * 100f / summary.checked).roundToInt()
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.placement_summary_checked, summary.checked, summary.forgotten, percent),
                style = MaterialTheme.typography.bodyMedium,
                color = if (percent > 20) LearnCoral else OnSurfaceMuted,
            )
        }
        if (summary.pending > 0) {
            TextButton(onClick = onUndo) { Text(stringResource(R.string.placement_undo_button, summary.pending), color = LearnCoral) }
        }
    }
}

@Composable
private fun Message(text: String, button: String, onClick: () -> Unit) {
    PageColumn {
        Spacer(Modifier.height(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        PrimaryAction(button, onClick)
    }
}

// Harf ve ekler -----------------------------------------------------------------------------------

@Composable
private fun BasicsView(step: PlacementStep.Basics, onAnswer: (Boolean) -> Unit) {
    PageColumn {
        Text(stringResource(R.string.placement_basics_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.placement_basics_body), style = MaterialTheme.typography.bodyMedium)
        ContentCard {
            step.words.forEach { word ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(word.meaning.substringBefore(";").trim(), style = MaterialTheme.typography.bodyMedium)
                        Text(word.type, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                    }
                    Text(word.vocalized, style = ArabicStyles.Form, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Text(
            stringResource(R.string.placement_basics_note, step.count, PlacementViewModel.BASICS_UP_TO),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceMuted,
        )
        PrimaryAction(stringResource(R.string.placement_basics_yes), onClick = { onAnswer(true) })
        SecondaryAction(stringResource(R.string.placement_basics_no), onClick = { onAnswer(false) })
    }
}

// Soru --------------------------------------------------------------------------------------------

@Composable
private fun QuestionView(step: PlacementStep.Question, onChoose: (Int) -> Unit) {
    val item = step.item
    val answered = step.chosen != null
    PageColumn {
        Surface(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(28.dp),
            color = KelimelerColors.CardBg,
            contentColor = OnSurface,
            border = BorderStroke(1.dp, KelimelerColors.CardBorder),
            shadowElevation = 6.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                MihrabFrame(Modifier.align(Alignment.Center).fillMaxWidth(0.7f).fillMaxHeight(0.8f))
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.word, style = ArabicStyles.Headword)
                    Text(item.type, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
                }
            }
        }
        Text(stringResource(R.string.placement_question_prompt), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
        item.choices.forEachIndexed { index, choice ->
            val color = when {
                !answered -> null
                index == item.answerIndex -> PrimaryMint
                index == step.chosen -> LearnCoral
                else -> null
            }
            ChoiceButton(choice, color, enabled = !answered) { onChoose(index) }
        }
        TextButton(
            onClick = { onChoose(-1) },
            enabled = !answered,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.placement_dont_know), color = if (step.chosen == -1) LearnCoral else PrimaryMint)
        }
        if (answered && step.chosen != item.answerIndex) {
            Text(
                stringResource(R.string.placement_correct_was, item.choices[item.answerIndex]),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChoiceButton(text: String, highlight: Color?, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = highlight?.copy(alpha = 0.16f) ?: KelimelerColors.CardBg,
        contentColor = OnSurface,
        border = BorderStroke(if (highlight != null) 2.dp else 1.dp, highlight ?: KelimelerColors.CardBorder),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (highlight == PrimaryMint) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(20.dp))
        }
    }
}

// Sonuç -------------------------------------------------------------------------------------------

@Composable
private fun ResultView(
    state: PlacementUiState,
    step: PlacementStep.Result,
    onCutoff: (Int) -> Unit,
    onApply: () -> Unit,
    onSkip: () -> Unit,
) {
    val count = state.placeable(step.cutoff)
    val days = ceil(count / StudyRepository.PLACEMENT_PER_DAY.toDouble()).toInt()
    PageColumn {
        ContentCard {
            Text(
                if (step.suggested == 0) stringResource(R.string.placement_result_zero) else stringResource(R.string.placement_result, step.suggested),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.placement_result_asked, step.asked), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        }
        ContentCard {
            SectionLabel(stringResource(R.string.placement_adjust_label))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(onClick = { onCutoff(step.cutoff - STEP) }, enabled = step.cutoff > 0, border = BorderStroke(1.dp, KelimelerColors.CardBorder)) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
                }
                Text(
                    stringResource(R.string.placement_first_n, step.cutoff),
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryMint,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                OutlinedIconButton(onClick = { onCutoff(step.cutoff + STEP) }, enabled = step.cutoff < state.maxRank, border = BorderStroke(1.dp, KelimelerColors.CardBorder)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
                }
            }
            // Kaydırıcı en fazla önerinin iki katı (+200) kadar uzanır; ince ayar butonlarla.
            val sliderMax = minOf(state.maxRank, maxOf(step.suggested * 2 + 200, step.cutoff)).coerceAtLeast(1)
            Slider(
                value = step.cutoff.toFloat(),
                onValueChange = { onCutoff(it.roundToInt()) },
                valueRange = 0f..sliderMax.toFloat(),
                colors = SliderDefaults.colors(thumbColor = PrimaryMint, activeTrackColor = PrimaryMint, inactiveTrackColor = KelimelerColors.ProgressTrack),
            )
            Text(
                if (count == 0) stringResource(R.string.placement_effect_none) else stringResource(R.string.placement_effect, count, StudyRepository.PLACEMENT_PER_DAY, days),
                style = MaterialTheme.typography.bodyMedium,
            )
            val extra = state.extraBasics(step.cutoff)
            if (extra > 0) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.placement_effect_basics, extra), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
            }
        }
        PrimaryAction(stringResource(R.string.placement_apply), onApply, enabled = count > 0)
        SecondaryAction(stringResource(R.string.placement_skip), onSkip)
    }
}

@Composable
private fun DoneView(step: PlacementStep.Done, onClose: () -> Unit) {
    PageColumn {
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
        Text(
            stringResource(R.string.placement_done, step.placed, step.cutoff + 1),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryAction(stringResource(R.string.placement_close), onClose)
    }
}

private const val STEP = 10
