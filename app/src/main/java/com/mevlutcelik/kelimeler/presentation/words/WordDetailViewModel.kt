package com.mevlutcelik.kelimeler.presentation.words

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.fsrs.DayClock
import com.mevlutcelik.fsrs.Scheduler
import com.mevlutcelik.kelimeler.data.deck.WordDetails
import com.mevlutcelik.kelimeler.data.local.CardStateDao
import com.mevlutcelik.kelimeler.data.local.CardStateEntity
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.local.ReviewEntity
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.local.WordEntity
import com.mevlutcelik.kelimeler.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

data class WordDetailUiState(
    val word: WordEntity? = null,
    val details: WordDetails? = null,
    val state: CardStateEntity? = null,
    val retrievability: Double? = null,
    val reviews: List<ReviewEntity> = emptyList(),
)

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    wordDao: WordDao,
    stateDao: CardStateDao,
    reviewDao: ReviewDao,
    clock: DayClock,
    json: Json,
) : ViewModel() {

    private val id = Routes.decodeWordId(checkNotNull(savedStateHandle[Routes.WORD_ARG]))
    private val scheduler = Scheduler(clock = clock)

    private val word = flow {
        val entity = wordDao.get(id)
        emit(entity to entity?.let { json.decodeFromString(WordDetails.serializer(), it.details) })
    }

    val state: StateFlow<WordDetailUiState> = combine(word, stateDao.observe(id), reviewDao.observeForWord(id)) { (entity, details), cardState, reviews ->
        WordDetailUiState(
            word = entity,
            details = details,
            state = cardState,
            retrievability = cardState?.let { scheduler.retrievability(it.toCard(), System.currentTimeMillis()) },
            reviews = reviews,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordDetailUiState())
}
