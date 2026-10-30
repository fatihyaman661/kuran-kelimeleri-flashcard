package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.deck.DeckVersion
import com.mevlutcelik.kelimeler.data.deck.SUPPORTED_DECK_FORMAT
import com.mevlutcelik.kelimeler.data.deck.WordDetails
import com.mevlutcelik.kelimeler.util.SearchText
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckFileTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val data = File("../data")

    @Test
    fun `version file describes the bundled deck`() {
        val version = json.decodeFromString<DeckVersion>(File(data, "deck-version.json").readText())
        assertTrue(version.format <= SUPPORTED_DECK_FORMAT)
        assertEquals(File(data, "deck.db.gz").length(), version.bytes)
        assertTrue(version.words > 0)
    }

    @Test
    fun `details json from the build script decodes`() {
        // scripts/build_deck.py'nin details sütununa yazdığı biçimden bir örnek.
        val details = json.decodeFromString<WordDetails>(
            """{"forms":[{"label":"Mâzî","ar":"قَالَ"}],"examples":[{"ar":"قَالَ رَبِّ","highlights":[[0,4]],"tr":"Rabbim dedi.","source":"Tâhâ 20/25"}],"mufredat":{"ar":"…","tr":"…"},"tags":["fiil"]}""",
        )
        assertEquals("قَالَ", details.forms.single().ar)
        assertEquals(listOf(0, 4), details.examples.single().highlights.single())
        assertEquals(null, details.table)
    }

    @Test
    fun `search ignores harakat, alef forms and Turkish accents`() {
        val qala = SearchText.of("قال", "قَالَ", "ق و ل", "قَالَ يَقُولُ قَوْل، قِيل", "demek, söylemek", "Fiil")
        assertTrue(SearchText.query("قال") in qala)
        assertTrue(SearchText.query("يقول") in qala)
        assertTrue(SearchText.query("ق و ل") in qala)
        assertTrue(SearchText.query("SÖYLEMEK") in qala)

        val allah = SearchText.of("اللّه", "اللّٰه", null, "", "Allah", "Özel isim")
        assertTrue(SearchText.query("ٱللَّهُ") in allah)
        assertTrue(SearchText.query("ozel") in allah)
    }
}
