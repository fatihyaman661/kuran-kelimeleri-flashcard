package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.study.functionWordAllowed
import com.mevlutcelik.kelimeler.data.study.isFunctionWord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordKindTest {

    @Test
    fun `verbs nouns and proper nouns are content words`() {
        listOf("Fiil", "İsim", "İsim – mastar", "İsim – ism-i fâil", "İsim / sıfat", "Özel isim").forEach {
            assertFalse(it, isFunctionWord(it))
        }
        listOf("Harf-i tarif", "Atıf harfi", "Muttasıl zamir", "Munfasıl zamir", "İşaret ismi", "İsm-i mevsûl", "İsm-i fiil", "Soru ismi").forEach {
            assertTrue(it, isFunctionWord(it))
        }
    }

    @Test
    fun `a function word needs three other words before the next one`() {
        assertTrue(functionWordAllowed(emptyList()))
        assertFalse(functionWordAllowed(listOf("Atıf harfi")))
        assertFalse(functionWordAllowed(listOf("Fiil", "İsim", "Atıf harfi")))
        assertTrue(functionWordAllowed(listOf("Fiil", "İsim", "Fiil", "Atıf harfi")))
    }
}
