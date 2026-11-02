package com.mevlutcelik.kelimeler.presentation.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * "Nasıl kullanılır" rehberinin gösterilip gösterilmediği. [seen] ayar okunana kadar null'dur;
 * böylece ilk karede yanlışlıkla yönlendirme yapılmaz.
 */
@HiltViewModel
class GuideViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val seen: StateFlow<Boolean?> = settings.guideSeen
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markSeen() {
        viewModelScope.launch { settings.setGuideSeen() }
    }
}
