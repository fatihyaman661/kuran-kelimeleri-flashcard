package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.glossary.GlossaryBlock
import com.mevlutcelik.kelimeler.data.glossary.GlossaryParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryTest {

    @Test
    fun `parses sections, terms, arabic names, paragraphs and bullets`() {
        val glossary = GlossaryParser.parse(
            """
            # Istılahat
            <!--
            ### Yorumdaki terim görünmez
            -->
            Başlıktan önceki metin atlanır.

            ## Temel
            ### Kök | اَلْجَذْرُ
            Birinci satır.
            İkinci satır.

            - madde **kalın**
            ### Arapçasız
            Açıklama.

            ## Harfler
            ### Harf-i cer | حَرْفُ الْجَرِّ
            Metin.
            """.trimIndent(),
        )
        assertEquals(listOf("Temel", "Harfler"), glossary.sections.map { it.title })
        val root = glossary.sections[0].terms[0]
        assertEquals("Kök", root.title)
        assertEquals("اَلْجَذْرُ", root.arabic)
        assertEquals(
            listOf(
                GlossaryBlock.Paragraph("Birinci satır."),
                GlossaryBlock.Paragraph("İkinci satır."),
                GlossaryBlock.Bullet("madde **kalın**"),
            ),
            root.blocks,
        )
        assertNull(glossary.sections[0].terms[1].arabic)
        assertEquals(2, glossary.sections[0].terms.size)
    }

    @Test
    fun `empty or headless text gives an empty glossary`() {
        assertTrue(GlossaryParser.parse("").isEmpty)
        assertTrue(GlossaryParser.parse("# Istılahat\nsadece metin").isEmpty)
    }

    @Test
    fun `bundled glossary file is well formed`() {
        val glossary = GlossaryParser.parse(File("../data/istilahat.md").readText())
        assertFalse(glossary.isEmpty)
        val terms = glossary.sections.flatMap { it.terms }
        terms.forEach { assertTrue("Açıklaması boş terim: ${it.title}", it.blocks.isNotEmpty()) }
        val duplicates = terms.groupBy { it.title }.filterValues { it.size > 1 }.keys
        assertTrue("Birden çok kez yazılmış terim: $duplicates", duplicates.isEmpty())
    }
}
