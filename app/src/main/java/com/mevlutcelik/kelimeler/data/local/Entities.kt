package com.mevlutcelik.kelimeler.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mevlutcelik.fsrs.Card
import com.mevlutcelik.fsrs.Phase

@Entity(tableName = "words", indices = [Index("rank")])
data class WordEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    val word: String,
    val vocalized: String,
    val type: String,
    val category: String,
    val meaning: String,
    val root: String?,
    val frequency: Int,
    /** [com.mevlutcelik.kelimeler.data.deck.WordDetails] JSON'u. */
    val details: String,
    /** Harekesiz Arapça + aksansız Türkçe; arama bunun üzerinde yapılır. */
    val searchText: String,
    /** Desteden çıkarılmış kelime: ilerlemesi saklanır ama çalışmaya ve listeye gelmez. */
    val retired: Boolean = false,
)

/**
 * Tekrar kaydı. Kayıtlar yalnızca eklenir; kart durumu bunlardan hesaplanır, senkron da bunları
 * kimliğe göre birleştirir. [synced] kaydın sunucuya gönderilip gönderilmediğini tutar.
 */
@Entity(tableName = "reviews", indices = [Index("wordId"), Index("reviewedAt"), Index("synced")])
data class ReviewEntity(
    @PrimaryKey val id: String,
    val wordId: String,
    val reviewedAt: Long,
    val rating: Int,
    val durationMs: Long,
    val synced: Boolean = false,
)

/** Tekrar kayıtlarından türetilen önbellek: kuyruk sorguları hızlı olsun diye tutulur. */
@Entity(tableName = "card_states", indices = [Index("due"), Index("phase")])
data class CardStateEntity(
    @PrimaryKey val wordId: String,
    val phase: Int,
    val step: Int,
    val stability: Double,
    val difficulty: Double,
    val due: Long,
    val lastReview: Long?,
    val reps: Int,
    val lapses: Int,
    /** Kelimenin ilk görüldüğü an; günlük yeni kelime sınırı bununla sayılır. */
    val introducedAt: Long,
) {
    fun toCard() = Card(Phase.of(phase), step, stability, difficulty, due, lastReview, reps, lapses)

    companion object {
        fun of(wordId: String, card: Card, introducedAt: Long) = CardStateEntity(
            wordId = wordId,
            phase = card.phase.value,
            step = card.step,
            stability = card.stability,
            difficulty = card.difficulty,
            due = card.due,
            lastReview = card.lastReview,
            reps = card.reps,
            lapses = card.lapses,
            introducedAt = introducedAt,
        )
    }
}

data class WordListRow(
    val id: String,
    val rank: Int,
    val vocalized: String,
    val meaning: String,
    val searchText: String,
    val phase: Int?,
    val due: Long?,
)

data class LearnedTotals(val words: Int, val frequency: Long)

data class ReviewTotals(val total: Int, val correct: Int)
