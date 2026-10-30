package com.mevlutcelik.fsrs

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Kullanıcının verdiği cevap. Sayısal değerler FSRS formüllerindeki G ile aynıdır. */
enum class Rating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4);

    companion object {
        fun of(value: Int): Rating = entries.first { it.value == value }
    }
}

enum class Phase(val value: Int) {
    New(0),
    Learning(1),
    Review(2),
    Relearning(3);

    companion object {
        fun of(value: Int): Phase = entries.first { it.value == value }
    }
}

/**
 * Bir kartın zamanlama durumu. [due] ve [lastReview] epoch milisaniyedir.
 * [step] yalnızca Learning/Relearning'de anlamlıdır.
 */
data class Card(
    val phase: Phase = Phase.New,
    val step: Int = 0,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val due: Long = 0L,
    val lastReview: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
)

/**
 * FSRS-6 ayarları. Varsayılan ağırlıklar py-fsrs 6 / ts-fsrs 5 ile aynıdır; kişiye göre
 * eğitilmiş ağırlıklar geldiğinde [weights] değiştirilir.
 */
data class FsrsParameters(
    val weights: List<Double> = DEFAULT_WEIGHTS,
    val desiredRetention: Double = 0.9,
    val learningSteps: List<Duration> = listOf(1.minutes, 10.minutes),
    val relearningSteps: List<Duration> = listOf(10.minutes),
    val maximumIntervalDays: Int = 36500,
    val enableFuzz: Boolean = true,
) {
    init {
        require(weights.size == 21) { "FSRS-6 21 ağırlık bekler, ${weights.size} geldi" }
        require(desiredRetention > 0.0 && desiredRetention < 1.0) { "desiredRetention (0, 1) aralığında olmalı" }
        require(maximumIntervalDays >= 1)
    }

    companion object {
        val DEFAULT_WEIGHTS = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722,
            0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425,
            0.0912, 0.0658, 0.1542,
        )
    }
}
