package com.mevlutcelik.kelimeler.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.data.backup.BackupRepository
import com.mevlutcelik.kelimeler.data.backup.InvalidBackupException
import com.mevlutcelik.kelimeler.data.deck.DeckCheck
import com.mevlutcelik.kelimeler.data.deck.DeckRepository
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.settings.DeckInfo
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import com.mevlutcelik.kelimeler.data.settings.StudySettings
import com.mevlutcelik.kelimeler.data.settings.SyncAccount
import com.mevlutcelik.kelimeler.data.sync.SessionExpiredException
import com.mevlutcelik.kelimeler.data.sync.SyncRepository
import com.mevlutcelik.kelimeler.data.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bir bölümün altında gösterilen kısa sonuç metni. */
data class Notice(val text: Int, val args: List<Any> = emptyList())

data class SettingsUiState(
    val study: StudySettings = StudySettings(),
    val deck: DeckInfo = DeckInfo(null, 0L),
    val wordCount: Int = 0,
    val syncConfigured: Boolean = false,
    val account: SyncAccount? = null,
    val unsynced: Int = 0,
    val syncState: SyncState = SyncState.Idle,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val deck: DeckRepository,
    private val backup: BackupRepository,
    private val sync: SyncRepository,
    wordDao: WordDao,
    reviewDao: ReviewDao,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settings.study,
        settings.deck,
        wordDao.observeActiveCount(),
        combine(settings.syncAccount, reviewDao.observeUnsyncedCount(), sync.state, ::Triple),
    ) { study, deckInfo, count, (account, unsynced, syncState) ->
        SettingsUiState(study, deckInfo, count, sync.isConfigured, account, unsynced, syncState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(syncConfigured = sync.isConfigured))

    private val _deckNotice = MutableStateFlow<Notice?>(null)
    val deckNotice: StateFlow<Notice?> = _deckNotice.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _backupNotice = MutableStateFlow<Notice?>(null)
    val backupNotice: StateFlow<Notice?> = _backupNotice.asStateFlow()

    fun setRetention(value: Float) = viewModelScope.launch { settings.setRetention(value) }

    fun setNewPerDay(value: Int) = viewModelScope.launch { settings.setNewPerDay(value) }

    fun checkDeck() {
        if (_checking.value) return
        _checking.value = true
        viewModelScope.launch {
            _deckNotice.value = when (val result = deck.checkForUpdate(force = true)) {
                DeckCheck.UpToDate -> Notice(R.string.deck_up_to_date)
                is DeckCheck.Updated -> if (result.change.removed > 0) {
                    Notice(R.string.home_deck_updated_removed, listOf(result.change.added, result.change.removed))
                } else {
                    Notice(R.string.home_deck_updated, listOf(result.change.added))
                }
                DeckCheck.NeedsAppUpdate -> Notice(R.string.deck_needs_app_update)
                is DeckCheck.Failed -> Notice(R.string.deck_check_failed)
            }
            _checking.value = false
        }
    }

    private val _syncError = MutableStateFlow(false)
    val syncError: StateFlow<Boolean> = _syncError.asStateFlow()

    fun signIn(activityContext: Context) = viewModelScope.launch {
        _syncError.value = false
        try {
            if (sync.signIn(activityContext)) sync.sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Credential Manager hesap bulunamadığında ya da ağ hatasında farklı türler atar.
            _syncError.value = true
        }
    }

    fun syncNow() = viewModelScope.launch {
        _syncError.value = false
        sync.sync()
    }

    fun signOut() = viewModelScope.launch { sync.signOut() }

    private val _syncNotice = MutableStateFlow<Notice?>(null)
    val syncNotice: StateFlow<Notice?> = _syncNotice.asStateFlow()

    fun deleteRemoteData() = viewModelScope.launch {
        _syncNotice.value = try {
            sync.deleteRemoteData()
            Notice(R.string.sync_deleted)
        } catch (e: IOException) {
            Notice(R.string.sync_failed)
        }
    }

    fun export(uri: Uri) = viewModelScope.launch {
        _backupNotice.value = try {
            Notice(R.string.backup_exported, listOf(backup.export(uri)))
        } catch (e: IOException) {
            Notice(R.string.backup_failed)
        }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        _backupNotice.value = try {
            Notice(R.string.backup_imported, listOf(backup.import(uri)))
        } catch (e: InvalidBackupException) {
            Notice(R.string.backup_invalid)
        } catch (e: IOException) {
            Notice(R.string.backup_failed)
        }
    }

    companion object {
        fun syncMessage(state: SyncState): Notice? = when (state) {
            SyncState.Idle -> null
            SyncState.Running -> Notice(R.string.sync_running)
            is SyncState.Done -> Notice(R.string.sync_done, listOf(state.pushed, state.pulled))
            is SyncState.Failed -> Notice(if (state.error is SessionExpiredException) R.string.sync_expired else R.string.sync_failed)
        }
    }
}
