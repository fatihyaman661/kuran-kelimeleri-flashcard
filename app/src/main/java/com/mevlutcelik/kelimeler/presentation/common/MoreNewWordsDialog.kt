package com.mevlutcelik.kelimeler.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint

private val Options = listOf(10, 25, 50)

/** Bugünkü yeni kelime sınırına ek seçtirir; ana sayfa ve oturum sonu ekranı kullanır. */
@Composable
fun MoreNewWordsDialog(newPerDay: Int, bonusToday: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.more_new_title)) },
        text = {
            Column {
                Text(
                    if (bonusToday > 0) stringResource(R.string.more_new_body_bonus, newPerDay, bonusToday)
                    else stringResource(R.string.more_new_body, newPerDay),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Options.forEach { amount ->
                        OutlinedButton(
                            onClick = { onPick(amount) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.6f)),
                        ) {
                            Text(stringResource(R.string.more_new_option, amount), color = PrimaryMint)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = OnSurfaceMuted) }
        },
        containerColor = KelimelerColors.CardBg,
    )
}
