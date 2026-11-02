package com.mevlutcelik.kelimeler.presentation.glossary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.kelimeler.data.glossary.Glossary
import com.mevlutcelik.kelimeler.data.glossary.GlossaryRepository
import com.mevlutcelik.kelimeler.data.glossary.GlossarySection
import com.mevlutcelik.kelimeler.util.SearchText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GlossaryUiState(
    val loading: Boolean = true,
    val sections: List<GlossarySection> = emptyList(),
)

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val repository: GlossaryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val state: StateFlow<GlossaryUiState> = combine(repository.glossary, _query) { glossary, query ->
        if (glossary == null) GlossaryUiState() else GlossaryUiState(loading = false, sections = glossary.filter(query))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlossaryUiState())

    init {
        viewModelScope.launch { repository.load() }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Başlıkta, Arapça adda ya da açıklamada geçen terimleri bırakır; boş kalan bölümler düşer. */
    private fun Glossary.filter(query: String): List<GlossarySection> {
        val needle = SearchText.query(query)
        if (needle.isEmpty()) return sections
        return sections.mapNotNull { section ->
            val terms = section.terms.filter { term ->
                val latin = SearchText.latin(term.title + " " + term.blocks.joinToString(" ") { it.text })
                val arabic = SearchText.arabic((term.arabic.orEmpty() + " " + term.blocks.joinToString(" ") { it.text })).replace(" ", "")
                needle in latin || needle in arabic
            }
            if (terms.isEmpty()) null else section.copy(terms = terms)
        }
    }
}
