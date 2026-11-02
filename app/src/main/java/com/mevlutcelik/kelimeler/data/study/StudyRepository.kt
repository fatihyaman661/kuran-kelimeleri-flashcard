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
import com.mevlutcelik.kelimeler.data.settings.NewOrder
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
    /** Kesintisiz çalışılan gün sayısı; bugün çalışılmadıysa dünden sayılır. */
    val streak: Int,
) {
    val hasWork get() = newAvailable + learningDue + reviewDue > 0
}

/**
 * Seviye testinin durumu: testle bilinen sayılan kelimelerden kaçı henüz sorulmadı, kaçı soruldu
 * ve sorulanların kaçında ilk cevap "Tekrar" oldu (testin ne kadar iyimser olduğunun ölçüsü).
 */
data class PlacementSummary(val pending: Int, val checked: Int, val forgotten: Int) {
    val placed get() = pending + checked
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
            streak = currentStreak(reviewDao.timesSince(0).mapTo(HashSet()) { clock.dayOf(it) }, todayIndex),
        )
    }

    /**
     * Sıradaki kart: vakti gelmiş öğrenme adımları her zaman önce (Anki'de de gün içi öğrenme
     * bekletilmez). Sonra vakti gelmiş tekrarlar ile günlük sınır dahilindeki yeni kelimeler ayardaki
     * sıraya göre ([NewOrder], [pickNew]). Hiçbiri yoksa 20 dakika içinde açılacak öğrenme kartı
     * (Anki'deki "önden öğrenme").
     */
    suspend fun nextCard(now: Long = System.currentTimeMillis(), mix: QueueMix = QueueMix()): StudyCard? {
        stateDao.nextLearning(now)?.let { return load(it.wordId, it, now) }
        val newLeft = minOf(newLeftToday(now), wordDao.newCount())
        if (pickNew(settings.currentStudy().newOrder, stateDao.reviewDueCount(now), newLeft, mix)) {
            nextNewWord()?.let { return load(it.id, null, now, it) }
        }
        stateDao.nextReview(now)?.let { return load(it.wordId, it, now) }
        if (newLeft > 0) nextNewWord()?.let { return load(it.id, null, now, it) }
        stateDao.nextLearning(now + LEARN_AHEAD_MS)?.let { return load(it.wordId, it, now) }
        return null
    }

    /**
     * Sıradaki yeni kelime: sıklık sırasıyla, ama bir yapı kelimesinden (harf, zamir eki…) sonra en az
     * [FUNCTION_WORD_GAP] başka kelime gelir. Başka kelime kalmadıysa yapı kelimeleri sırayla devam eder.
     */
    private suspend fun nextNewWord(): WordEntity? {
        if (functionWordAllowed(stateDao.recentIntroducedTypes(FUNCTION_WORD_GAP))) return wordDao.nextNew()
        return wordDao.nextNewContent() ?: wordDao.nextNew()
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
        // Sıfırlama çalışma ekranındaki "geri al" ile geri alınmaz.
        if (last.rating == ReviewEntity.RESET) return UndoResult.Nothing
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
            if (changed.any { it.rating == ReviewEntity.RESET }) {
                rebuildAll()
            } else {
                changed.mapTo(HashSet()) { it.wordId }.forEach { rebuild(it) }
            }
            changed.size
        }
    }

    /**
     * Seviye testi: [maxRank]'e kadar hiç görülmemiş kelimeleri öğrenilmiş sayar. Her biri Review
     * aşamasına [PLACEMENT_STABILITY_DAYS] kararlılıkla girer; ilk tekrarları yarından başlayarak günde
     * en fazla [PLACEMENT_PER_DAY] tane olacak şekilde serpiştirilir. Sıra kesme noktasından geriye
     * doğrudur: testin en çok yanılabileceği kelimeler önce sorulur. Önceki bir testten bekleyen
     * kelimelerin günleri dolu sayılır. [functionWordsUpTo] > 0 ise ("Harfleri ve ekleri biliyorum")
     * o sıraya kadarki bütün yapı kelimeleri de kesme noktasından bağımsız olarak eklenir. Bilinen
     * sayılan kelime sayısını döner.
     */
    suspend fun applyPlacement(maxRank: Int, functionWordsUpTo: Int = 0, now: Long = System.currentTimeMillis()): Int {
        val ids = wordDao.unseenUpTo(maxRank, functionWordsUpTo)
        if (ids.isEmpty()) return 0
        val taken = stateDao.pendingPlacementDues().groupingBy { clock.dayOf(it) }.eachCount().toMutableMap()
        val scheduler = scheduler()
        var day = clock.dayOf(now) + 1
        val reviews = ArrayList<ReviewEntity>(ids.size)
        val states = ArrayList<CardStateEntity>(ids.size)
        for (id in ids) {
            while ((taken[day] ?: 0) >= PLACEMENT_PER_DAY) day++
            taken[day] = (taken[day] ?: 0) + 1
            val due = clock.startOfDay(day)
            reviews += ReviewEntity(
                id = UUID.randomUUID().toString(),
                wordId = id,
                reviewedAt = now,
                rating = ReviewEntity.PLACEMENT,
                durationMs = due - now,
            )
            // Testle gelen kelime bugünün yeni kelime sınırından yemesin: ilk görülme anı 0.
            states += CardStateEntity.of(id, scheduler.placed(now, due, PLACEMENT_STABILITY_DAYS), introducedAt = 0L)
        }
        db.withTransaction {
            reviewDao.insertAll(reviews)
            stateDao.upsertAll(states)
        }
        return ids.size
    }

    /**
     * Testle bilinen sayılıp henüz hiç sorulmamış kelimeleri yeniden "yeni" yapar. Sorulmuş olanlar
     * kalır: onların durumu artık kullanıcının kendi cevaplarından geliyor. Geri alınan sayıyı döner.
     */
    suspend fun undoPlacement(now: Long = System.currentTimeMillis()): Int {
        val ids = stateDao.pendingPlacementIds()
        if (ids.isEmpty()) return 0
        db.withTransaction {
            reviewDao.insertAll(
                ids.map { ReviewEntity(UUID.randomUUID().toString(), it, now, ReviewEntity.PLACEMENT_UNDO, 0) },
            )
            ids.forEach { rebuild(it) }
        }
        return ids.size
    }

    suspend fun placementSummary(): PlacementSummary {
        val pending = stateDao.pendingPlacementIds().size
        var checked = 0
        var forgotten = 0
        // Testle gelmiş ve en az bir kez cevaplanmış kelimeler: ilk cevaba bakılır.
        reviewDao.allSinceReset().groupBy { it.wordId }.forEach { (_, records) ->
            val placement = ReviewEntity.activePlacement(records) ?: return@forEach
            val first = records.firstOrNull { ReviewEntity.isAnswer(it.rating) && it.reviewedAt >= placement.reviewedAt } ?: return@forEach
            checked++
            if (first.rating == Rating.Again.value) forgotten++
        }
        return PlacementSummary(pending = pending, checked = checked, forgotten = forgotten)
    }

    /**
     * "İlerlemeyi sıfırla": bütün kelimeler hiç çalışılmamış hâle döner. Eski kayıtlar silinmez, bir
     * [ReviewEntity.RESET] kaydı eklenir; ondan önceki kayıtlar hiçbir hesaba girmez. Kayıt senkronla
     * diğer cihazlara da gider. Ayarlar olduğu gibi kalır.
     */
    suspend fun resetProgress(now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            // Başka bir cihazdan gelmiş, saati ileride bir kayıt da sıfırlamanın önünde kalsın.
            val at = maxOf(now, (reviewDao.latest()?.reviewedAt ?: 0L) + 1)
            reviewDao.insert(ReviewEntity(UUID.randomUUID().toString(), ReviewEntity.RESET_WORD_ID, at, ReviewEntity.RESET, 0))
            stateDao.deleteAll()
            suspendedDao.deleteAll()
        }
    }

    /** Senkronla ya da yedekten bir sıfırlama geldiğinde bütün kart durumlarını baştan hesaplar. */
    private suspend fun rebuildAll() {
        stateDao.deleteAll()
        suspendedDao.deleteAll()
        reviewDao.wordIdsSinceReset().forEach { rebuild(it) }
    }

    private suspend fun rebuild(wordId: String) {
        if (wordId == ReviewEntity.RESET_WORD_ID) return
        val records = reviewDao.forWord(wordId)
        val suspendedSince = ReviewEntity.suspendedSince(records)
        if (suspendedSince != null) suspendedDao.upsert(SuspendedEntity(wordId, suspendedSince)) else suspendedDao.delete(wordId)

        val answers = records.filter { ReviewEntity.isAnswer(it.rating) }
        val placement = ReviewEntity.activePlacement(records)
        if (placement != null) {
            val scheduler = scheduler()
            val start = scheduler.placed(placement.reviewedAt, placement.reviewedAt + placement.durationMs, PLACEMENT_STABILITY_DAYS)
            val card = scheduler.replay(answers.map { it.reviewedAt to Rating.of(it.rating) }, seed(wordId, 0), start)
            stateDao.upsert(CardStateEntity.of(wordId, card, introducedAt = 0L))
            return
        }
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

        /** Seviye testinde bilinen sayılan kelimenin başlangıç kararlılığı (gün). */
        const val PLACEMENT_STABILITY_DAYS = 30.0

        /** Seviye testinden gelen kelimelerden bir günde en fazla kaçının ilk tekrarı olur. */
        const val PLACEMENT_PER_DAY = 3
        private const val LEARN_AHEAD_MS = 20 * 60 * 1000L
        private const val MAX_DURATION_MS = 5 * 60 * 1000L
    }
}
