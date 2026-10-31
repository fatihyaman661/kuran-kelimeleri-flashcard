package com.mevlutcelik.kelimeler.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import com.mevlutcelik.kelimeler.data.study.StatsRepository
import com.mevlutcelik.kelimeler.data.study.StudyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel @Inject constructor(
    stats: StatsRepository,
    reviewDao: ReviewDao,
    settings: SettingsRepository,
) : ViewModel() {

    /** Yeni bir tekrar ya da sınır değişikliği istatistikleri yeniden hesaplatır. */
    val state: StateFlow<StudyStats?> = combine(reviewDao.observeCount(), settings.study) { _, _ -> }
        .mapLatest { stats.stats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
