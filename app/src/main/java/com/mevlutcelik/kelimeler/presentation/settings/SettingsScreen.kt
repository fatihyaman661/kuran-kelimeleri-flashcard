package com.mevlutcelik.kelimeler.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.kelimeler.BuildConfig
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.settings.StudySettings
import com.mevlutcelik.kelimeler.data.sync.SyncState
import com.mevlutcelik.kelimeler.presentation.common.ContentCard
import com.mevlutcelik.kelimeler.presentation.common.ScreenHeader
import com.mevlutcelik.kelimeler.presentation.common.IconBadge
import com.mevlutcelik.kelimeler.presentation.common.relativeTime
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerColors
import com.mevlutcelik.kelimeler.presentation.theme.LearnCoral
import com.mevlutcelik.kelimeler.presentation.theme.OnMint
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        ScreenHeader(stringResource(R.string.nav_settings), stringResource(R.string.settings_subtitle))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StudySection(state.study, viewModel::setRetention, viewModel::setNewPerDay)
            DeckSection(state, viewModel)
            SyncSection(state, viewModel)
            BackupSection(viewModel)
            AboutSection()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StudySection(study: StudySettings, onRetention: (Float) -> Unit, onNewPerDay: (Int) -> Unit) {
    // Kaydırırken her adımda DataStore'a yazmamak için değer bırakılınca kaydedilir.
    var retention by remember(study.desiredRetention) { mutableFloatStateOf(study.desiredRetention) }
    ContentCard {
        SectionTitle(Icons.Filled.Psychology, stringResource(R.string.section_study))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.retention_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("%${(retention * 100).roundToInt()}", style = MaterialTheme.typography.titleMedium, color = PrimaryMint, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = retention,
            onValueChange = { retention = (it * 100).roundToInt() / 100f },
            onValueChangeFinished = { onRetention(retention) },
            valueRange = StudySettings.MIN_RETENTION..StudySettings.MAX_RETENTION,
            colors = SliderDefaults.colors(thumbColor = PrimaryMint, activeTrackColor = PrimaryMint, inactiveTrackColor = KelimelerColors.ProgressTrack),
        )
        Text(stringResource(R.string.retention_body), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.new_per_day_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.new_per_day_body), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedIconButton(onClick = { onNewPerDay(study.newPerDay - 5) }, enabled = study.newPerDay > 0, border = BorderStroke(1.dp, KelimelerColors.CardBorder)) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
            }
            Text(
                study.newPerDay.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryMint,
                modifier = Modifier.width(44.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            OutlinedIconButton(onClick = { onNewPerDay(study.newPerDay + 5) }, enabled = study.newPerDay < StudySettings.MAX_NEW_PER_DAY, border = BorderStroke(1.dp, KelimelerColors.CardBorder)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
            }
        }
    }
}

@Composable
private fun DeckSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val checking by viewModel.checking.collectAsStateWithLifecycle()
    val notice by viewModel.deckNotice.collectAsStateWithLifecycle()
    ContentCard {
        SectionTitle(Icons.Filled.CloudDownload, stringResource(R.string.section_deck))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.deck_version, state.deck.version.orEmpty(), state.wordCount), style = MaterialTheme.typography.bodyLarge)
        if (state.deck.checkedAt > 0) {
            Text(stringResource(R.string.deck_checked, context.relativeTime(state.deck.checkedAt)), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            text = stringResource(if (checking) R.string.deck_checking else R.string.deck_check),
            onClick = viewModel::checkDeck,
            enabled = !checking,
            busy = checking,
        )
        NoticeText(notice)
    }
}

@Composable
private fun SyncSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val syncNotice by viewModel.syncNotice.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.sync_delete_title)) },
            text = { Text(stringResource(R.string.sync_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteRemoteData() }) {
                    Text(stringResource(R.string.delete), color = LearnCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel), color = OnSurfaceMuted) }
            },
            containerColor = KelimelerColors.CardBg,
        )
    }
    ContentCard {
        SectionTitle(Icons.Filled.Sync, stringResource(R.string.section_sync))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.sync_body), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        Spacer(Modifier.height(12.dp))
        val account = state.account
        when {
            !state.syncConfigured -> Text(stringResource(R.string.sync_not_configured), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
            account == null -> {
                PrimaryButton(stringResource(R.string.sync_sign_in), onClick = { viewModel.signIn(context) })
                if (syncError) NoticeText(Notice(R.string.sync_failed))
                SettingsViewModel.syncMessage(state.syncState)?.let { NoticeText(it) }
            }
            else -> {
                Text(account.email, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (account.lastSyncAt > 0) stringResource(R.string.sync_last, context.relativeTime(account.lastSyncAt)) else stringResource(R.string.sync_never),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                )
                if (state.unsynced > 0) {
                    Text(stringResource(R.string.sync_pending, state.unsynced), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val running = state.syncState == SyncState.Running
                    SecondaryButton(stringResource(R.string.sync_now), onClick = viewModel::syncNow, enabled = !running, busy = running)
                    TextButton(onClick = viewModel::signOut) { Text(stringResource(R.string.sync_sign_out), color = OnSurfaceMuted) }
                }
                SettingsViewModel.syncMessage(state.syncState)?.takeIf { state.syncState != SyncState.Running }?.let { NoticeText(it) }
                TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.sync_delete), color = LearnCoral)
                }
            }
        }
        NoticeText(syncNotice)
    }
}

@Composable
private fun BackupSection(viewModel: SettingsViewModel) {
    val notice by viewModel.backupNotice.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    val fileName = stringResource(R.string.backup_file_name, LocalDate.now().toString())
    ContentCard {
        SectionTitle(Icons.Filled.SaveAlt, stringResource(R.string.section_backup))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.backup_body), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(stringResource(R.string.backup_export), onClick = { exportLauncher.launch(fileName) })
            SecondaryButton(stringResource(R.string.backup_import), onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) })
        }
        NoticeText(notice)
    }
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    val sourceUrl = stringResource(R.string.source_code_url)
    ContentCard {
        SectionTitle(Icons.Filled.Info, stringResource(R.string.section_about))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.about_sources), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        listOf(R.string.source_tanzil, R.string.source_corpus, R.string.source_fsrs, R.string.source_font).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        Row {
            TextButton(onClick = { uriHandler.openUri(sourceUrl) }) {
                Text(stringResource(R.string.about_source_code), color = PrimaryMint)
            }
            // Gizlilik politikası senkron sunucusuyla birlikte yayınlanır (server/public/gizlilik.html).
            if (BuildConfig.SYNC_URL.isNotBlank()) {
                TextButton(onClick = { uriHandler.openUri(BuildConfig.SYNC_URL + "/gizlilik.html") }) {
                    Text(stringResource(R.string.about_privacy), color = PrimaryMint)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = OnMint),
    ) { Text(text) }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, busy: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) PrimaryMint.copy(alpha = 0.6f) else KelimelerColors.CardBorder),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryMint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) PrimaryMint else OnSurfaceMuted)
    }
}

@Composable
private fun NoticeText(notice: Notice?) {
    if (notice == null) return
    Text(
        stringResource(notice.text, *notice.args.toTypedArray()),
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceMuted,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
