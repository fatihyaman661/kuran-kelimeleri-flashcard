package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.placement.PlacementAnswer
import com.mevlutcelik.kelimeler.data.placement.PlacementBands
import com.mevlutcelik.kelimeler.data.placement.PlacementItem
import com.mevlutcelik.kelimeler.data.placement.PlacementTest
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementTestTest {

    private val maxRank = 4818

    /** Her banttan 20 soru, bant boyunca eşit aralıklı sıralarla (havuz betiğinin yaptığı gibi). */
    private val pool: List<PlacementItem> = PlacementBands.flatMapIndexed { band, range ->
        val high = minOf(range.last, maxRank)
        (0 until 20).map { i ->
            val rank = range.first + (high - range.first) * i / 20
            PlacementItem("w$rank", rank, band, "w", "Fiil", listOf("a", "b", "c", "d"), 0)
        }
    }

    /** İlk [known] kelimeyi bilen, ötesini hiç bilmeyen ve dürüstçe "Bilmiyorum" diyen kullanıcı. */
    private fun run(known: Int, unknownAnswer: PlacementAnswer = PlacementAnswer.DontKnow): PlacementTest {
        val test = PlacementTest(pool, maxRank, Random(1))
        while (test.result == null) {
            val item = test.current!!
            test.answer(if (item.rank <= known) PlacementAnswer.Correct else unknownAnswer)
        }
        return test
    }

    @Test
    fun `beginner ends near zero`() {
        val result = run(known = 0).result!!
        assertTrue("cutoff ${result.cutoffRank}", result.cutoffRank <= 50)
    }

    @Test
    fun `knowing everything places the whole deck`() {
        assertEquals(maxRank, run(known = maxRank).result!!.cutoffRank)
    }

    @Test
    fun `cutoff lands in the band where knowledge stops`() {
        listOf(150, 300, 700, 1200).forEach { known ->
            val result = run(known).result!!
            val band = PlacementBands.indexOfFirst { known in it }
            val range = PlacementBands[band]
            // Kesme noktası bilinen sınırın bulunduğu bantta ya da bir alt bandın sonunda olmalı.
            assertTrue("known $known → ${result.cutoffRank}", result.cutoffRank in (range.first - 1 - range.count())..range.last)
            assertTrue("asked ${result.asked}", result.asked <= PlacementTest.MAX_QUESTIONS)
        }
    }

    @Test
    fun `random guessing does not inflate the result`() {
        // Bilmediğinde hep yanlış işaretleyen kullanıcı, "Bilmiyorum" diyenden ileri gitmemeli.
        assertTrue(run(300, PlacementAnswer.Wrong).result!!.cutoffRank <= run(300).result!!.cutoffRank)
    }
}
