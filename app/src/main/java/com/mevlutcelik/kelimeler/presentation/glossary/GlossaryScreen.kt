package com.mevlutcelik.kelimeler.presentation.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.glossary.GlossaryBlock
import com.mevlutcelik.kelimeler.data.glossary.GlossaryTerm
import com.mevlutcelik.kelimeler.presentation.common.ScreenHeader
import com.mevlutcelik.kelimeler.presentation.common.SectionLabel
import com.mevlutcelik.kelimeler.presentation.theme.ArabicStyles
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint

@Composable
fun GlossaryScreen(viewModel: GlossaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        ScreenHeader(stringResource(R.string.nav_glossary), stringResource(R.string.glossary_subtitle))
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.glossary_search)) },
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
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryMint)
            }
            state.sections.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.words_empty), color = OnSurfaceMuted)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                state.sections.forEach { section ->
                    // Anahtar verilmiyor: dosyada aynı başlık iki kez yazılırsa liste çökmesin.
                    item {
                        SectionLabel(section.title, Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp))
                    }
                    items(section.terms) { term ->
                        TermItem(term)
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = KelimelerColors.CardBorder.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TermItem(term: GlossaryTerm) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(term.title, style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.weight(1f))
            if (term.arabic != null) {
                Spacer(Modifier.width(12.dp))
                Text(term.arabic, style = ArabicStyles.ListItem)
            }
        }
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            term.blocks.forEach { block ->
                when (block) {
                    is GlossaryBlock.Paragraph -> Text(
                        rich(block.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                    is GlossaryBlock.Bullet -> Row {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = PrimaryMint)
                        Spacer(Modifier.width(8.dp))
                        Text(rich(block.text), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    }
                }
            }
        }
    }
}

/** Metindeki **kalın** işaretlerini kalın yazıya çevirir. */
private fun rich(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
    }
}
