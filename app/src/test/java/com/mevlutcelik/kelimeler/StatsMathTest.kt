package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.study.DayCount
import com.mevlutcelik.kelimeler.data.study.currentStreak
import com.mevlutcelik.kelimeler.data.study.finishDays
import com.mevlutcelik.kelimeler.data.study.forecast
import com.mevlutcelik.kelimeler.data.study.longestStreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsMathTest {

    @Test
    fun `streak counts back from today or from yesterday when today is not done yet`() {
        assertEquals(3, currentStreak(setOf(98, 99, 100), today = 100))
        assertEquals(2, currentStreak(setOf(98, 99), today = 100))
        assertEquals(0, currentStreak(setOf(97, 98), today = 100))
        assertEquals(0, currentStreak(emptySet(), today = 100))
    }

    @Test
    fun `longest streak is the longest run of consecutive days`() {
        assertEquals(4, longestStreak(setOf(1, 2, 3, 4, 7, 8, 20)))
        assertEquals(1, longestStreak(setOf(5)))
        assertEquals(0, longestStreak(emptySet()))
    }

    @Test
    fun `finish estimate divides the remaining words by the daily limit`() {
        assertEquals(453, finishDays(unseen = 4524, newPerDay = 10))
        assertEquals(0, finishDays(unseen = 0, newPerDay = 10))
        assertNull(finishDays(unseen = 100, newPerDay = 0))
    }

    @Test
    fun `overdue cards land on today in the forecast`() {
        assertEquals(
            listOf(DayCount(100, 3), DayCount(101, 0), DayCount(102, 1)),
            forecast(listOf(95, 99, 100, 102, 150), today = 100, days = 3),
        )
    }
}
