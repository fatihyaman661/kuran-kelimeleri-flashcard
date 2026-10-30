package com.mevlutcelik.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitMix64Test {

    /** SplitMix64'ün yayımlanmış referans çıktıları; değişirse geçmişten hesaplanan aralıklar kayar. */
    @Test
    fun `matches reference outputs`() {
        assertEquals(0xE220A8397B1DCDAFuL.toLong(), SplitMix64.mix(0L))
        assertEquals(6457827717110365317L, SplitMix64.mix(1234567L))
        assertEquals(0.3500795420214081, SplitMix64.unitDouble(1234567L), 0.0)
    }

    @Test
    fun `unit double stays in range and spreads`() {
        val values = (0L until 1000L).map { SplitMix64.unitDouble(it) }
        assertTrue(values.all { it >= 0.0 && it < 1.0 })
        assertTrue(values.average() in 0.45..0.55)
    }
}
