package com.mevlutcelik.kelimeler.presentation.guide

import com.mevlutcelik.kelimeler.data.deck.Example
import com.mevlutcelik.kelimeler.data.deck.Mufredat
import com.mevlutcelik.kelimeler.data.deck.WordDetails
import com.mevlutcelik.kelimeler.data.deck.WordForm
import com.mevlutcelik.kelimeler.data.local.WordEntity

/*
 * Rehberde gösterilen örnek veriler. scripts/build_deck.py'nin ürettiği destenin (data/deck.db.gz)
 * kopyasıdır; rehber deste yüklenmeden de (ilk açılışta) çalışsın diye koda gömülüdür. Destedeki
 * sayılar biraz değişirse burası güncellenmese de anlatım bozulmaz.
 */

/** Örnek kart: عَلِمَ (bilmek). */
internal val GuideWord = WordEntity(
    id = "c%J`+[Ird&",
    rank = 48,
    word = "علم",
    vocalized = "عَلِمَ",
    type = "Fiil",
    category = "Fiil – I. bab (فَعِلَ يَفْعَلُ)",
    meaning = "bilmek; öğrenmek, farkına varmak; (Allah için) ayırt edip ortaya çıkarmak",
    root = "ع ل م",
    frequency = 382,
    details = "",
    searchText = "",
)

internal val GuideDetails = WordDetails(
    forms = listOf(
        WordForm("Mâzî", "عَلِمَ"),
        WordForm("Muzâri", "يَعْلَمُ"),
        WordForm("Mastar", "عِلْم"),
    ),
    examples = listOf(
        Example(
            ar = "كَلَّا سَ\u200d\u200dيَعْلَمُ\u200d\u200dونَ",
            highlights = listOf(listOf(11, 20)),
            tr = "Hayır, ilerde bilecekler.",
            source = "Nebe' 78/4",
        ),
        Example(
            ar = "كَلَّا سَوْفَ تَعْلَمُ\u200d\u200dونَ",
            highlights = listOf(listOf(14, 23)),
            tr = "Hayır! Yakında bileceksiniz.",
            source = "Tekâsür 102/3",
        ),
        Example(
            ar = "عَلِمَتْ نَفْسٌ مَّآ أَحْضَرَتْ",
            highlights = listOf(listOf(0, 8)),
            tr = "Herkes ne getirmiş olduğunu anlar.",
            source = "Tekvîr 81/14",
        ),
    ),
    mufredat = Mufredat(
        ar = "العِلْمُ: إدراك الشيء بحقيقته، وذلك ضربان: أحدهما: إدراك ذات الشيء. والثاني: الحكم على الشيء بوجود شيء هو موجود له، أو نفي شيء هو منفيّ عنه. فالأوّل: هو المتعدّي إلى مفعول واحد نحو: لا تَعْلَمُونَهُمُ اللَّهُ يَعْلَمُهُمْ. والثاني: المتعدّي إلى مفعولين، نحو قوله: فَإِنْ عَلِمْتُمُوهُنَّ مُؤْمِناتٍ، وقوله: يَوْمَ يَجْمَعُ اللَّهُ الرُّسُلَ إلى قوله: لا عِلْمَ لَنا فإشارة إلى أنّ عقولهم طاشت",
        tr = "İlim: bir şeyi hakikatiyle idrak etmektir. Bu iki türlüdür: Birincisi: şeyin zatını idrak etmek. İkincisi: bir şeye, onda var olan bir şeyin varlığıyla hükmetmek ya da ondan nefyedilmiş bir şeyi nefyetmek. Birincisi tek bir mef'ûle geçişli olandır; «Siz onları bilmezsiniz (lâ ta'lemûnehum), Allah onları bilir» gibi. İkincisi iki mef'ûle geçişli olandır; «Eğer onları mümin kadınlar olarak bilirseniz (alimtumûhunne)» âyeti gibi. «Allah'ın peygamberleri toplayacağı gün…» âyetinde «Bizim hiçbir bilgimiz yok» sözüne kadar (olan kısım), akıllarının başlarından gittiğine işarettir.",
    ),
)

/** Kur'an'da en sık geçen ilk kelimeler: sıra, harekeli yazılış, tür, geçiş sayısı. */
internal data class GuideFrequent(val rank: Int, val ar: String, val type: String, val count: Int)

internal val GuideTopWords = listOf(
    GuideFrequent(1, "الْ", "Harf-i tarif", 8377),
    GuideFrequent(2, "وَ", "Atıf harfi", 8177),
    GuideFrequent(3, "ـوا / ـو", "Muttasıl zamir", 6555),
    GuideFrequent(4, "ـهُمْ", "Muttasıl zamir", 3458),
    GuideFrequent(5, "ـهُ", "Muttasıl zamir", 3364),
    GuideFrequent(6, "مِنْ", "Harf-i cer", 3226),
)

/** Destedeki kelime sayısı ve Kur'an metnindeki toplam kelime geçişi. */
internal const val GuideTotalWords = 4818

/** İlk N kelimenin Kur'an metnindeki kelime geçişlerinin yüzde kaçını oluşturduğu. */
internal val GuideCoverage = listOf(10 to 34, 100 to 71, 500 to 88, 1000 to 93)
