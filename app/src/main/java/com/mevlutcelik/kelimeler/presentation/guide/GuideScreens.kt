package com.mevlutcelik.kelimeler.presentation.guide

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.study.LEECH_LAPSES
import com.mevlutcelik.kelimeler.presentation.common.ExampleVerse
import com.mevlutcelik.kelimeler.presentation.common.IconBadge
import com.mevlutcelik.kelimeler.presentation.common.MihrabFrame
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
import com.mevlutcelik.kelimeler.presentation.common.StatusDot
import com.mevlutcelik.kelimeler.presentation.common.metaLine
import com.mevlutcelik.kelimeler.presentation.theme.AccentGold
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.HardAmber
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.LocalPalette
import com.mevlutcelik.kelimeler.presentation.theme.NewBlue
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint

/** Bir görseldeki açıklamalı parça: başlığı ve açıklaması. */
private interface ExplainedPart {
    @get:StringRes val title: Int
    @get:StringRes val body: Int
}

/**
 * Görselin altındaki numaralı liste: her satır bir parçayı seçer; seçili olanın açıklaması hemen
 * altında açılır. Görseldeki parçaya dokunmak da aynı seçimi yapar.
 */
@Composable
private fun <T : ExplainedPart> PartList(
    parts: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    extra: @Composable (T) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEachIndexed { index, part ->
            val active = part == selected
            Surface(
                onClick = { onSelect(part) },
                shape = RoundedCornerShape(14.dp),
                color = if (active) PrimaryMint.copy(alpha = 0.10f) else KelimelerColors.CardBg,
                border = BorderStroke(1.dp, if (active) PrimaryMint.copy(alpha = 0.5f) else KelimelerColors.CardBorder),
                contentColor = OnSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberBadge(index + 1, active)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(part.title),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (active) PrimaryMint else OnSurface,
                        )
                    }
                    if (active) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(part.body), style = MaterialTheme.typography.bodyMedium)
                        extra(part)
                    }
                }
            }
        }
    }
}

// 9 ─ Çalışma ekranının üst çubuğu -------------------------------------------------------------------

private enum class TopBarPart(override val title: Int, override val body: Int) : ExplainedPart {
    Close(R.string.guide_top_close_title, R.string.guide_top_close_body),
    Progress(R.string.guide_top_progress_title, R.string.guide_top_progress_body),
    Counts(R.string.guide_top_counts_title, R.string.guide_top_counts_body),
    Suspend(R.string.guide_top_suspend_title, R.string.guide_top_suspend_body),
    Undo(R.string.guide_top_undo_title, R.string.guide_top_undo_body),
}

@Composable
internal fun TopBarPage() {
    var selected by rememberSaveable { mutableStateOf(TopBarPart.Close) }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_top_title))
        GuideBody(stringResource(R.string.guide_top_body))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = KelimelerColors.CardBg,
            border = BorderStroke(1.dp, KelimelerColors.CardBorder),
            contentColor = OnSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                MockIcon(Icons.Filled.Close, selected == TopBarPart.Close) { selected = TopBarPart.Close }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .guideHighlight(selected == TopBarPart.Progress)
                            .clickable { selected = TopBarPart.Progress },
                    ) {
                        LinearProgressIndicator(
                            progress = { 0.35f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = PrimaryMint,
                            trackColor = KelimelerColors.ProgressTrack,
                            drawStopIndicator = {},
                            gapSize = 0.dp,
                        )
                    }
                    Row(
                        Modifier
                            .guideHighlight(selected == TopBarPart.Counts)
                            .clickable { selected = TopBarPart.Counts },
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        MockCount(7, NewBlue, current = true)
                        MockCount(2, LearnCoral, current = false)
                        MockCount(15, PrimaryMint, current = false)
                    }
                }
                MockIcon(Icons.Outlined.PauseCircle, selected == TopBarPart.Suspend) { selected = TopBarPart.Suspend }
                MockIcon(Icons.AutoMirrored.Filled.Undo, selected == TopBarPart.Undo) { selected = TopBarPart.Undo }
            }
        }
        PartList(TopBarPart.entries, selected, { selected = it }) { part ->
            if (part == TopBarPart.Counts) {
                Spacer(Modifier.height(8.dp))
                CountLegend(NewBlue, stringResource(R.string.guide_count_new))
                CountLegend(LearnCoral, stringResource(R.string.guide_count_learning))
                CountLegend(PrimaryMint, stringResource(R.string.guide_count_review))
            }
        }
    }
}

@Composable
private fun MockIcon(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .guideHighlight(selected, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = OnSurface)
    }
}

@Composable
private fun MockCount(value: Int, color: Color, current: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        StatusDot(color)
        Spacer(Modifier.width(4.dp))
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (current) OnSurface else OnSurfaceMuted,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CountLegend(color: Color, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        StatusDot(color, Modifier.padding(top = 6.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// 10 ─ Kartın ön yüzü ----------------------------------------------------------------------------

private enum class FrontPart(override val title: Int, override val body: Int) : ExplainedPart {
    Kind(R.string.guide_front_kind_title, R.string.guide_front_kind_body),
    Word(R.string.guide_front_word_title, R.string.guide_front_word_body),
    Type(R.string.guide_front_type_title, R.string.guide_front_type_body),
    Tap(R.string.guide_front_tap_title, R.string.guide_front_tap_body),
}

@Composable
internal fun FrontPage() {
    var selected by rememberSaveable { mutableStateOf(FrontPart.Kind) }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_front_title))
        GuideBody(stringResource(R.string.guide_front_body))
        Surface(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            shape = RoundedCornerShape(28.dp),
            color = KelimelerColors.CardBg,
            contentColor = OnSurface,
            border = BorderStroke(1.dp, Brush.verticalGradient(listOf(PrimaryMint.copy(alpha = 0.35f), KelimelerColors.CardBorder))),
            shadowElevation = 6.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                MihrabFrame(Modifier.align(Alignment.Center).fillMaxWidth(0.8f).fillMaxHeight(0.74f))
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .guideHighlight(selected == FrontPart.Kind, shape = RoundedCornerShape(50))
                        .clickable { selected = FrontPart.Kind },
                ) {
                    KindChip(stringResource(R.string.kind_new), NewBlue)
                }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        GuideWord.word,
                        style = ArabicStyles.Headword,
                        modifier = Modifier
                            .guideHighlight(selected == FrontPart.Word)
                            .clickable { selected = FrontPart.Word }
                            .padding(horizontal = 12.dp),
                    )
                    Text(
                        GuideWord.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted,
                        modifier = Modifier
                            .guideHighlight(selected == FrontPart.Type)
                            .clickable { selected = FrontPart.Type }
                            .padding(horizontal = 12.dp),
                    )
                }
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .guideHighlight(selected == FrontPart.Tap)
                        .clickable { selected = FrontPart.Tap },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, tint = KelimelerColors.NavInactive, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.study_tap_to_reveal), style = MaterialTheme.typography.bodySmall, color = KelimelerColors.NavInactive)
                }
            }
        }
        PartList(FrontPart.entries, selected, { selected = it }) { part ->
            if (part == FrontPart.Kind) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KindChip(stringResource(R.string.kind_new), NewBlue)
                    KindChip(stringResource(R.string.kind_learning), LearnCoral)
                    KindChip(stringResource(R.string.kind_review), PrimaryMint)
                }
            }
        }
    }
}

// 11 ─ Kartın arka yüzü ----------------------------------------------------------------------------

private enum class BackPart(override val title: Int, override val body: Int) : ExplainedPart {
    Headword(R.string.guide_back_headword_title, R.string.guide_back_headword_body),
    Forms(R.string.guide_back_forms_title, R.string.guide_back_forms_body),
    Meaning(R.string.guide_back_meaning_title, R.string.guide_back_meaning_body),
    Meta(R.string.guide_back_meta_title, R.string.guide_back_meta_body),
    Examples(R.string.guide_back_examples_title, R.string.guide_back_examples_body),
    Mufredat(R.string.guide_back_mufredat_title, R.string.guide_back_mufredat_body),
    Close(R.string.guide_back_close_title, R.string.guide_back_close_body),
}

@Composable
internal fun BackPage() {
    var selected by rememberSaveable { mutableStateOf(BackPart.Headword) }
    var mufredatOpen by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val positions = remember { mutableStateMapOf<BackPart, Int>() }
    // Seçilen parça kartın kaydırılabilir içinde görünür olsun.
    LaunchedEffect(selected) {
        positions[selected]?.let { scroll.animateScrollTo((it - 24).coerceAtLeast(0)) }
    }
    val select: (BackPart) -> Unit = { part ->
        selected = part
        if (part == BackPart.Mufredat) mufredatOpen = true
    }
    fun Modifier.part(part: BackPart): Modifier = this
        .onGloballyPositioned { positions[part] = it.positionInParent().y.toInt() }
        .clickable(interactionSource = MutableInteractionSource(), indication = null) { select(part) }

    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_back_title))
        GuideBody(stringResource(R.string.guide_back_body))
        Surface(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            shape = RoundedCornerShape(28.dp),
            color = KelimelerColors.CardBg,
            contentColor = OnSurface,
            border = BorderStroke(1.dp, KelimelerColors.CardBorder),
            shadowElevation = 6.dp,
        ) {
            Box {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.part(BackPart.Headword).guideHighlight(selected == BackPart.Headword).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(GuideWord.vocalized, style = ArabicStyles.Headword, color = PrimaryMint)
                        Text(GuideWord.category, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.part(BackPart.Forms).guideHighlight(selected == BackPart.Forms)) {
                        Surface(
                            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = KelimelerColors.CardBg,
                            border = BorderStroke(1.dp, KelimelerColors.CardBorder),
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                GuideDetails.forms.forEach { form ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(form.label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
                                        Text(form.ar, style = ArabicStyles.Form)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        GuideWord.meaning,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.part(BackPart.Meaning).guideHighlight(selected == BackPart.Meaning).fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        metaLine(GuideWord),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.part(BackPart.Meta).guideHighlight(selected == BackPart.Meta).fillMaxWidth(),
                    )
                    HorizontalDivider(Modifier.padding(top = 14.dp, bottom = 8.dp), color = KelimelerColors.CardBorder)
                    Column(Modifier.part(BackPart.Examples).guideHighlight(selected == BackPart.Examples).fillMaxWidth()) {
                        SectionLabel(stringResource(R.string.section_examples))
                        GuideDetails.examples.take(2).forEach { example ->
                            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                                ExampleVerse(example, Modifier.fillMaxWidth())
                                Text(
                                    "${example.tr}  (${example.source.orEmpty()})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LocalPalette.current.translation,
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.show_all_examples, 10),
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryMint,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 8.dp), color = KelimelerColors.CardBorder)
                    Column(Modifier.part(BackPart.Mufredat).guideHighlight(selected == BackPart.Mufredat).fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { mufredatOpen = !mufredatOpen; selected = BackPart.Mufredat },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionLabel(stringResource(R.string.section_mufredat), Modifier.weight(1f))
                            Text(stringResource(if (mufredatOpen) R.string.hide else R.string.show), style = MaterialTheme.typography.labelLarge, color = PrimaryMint)
                        }
                        if (mufredatOpen) {
                            GuideDetails.mufredat?.let { m ->
                                Text(m.ar, style = ArabicStyles.Prose, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                                Text(m.tr, style = MaterialTheme.typography.bodyMedium, color = LocalPalette.current.translation, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .guideHighlight(selected == BackPart.Close, shape = RoundedCornerShape(50)),
                ) {
                    FilledTonalIconButton(
                        onClick = { select(BackPart.Close) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = KelimelerColors.CardBorder,
                            contentColor = OnSurfaceMuted,
                        ),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        PartList(BackPart.entries, selected, select)
    }
}

// 12 ─ Zorlanan kelimeler ve askıya alma -------------------------------------------------------------

@Composable
internal fun LeechPage() {
    var hint by rememberSaveable { mutableStateOf(false) }
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_leech_title))
        GuideBody(stringResource(R.string.guide_leech_body, LEECH_LAPSES))
        MockCardFront(
            word = GuideWord.word,
            type = GuideWord.type,
            chip = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindChip(stringResource(R.string.kind_review), PrimaryMint)
                    KindChip(stringResource(R.string.leech_badge), HardAmber)
                }
            },
            modifier = Modifier.fillMaxWidth().height(340.dp),
            extra = {
                Spacer(Modifier.height(4.dp))
                if (hint) {
                    ExampleVerse(GuideDetails.examples.first(), Modifier.fillMaxWidth(), ArabicStyles.Verse.copy(textAlign = TextAlign.Center))
                }
                TextButton(onClick = { hint = !hint }) {
                    Text(stringResource(if (hint) R.string.leech_hint_hide else R.string.leech_hint_show), color = HardAmber)
                }
            },
        )
        GuideNote(stringResource(R.string.guide_leech_hint))
        // Kartın arka yüzündeki uyarı.
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = HardAmber.copy(alpha = 0.10f),
            contentColor = OnSurface,
            border = BorderStroke(1.dp, HardAmber.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)) {
                Text(stringResource(R.string.leech_title, LEECH_LAPSES), style = MaterialTheme.typography.titleMedium, color = HardAmber)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.guide_leech_banner_short), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Icon(Icons.Outlined.PauseCircle, contentDescription = null, tint = HardAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.leech_suspend), color = HardAmber)
                }
            }
        }
        GuideBody(stringResource(R.string.guide_suspend_body))
        TipBox(stringResource(R.string.guide_suspend_tip))
    }
}

// 13 ─ Menüdeki ekranlar -------------------------------------------------------------------------------

@Composable
internal fun MenuPage(onStart: () -> Unit) {
    GuidePageColumn {
        GuideHeading(stringResource(R.string.guide_menu_title))
        GuideBody(stringResource(R.string.guide_menu_body))
        MenuRow(Icons.Filled.Today, R.string.nav_today, R.string.guide_menu_today)
        MenuRow(Icons.AutoMirrored.Filled.MenuBook, R.string.nav_words, R.string.guide_menu_words)
        MenuRow(Icons.Filled.Insights, R.string.nav_stats, R.string.guide_menu_stats)
        MenuRow(Icons.Filled.Settings, R.string.nav_settings, R.string.guide_menu_settings)
        MenuRow(Icons.Filled.School, R.string.nav_guide, R.string.guide_menu_guide)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.guide_start)) }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, @StringRes title: Int, @StringRes body: Int) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = KelimelerColors.CardBg,
        border = BorderStroke(1.dp, KelimelerColors.CardBorder),
        contentColor = OnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = AccentGold)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
