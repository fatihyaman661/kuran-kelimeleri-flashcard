package com.mevlutcelik.kelimeler.data.placement

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Sıklık bantları; scripts/placement.py içindeki BANDS ile aynı olmalı. Son bandın üst sınırı
 * destenin en büyük sırasıdır.
 */
val PlacementBands: List<IntRange> = listOf(1..50, 51..100, 101..200, 201..400, 401..800, 801..1600, 1601..3200, 3201..Int.MAX_VALUE)

/** Deste dosyasındaki `placement` tablosunun satırı: doğru şık ve üç çeldirici. */
@Serializable
data class PlacementQuestion(val wordId: String, val band: Int, val answer: String, val options: List<String>)

/** Teste hazır soru: kelimenin kartın önündeki hâli (harekesiz + tür) ve karıştırılmış şıklar. */
data class PlacementItem(
    val wordId: String,
    val rank: Int,
    val band: Int,
    val word: String,
    val type: String,
    val choices: List<String>,
    val answerIndex: Int,
)

enum class PlacementAnswer { Correct, Wrong, DontKnow }

data class PlacementResult(
    /** Bu sıraya kadarki kelimeler biliniyor sayılır; 0 ise hiçbiri. */
    val cutoffRank: Int,
    val asked: Int,
)

/**
 * Uyarlanabilir seviye testi. Bir banttan [PER_VISIT] soru sorulur; şans düzeltmeli puan (doğru +1,
 * "Bilmiyorum" 0, yanlış −⅓) [PASS] oranını geçerse üst banda, geçmezse alt banda gidilir. Geçilen
 * ve geçilemeyen komşu iki bant bulununca geçilemeyende [REFINE] soru daha sorulur ve kesme noktası
 * o bandın içinde, puana göre logaritmik ölçekte hesaplanır.
 *
 * Ekrandan bağımsız, saf Kotlin: birim testleri (PlacementTestTest) bunu doğrudan çalıştırır.
 */
class PlacementTest(
    items: List<PlacementItem>,
    private val maxRank: Int,
    private val random: Random = Random.Default,
) {
    private val remaining: List<MutableList<PlacementItem>> =
        PlacementBands.indices.map { band -> items.filter { it.band == band }.shuffled(random).toMutableList() }
    private val asked = IntArray(PlacementBands.size)
    private val score = DoubleArray(PlacementBands.size)
    private val passed = BooleanArray(PlacementBands.size)
    private val failed = BooleanArray(PlacementBands.size)

    private var band = firstBand()
    private var visitLeft = PER_VISIT
    private var refining = false
    private var total = 0

    var current: PlacementItem? = null
        private set
    var result: PlacementResult? = null
        private set

    /** Şimdiye kadar sorulan soru sayısı. */
    val answered get() = total

    init {
        if (band < 0) finish(0) else advance()
    }

    fun answer(answer: PlacementAnswer) {
        if (current == null || result != null) return
        total++
        asked[band]++
        score[band] += when (answer) {
            PlacementAnswer.Correct -> 1.0
            PlacementAnswer.DontKnow -> 0.0
            PlacementAnswer.Wrong -> -1.0 / 3
        }
        visitLeft--
        advance()
    }

    private fun ratio(b: Int) = if (asked[b] == 0) 0.0 else (score[b] / asked[b]).coerceIn(0.0, 1.0)

    private fun advance() {
        if (total >= MAX_QUESTIONS) return finish(cutoffIn(band))
        if (visitLeft > 0 && remaining[band].isNotEmpty()) {
            current = remaining[band].removeAt(remaining[band].lastIndex)
            return
        }
        if (refining) return finish(cutoffIn(band))
        decide()
    }

    /** Ziyaret bitti: bandı geçti mi, nereye gidilecek? */
    private fun decide() {
        if (asked[band] == 0) {
            // Soru kalmamış boş bant: geçilmiş say ve yukarı devam et.
            passed[band] = true
        } else if (ratio(band) >= PASS) {
            passed[band] = true
        } else {
            failed[band] = true
        }
        if (passed[band]) {
            val up = nextWithQuestions(band, +1)
            when {
                up == null -> finish(maxRank)
                failed[up] -> refine(up)
                else -> visit(up)
            }
        } else {
            val down = nextWithQuestions(band, -1)
            when {
                down == null -> finish(cutoffIn(band))
                passed[down] -> refine(band)
                else -> visit(down)
            }
        }
    }

    private fun visit(target: Int) {
        band = target
        visitLeft = PER_VISIT
        advance()
    }

    /** Geçilemeyen sınır bandında birkaç soru daha: kesme noktası o bandın içinde. */
    private fun refine(target: Int) {
        band = target
        refining = true
        visitLeft = REFINE
        advance()
    }

    /** [from]'dan [step] yönünde sorusu olan ilk bant; yoksa null. */
    private fun nextWithQuestions(from: Int, step: Int): Int? {
        var b = from + step
        while (b in PlacementBands.indices) {
            if (remaining[b].isNotEmpty() || asked[b] > 0) return b
            b += step
        }
        return null
    }

    /** [b] bandının içinde, puana göre kesme noktası: puan [PASS]'e yaklaştıkça bandın sonuna. */
    private fun cutoffIn(b: Int): Int {
        val range = PlacementBands[b]
        val low = range.first - 1
        val high = minOf(range.last, maxRank)
        val fraction = (ratio(b) / PASS).coerceIn(0.0, 1.0)
        if (fraction <= 0.0 || high <= low) return max(low, 0)
        val from = ln(max(low, 1).toDouble())
        val to = ln(high.toDouble())
        return exp(from + fraction * (to - from)).roundToInt().coerceIn(max(low, 0), high)
    }

    private fun firstBand(): Int {
        if (remaining.getOrNull(START_BAND)?.isNotEmpty() == true) return START_BAND
        return remaining.indexOfFirst { it.isNotEmpty() }
    }

    private fun finish(cutoff: Int) {
        current = null
        if (result == null) result = PlacementResult(cutoff.coerceIn(0, maxRank), total)
    }

    companion object {
        // 6 soruluk ziyaretler, gürültülü cevaplarla yapılan benzetimde 5'e göre belirgin daha isabetli
        // çıktı (ortalama ~26 soru); 8'e çıkmak isabeti artırmadan testi uzatıyor.
        const val PER_VISIT = 6
        const val REFINE = 6
        /** 6 sorudan 5 doğru 1 yanlış (puan 0,78) geçer; 4 doğru 2 "Bilmiyorum" (0,67) geçmez. */
        const val PASS = 0.7
        const val START_BAND = 2
        const val MAX_QUESTIONS = 45

        /** Havuz satırını, kelimenin bilgileriyle ve karıştırılmış şıklarla teste hazırlar. */
        fun item(question: PlacementQuestion, rank: Int, word: String, type: String, random: Random = Random.Default): PlacementItem {
            val choices = (question.options + question.answer).shuffled(random)
            return PlacementItem(question.wordId, rank, question.band, word, type, choices, choices.indexOf(question.answer))
        }
    }
}
