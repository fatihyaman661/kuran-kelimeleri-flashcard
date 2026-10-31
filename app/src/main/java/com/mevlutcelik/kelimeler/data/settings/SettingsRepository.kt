package com.mevlutcelik.kelimeler.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mevlutcelik.fsrs.FsrsParameters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Yeni kelimelerin vakti gelmiş tekrarlara göre sırası (Anki'deki "Yeni/tekrar sırası"). */
enum class NewOrder(val key: String) {
    Mixed("mixed"),
    AfterReviews("after"),
    BeforeReviews("before");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: Mixed
    }
}

/**
 * [bonusNew]: yalnızca [bonusNewDay] gününe (DayClock günü) eklenen yeni kelime; ertesi gün sınır
 * yine [newPerDay] olur (Anki'deki "bugünkü yeni kart sınırını artır").
 */
data class StudySettings(
    val desiredRetention: Float = DEFAULT_RETENTION,
    val newPerDay: Int = DEFAULT_NEW_PER_DAY,
    val bonusNewDay: Long = -1,
    val bonusNew: Int = 0,
    val newOrder: NewOrder = NewOrder.Mixed,
) {
    fun fsrs() = FsrsParameters(desiredRetention = desiredRetention.toDouble())

    fun bonusFor(day: Long) = if (day == bonusNewDay) bonusNew else 0

    fun newLimit(day: Long) = newPerDay + bonusFor(day)

    companion object {
        const val DEFAULT_RETENTION = 0.90f
        const val DEFAULT_NEW_PER_DAY = 10
        const val MIN_RETENTION = 0.80f
        const val MAX_RETENTION = 0.97f
        const val MAX_NEW_PER_DAY = 100
        const val MAX_BONUS_NEW = 500
    }
}

data class DeckInfo(val version: String?, val checkedAt: Long)

data class SyncAccount(val token: String, val email: String, val cursor: Long, val lastSyncAt: Long)

@Singleton
class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    val study: Flow<StudySettings> = store.data.map {
        StudySettings(
            desiredRetention = it[RETENTION] ?: StudySettings.DEFAULT_RETENTION,
            newPerDay = it[NEW_PER_DAY] ?: StudySettings.DEFAULT_NEW_PER_DAY,
            bonusNewDay = it[BONUS_NEW_DAY] ?: -1,
            bonusNew = it[BONUS_NEW] ?: 0,
            newOrder = NewOrder.of(it[NEW_ORDER]),
        )
    }

    val deck: Flow<DeckInfo> = store.data.map { DeckInfo(it[DECK_VERSION], it[DECK_CHECKED_AT] ?: 0L) }

    val syncAccount: Flow<SyncAccount?> = store.data.map { prefs ->
        prefs[SYNC_TOKEN]?.let { SyncAccount(it, prefs[SYNC_EMAIL].orEmpty(), prefs[SYNC_CURSOR] ?: 0L, prefs[SYNC_AT] ?: 0L) }
    }

    suspend fun currentStudy() = study.first()

    suspend fun setRetention(value: Float) {
        store.edit { it[RETENTION] = value.coerceIn(StudySettings.MIN_RETENTION, StudySettings.MAX_RETENTION) }
    }

    suspend fun setNewPerDay(value: Int) {
        store.edit { it[NEW_PER_DAY] = value.coerceIn(0, StudySettings.MAX_NEW_PER_DAY) }
    }

    suspend fun setNewOrder(value: NewOrder) {
        store.edit { it[NEW_ORDER] = value.key }
    }

    /** [day] için yeni kelime sınırına [amount] ekler; başka bir günden kalan ek sıfırlanır. */
    suspend fun addBonusNew(day: Long, amount: Int) {
        store.edit {
            val current = if (it[BONUS_NEW_DAY] == day) it[BONUS_NEW] ?: 0 else 0
            it[BONUS_NEW_DAY] = day
            it[BONUS_NEW] = (current + amount).coerceIn(0, StudySettings.MAX_BONUS_NEW)
        }
    }

    suspend fun setDeckVersion(version: String, checkedAt: Long) {
        store.edit {
            it[DECK_VERSION] = version
            it[DECK_CHECKED_AT] = checkedAt
        }
    }

    suspend fun setDeckCheckedAt(checkedAt: Long) {
        store.edit { it[DECK_CHECKED_AT] = checkedAt }
    }

    suspend fun setSyncAccount(token: String, email: String) {
        store.edit {
            it[SYNC_TOKEN] = token
            it[SYNC_EMAIL] = email
            it[SYNC_CURSOR] = 0L
        }
    }

    suspend fun setSyncProgress(cursor: Long, at: Long) {
        store.edit {
            it[SYNC_CURSOR] = cursor
            it[SYNC_AT] = at
        }
    }

    suspend fun clearSyncAccount() {
        store.edit {
            it.remove(SYNC_TOKEN)
            it.remove(SYNC_EMAIL)
            it.remove(SYNC_CURSOR)
            it.remove(SYNC_AT)
        }
    }

    private companion object {
        val RETENTION = floatPreferencesKey("desired_retention")
        val NEW_PER_DAY = intPreferencesKey("new_per_day")
        val BONUS_NEW_DAY = longPreferencesKey("bonus_new_day")
        val BONUS_NEW = intPreferencesKey("bonus_new")
        val NEW_ORDER = stringPreferencesKey("new_order")
        val DECK_VERSION = stringPreferencesKey("deck_version")
        val DECK_CHECKED_AT = longPreferencesKey("deck_checked_at")
        val SYNC_TOKEN = stringPreferencesKey("sync_token")
        val SYNC_EMAIL = stringPreferencesKey("sync_email")
        val SYNC_CURSOR = longPreferencesKey("sync_cursor")
        val SYNC_AT = longPreferencesKey("sync_at")
    }
}
