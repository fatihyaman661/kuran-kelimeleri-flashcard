package com.mevlutcelik.kelimeler.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    /** Kelime tablosuna başka tablo bağlı değil; aynı kimlikli satırı değiştirmek güvenli. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("UPDATE words SET retired = 1")
    suspend fun retireAll()

    @Query("SELECT id FROM words WHERE retired = 0")
    suspend fun activeIds(): List<String>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun get(id: String): WordEntity?

    @Query("SELECT COUNT(*) FROM words WHERE retired = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(frequency), 0) FROM words WHERE retired = 0")
    suspend fun totalFrequency(): Long

    @Query(
        """SELECT w.* FROM words w WHERE w.retired = 0
           AND NOT EXISTS (SELECT 1 FROM card_states s WHERE s.wordId = w.id)
           ORDER BY w.rank LIMIT 1""",
    )
    suspend fun nextNew(): WordEntity?

    @Query(
        """SELECT COUNT(*) FROM words w WHERE w.retired = 0
           AND NOT EXISTS (SELECT 1 FROM card_states s WHERE s.wordId = w.id)""",
    )
    suspend fun newCount(): Int

    @Query(
        """SELECT w.id, w.rank, w.vocalized, w.meaning, w.searchText, s.phase, s.due
           FROM words w LEFT JOIN card_states s ON s.wordId = w.id
           WHERE w.retired = 0 ORDER BY w.rank""",
    )
    fun observeList(): Flow<List<WordListRow>>
}

@Dao
interface CardStateDao {
    @Upsert
    suspend fun upsert(state: CardStateEntity)

    @Query("DELETE FROM card_states WHERE wordId = :wordId")
    suspend fun delete(wordId: String)

    @Query("SELECT * FROM card_states WHERE wordId = :wordId")
    suspend fun get(wordId: String): CardStateEntity?

    @Query("SELECT * FROM card_states WHERE wordId = :wordId")
    fun observe(wordId: String): Flow<CardStateEntity?>

    /** Vakti gelmiş kart: önce öğrenme adımındakiler, sonra tekrarlar, en eski vadeli önce. */
    @Query(
        """SELECT s.* FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.due <= :until
           ORDER BY CASE WHEN s.phase = 2 THEN 1 ELSE 0 END, s.due LIMIT 1""",
    )
    suspend fun nextDue(until: Long): CardStateEntity?

    @Query(
        """SELECT s.* FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.phase IN (1, 3) AND s.due <= :until
           ORDER BY s.due LIMIT 1""",
    )
    suspend fun nextLearning(until: Long): CardStateEntity?

    @Query(
        """SELECT MIN(s.due) FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.phase IN (1, 3)""",
    )
    suspend fun nextLearningDue(): Long?

    @Query(
        """SELECT COUNT(*) FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.phase = 2 AND s.due <= :until""",
    )
    suspend fun reviewDueCount(until: Long): Int

    @Query(
        """SELECT COUNT(*) FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.phase IN (1, 3) AND s.due <= :until""",
    )
    suspend fun learningDueCount(until: Long): Int

    @Query(
        """SELECT COUNT(*) FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.phase IN (1, 3)""",
    )
    suspend fun learningCount(): Int

    @Query("SELECT COUNT(*) FROM card_states WHERE introducedAt >= :since")
    suspend fun introducedSince(since: Long): Int

    @Query(
        """SELECT COUNT(*) AS words, COALESCE(SUM(w.frequency), 0) AS frequency
           FROM card_states s JOIN words w ON w.id = s.wordId
           WHERE w.retired = 0 AND s.phase = 2""",
    )
    suspend fun learned(): LearnedTotals
}

@Dao
interface ReviewDao {
    @Insert
    suspend fun insert(review: ReviewEntity)

    /** Zaten olan kimlikler atlanır; dönen listede atlananlar -1'dir. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(reviews: List<ReviewEntity>): List<Long>

    @Query("SELECT * FROM reviews WHERE wordId = :wordId ORDER BY reviewedAt")
    suspend fun forWord(wordId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE wordId = :wordId ORDER BY reviewedAt DESC")
    fun observeForWord(wordId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY reviewedAt DESC LIMIT 1")
    suspend fun latest(): ReviewEntity?

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM reviews ORDER BY reviewedAt")
    suspend fun all(): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE synced = 0 ORDER BY reviewedAt LIMIT :limit")
    suspend fun unsynced(limit: Int): List<ReviewEntity>

    @Query("UPDATE reviews SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("UPDATE reviews SET synced = 0")
    suspend fun markAllUnsynced()

    @Query("SELECT COUNT(*) FROM reviews WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reviews")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun count(): Int

    @Query("SELECT reviewedAt FROM reviews WHERE reviewedAt >= :since")
    suspend fun timesSince(since: Long): List<Long>

    @Query(
        """SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN rating > 1 THEN 1 ELSE 0 END), 0) AS correct
           FROM reviews WHERE reviewedAt >= :since""",
    )
    suspend fun totalsSince(since: Long): ReviewTotals
}
