package com.mevlutcelik.kelimeler.data.placement

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.mevlutcelik.kelimeler.data.local.WordDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Seviye tespiti soru havuzu. Havuz deste dosyasının `placement` tablosundan gelir
 * (scripts/build_deck.py); deste her yüklendiğinde [importFrom] ile küçük bir JSON dosyasına yazılır.
 * Kelime tablosuna dokunmadığı için veritabanı şeması değişmez.
 */
@Singleton
class PlacementRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordDao: WordDao,
    private val json: Json,
) {
    private val file get() = File(context.filesDir, POOL_FILE)
    private val serializer = ListSerializer(PlacementQuestion.serializer())

    /**
     * Açık deste dosyasından havuzu okuyup kaydeder; tablo yoksa (eski deste) ya da okunamazsa havuz
     * silinir. Havuzdaki bir sorun deste yüklemesini hiçbir zaman bozmaz.
     */
    fun importFrom(source: SQLiteDatabase) {
        try {
            readFrom(source)
        } catch (e: SQLException) {
            file.delete()
        } catch (e: SerializationException) {
            file.delete()
        } catch (e: IllegalArgumentException) {
            file.delete()
        } catch (e: IOException) {
            file.delete()
        }
    }

    private fun readFrom(source: SQLiteDatabase) {
        val exists = source.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'placement'", null).use { it.moveToFirst() }
        if (!exists) {
            file.delete()
            return
        }
        val options = ListSerializer(String.serializer())
        val questions = source.rawQuery("SELECT word_id, band, answer, options FROM placement", null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(PlacementQuestion(c.getString(0), c.getInt(1), c.getString(2), json.decodeFromString(options, c.getString(3))))
                }
            }
        }
        file.writeText(json.encodeToString(serializer, questions))
    }

    /** Yeni bir test; havuz yoksa ya da hiçbir sorunun kelimesi destede değilse null. */
    suspend fun newTest(random: Random = Random.Default): PlacementTest? = withContext(Dispatchers.IO) {
        val pool = loadPool()
        val items = pool.mapNotNull { question ->
            val word = wordDao.get(question.wordId)?.takeIf { !it.retired } ?: return@mapNotNull null
            PlacementTest.item(question, word.rank, word.word, word.type, random)
        }
        if (items.isEmpty()) null else PlacementTest(items, wordDao.maxRank(), random)
    }

    private fun loadPool(): List<PlacementQuestion> {
        if (!file.exists()) importBundled()
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(serializer, file.readText())
        } catch (e: SerializationException) {
            file.delete()
            emptyList()
        } catch (e: IllegalArgumentException) {
            file.delete()
            emptyList()
        }
    }

    /**
     * Deste, havuzu olmayan eski bir sürümden yüklenmişse (uygulama güncellenmiş ama deste henüz
     * değişmemiş) havuz uygulamayla gelen kopyadan okunur.
     */
    private fun importBundled() {
        val copy = File(context.cacheDir, "placement-bundled.db")
        try {
            context.assets.open(BUNDLED_DECK).use { input -> copy.outputStream().use { input.copyTo(it) } }
            SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READONLY).use { importFrom(it) }
        } catch (e: IOException) {
            // Havuz yok sayılır; ekran testin şu an yapılamadığını söyler.
        } catch (e: SQLException) {
            // Aynı şekilde: havuz yok sayılır.
        } finally {
            copy.delete()
        }
    }

    private companion object {
        const val POOL_FILE = "placement.json"

        /** DeckRepository'deki gibi: AGP assets'teki deck.db.gz'yi açıp deck.db olarak koyar. */
        const val BUNDLED_DECK = "deck.db"
    }
}
