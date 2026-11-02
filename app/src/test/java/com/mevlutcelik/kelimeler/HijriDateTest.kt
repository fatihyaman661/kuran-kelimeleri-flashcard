package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.presentation.common.hijriDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HijriDateTest {

    @Test
    fun `gregorian dates convert to umm al-qura with turkish month names`() {
        assertEquals("1 Ramazan 1447", hijriDate(LocalDate.of(2026, 2, 18)))
        assertEquals("1 Muharrem 1448", hijriDate(LocalDate.of(2026, 6, 16)))
        assertEquals("10 Zilhicce 1447", hijriDate(LocalDate.of(2026, 5, 27)))
    }
}
