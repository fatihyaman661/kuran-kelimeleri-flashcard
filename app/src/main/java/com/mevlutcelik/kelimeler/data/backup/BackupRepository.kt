package com.mevlutcelik.kelimeler.data.backup

import android.content.Context
import android.net.Uri
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.local.ReviewEntity
import com.mevlutcelik.kelimeler.data.study.StudyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: Long,
    val reviews: List<BackupReview>,
)

@Serializable
data class BackupReview(
    val id: String,
    val wordId: String,
    val reviewedAt: Long,
    val rating: Int,
    val durationMs: Long = 0,
) {
    fun toEntity(synced: Boolean) = ReviewEntity(id, wordId, reviewedAt, rating, durationMs, synced)

    companion object {
        fun of(e: ReviewEntity) = BackupReview(e.id, e.wordId, e.reviewedAt, e.rating, e.durationMs)
    }
}

class InvalidBackupException : IOException("Bu dosya bir Kur'an Kelimeleri yedeği değil")

/**
 * İlerleme = tekrar kayıtları. Yedek dosyası yalnızca bu kayıtları taşır; kart durumları içe
 * aktarılırken yeniden hesaplanır, kelime listesi zaten depodan gelir.
 *
 * Kullanıcının dışa aktardığı dosyaya ek olarak her oturumdan sonra files/auto-backup/progress.json
 * yazılır. Android'in otomatik yedeği yalnızca bu klasörü Google hesabına yedekler (bkz.
 * res/xml/data_extraction_rules.xml); uygulama yeniden kurulunca kayıtlar buradan geri gelir.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reviewDao: ReviewDao,
    private val study: StudyRepository,
    private val json: Json,
) {
    private val autoBackup get() = File(context.filesDir, "auto-backup/progress.json")

    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val file = snapshot()
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Dosya açılamadı")
        stream.bufferedWriter().use { it.write(json.encodeToString(BackupFile.serializer(), file)) }
        file.reviews.size
    }

    /** Dosyadaki kayıtları mevcut ilerlemeyle birleştirir; eklenen kayıt sayısını döner. */
    suspend fun import(uri: Uri): Int {
        val file = withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Dosya açılamadı")
            stream.bufferedReader().use { read(it.readText()) }
        }
        return study.mergeReviews(file.reviews.map { it.toEntity(synced = false) })
    }

    suspend fun writeAutoBackup() = withContext(Dispatchers.IO) {
        val file = snapshot()
        if (file.reviews.isEmpty()) return@withContext
        val target = autoBackup
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "progress.json.tmp")
        temp.writeText(json.encodeToString(BackupFile.serializer(), file))
        temp.renameTo(target)
    }

    /** Yeni kurulumda (hiç tekrar kaydı yokken) Android yedeğinden gelen kayıtları geri yükler. */
    suspend fun restoreAutoBackupIfEmpty(): Int {
        if (reviewDao.count() > 0) return 0
        val file = withContext(Dispatchers.IO) {
            autoBackup.takeIf { it.exists() }?.let { runCatching { read(it.readText()) }.getOrNull() }
        } ?: return 0
        return study.mergeReviews(file.reviews.map { it.toEntity(synced = false) })
    }

    private suspend fun snapshot() = BackupFile(
        format = FORMAT,
        version = VERSION,
        exportedAt = System.currentTimeMillis(),
        reviews = reviewDao.all().map(BackupReview::of),
    )

    private fun read(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException()
        }
        if (file.format != FORMAT || file.version > VERSION) throw InvalidBackupException()
        return file
    }

    companion object {
        const val FORMAT = "kuran-kelimeleri-yedek"
        const val VERSION = 1
    }
}
