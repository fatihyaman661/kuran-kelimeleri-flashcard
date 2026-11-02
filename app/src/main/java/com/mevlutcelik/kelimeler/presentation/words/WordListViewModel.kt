package com.mevlutcelik.kelimeler.presentation.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.fsrs.DayClock
import com.mevlutcelik.fsrs.Phase
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.local.WordListRow
import com.mevlutcelik.kelimeler.data.study.isLeech
import com.mevlutcelik.kelimeler.util.SearchText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

enum class WordFilter { All, New, Learning, Learned, Leech, Suspended }

data class WordRowUi(
    val id: String,
    val rank: Int,
    val vocalized: String,
    val meaning: String,
    val phase: Phase,
    /** Tekrara kalan süre; vakti geldiyse 0, hiç çalışılmadıysa null. */
    val dueIn: Long?,
    val suspended: Boolean,
    /** Zorlanan kelime (bkz. [isLeech]); askıdaysa işaretlenmez. */
    val leech: Boolean,
)

data class WordListUiState(
    val loading: Boolean = true,
    val rows: List<WordRowUi> = emptyList(),
    val counts: Map<WordFilter, Int> = emptyMap(),
)

@HiltViewModel
@OptIn(FlowPreview::class)
class WordListViewModel @Inject constructor(
    wordDao: WordDao,
    private val clock: DayClock,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(WordFilter.All)
    val filter: StateFlow<WordFilter> = _filter.asStateFlow()

    val state: StateFlow<WordListUiState> = combine(wordDao.observeList(), _query.debounce(200), _filter) { rows, query, filter ->
        val now = System.currentTimeMillis()
        val needle = SearchText.query(query)
        val matching = if (needle.isEmpty()) rows else rows.filter { needle in it.searchText }
        val ui = matching.map { it.toUi(now) }
        WordListUiState(
            loading = false,
            rows = ui.filter { filter.matches(it) },
            counts = WordFilter.entries.associateWith { f -> ui.count { f.matches(it) } },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordListUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onFilterChange(value: WordFilter) {
        _filter.value = value
    }

    /** Askıdaki kelime yalnızca "Tümü" ve "Askıda" altında görünür. */
    private fun WordFilter.matches(row: WordRowUi) = when (this) {
        WordFilter.All -> true
        WordFilter.Suspended -> row.suspended
        WordFilter.New -> !row.suspended && row.phase == Phase.New
        WordFilter.Learning -> !row.suspended && (row.phase == Phase.Learning || row.phase == Phase.Relearning)
        WordFilter.Learned -> !row.suspended && row.phase == Phase.Review
        WordFilter.Leech -> row.leech
    }

    private fun WordListRow.toUi(now: Long): WordRowUi {
        val phase = phase?.let(Phase::of) ?: Phase.New
        val dueIn = due?.let {
            when {
                it <= now -> 0L
                phase == Phase.Review -> (clock.dayOf(it) - clock.dayOf(now)) * DAY_MS
                else -> it - now
            }
        }
        return WordRowUi(id, rank, vocalized, meaning, phase, dueIn, suspended, leech = !suspended && isLeech(lapses))
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
