package com.mevlutcelik.kelimeler.data.study

/**
 * Harf, zamir eki, işaret ismi, mevsul gibi "yapı" kelimeleri: fiil, isim ya da özel isim olmayan her
 * tür. Destenin başı bunlarla dolu (ilk 50 kartın 40'ı); arka arkaya gelince yeni başlayanı bunaltıyor.
 * Bu yüzden yeni kelimelerde bir yapı kelimesinden sonra en az [FUNCTION_WORD_GAP] başka kelime gelir.
 *
 * WordDao.nextNewContent ve recentIntroducedTypes içindeki SQL koşuluyla aynı olmalı.
 */
fun isFunctionWord(type: String) = type != "Fiil" && type != "Özel isim" && !type.startsWith("İsim")

/** Bir yapı kelimesinden sonra, yenisi gelmeden önce tanıtılacak en az kelime sayısı. */
const val FUNCTION_WORD_GAP = 3

/**
 * Sıradaki yeni kelime yapı kelimesi olabilir mi: en son tanıtılan [FUNCTION_WORD_GAP] kelimenin
 * hiçbiri yapı kelimesi değilse. [recentTypes] en yeniden eskiye, en fazla [FUNCTION_WORD_GAP] tane.
 */
fun functionWordAllowed(recentTypes: List<String>) = recentTypes.take(FUNCTION_WORD_GAP).none(::isFunctionWord)
