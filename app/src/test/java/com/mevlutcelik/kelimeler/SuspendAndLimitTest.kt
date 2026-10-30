package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.local.ReviewEntity
import com.mevlutcelik.kelimeler.data.settings.StudySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendAndLimitTest {

    private fun record(at: Long, rating: Int) = ReviewEntity("r$at", "w", at, rating, 0)

    @Test
    fun `only ratings 1 to 4 are answers`() {
        assertTrue((1..4).all(ReviewEntity::isAnswer))
        assertFalse(ReviewEntity.isAnswer(ReviewEntity.SUSPEND))
        assertFalse(ReviewEntity.isAnswer(ReviewEntity.UNSUSPEND))
        assertTrue(ReviewEntity.isKnown(ReviewEntity.SUSPEND))
        assertFalse(ReviewEntity.isKnown(0))
        assertFalse(ReviewEntity.isKnown(5))
    }

    @Test
    fun `last toggle decides whether the word is suspended`() {
        assertNull(ReviewEntity.suspendedSince(emptyList()))
        assertNull(ReviewEntity.suspendedSince(listOf(record(1, 3), record(2, 4))))
        assertEquals(5L, ReviewEntity.suspendedSince(listOf(record(1, 3), record(5, ReviewEntity.SUSPEND))))
        assertNull(ReviewEntity.suspendedSince(listOf(record(5, ReviewEntity.SUSPEND), record(6, ReviewEntity.UNSUSPEND))))
        // Askıdan çıkarılıp yeniden askıya alınınca tarih son olaydan gelir; aradaki cevaplar durumu değiştirmez.
        assertEquals(
            9L,
            ReviewEntity.suspendedSince(
                listOf(record(5, ReviewEntity.SUSPEND), record(6, ReviewEntity.UNSUSPEND), record(7, 3), record(9, ReviewEntity.SUSPEND), record(10, 1)),
            ),
        )
    }

    @Test
    fun `bonus new words apply only to their day`() {
        val settings = StudySettings(newPerDay = 10, bonusNewDay = 100, bonusNew = 50)
        assertEquals(60, settings.newLimit(100))
        assertEquals(10, settings.newLimit(101))
        assertEquals(0, settings.bonusFor(99))
        assertEquals(10, StudySettings(newPerDay = 10).newLimit(100))
    }
}
