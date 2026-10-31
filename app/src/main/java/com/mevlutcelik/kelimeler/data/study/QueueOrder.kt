package com.mevlutcelik.kelimeler.data.study

import com.mevlutcelik.kelimeler.data.settings.NewOrder

/**
 * Oturumda son yeni kelimeden bu yana gösterilen tekrar sayısı ve son tekrardan bu yana gösterilen
 * yeni kelime sayısı. [NewOrder.Mixed]'de yeni kelimeleri tekrarların arasına eşit aralıkla dağıtmak için.
 */
data class QueueMix(val reviewsSinceNew: Int = 0, val newSinceReview: Int = 0) {
    fun after(kind: CardKind) = when (kind) {
        CardKind.New -> QueueMix(reviewsSinceNew = 0, newSinceReview = newSinceReview + 1)
        CardKind.Review -> QueueMix(reviewsSinceNew = reviewsSinceNew + 1, newSinceReview = 0)
        CardKind.Learning -> this
    }
}

/**
 * Vakti gelmiş tekrar ile yeni kelime arasında sıradakinin yeni kelime olup olmayacağı.
 *
 * Karışık sırada az olan tür, çok olanın arasına eşit aralıkla serpiştirilir: ör. 10 tekrar ve 2 yeni
 * kelimede T T T Y T T T T Y T T T, 1 tekrar ve 10 yeni kelimede tekrar ortaya düşer. Kalan sayılar
 * her kartta yeniden okunduğu için oturum ortasında eklenen kelimeler de dağılıma katılır.
 */
fun pickNew(order: NewOrder, reviewsDue: Int, newLeft: Int, mix: QueueMix): Boolean = when {
    newLeft <= 0 -> false
    reviewsDue <= 0 -> true
    order == NewOrder.BeforeReviews -> true
    order == NewOrder.AfterReviews -> false
    newLeft >= reviewsDue -> reviewsDue * (mix.newSinceReview + 1) <= newLeft
    else -> newLeft * (mix.reviewsSinceNew + 1) > reviewsDue
}
