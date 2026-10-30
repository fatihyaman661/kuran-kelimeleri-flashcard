package com.mevlutcelik.fsrs

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {

    private val zone = ZoneOffset.UTC
    private val clock = ZonedDayClock(zone)
    private val noFuzz = Scheduler(FsrsParameters(enableFuzz = false), clock)
    private val w = FsrsParameters.DEFAULT_WEIGHTS

    private fun at(text: String) = LocalDateTime.parse(text).toInstant(zone).toEpochMilli()
    private fun daysBetween(from: Long, to: Long) = clock.dayOf(to) - clock.dayOf(from)

    @Test
    fun `new card follows the learning steps`() {
        val now = at("2026-10-30T10:00:00")
        val preview = noFuzz.preview(Card(), now)

        assertEquals(now + 1.minutes.inWholeMilliseconds, preview.getValue(Rating.Again).due)
        assertEquals(now + 5.5.minutes.inWholeMilliseconds, preview.getValue(Rating.Hard).due)
        assertEquals(now + 10.minutes.inWholeMilliseconds, preview.getValue(Rating.Good).due)
        assertEquals(Phase.Learning, preview.getValue(Rating.Good).phase)
        assertEquals(1, preview.getValue(Rating.Good).step)

        // Kolay doğrudan mezun eder; %90 hedefte aralık başlangıç kararlılığına (w3 ≈ 8.3) eşittir.
        val easy = preview.getValue(Rating.Easy)
        assertEquals(Phase.Review, easy.phase)
        assertEquals(8L, daysBetween(now, easy.due))
        assertEquals(clock.startOfDay(clock.dayOf(easy.due)), easy.due)
    }

    @Test
    fun `initial stability and difficulty come from the weights`() {
        val now = at("2026-10-30T10:00:00")
        for (rating in Rating.entries) {
            val card = noFuzz.review(Card(), rating, now)
            assertEquals(w[rating.value - 1], card.stability, 1e-9)
            val d0 = (w[4] - kotlin.math.exp(w[5] * (rating.value - 1)) + 1).coerceIn(1.0, 10.0)
            assertEquals(d0, card.difficulty, 1e-9)
            assertEquals(1, card.reps)
        }
    }

    @Test
    fun `two goods on the same day graduate with short term stability`() {
        val start = at("2026-10-30T10:00:00")
        val first = noFuzz.review(Card(), Rating.Good, start)
        val second = noFuzz.review(first, Rating.Good, first.due)

        assertEquals(Phase.Review, second.phase)
        // Aynı gün İyi: artış en az 1 olduğundan kararlılık w2'de kalır → 2 gün.
        assertEquals(w[2], second.stability, 1e-9)
        assertEquals(2L, daysBetween(start, second.due))
    }

    @Test
    fun `interval equals stability at ninety percent retention`() {
        for (s in listOf(1.0, 3.6, 12.2, 45.0, 400.0)) {
            assertEquals(Math.round(s).toInt().coerceAtLeast(1), noFuzz.nextIntervalDays(s))
        }
        val higher = Scheduler(FsrsParameters(desiredRetention = 0.95, enableFuzz = false), clock)
        assertTrue(higher.nextIntervalDays(30.0) < noFuzz.nextIntervalDays(30.0))
    }

    @Test
    fun `retrievability is ninety percent after stability days`() {
        val last = at("2026-10-01T10:00:00")
        val card = Card(phase = Phase.Review, stability = 10.0, difficulty = 5.0, lastReview = last)
        val r = noFuzz.retrievability(card, at("2026-10-11T10:00:00"))
        assertEquals(0.9, r, 1e-9)
        assertTrue(noFuzz.retrievability(card, at("2026-10-31T10:00:00")) < r)
    }

    @Test
    fun `review answers keep hard below good below easy`() {
        val last = at("2026-10-01T10:00:00")
        val card = Card(phase = Phase.Review, stability = 10.0, difficulty = 5.0, lastReview = last, reps = 4, due = at("2026-10-11T04:00:00"))
        val now = at("2026-10-11T12:00:00")
        repeat(50) { seed ->
            val p = Scheduler(FsrsParameters(), clock).preview(card, now, seed.toLong())
            val hard = daysBetween(now, p.getValue(Rating.Hard).due)
            val good = daysBetween(now, p.getValue(Rating.Good).due)
            val easy = daysBetween(now, p.getValue(Rating.Easy).due)
            assertTrue("hard=$hard good=$good", hard < good)
            assertTrue("good=$good easy=$easy", good < easy)
        }
        val p = noFuzz.preview(card, now)
        assertTrue(p.getValue(Rating.Good).stability > card.stability)
        assertTrue(p.getValue(Rating.Again).difficulty > card.difficulty)
        assertTrue(p.getValue(Rating.Easy).difficulty < card.difficulty)
    }

    @Test
    fun `forgetting a review card relearns and counts a lapse`() {
        val card = Card(phase = Phase.Review, stability = 20.0, difficulty = 5.0, lastReview = at("2026-10-01T10:00:00"), reps = 5)
        val now = at("2026-10-21T10:00:00")
        val again = noFuzz.review(card, Rating.Again, now)

        assertEquals(Phase.Relearning, again.phase)
        assertEquals(1, again.lapses)
        assertTrue(again.stability < card.stability)
        assertEquals(now + 10.minutes.inWholeMilliseconds, again.due)

        val back = noFuzz.review(again, Rating.Good, again.due)
        assertEquals(Phase.Review, back.phase)
        assertTrue(daysBetween(again.due, back.due) >= 1)
    }

    @Test
    fun `fuzz is deterministic and stays near the interval`() {
        val scheduler = Scheduler(FsrsParameters(), clock)
        val card = Card(phase = Phase.Review, stability = 30.0, difficulty = 5.0, lastReview = at("2026-09-30T10:00:00"), reps = 6)
        val now = at("2026-10-30T10:00:00")
        val base = noFuzz.review(card, Rating.Good, now)
        val baseDays = daysBetween(now, base.due)
        repeat(30) { seed ->
            val a = scheduler.review(card, Rating.Good, now, seed.toLong())
            val b = scheduler.review(card, Rating.Good, now, seed.toLong())
            assertEquals(a, b)
            val days = daysBetween(now, a.due)
            assertTrue("$days vs $baseDays", abs(days - baseDays) <= baseDays * 0.1 + 2)
        }
    }

    @Test
    fun `replay rebuilds the same card as live reviews`() {
        val scheduler = Scheduler(FsrsParameters(), clock)
        val times = listOf("2026-10-01T09:00:00", "2026-10-01T09:01:30", "2026-10-01T09:12:00", "2026-10-03T08:00:00", "2026-10-09T20:00:00", "2026-10-30T07:00:00")
            .map(::at)
        val ratings = listOf(Rating.Again, Rating.Good, Rating.Good, Rating.Good, Rating.Again, Rating.Easy)
        val seedBase = 1234L
        var live = Card()
        times.zip(ratings).forEach { (t, r) -> live = scheduler.review(live, r, t, seedBase + live.reps) }

        assertEquals(live, scheduler.replay(times.zip(ratings).shuffled(), seedBase))
        assertEquals(6, live.reps)
        assertEquals(1, live.lapses)
    }

    @Test
    fun `day rolls over at four in the morning`() {
        assertEquals(clock.dayOf(at("2026-10-29T12:00:00")), clock.dayOf(at("2026-10-30T03:59:00")))
        assertEquals(clock.dayOf(at("2026-10-29T12:00:00")) + 1, clock.dayOf(at("2026-10-30T04:00:00")))
        assertEquals(at("2026-10-30T04:00:00"), clock.startOfDay(clock.dayOf(at("2026-10-30T15:00:00"))))
    }
}
