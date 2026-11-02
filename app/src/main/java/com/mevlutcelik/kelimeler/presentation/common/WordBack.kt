package com.mevlutcelik.kelimeler.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.deck.Example
import com.mevlutcelik.kelimeler.data.deck.FormTable
import com.mevlutcelik.kelimeler.data.deck.Mufredat
import com.mevlutcelik.kelimeler.data.deck.TableCell
import com.mevlutcelik.kelimeler.data.deck.WordDetails
import com.mevlutcelik.kelimeler.data.deck.WordForm
import com.mevlutcelik.kelimeler.data.local.WordEntity
import com.mevlutcelik.kelimeler.presentation.theme.AccentGold
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint

private val TranslationColor = androidx.compose.ui.graphics.Color(0xFFB5B2AB)

/**
 * Kartın arka yüzü; destedeki Anki şablonunun (Harekeli, Tip, Çekim, Anlam, kök/sıklık, Örnekler,
 * el-Müfredât) Compose karşılığı. Çalışma ekranında da kelime ayrıntısında da kullanılır.
 */
@Composable
fun WordBack(
    word: WordEntity,
    details: WordDetails,
    modifier: Modifier = Modifier,
    initialExamples: Int = 3,
    mufredatExpanded: Boolean = false,
    /** Kelime ayrıntısında başlık (kelime, tür, anlam) ayrı bir kahraman kartında gösterilir. */
    showHeader: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (showHeader) {
            Text(word.vocalized, style = ArabicStyles.Headword, color = PrimaryMint)
            Text(word.category, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, textAlign = TextAlign.Center)
        }

        if (details.forms.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FormsBox(details.forms)
        }
        details.table?.let {
            Spacer(Modifier.height(14.dp))
            FormTableView(it)
        }

        if (showHeader) {
            Spacer(Modifier.height(16.dp))
            Text(
                word.meaning,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(metaLine(word), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = OnSurfaceMuted, textAlign = TextAlign.Center)
        }

        if (details.examples.isNotEmpty()) {
            if (showHeader || details.forms.isNotEmpty() || details.table != null) SectionDivider()
            Examples(details.examples, initialExamples)
        }
        details.mufredat?.let {
            SectionDivider()
            MufredatSection(it, mufredatExpanded)
        }
    }
}

@Composable
fun metaLine(word: WordEntity): AnnotatedString {
    val meta = stringResource(R.string.detail_meta, word.frequency, word.rank)
    val rootLabel = stringResource(R.string.detail_root)
    return buildAnnotatedString {
        if (!word.root.isNullOrBlank()) {
            append("$rootLabel: ")
            withStyle(ArabicStyles.span(17.sp, OnSurface)) { append(word.root) }
            append(" · ")
        }
        append(meta)
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(top = 18.dp, bottom = 12.dp), color = KelimelerColors.CardBorder)
}

@Composable
private fun FormsBox(forms: List<WordForm>) {
    Surface(
        modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = KelimelerColors.CardBg,
        border = BorderStroke(1.dp, KelimelerColors.CardBorder),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            forms.forEach { form ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(form.label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
                    Text(form.ar, style = ArabicStyles.Form)
                }
            }
        }
    }
}

@Composable
private fun FormTableView(table: FormTable) {
    Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.border(1.dp, KelimelerColors.CardBorder, RoundedCornerShape(8.dp))) {
            table.rows.forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min)) {
                    row.forEach { cell -> TableCellView(cell, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
        table.note?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun TableCellView(cell: TableCell, modifier: Modifier) {
    val background = when {
        cell.isHeader -> AccentGold.copy(alpha = 0.06f)
        cell.isCurrent -> PrimaryMint.copy(alpha = 0.12f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Column(
        modifier
            .background(background)
            .border(0.5.dp, KelimelerColors.CardBorder)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            cell.isArabic -> Text(
                cell.text,
                style = ArabicStyles.TableCell,
                color = if (cell.isCurrent) PrimaryMint else OnSurface,
                fontWeight = if (cell.isCurrent) FontWeight.Bold else null,
            )
            else -> Text(
                cell.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (cell.isHeader || cell.isMuted) OnSurfaceMuted else OnSurface,
                fontWeight = if (cell.isHeader) FontWeight.SemiBold else null,
                textAlign = TextAlign.Center,
            )
        }
        // Alt yazı her hücre türünde gösterilir (ör. işaret isimleri tablosunda Arapça şeklin altındaki "231 defa").
        cell.sub?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = OnSurfaceMuted, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun Examples(examples: List<Example>, initial: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) examples else examples.take(initial)
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.section_examples))
        shown.forEach { ExampleView(it) }
        if (examples.size > initial) {
            Text(
                text = if (expanded) stringResource(R.string.show_less) else stringResource(R.string.show_all_examples, examples.size),
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryMint,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

/** Örnek ayetin Arapçası, kelime vurgulu. Zorlanan kelimede kartın önünde meal olmadan ipucu olarak da kullanılır. */
@Composable
fun ExampleVerse(example: Example, modifier: Modifier = Modifier, style: TextStyle = ArabicStyles.Verse) {
    val verse = remember(example) {
        buildAnnotatedString {
            append(example.ar)
            example.highlights.forEach { (start, end) ->
                val s = start.coerceIn(0, example.ar.length)
                val e = end.coerceIn(s, example.ar.length)
                addStyle(SpanStyle(color = KelimelerColors.Highlight), s, e)
            }
        }
    }
    Text(verse, style = style, modifier = modifier)
}

@Composable
private fun ExampleView(example: Example) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)) {
        ExampleVerse(example, Modifier.fillMaxWidth())
        example.form?.let { form ->
            Text(
                buildAnnotatedString {
                    withStyle(ArabicStyles.span(16.sp)) { append(form.ar) }
                    append(" · ${form.label}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = LearnCoral,
            )
        }
        Text(
            buildAnnotatedString {
                append(example.tr)
                example.source?.let {
                    append("  ")
                    withStyle(SpanStyle(color = AccentGold, fontSize = 12.sp)) { append("($it)") }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TranslationColor,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MufredatSection(mufredat: Mufredat, initiallyExpanded: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(R.string.section_mufredat), Modifier.weight(1f))
            Text(
                stringResource(if (expanded) R.string.hide else R.string.show),
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryMint,
            )
        }
        if (expanded) {
            if (mufredat.ar.isNotBlank()) {
                Text(mufredat.ar, style = ArabicStyles.Prose, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            if (mufredat.tr.isNotBlank()) {
                Text(mufredat.tr, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = TranslationColor, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
