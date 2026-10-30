package com.mevlutcelik.kelimeler.presentation.study

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.fsrs.DayClock
import com.mevlutcelik.fsrs.Phase
import com.mevlutcelik.fsrs.Rating
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.StudyCard
import com.mevlutcelik.kelimeler.data.study.StudyRepository
import com.mevlutcelik.kelimeler.data.study.UndoResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyCounts(val new: Int = 0, val learning: Int = 0, val review: Int = 0)

data class StudyUiState(
    val loading: Boolean = true,
    val card: StudyCard? = null,
    val revealed: Boolean = false,
    val counts: StudyCounts = StudyCounts(),
    /** Her cevap için bir sonraki tekrara kalan süre; tekrar kartlarında gün sayısına yuvarlanmış. */
    val intervals: Map<Rating, Long> = emptyMap(),
    val reviewedInSession: Int = 0,
    val nextLearningDue: Long? = null,
)

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val study: StudyRepository,
    private val clock: DayClock,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var shownAt = 0L
    private var busy = false

    init {
        loadNext()
    }

    private fun loadNext() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val card = study.nextCard(now)
            val overview = study.overview(now)
            _state.update {
                it.copy(
                    loading = false,
                    card = card,
                    revealed = false,
                    counts = StudyCounts(overview.newAvailable, overview.learningDue, overview.reviewDue),
                    intervals = card?.let { c -> intervals(c, now) }.orEmpty(),
                    nextLearningDue = overview.nextLearningDue,
                )
            }
            shownAt = System.currentTimeMillis()
            busy = false
        }
    }

    private fun intervals(card: StudyCard, now: Long): Map<Rating, Long> = card.preview.mapValues { (_, next) ->
        if (next.phase == Phase.Review) (clock.dayOf(next.due) - clock.dayOf(now)) * DAY_MS else next.due - now
    }

    fun reveal() {
        _state.update { it.copy(revealed = true) }
    }

    fun answer(rating: Rating) {
        val card = _state.value.card ?: return
        if (!_state.value.revealed || busy) return
        busy = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            study.answer(card, rating, now, now - shownAt)
            _state.update { it.copy(reviewedInSession = it.reviewedInSession + 1) }
            loadNext()
        }
    }

    fun undo() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            @StringRes val message = when (study.undoLast()) {
                is UndoResult.Undone -> {
                    _state.update { it.copy(reviewedInSession = (it.reviewedInSession - 1).coerceAtLeast(0)) }
                    R.string.study_undone
                }
                UndoResult.AlreadySynced -> R.string.study_undo_synced
                UndoResult.Nothing -> R.string.study_nothing_to_undo
            }
            _messages.send(message)
            loadNext()
        }
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
