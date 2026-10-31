package com.mevlutcelik.kelimeler.data.study

import com.mevlutcelik.fsrs.DayClock
import com.mevlutcelik.kelimeler.data.local.CardStateDao
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.local.WordDao
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Bir günün çalışması; [day] [DayClock] günü. */
data class DayActivity(val day: Long, val reviews: Int, val correct: Int, val newWords: Int)

data class StudyStats(
    val today: Long,
    /** Çalışılan günler; ısı haritası bunu gün gün okur. */
    val activity: Map<Long, DayActivity>,
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Int,
    val totalReviews: Int,
    /** Bugünden başlayarak [FORECAST_DAYS] günün tekrar yükü; vakti geçmişler bugüne sayılır. */
    val forecast: List<DayCount>,
    val unseenWords: Int,
    val newPerDay: Int,
    /** Bugünkü sınırla kalan kelimelerin hepsini görmek için gereken gün; sınır 0 ise null. */
    val finishDays: Int?,
    val matureWords: Int,
    val totalWords: Int,
) {
    companion object {
        const val FORECAST_DAYS = 30

        /** FSRS kararlılığı bu kadar günü geçen kelime "kalıcı" sayılır (Anki'deki "olgun" kart). */
        const val MATURE_DAYS = 21.0
    }
}

/** Bugün çalışılmadıysa seri dünden sayılır; bugün henüz bitmemiş bir gündür. */
fun currentStreak(days: Set<Long>, today: Long): Int {
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) {
        streak++
        day--
    }
    return streak
}

fun longestStreak(days: Set<Long>): Int {
    var longest = 0
    var run = 0
    var previous: Long? = null
    for (day in days.sorted()) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    return longest
}

fun finishDays(unseen: Int, newPerDay: Int): Int? = when {
    unseen <= 0 -> 0
    newPerDay <= 0 -> null
    else -> ceil(unseen / newPerDay.toDouble()).toInt()
}

/** [dueDays]: kartların vade günleri. Vakti geçmiş olanlar bugüne eklenir. */
fun forecast(dueDays: List<Long>, today: Long, days: Int): List<DayCount> {
    val counts = dueDays.groupingBy { maxOf(it, today) }.eachCount()
    return (today until today + days).map { DayCount(it, counts[it] ?: 0) }
}

/** İstatistik ekranının verisi: hepsi tekrar kayıtlarından ve kart durumlarından hesaplanır. */
@Singleton
class StatsRepository @Inject constructor(
    private val wordDao: WordDao,
    private val stateDao: CardStateDao,
    private val reviewDao: ReviewDao,
    private val settings: SettingsRepository,
    private val clock: DayClock,
) {
    /** Sorgular birbirinden bağımsız; sırayla değil aynı anda çalışırlar. */
    suspend fun stats(now: Long = System.currentTimeMillis()): StudyStats = coroutineScope {
        val today = clock.dayOf(now)
        val until = clock.startOfDay(today + StudyStats.FORECAST_DAYS)
        val answers = async { reviewDao.answersSince(0) }
        val introduced = async { stateDao.introducedTimesSince(0) }
        val dues = async { stateDao.dueTimesBefore(until) }
        val unseen = async { wordDao.newCount() }
        val mature = async { stateDao.matureCount(StudyStats.MATURE_DAYS) }
        val total = async { wordDao.activeCount() }
        val newPerDay = settings.currentStudy().newPerDay

        val newByDay = introduced.await().groupingBy { clock.dayOf(it) }.eachCount()
        val activity = answers.await().groupBy { clock.dayOf(it.reviewedAt) }.mapValues { (day, rows) ->
            DayActivity(day, rows.size, rows.count { it.rating > 1 }, newByDay[day] ?: 0)
        }
        StudyStats(
            today = today,
            activity = activity,
            currentStreak = currentStreak(activity.keys, today),
            longestStreak = longestStreak(activity.keys),
            activeDays = activity.size,
            totalReviews = answers.await().size,
            forecast = forecast(dues.await().map(clock::dayOf), today, StudyStats.FORECAST_DAYS),
            unseenWords = unseen.await(),
            newPerDay = newPerDay,
            finishDays = finishDays(unseen.await(), newPerDay),
            matureWords = mature.await(),
            totalWords = total.await(),
        )
    }
}
