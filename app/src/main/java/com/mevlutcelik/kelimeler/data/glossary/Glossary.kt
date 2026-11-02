package com.mevlutcelik.kelimeler.data.glossary

data class Glossary(val sections: List<GlossarySection>) {
    val isEmpty get() = sections.none { it.terms.isNotEmpty() }
}

data class GlossarySection(val title: String, val terms: List<GlossaryTerm>)

data class GlossaryTerm(
    val title: String,
    /** Başlıktaki "|" işaretinden sonraki Arapça ad; yoksa null. */
    val arabic: String?,
    val blocks: List<GlossaryBlock>,
)

sealed interface GlossaryBlock {
    val text: String

    data class Paragraph(override val text: String) : GlossaryBlock
    data class Bullet(override val text: String) : GlossaryBlock
}

/**
 * data/istilahat.md'yi okur. Biçim dosyanın başındaki yorumda anlatılır:
 * "## Bölüm", "### Terim | عربي", her dolu satır bir paragraf, "- " ile başlayanlar madde.
 * Hata vermez: ilk terimden önceki metni ve boş satırları atlar.
 */
object GlossaryParser {
    private val comment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)

    fun parse(text: String): Glossary {
        val sections = mutableListOf<GlossarySection>()
        var sectionTitle: String? = null
        var terms = mutableListOf<GlossaryTerm>()
        var term: TermBuilder? = null

        fun closeTerm() {
            term?.let { terms += it.build() }
            term = null
        }

        fun closeSection() {
            closeTerm()
            if (terms.isNotEmpty()) sections += GlossarySection(sectionTitle.orEmpty(), terms)
            terms = mutableListOf()
        }

        for (raw in text.replace(comment, "").lines()) {
            val line = raw.trim()
            when {
                line.startsWith("### ") -> {
                    closeTerm()
                    val heading = line.removePrefix("### ")
                    val title = heading.substringBefore('|').trim()
                    val arabic = heading.substringAfter('|', "").trim().ifEmpty { null }
                    if (title.isNotEmpty()) term = TermBuilder(title, arabic)
                }
                line.startsWith("## ") -> {
                    closeSection()
                    sectionTitle = line.removePrefix("## ").trim()
                }
                line.startsWith("# ") -> closeTerm()
                else -> term?.add(line)
            }
        }
        closeSection()
        return Glossary(sections)
    }

    private class TermBuilder(val title: String, val arabic: String?) {
        private val blocks = mutableListOf<GlossaryBlock>()

        fun add(line: String) {
            when {
                line.isEmpty() -> Unit
                line.startsWith("- ") || line.startsWith("* ") -> blocks += GlossaryBlock.Bullet(line.substring(2).trim())
                else -> blocks += GlossaryBlock.Paragraph(line)
            }
        }

        fun build() = GlossaryTerm(title, arabic, blocks.toList())
    }
}
