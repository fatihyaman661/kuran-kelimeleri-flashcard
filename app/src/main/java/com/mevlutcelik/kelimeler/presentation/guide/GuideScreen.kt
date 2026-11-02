package com.mevlutcelik.kelimeler.presentation.guide

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.presentation.common.ScreenHeader
import com.mevlutcelik.kelimeler.presentation.theme.Background
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import kotlinx.coroutines.launch

/** Rehberin iki bölümü: herkesin okuması gereken temel bilgiler ve isteyene ekranların ayrıntısı. */
internal enum class GuidePart(@StringRes val label: Int) {
    Basics(R.string.guide_part_basics),
    Screens(R.string.guide_part_screens),
}

internal enum class GuidePage(val part: GuidePart) {
    Welcome(GuidePart.Basics),
    Frequency(GuidePart.Basics),
    Spacing(GuidePart.Basics),
    Flow(GuidePart.Basics),
    Buttons(GuidePart.Basics),
    Durations(GuidePart.Basics),
    Fsrs(GuidePart.Basics),
    Daily(GuidePart.Basics),
    TopBar(GuidePart.Screens),
    Front(GuidePart.Screens),
    Back(GuidePart.Screens),
    Leech(GuidePart.Screens),
    Menu(GuidePart.Screens),
}

/**
 * "Nasıl kullanılır": aralıklı tekrarı hiç bilmeyen birine uygulamayı sayfa sayfa, etkileşimli
 * örneklerle anlatır. İlk açılışta kendiliğinden açılır, menüden de her zaman ulaşılır.
 */
@Composable
fun GuideScreen(onClose: () -> Unit) {
    val pages = GuidePage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val current = pages[pagerState.currentPage]
    val goTo: (GuidePage) -> Unit = { page -> scope.launch { pagerState.animateScrollToPage(page.ordinal) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = stringResource(R.string.guide_title),
            subtitle = stringResource(current.part.label),
            trailing = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.guide_close), color = PrimaryMint) }
            },
        )
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / pages.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(4.dp),
            color = PrimaryMint,
            trackColor = KelimelerColors.ProgressTrack,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { index ->
            GuidePageContent(pages[index], goTo, onClose)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { pages.getOrNull(pagerState.currentPage - 1)?.let(goTo) },
                enabled = pagerState.currentPage > 0,
            ) {
                Text(stringResource(R.string.guide_prev))
            }
            Text(
                stringResource(R.string.guide_page_of, pagerState.currentPage + 1, pages.size),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            val last = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = { if (last) onClose() else goTo(pages[pagerState.currentPage + 1]) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
            ) {
                Text(stringResource(if (last) R.string.guide_finish else R.string.guide_next))
            }
        }
    }
}

@Composable
private fun GuidePageContent(page: GuidePage, goTo: (GuidePage) -> Unit, onClose: () -> Unit) {
    when (page) {
        GuidePage.Welcome -> WelcomePage()
        GuidePage.Frequency -> FrequencyPage()
        GuidePage.Spacing -> SpacingPage()
        GuidePage.Flow -> FlowPage()
        GuidePage.Buttons -> ButtonsPage()
        GuidePage.Durations -> DurationsPage()
        GuidePage.Fsrs -> FsrsPage()
        GuidePage.Daily -> DailyPage(onStart = onClose, onMore = { goTo(GuidePage.TopBar) })
        GuidePage.TopBar -> TopBarPage()
        GuidePage.Front -> FrontPage()
        GuidePage.Back -> BackPage()
        GuidePage.Leech -> LeechPage()
        GuidePage.Menu -> MenuPage(onStart = onClose)
    }
}

/** Bir rehber sayfasının iskeleti: kaydırılabilir, geniş ekranda ortalanmış sütun. */
@Composable
internal fun GuidePageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun GuideHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = OnSurface)
}

@Composable
internal fun GuideBody(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = OnSurface) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color, modifier = modifier)
}

@Composable
internal fun GuideNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = modifier)
}
