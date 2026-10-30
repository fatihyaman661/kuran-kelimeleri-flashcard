package com.mevlutcelik.fsrs

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Günlerin nerede başlayıp bittiğini söyler. FSRS geçen süreyi gün sayısıyla ölçer; tekrar kartları
 * da günün başında açılır. Anki gibi gün varsayılan olarak gece 04:00'te döner, böylece gece yarısından
 * sonra yapılan tekrarlar hâlâ "bugün" sayılır.
 */
interface DayClock {
    fun dayOf(epochMillis: Long): Long
    fun startOfDay(day: Long): Long
}

class ZonedDayClock(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val rolloverHour: Int = 4,
) : DayClock {
    override fun dayOf(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).minusHours(rolloverHour.toLong()).toLocalDate().toEpochDay()

    override fun startOfDay(day: Long): Long =
        LocalDate.ofEpochDay(day).atTime(rolloverHour, 0).atZone(zone).toInstant().toEpochMilli()
}
