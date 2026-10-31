package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.study.LEECH_LAPSES
import com.mevlutcelik.kelimeler.data.study.isLeech
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeechTest {
    @Test
    fun `a word becomes a leech at the lapse threshold`() {
        assertFalse(isLeech(null))
        assertFalse(isLeech(LEECH_LAPSES - 1))
        assertTrue(isLeech(LEECH_LAPSES))
        assertTrue(isLeech(LEECH_LAPSES + 3))
    }
}
