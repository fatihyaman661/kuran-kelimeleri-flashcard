package com.mevlutcelik.kelimeler.data.deck

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.mevlutcelik.kelimeler.BuildConfig
import com.mevlutcelik.kelimeler.data.local.AppDatabase
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.local.WordEntity
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import com.mevlutcelik.kelimeler.util.SearchText
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

data class DeckChange(val version: String, val added: Int, val removed: Int)

sealed interface DeckCheck {
    data object UpToDate : DeckCheck
    data class Updated(val change: DeckChange) : DeckCheck
    /** Deste uygulamanın tanımadığı yeni bir biçimde; mağazadan güncelleme gerekiyor. */
    data object NeedsAppUpdate : DeckCheck
    data class Failed(val error: Throwable) : DeckCheck
}

private class UnsupportedDeckException : IOException("Desteklenmeyen deste biçimi")

/**
 * Kelime listesinin tek kaynağı bu depodaki data/deck.db.gz'dir (scripts/build_deck.py üretir).
 * İlk açılışta uygulamayla gelen kopya, sonra GitHub'daki yeni sürümler aynı yoldan alınır: dosya
 * açılır, satırlar kendi veritabanımıza kopyalanır. Kelimeler kimliklerine göre güncellenir;
 * listeden çıkanlar silinmez, ilerlemeleri kalır.
 */
@Singleton
class DeckRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val wordDao: WordDao,
    private val settings: SettingsRepository,
    private val http: OkHttpClient,
    private val json: Json,
) {
    private val mutex = Mutex()

    private val staging get() = File(context.noBackupFilesDir, "deck-import.db")

    /** Hiç deste yüklenmemişse uygulamayla gelen kopyayı yükler. */
    suspend fun ensureInstalled() = mutex.withLock {
        if (settings.deck.first().version != null) return@withLock
        // AGP, assets'teki .gz dosyalarını derlemede açıp uzantıyı siler: APK'da düz deck.db durur.
        withContext(Dispatchers.IO) {
            context.assets.open(BUNDLED_FILE).use { input -> staging.outputStream().use { input.copyTo(it) } }
        }
        import(System.currentTimeMillis())
    }

    /** Son kontrolden bu yana [CHECK_INTERVAL_MS] geçtiyse ya da [force] ise GitHub'daki sürüme bakar. */
    suspend fun checkForUpdate(force: Boolean): DeckCheck = mutex.withLock {
        val now = System.currentTimeMillis()
        val installed = settings.deck.first()
        if (!force && now - installed.checkedAt < CHECK_INTERVAL_MS) return@withLock DeckCheck.UpToDate
        try {
            val remote = withContext(Dispatchers.IO) {
                fetch(VERSION_FILE) { json.decodeFromString(DeckVersion.serializer(), it.bufferedReader().readText()) }
            }
            when {
                remote.format > SUPPORTED_DECK_FORMAT -> DeckCheck.NeedsAppUpdate.also { settings.setDeckCheckedAt(now) }
                remote.version == installed.version -> DeckCheck.UpToDate.also { settings.setDeckCheckedAt(now) }
                else -> {
                    withContext(Dispatchers.IO) { fetch(DECK_FILE, ::unpack) }
                    DeckCheck.Updated(import(now))
                }
            }
        } catch (e: UnsupportedDeckException) {
            DeckCheck.NeedsAppUpdate
        } catch (e: IOException) {
            DeckCheck.Failed(e)
        } catch (e: IllegalArgumentException) {
            // Bozuk sürüm dosyası ya da boş deste: mevcut deste olduğu gibi kalır.
            DeckCheck.Failed(e)
        } catch (e: SQLException) {
            // Yarım inmiş ya da bozuk deste dosyası.
            DeckCheck.Failed(e)
        }
    }

    private fun unpack(input: InputStream) {
        GZIPInputStream(input.buffered()).use { gz -> staging.outputStream().use { gz.copyTo(it) } }
    }

    private fun <T> fetch(file: String, read: (InputStream) -> T): T {
        val request = Request.Builder()
            .url(BuildConfig.DECK_BASE_URL + file)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $file")
            return read(response.body!!.byteStream())
        }
    }

    /** Çıkarılmış deste dosyasını kelime tablosuna kopyalar, sonra dosyayı siler. */
    private suspend fun import(now: Long): DeckChange = withContext(Dispatchers.IO) {
        try {
            SQLiteDatabase.openDatabase(staging.path, null, SQLiteDatabase.OPEN_READONLY).use { source ->
                val meta = source.rawQuery("SELECT key, value FROM meta", null).use { c ->
                    buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
                }
                val format = meta["format"]?.toIntOrNull() ?: throw IOException("Deste biçimi bilinmiyor")
                if (format > SUPPORTED_DECK_FORMAT) throw UnsupportedDeckException()
                val version = meta["version"] ?: throw IOException("Deste sürümü bilinmiyor")

                val before = wordDao.activeIds().toSet()
                val after = db.withTransaction {
                    wordDao.retireAll()
                    source.rawQuery(WORDS_QUERY, null).use { c ->
                        val batch = ArrayList<WordEntity>(BATCH)
                        while (c.moveToNext()) {
                            batch += c.toEntity()
                            if (batch.size == BATCH) {
                                wordDao.insertAll(batch)
                                batch.clear()
                            }
                        }
                        wordDao.insertAll(batch)
                    }
                    val ids = wordDao.activeIds().toSet()
                    // Boş deste bütün kelimeleri emekliye ayırmasın: işlem geri alınır.
                    require(ids.isNotEmpty()) { "Deste boş" }
                    ids
                }
                settings.setDeckVersion(version, now)
                DeckChange(version, added = (after - before).size, removed = (before - after).size)
            }
        } finally {
            staging.delete()
        }
    }

    private fun Cursor.toEntity(): WordEntity {
        val word = getString(2)
        val vocalized = getString(3)
        val type = getString(4)
        val meaning = getString(6)
        val root = if (isNull(7)) null else getString(7)
        return WordEntity(
            id = getString(0),
            rank = getInt(1),
            word = word,
            vocalized = vocalized,
            type = type,
            category = getString(5),
            meaning = meaning,
            root = root,
            frequency = getInt(8),
            details = getString(10),
            searchText = SearchText.of(word, vocalized, root, getString(9), meaning, type),
        )
    }

    private companion object {
        const val DECK_FILE = "deck.db.gz"
        const val BUNDLED_FILE = "deck.db"
        const val VERSION_FILE = "deck-version.json"
        const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
        const val BATCH = 500
        const val WORDS_QUERY =
            "SELECT id, rank, word, vocalized, type, category, meaning, root, frequency, forms, details FROM words ORDER BY rank"
    }
}
