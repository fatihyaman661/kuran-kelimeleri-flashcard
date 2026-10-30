package com.mevlutcelik.kelimeler.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.kelimeler.data.AppStartup
import com.mevlutcelik.kelimeler.data.deck.DeckChange
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import com.mevlutcelik.kelimeler.data.study.StudyOverview
import com.mevlutcelik.kelimeler.data.study.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val ready: Boolean = false,
    val overview: StudyOverview? = null,
    val deckChange: DeckChange? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val startup: AppStartup,
    private val study: StudyRepository,
    reviewDao: ReviewDao,
    wordDao: WordDao,
    settings: SettingsRepository,
) : ViewModel() {

    /** Öğrenme kartlarının vakti dakikalar içinde geldiği için sayılar ekranda açıkken de yenilenir. */
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(30_000)
        }
    }

    private val overview = combine(startup.ready, reviewDao.observeCount(), wordDao.observeActiveCount(), settings.study, ticker) { ready, _, _, _, _ -> ready }
        .mapLatest { ready -> if (ready) study.overview() else null }

    val state: StateFlow<HomeUiState> = combine(startup.ready, overview, startup.deckChange) { ready, overview, change ->
        HomeUiState(ready = ready, overview = overview, deckChange = change)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun dismissDeckChange() = startup.dismissDeckChange()
}
