package com.mevlutcelik.kelimeler.data.study

/**
 * Zorlanan kelime (Anki'de "leech"): öğrenildikten sonra bu kadar kez unutulmuş. Uygulama bunu yalnızca
 * işaretler; askıya almak kullanıcının seçimidir. Anki'nin varsayılan eşiği de 8.
 */
const val LEECH_LAPSES = 8

fun isLeech(lapses: Int?) = lapses != null && lapses >= LEECH_LAPSES
