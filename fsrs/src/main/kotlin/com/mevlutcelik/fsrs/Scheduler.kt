package com.mevlutcelik.fsrs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * FSRS-6 zamanlayıcısı. Formüller ve öğrenme adımı kuralları py-fsrs 6'nın `Scheduler.review_card`
 * davranışını izler; tek fark geçen sürenin [DayClock] günleriyle ölçülmesi ve tekrar kartlarının
 * gün başına konmasıdır.
 *
 * Aynı kart, zaman ve [fuzzSeed] ile her çağrı aynı sonucu verir. Böylece kart durumu tekrar
 * kayıtlarından ([replay]) birebir yeniden hesaplanabilir.
 */
class Scheduler(
    val parameters: FsrsParameters = FsrsParameters(),
    private val clock: DayClock = ZonedDayClock(),
) {
    private val w = parameters.weights
    private val decay = -w[20]
    private val factor = 0.9.pow(1.0 / decay) - 1.0

    /** Kartın [now] anında hatırlanma olasılığı. Hiç görülmemiş kartta 0'dır. */
    fun retrievability(card: Card, now: Long): Double {
        val last = card.lastReview ?: return 0.0
        if (card.phase == Phase.New) return 0.0
        val elapsed = max(0L, clock.dayOf(now) - clock.dayOf(last))
        return (1.0 + factor * elapsed / card.stability).pow(decay)
    }

    /** Dört cevabın her biri için kartın alacağı yeni durum (butonların üstündeki süreler için). */
    fun preview(card: Card, now: Long, fuzzSeed: Long = 0L): Map<Rating, Card> {
        val next = Rating.entries.associateWith { next(card, it, now, fuzzSeed) }
        if (card.phase != Phase.Review) return next
        // Tekrar aşamasında Zor ≤ İyi < Kolay sıralamasını koru (fuzz ya da yuvarlama bozmasın).
        val today = clock.dayOf(now)
        fun days(c: Card) = clock.dayOf(c.due) - today
        val hard = min(days(next.getValue(Rating.Hard)), days(next.getValue(Rating.Good)))
        val good = max(days(next.getValue(Rating.Good)), hard + 1)
        val easy = max(days(next.getValue(Rating.Easy)), good + 1)
        fun at(rating: Rating, ivl: Long) = next.getValue(rating).copy(due = clock.startOfDay(today + ivl))
        return next + mapOf(
            Rating.Hard to at(Rating.Hard, hard),
            Rating.Good to at(Rating.Good, good),
            Rating.Easy to at(Rating.Easy, easy),
        )
    }

    fun review(card: Card, rating: Rating, now: Long, fuzzSeed: Long = 0L): Card =
        preview(card, now, fuzzSeed).getValue(rating)

    /** Tekrar kayıtlarını sırayla uygulayarak kartın bugünkü durumunu hesaplar. */
    fun replay(reviews: List<Pair<Long, Rating>>, seedBase: Long): Card =
        reviews.sortedBy { it.first }.fold(Card()) { card, (time, rating) ->
            review(card, rating, time, seedBase + card.reps)
        }

    private fun next(card: Card, rating: Rating, now: Long, fuzzSeed: Long): Card {
        val elapsedDays = card.lastReview?.let { clock.dayOf(now) - clock.dayOf(it) }
        val sameDay = elapsedDays != null && elapsedDays < 1

        val stability: Double
        val difficulty: Double
        if (card.phase == Phase.New) {
            stability = initialStability(rating)
            difficulty = initialDifficulty(rating, clamp = true)
        } else {
            stability = when {
                sameDay -> shortTermStability(card.stability, rating)
                else -> nextStability(card, rating, retrievability(card, now))
            }
            difficulty = nextDifficulty(card.difficulty, rating)
        }

        val base = card.copy(
            stability = max(stability, STABILITY_MIN),
            difficulty = difficulty,
            lastReview = now,
            reps = card.reps + 1,
            lapses = card.lapses + if (card.phase == Phase.Review && rating == Rating.Again) 1 else 0,
        )
        return when (card.phase) {
            Phase.New -> stepped(base.copy(phase = Phase.Learning, step = 0), rating, now, fuzzSeed, parameters.learningSteps)
            Phase.Learning -> stepped(base, rating, now, fuzzSeed, parameters.learningSteps)
            Phase.Relearning -> stepped(base, rating, now, fuzzSeed, parameters.relearningSteps)
            Phase.Review -> when {
                rating == Rating.Again && parameters.relearningSteps.isNotEmpty() ->
                    base.copy(phase = Phase.Relearning, step = 0, due = now + parameters.relearningSteps[0].inWholeMilliseconds)
                else -> graduate(base, now, fuzzSeed)
            }
        }
    }

    /** Learning ve Relearning aşamalarındaki adım kuralları (py-fsrs ile aynı). */
    private fun stepped(card: Card, rating: Rating, now: Long, fuzzSeed: Long, steps: List<Duration>): Card {
        if (steps.isEmpty() || (card.step >= steps.size && rating != Rating.Again)) return graduate(card, now, fuzzSeed)
        fun after(d: Duration) = now + d.inWholeMilliseconds
        return when (rating) {
            Rating.Again -> card.copy(step = 0, due = after(steps[0]))
            Rating.Hard -> card.copy(
                due = when {
                    card.step == 0 && steps.size == 1 -> after(steps[0] * 1.5)
                    card.step == 0 -> after((steps[0] + steps[1]) / 2)
                    else -> after(steps[card.step])
                },
            )
            Rating.Good ->
                if (card.step + 1 >= steps.size) graduate(card, now, fuzzSeed)
                else card.copy(step = card.step + 1, due = after(steps[card.step + 1]))
            Rating.Easy -> graduate(card, now, fuzzSeed)
        }
    }

    private fun graduate(card: Card, now: Long, fuzzSeed: Long): Card {
        var interval = nextIntervalDays(card.stability)
        if (parameters.enableFuzz) interval = fuzz(interval, fuzzSeed)
        return card.copy(phase = Phase.Review, step = 0, due = clock.startOfDay(clock.dayOf(now) + interval))
    }

    internal fun nextIntervalDays(stability: Double): Int {
        val raw = stability / factor * (parameters.desiredRetention.pow(1.0 / decay) - 1.0)
        return raw.roundToInt().coerceIn(1, parameters.maximumIntervalDays)
    }

    private fun fuzz(interval: Int, seed: Long): Int {
        if (interval < 2.5) return interval
        var delta = 1.0
        for ((start, end, factor) in FUZZ_RANGES) {
            delta += factor * max(min(interval.toDouble(), end) - start, 0.0)
        }
        val maxIvl = min((interval + delta).roundToInt(), parameters.maximumIntervalDays)
        val minIvl = min(max(2, (interval - delta).roundToInt()), maxIvl)
        val fuzzed = SplitMix64.unitDouble(seed) * (maxIvl - minIvl + 1) + minIvl
        return min(fuzzed.toInt(), parameters.maximumIntervalDays)
    }

    private fun initialStability(rating: Rating) = w[rating.value - 1]

    private fun initialDifficulty(rating: Rating, clamp: Boolean): Double {
        val d = w[4] - exp(w[5] * (rating.value - 1)) + 1.0
        return if (clamp) d.coerceIn(1.0, 10.0) else d
    }

    private fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val delta = -w[6] * (rating.value - 3)
        val damped = difficulty + delta * (10.0 - difficulty) / 9.0
        val reverted = w[7] * initialDifficulty(Rating.Easy, clamp = false) + (1 - w[7]) * damped
        return reverted.coerceIn(1.0, 10.0)
    }

    private fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = exp(w[17] * (rating.value - 3 + w[18])) * stability.pow(-w[19])
        if (rating == Rating.Good || rating == Rating.Easy) increase = max(increase, 1.0)
        return stability * increase
    }

    private fun nextStability(card: Card, rating: Rating, r: Double): Double {
        val d = card.difficulty
        val s = card.stability
        if (rating == Rating.Again) {
            val longTerm = w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp((1 - r) * w[14])
            val shortTerm = s / exp(w[17] * w[18])
            return min(longTerm, shortTerm)
        }
        val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) w[16] else 1.0
        return s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp((1 - r) * w[10]) - 1) * hardPenalty * easyBonus)
    }

    private companion object {
        const val STABILITY_MIN = 0.001
        val FUZZ_RANGES = listOf(
            Triple(2.5, 7.0, 0.15),
            Triple(7.0, 20.0, 0.1),
            Triple(20.0, Double.POSITIVE_INFINITY, 0.05),
        )
    }
}
