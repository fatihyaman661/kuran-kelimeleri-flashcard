package com.mevlutcelik.kelimeler

import com.mevlutcelik.kelimeler.data.settings.NewOrder
import com.mevlutcelik.kelimeler.data.study.CardKind
import com.mevlutcelik.kelimeler.data.study.QueueMix
import com.mevlutcelik.kelimeler.data.study.pickNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueOrderTest {

    /** [reviews] tekrar ve [news] yeni kelimelik bir oturumu baştan sona oynatır; Y yeni, T tekrar. */
    private fun session(order: NewOrder, reviews: Int, news: Int): String {
        var r = reviews
        var n = news
        var mix = QueueMix()
        return buildString {
            while (r > 0 || n > 0) {
                if (pickNew(order, r, n, mix)) {
                    append('Y'); n--; mix = mix.after(CardKind.New)
                } else {
                    append('T'); r--; mix = mix.after(CardKind.Review)
                }
            }
        }
    }

    @Test
    fun `after and before keep the two groups apart`() {
        assertEquals("TTTYY", session(NewOrder.AfterReviews, 3, 2))
        assertEquals("YYTTT", session(NewOrder.BeforeReviews, 3, 2))
    }

    @Test
    fun `mixed spreads the smaller group evenly`() {
        assertEquals("TTTYTTTTYTTT", session(NewOrder.Mixed, 10, 2))
        assertEquals("YTYTYT", session(NewOrder.Mixed, 3, 3))
        assertEquals("YYYYYTYYYYY", session(NewOrder.Mixed, 1, 10))
    }

    @Test
    fun `whatever exists is shown when the other side is empty`() {
        assertTrue(pickNew(NewOrder.AfterReviews, 0, 5, QueueMix()))
        assertFalse(pickNew(NewOrder.BeforeReviews, 5, 0, QueueMix()))
        assertFalse(pickNew(NewOrder.Mixed, 0, 0, QueueMix()))
    }

    @Test
    fun `learning cards do not move the mix`() {
        val mix = QueueMix(reviewsSinceNew = 2, newSinceReview = 0)
        assertEquals(mix, mix.after(CardKind.Learning))
    }
}
