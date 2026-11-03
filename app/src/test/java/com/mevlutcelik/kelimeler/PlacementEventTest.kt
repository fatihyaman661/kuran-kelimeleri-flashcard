package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.local.ReviewEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementEventTest {

    private fun record(at: Long, rating: Int) = ReviewEntity("r$at", "w", at, rating, 0)

    @Test
    fun `placement events are known but are not answers`() {
        assertTrue(ReviewEntity.isKnown(ReviewEntity.PLACEMENT))
        assertTrue(ReviewEntity.isKnown(ReviewEntity.PLACEMENT_UNDO))
        assertFalse(ReviewEntity.isAnswer(ReviewEntity.PLACEMENT))
        assertFalse(ReviewEntity.isKnown(-5))
    }

    @Test
    fun `last placement event decides and real history wins`() {
        assertNull(ReviewEntity.activePlacement(listOf(record(1, 3))))
        assertEquals(5L, ReviewEntity.activePlacement(listOf(record(5, ReviewEntity.PLACEMENT), record(9, 3)))?.reviewedAt)
        assertNull(ReviewEntity.activePlacement(listOf(record(5, ReviewEntity.PLACEMENT), record(6, ReviewEntity.PLACEMENT_UNDO))))
        // Testten önce çalışılmış kelimeye test dokunmaz.
        assertNull(ReviewEntity.activePlacement(listOf(record(1, 3), record(5, ReviewEntity.PLACEMENT))))
        // Askıya alma olayı testi etkilemez.
        assertEquals(5L, ReviewEntity.activePlacement(listOf(record(2, ReviewEntity.SUSPEND), record(5, ReviewEntity.PLACEMENT)))?.reviewedAt)
    }
}
