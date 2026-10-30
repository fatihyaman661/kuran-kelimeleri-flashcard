package com.mevlutcelik.kelimeler.data.deck

import kotlinx.serialization.Serializable

/** Uygulamanın okuyabildiği en yeni deste biçimi (scripts/build_deck.py içindeki FORMAT). */
const val SUPPORTED_DECK_FORMAT = 1

/** data/deck-version.json: güncelleme kontrolünde yalnızca bu küçük dosya indirilir. */
@Serializable
data class DeckVersion(
    val format: Int,
    val version: String,
    val words: Int,
    val bytes: Long = 0,
)

/** Kartın arka yüzündeki ayrıntılar; deste dosyasında ve veritabanında JSON olarak durur. */
@Serializable
data class WordDetails(
    val forms: List<WordForm> = emptyList(),
    val table: FormTable? = null,
    val examples: List<Example> = emptyList(),
    val mufredat: Mufredat? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class WordForm(val label: String, val ar: String)

@Serializable
data class FormTable(val rows: List<List<TableCell>>, val note: String? = null)

@Serializable
data class TableCell(val text: String, val kind: String, val sub: String? = null) {
    val isHeader get() = kind == "header"
    val isArabic get() = kind == "arabic" || kind == "current"
    val isCurrent get() = kind == "current"
    val isMuted get() = kind == "muted"
}

@Serializable
data class Example(
    val ar: String,
    /** Vurgulanan kelimenin [başlangıç, bitiş) aralıkları, UTF-16 indeksleriyle. */
    val highlights: List<List<Int>> = emptyList(),
    val tr: String,
    val source: String? = null,
    val form: ExampleForm? = null,
)

@Serializable
data class ExampleForm(val ar: String, val label: String)

@Serializable
data class Mufredat(val ar: String, val tr: String)
