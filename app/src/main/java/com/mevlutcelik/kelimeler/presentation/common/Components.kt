package com.mevlutcelik.kelimeler.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mevlutcelik.kelimeler.presentation.navigation.NavigationDrawerMenuButton
import com.mevlutcelik.kelimeler.presentation.theme.AccentGold
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted

/** Destedeki ".h" başlıkları gibi: altın renkli, büyük harfli küçük etiket. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = AccentGold) {
    Text(
        text = text.uppercase(java.util.Locale.forLanguageTag("tr")),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    color: Color = KelimelerColors.CardBg,
    border: Color = KelimelerColors.CardBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = color,
        contentColor = OnSurface,
        border = BorderStroke(1.dp, border),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** Sekme ekranlarının başlığı: hamburger menü butonu, başlık ve alt başlık (Mescid'deki düzen). */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationDrawerMenuButton()
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(color, CircleShape))
}
