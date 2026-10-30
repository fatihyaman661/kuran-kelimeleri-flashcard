package com.mevlutcelik.fsrs

/**
 * Tohumdan sabit sözde rastgele değer üreten SplitMix64 karıştırıcısı.
 *
 * `kotlin.random.Random(seed)` aynı tohum için aynı diziyi yalnızca aynı Kotlin sürümü içinde
 * garanti eder. Kart durumu tekrar kayıtlarından yeniden hesaplandığı için fuzz değerinin
 * Kotlin/JVM sürümünden, platformdan ve cihazdan bağımsız olarak her zaman aynı çıkması gerekir;
 * bu fonksiyon yalnızca sabit tamsayı işlemleri kullanır.
 */
internal object SplitMix64 {
    private val GOLDEN_GAMMA = 0x9E3779B97F4A7C15uL.toLong()
    private val MIX_1 = 0xBF58476D1CE4E5B9uL.toLong()
    private val MIX_2 = 0x94D049BB133111EBuL.toLong()

    /** Durumu [seed] olan SplitMix64 üretecinin ilk çıktısı. */
    fun mix(seed: Long): Long {
        var z = seed + GOLDEN_GAMMA
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** [0, 1) aralığında, [seed] ile belirlenen sabit değer (53 bit hassasiyet). */
    fun unitDouble(seed: Long): Double = (mix(seed) ushr 11) * (1.0 / (1L shl 53))
}
