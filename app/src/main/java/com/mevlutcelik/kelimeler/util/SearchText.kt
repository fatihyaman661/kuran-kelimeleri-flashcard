package com.mevlutcelik.kelimeler.util

import java.text.Normalizer
import java.util.Locale

/** Aramada hareke, elif çeşitleri ve Türkçe şapkalı harfler fark etmesin diye metni sadeleştirir. */
object SearchText {
    private val turkish = Locale.forLanguageTag("tr")
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640\\u200C\\u200D]")
    private val alefs = Regex("[أإآٱ]")
    private val combining = Regex("\\p{Mn}+")
    private val arabicLetter = Regex("[\\u0600-\\u06FF]")

    fun arabic(value: String): String = value
        .replace(arabicMarks, "")
        .replace(alefs, "ا")
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace('ؤ', 'و')
        .replace('ئ', 'ي')

    fun latin(value: String): String =
        Normalizer.normalize(value.lowercase(turkish), Normalizer.Form.NFD)
            .replace(combining, "")
            .replace('ı', 'i')

    /** [forms]: çekimlerin Arapçası, boşlukla ayrılmış. */
    fun of(word: String, vocalized: String, root: String?, forms: String, meaning: String, type: String): String {
        val arabicParts = listOfNotNull(word, vocalized, root?.replace(" ", ""), forms)
        return arabicParts.joinToString(" ") { arabic(it) } + " " + latin("$meaning $type")
    }

    /** Sorguyu kelimenin [of] metniyle karşılaştırılabilir hale getirir. */
    fun query(value: String): String {
        val trimmed = value.trim()
        return if (arabicLetter.containsMatchIn(trimmed)) arabic(trimmed).replace(" ", "") else latin(trimmed)
    }
}
