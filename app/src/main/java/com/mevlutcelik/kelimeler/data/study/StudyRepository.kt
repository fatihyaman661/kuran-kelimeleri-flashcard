package com.mevlutcelik.kelimeler.data.study

import androidx.room.withTransaction
import com.mevlutcelik.fsrs.Card
import com.mevlutcelik.fsrs.DayClock
import com.mevlutcelik.fsrs.Phase
import com.mevlutcelik.fsrs.Rating
import com.mevlutcelik.fsrs.Scheduler
import com.mevlutcelik.kelimeler.data.deck.WordDetails
import com.mevlutcelik.kelimeler.data.local.AppDatabase
import com.mevlutcelik.kelimeler.data.local.CardStateDao
import com.mevlutcelik.kelimeler.data.local.CardStateEntity
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.local.ReviewEntity
import com.mevlutcelik.kelimeler.data.local.SuspendedDao
import com.mevlutcelik.kelimeler.data.local.SuspendedEntity
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.local.WordEntity
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

enum class CardKind { New, Learning, Review }

data class StudyCard(
    val word: WordEntity,
    val details: WordDetails,
    val state: CardStateEntity?,
    val kind: CardKind,
    /** Her cevap için kartın gideceği durum; butonların üstündeki süreler buradan gelir. */
    val preview: Map<Rating, Card>,
)

/** [day]: [DayClock] günü (epoch günü, 04:00'te döner). */
data class DayCount(val day: Long, val reviews: Int)

data class StudyOverview(
    val newAvailable: Int,
    val learningDue: Int,
    val reviewDue: Int,
    val nextLearningDue: Long?,
    val learnedWords: Int,
    val learningWords: Int,
    val suspendedWords: Int,
    val totalWords: Int,
    /** Öğrenilen ve askıdaki (bilinen) kelimelerin metindeki toplam geçişi. */
    val knownFrequency: Long,
    val totalFrequency: Long,
    /** Hiç görülmemiş, askıda olmayan kelime sayısı; bugünkü sınır artırılabilir mi diye bakılır. */
    val unseenWords: Int,
    val newPerDay: Int,
    val bonusNewToday: Int,
    val reviewsToday: Int,
    val correctToday: Int,
    val newToday: Int,
    /** Son 30 günün tekrar sayıları, eskiden yeniye; son eleman bugün. */
    val daily: List<DayCount>,
) {
    val hasWork get() = newAvailable + learningDue + reviewDue > 0
}

sealed interface UndoResult {
    /** [answer]: geri alınan kayıt bir cevap mıydı (değilse askıya alma). */
    data class Undone(val wordId: String, val answer: Boolean) : UndoResult
    data object Nothing : UndoResult
    /** Son tekrar sunucuya gönderilmiş; geri almak cihazlar arasında tutarsızlık yaratır. */
    data object AlreadySynced : UndoResult
}

/** Çalışma kuyruğu ve tekrar kayıtları. Kart durumu her zaman tekrar kayıtlarından türetilebilir. */
@Singleton
class StudyRepository @Inject constructor(
    private val db: AppDatabase,
    private val wordDao: WordDao,
    private val stateDao: CardStateDao,
    private val reviewDao: ReviewDao,
    private val suspendedDao: SuspendedDao,
    private val settings: SettingsRepository,
    private val clock: DayClock,
    private val json: Json,
) {
    private suspend fun scheduler() = Scheduler(settings.currentStudy().fsrs(), clock)

    private fun todayStart(now: Long) = clock.startOfDay(clock.dayOf(now))

    private suspend fun newLeftToday(now: Long): Int {
        val limit = settings.currentStudy().newLimit(clock.dayOf(now))
        return (limit - stateDao.introducedSince(todayStart(now))).coerceAtLeast(0)
    }

    suspend fun overview(now: Long = System.currentTimeMillis()): StudyOverview {
        val learned = stateDao.learned()
        val suspended = suspendedDao.totals()
        val unseen = wordDao.newCount()
        val study = settings.currentStudy()
        val today = reviewDao.totalsSince(todayStart(now))
        val todayIndex = clock.dayOf(now)
        val firstDay = todayIndex - ACTIVITY_DAYS + 1
        val perDay = reviewDao.timesSince(clock.startOfDay(firstDay)).groupingBy { clock.dayOf(it) }.eachCount()
        return StudyOverview(
            newAvailable = minOf(newLeftToday(now), unseen),
            learningDue = stateDao.learningDueCount(now + LEARN_AHEAD_MS),
            reviewDue = stateDao.reviewDueCount(now),
            nextLearningDue = stateDao.nextLearningDue(),
            learnedWords = learned.words,
            learningWords = stateDao.learningCount(),
            suspendedWords = suspended.words,
            totalWords = wordDao.activeIds().size,
            knownFrequency = learned.frequency + suspended.frequency,
            totalFrequency = wordDao.totalFrequency(),
            unseenWords = unseen,
            newPerDay = study.newPerDay,
            bonusNewToday = study.bonusFor(todayIndex),
            reviewsToday = today.total,
            correctToday = today.correct,
            newToday = stateDao.introducedSince(todayStart(now)),
            daily = (firstDay..todayIndex).map { DayCount(it, perDay[it] ?: 0) },
        )
    }

    /**
     * Sıradaki kart: vakti gelmiş öğrenme/tekrar kartları, sonra günlük sınır dahilinde yeni kelime,
     * hiçbiri yoksa 20 dakika içinde açılacak öğrenme kartı (Anki'deki "önden öğrenme").
     */
    suspend fun nextCard(now: Long = System.currentTimeMillis()): StudyCard? {
        stateDao.nextDue(now)?.let { return load(it.wordId, it, now) }
        if (newLeftToday(now) > 0) wordDao.nextNew()?.let { return load(it.id, null, now, it) }
        stateDao.nextLearning(now + LEARN_AHEAD_MS)?.let { return load(it.wordId, it, now) }
        return null
    }

    private suspend fun load(wordId: String, state: CardStateEntity?, now: Long, word: WordEntity? = null): StudyCard? {
        val entity = word ?: wordDao.get(wordId) ?: return null
        val card = state?.toCard() ?: Card()
        val kind = when (card.phase) {
            Phase.New -> CardKind.New
            Phase.Learning, Phase.Relearning -> CardKind.Learning
            Phase.Review -> CardKind.Review
        }
        return StudyCard(
            word = entity,
            details = json.decodeFromString(WordDetails.serializer(), entity.details),
            state = state,
            kind = kind,
            preview = scheduler().preview(card, now, seed(wordId, card.reps)),
        )
    }

    suspend fun answer(card: StudyCard, rating: Rating, now: Long, durationMs: Long) {
        // Önizleme kart açıldığında hesaplandı; kayıtla birebir aynı olsun diye cevap anıyla yeniden hesapla.
        val current = card.state?.toCard() ?: Card()
        val next = scheduler().review(current, rating, now, seed(card.word.id, current.reps))
        db.withTransaction {
            reviewDao.insert(
                ReviewEntity(
                    id = UUID.randomUUID().toString(),
                    wordId = card.word.id,
                    reviewedAt = now,
                    rating = rating.value,
                    durationMs = durationMs.coerceIn(0, MAX_DURATION_MS),
                ),
            )
            stateDao.upsert(CardStateEntity.of(card.word.id, next, card.state?.introducedAt ?: now))
        }
    }

    /**
     * Kelimeyi askıya alır ya da askıdan çıkarır. Askıdaki kelime çalışmaya gelmez; ilerlemesi
     * saklanır ve askıdan çıkınca kaldığı yerden devam eder. Durum zaten istenen gibiyse kayıt eklenmez.
     */
    suspend fun setSuspended(wordId: String, suspended: Boolean, now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            if ((suspendedDao.get(wordId) != null) == suspended) return@withTransaction
            reviewDao.insert(
                ReviewEntity(
                    id = UUID.randomUUID().toString(),
                    wordId = wordId,
                    reviewedAt = now,
                    rating = if (suspended) ReviewEntity.SUSPEND else ReviewEntity.UNSUSPEND,
                    durationMs = 0,
                ),
            )
            rebuild(wordId)
        }
    }

    /** Yalnızca bugün için yeni kelime sınırını [amount] kadar artırır. */
    suspend fun addNewToday(amount: Int, now: Long = System.currentTimeMillis()) {
        settings.addBonusNew(clock.dayOf(now), amount)
    }

    suspend fun undoLast(): UndoResult {
        val last = reviewDao.latest() ?: return UndoResult.Nothing
        if (last.synced) return UndoResult.AlreadySynced
        db.withTransaction {
            reviewDao.delete(last.id)
            rebuild(last.wordId)
        }
        return UndoResult.Undone(last.wordId, ReviewEntity.isAnswer(last.rating))
    }

    /**
     * Yedekten ya da sunucudan gelen kayıtları ekler (var olanlar atlanır) ve etkilenen kelimelerin
     * durumunu kayıtlardan yeniden hesaplar. Eklenen kayıt sayısını döner.
     */
    suspend fun mergeReviews(reviews: List<ReviewEntity>): Int {
        // Daha yeni bir sürümün yazdığı, bu sürümün tanımadığı kayıtlar atlanır.
        val known = reviews.filter { ReviewEntity.isKnown(it.rating) }
        if (known.isEmpty()) return 0
        return db.withTransaction {
            val inserted = reviewDao.insertAll(known)
            val changed = known.filterIndexed { i, _ -> inserted[i] != -1L }
            changed.mapTo(HashSet()) { it.wordId }.forEach { rebuild(it) }
            changed.size
        }
    }

    private suspend fun rebuild(wordId: String) {
        val records = reviewDao.forWord(wordId)
        val suspendedSince = ReviewEntity.suspendedSince(records)
        if (suspendedSince != null) suspendedDao.upsert(SuspendedEntity(wordId, suspendedSince)) else suspendedDao.delete(wordId)

        val answers = records.filter { ReviewEntity.isAnswer(it.rating) }
        if (answers.isEmpty()) {
            stateDao.delete(wordId)
            return
        }
        val card = scheduler().replay(answers.map { it.reviewedAt to Rating.of(it.rating) }, seed(wordId, 0))
        stateDao.upsert(CardStateEntity.of(wordId, card, answers.first().reviewedAt))
    }

    /** Fuzz tohumu kelimeye ve tekrar sayısına bağlı: önizleme, cevap ve yeniden hesaplama aynı sonucu verir. */
    private fun seed(wordId: String, reps: Int) = (wordId.hashCode().toLong() shl 20) + reps

    companion object {
        const val ACTIVITY_DAYS = 30
        private const val LEARN_AHEAD_MS = 20 * 60 * 1000L
        private const val MAX_DURATION_MS = 5 * 60 * 1000L
    }
}
