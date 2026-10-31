package com.mevlutcelik.kelimeler.presentation.words

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.fsrs.Phase
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.presentation.common.ScreenHeader
import com.mevlutcelik.kelimeler.presentation.common.StatusDot
import com.mevlutcelik.kelimeler.presentation.common.label
import com.mevlutcelik.kelimeler.presentation.common.shortDuration
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.NewBlue
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint

@Composable
fun WordListScreen(
    onOpenWord: (String) -> Unit,
    viewModel: WordListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        ScreenHeader(stringResource(R.string.nav_words), stringResource(R.string.words_subtitle))
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.words_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryMint.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = KelimelerColors.CardBg,
                unfocusedContainerColor = KelimelerColors.CardBg,
                cursorColor = PrimaryMint,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(WordFilter.entries) { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { viewModel.onFilterChange(f) },
                    label = { Text("${stringResource(f.label())} · ${state.counts[f] ?: 0}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryMint,
                        selectedLabelColor = OnMint,
                    ),
                )
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryMint)
            }
            state.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.words_empty), color = OnSurfaceMuted)
            }
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(state.rows, key = { it.id }) { row ->
                    WordRow(row, onClick = { onOpenWord(row.id) })
                    HorizontalDivider(Modifier.padding(start = 74.dp, end = 16.dp), color = KelimelerColors.CardBorder.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun WordFilter.label() = when (this) {
    WordFilter.All -> R.string.filter_all
    WordFilter.New -> R.string.filter_new
    WordFilter.Learning -> R.string.filter_learning
    WordFilter.Learned -> R.string.filter_learned
    WordFilter.Suspended -> R.string.filter_suspended
}

@Composable
private fun WordRow(row: WordRowUi, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(KelimelerColors.CardBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(row.rank.toString(), style = MaterialTheme.typography.labelLarge, color = OnSurfaceMuted)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.meaning, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (row.suspended) KelimelerColors.Suspended else row.phase.color())
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        row.suspended -> stringResource(R.string.filter_suspended)
                        row.phase == Phase.New -> stringResource(row.phase.label())
                        row.dueIn == 0L -> stringResource(R.string.state_due_now)
                        row.dueIn != null -> stringResource(R.string.state_due_in, context.shortDuration(row.dueIn))
                        else -> stringResource(row.phase.label())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(row.vocalized, style = ArabicStyles.ListItem)
    }
}

private fun Phase.color() = when (this) {
    Phase.New -> NewBlue
    Phase.Learning, Phase.Relearning -> LearnCoral
    Phase.Review -> PrimaryMint
}
