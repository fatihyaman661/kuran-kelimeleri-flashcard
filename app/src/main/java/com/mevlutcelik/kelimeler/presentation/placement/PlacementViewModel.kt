package com.mevlutcelik.kelimeler.presentation.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.local.WordEntity
import com.mevlutcelik.kelimeler.data.placement.PlacementAnswer
import com.mevlutcelik.kelimeler.data.placement.PlacementItem
import com.mevlutcelik.kelimeler.data.placement.PlacementRepository
import com.mevlutcelik.kelimeler.data.placement.PlacementTest
import com.mevlutcelik.kelimeler.data.study.PlacementSummary
import com.mevlutcelik.kelimeler.data.study.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PlacementStep {
    data object Loading : PlacementStep
    data object Intro : PlacementStep

    /** Havuz yok (deste henüz inmemiş) ya da boş. */
    data object Unavailable : PlacementStep

    /** "Bu harfleri ve ekleri biliyor musun?": en sık yapı kelimeleri, sorulardan önce. */
    data class Basics(val words: List<WordEntity>, val count: Int) : PlacementStep

    /**
     * Önce yalnızca kelime gösterilir; kullanıcı "Biliyorum" derse ([revealed]) şıklar açılır. Şıklar
     * hatırlatmasın, yalnızca doğrulasın diye. [chosen]: verilen cevap (şık sırası; "Bilmiyorum" için -1);
     * null ise henüz cevaplanmadı.
     */
    data class Question(val item: PlacementItem, val number: Int, val revealed: Boolean = false, val chosen: Int? = null) : PlacementStep

    /** [cutoff] kullanıcının kaydırıcıyla düzelttiği değer; [suggested] testin önerisi. */
    data class Result(val suggested: Int, val cutoff: Int, val asked: Int) : PlacementStep
    data object Applying : PlacementStep
    data class Done(val placed: Int, val cutoff: Int) : PlacementStep
}

data class PlacementUiState(
    val step: PlacementStep = PlacementStep.Loading,
    val summary: PlacementSummary? = null,
    val maxRank: Int = 0,
    /** Hiç görülmemiş, askıda olmayan kelimelerin sıraları: kesme noktasına göre kaç kelimenin bilinen sayılacağı. */
    val unseenRanks: List<Int> = emptyList(),
    /** İlk [PlacementViewModel.BASICS_UP_TO] kelimedeki, hiç görülmemiş yapı kelimelerinin sıraları. */
    val functionRanks: List<Int> = emptyList(),
    /** Kullanıcı "Harfleri ve ekleri biliyorum" dedi mi. */
    val knowsBasics: Boolean = false,
    val undone: Int? = null,
) {
    /** Kesme noktasına göre bilinen sayılacak kelime sayısı (harf ve ekler dahil). */
    fun placeable(cutoff: Int) = unseenRanks.count { it <= cutoff } + extraBasics(cutoff)

    /** Kesme noktasının ötesinde kalıp yalnızca "Harfleri biliyorum" yüzünden eklenecek harf ve ekler. */
    fun extraBasics(cutoff: Int) = if (knowsBasics) functionRanks.count { it > cutoff } else 0
}

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val placement: PlacementRepository,
    private val study: StudyRepository,
    private val wordDao: WordDao,
) : ViewModel() {

    private val _state = MutableStateFlow(PlacementUiState())
    val state: StateFlow<PlacementUiState> = _state.asStateFlow()

    private var test: PlacementTest? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(summary = study.placementSummary(), maxRank = wordDao.maxRank(), step = PlacementStep.Intro) }
        }
    }

    fun start() {
        viewModelScope.launch {
            _state.update { it.copy(step = PlacementStep.Loading, undone = null) }
            val created = placement.newTest()
            test = created
            if (created == null) {
                _state.update { it.copy(step = PlacementStep.Unavailable) }
                return@launch
            }
            val functionRanks = wordDao.unseenFunctionRanksUpTo(BASICS_UP_TO)
            _state.update { it.copy(functionRanks = functionRanks, knowsBasics = false) }
            // Harf ve eklerin hepsi zaten çalışılmışsa sormaya gerek yok.
            if (functionRanks.isEmpty()) {
                showNext(created)
            } else {
                _state.update { it.copy(step = PlacementStep.Basics(wordDao.topFunctionWords(BASICS_SHOWN), functionRanks.size)) }
            }
        }
    }

    /** "Bu harfleri ve ekleri biliyor musun?" cevabı; sonra sorular başlar. */
    fun answerBasics(knows: Boolean) {
        val running = test ?: return
        if (_state.value.step !is PlacementStep.Basics) return
        _state.update { it.copy(knowsBasics = knows) }
        viewModelScope.launch { showNext(running) }
    }

    /** [index]: seçilen şık; -1 "Bilmiyorum". Doğru cevap kısa bir süre gösterilir, sonra sıradaki soru gelir. */
    /** "Biliyorum": şıkları aç. */
    fun knowIt() {
        val current = _state.value.step as? PlacementStep.Question ?: return
        if (current.chosen != null) return
        _state.update { it.copy(step = current.copy(revealed = true)) }
    }

    fun choose(index: Int) {
        val current = _state.value.step as? PlacementStep.Question ?: return
        // Şık ancak "Biliyorum" dendikten sonra seçilebilir; "Bilmiyorum" her an.
        if (index >= 0 && !current.revealed) return
        val running = test ?: return
        if (current.chosen != null) return
        _state.update { it.copy(step = current.copy(chosen = index)) }
        val answer = when (index) {
            -1 -> PlacementAnswer.DontKnow
            current.item.answerIndex -> PlacementAnswer.Correct
            else -> PlacementAnswer.Wrong
        }
        viewModelScope.launch {
            delay(if (answer == PlacementAnswer.Correct) FEEDBACK_MS else FEEDBACK_WRONG_MS)
            running.answer(answer)
            showNext(running)
        }
    }

    private suspend fun showNext(running: PlacementTest) {
        val result = running.result
        val item = running.current
        when {
            result != null -> {
                val ranks = wordDao.unseenRanks()
                _state.update {
                    it.copy(
                        unseenRanks = ranks,
                        step = PlacementStep.Result(result.cutoffRank, result.cutoffRank, result.asked),
                    )
                }
            }
            item != null -> _state.update { it.copy(step = PlacementStep.Question(item, running.answered + 1)) }
        }
    }

    fun setCutoff(value: Int) {
        val result = _state.value.step as? PlacementStep.Result ?: return
        _state.update { it.copy(step = result.copy(cutoff = value.coerceIn(0, it.maxRank))) }
    }

    fun apply() {
        val result = _state.value.step as? PlacementStep.Result ?: return
        viewModelScope.launch {
            _state.update { it.copy(step = PlacementStep.Applying) }
            val placed = study.applyPlacement(result.cutoff, if (_state.value.knowsBasics) BASICS_UP_TO else 0)
            _state.update { it.copy(step = PlacementStep.Done(placed, result.cutoff), summary = study.placementSummary()) }
        }
    }

    fun undo() {
        viewModelScope.launch {
            val undone = study.undoPlacement()
            _state.update { it.copy(undone = undone, summary = study.placementSummary()) }
        }
    }

    fun backToIntro() {
        test = null
        _state.update { it.copy(step = PlacementStep.Intro) }
    }

    companion object {
        /** "Harfleri ve ekleri biliyorum" denirse bu sıraya kadarki bütün yapı kelimeleri bilinen sayılır. */
        const val BASICS_UP_TO = 500

        /** Soru ekranında gösterilen en sık yapı kelimesi sayısı. */
        private const val BASICS_SHOWN = 18
        private const val FEEDBACK_MS = 550L
        private const val FEEDBACK_WRONG_MS = 1300L
    }
}
