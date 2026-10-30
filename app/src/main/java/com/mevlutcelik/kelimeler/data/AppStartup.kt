package com.mevlutcelik.kelimeler.data

import android.util.Log
import com.mevlutcelik.kelimeler.data.backup.BackupRepository
import com.mevlutcelik.kelimeler.data.deck.DeckChange
import com.mevlutcelik.kelimeler.data.deck.DeckCheck
import com.mevlutcelik.kelimeler.data.deck.DeckRepository
import com.mevlutcelik.kelimeler.data.sync.SyncRepository
import com.mevlutcelik.kelimeler.di.ApplicationScope
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Açılış sırası: uygulamayla gelen desteyi yükle → yeni kurulumsa Android yedeğinden ilerlemeyi
 * geri getir → (çalışma artık açık) GitHub'da yeni deste var mı bak → girişliyse senkronla.
 */
@Singleton
class AppStartup @Inject constructor(
    private val deck: DeckRepository,
    private val backup: BackupRepository,
    private val sync: SyncRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _deckChange = MutableStateFlow<DeckChange?>(null)
    /** Arka planda uygulanan deste güncellemesi; ana sayfada bir kez gösterilir. */
    val deckChange: StateFlow<DeckChange?> = _deckChange.asStateFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                deck.ensureInstalled()
                backup.restoreAutoBackupIfEmpty()
            } catch (e: Exception) {
                Log.e(TAG, "Açılış hazırlığı başarısız", e)
            }
            _ready.value = true

            val check = deck.checkForUpdate(force = false)
            if (check is DeckCheck.Updated && (check.change.added > 0 || check.change.removed > 0)) {
                _deckChange.value = check.change
            }
            sync.sync()
        }
    }

    fun dismissDeckChange() {
        _deckChange.value = null
    }

    private companion object {
        const val TAG = "AppStartup"
    }
}
