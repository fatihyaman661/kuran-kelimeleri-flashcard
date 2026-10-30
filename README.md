# Kur'an Kelimeleri Flashcard

Kur'an-ı Kerim'de geçen kelimeleri aralıklı tekrar (spaced repetition) yöntemiyle öğrenmek için mobil uygulama.

> 🚧 Geliştirme aşamasında.

## Özellikler (planlanan)

- **FSRS zamanlama:** Anki'nin de kullandığı FSRS algoritmasıyla tekrar planlama, hedef hatırlama oranı (desired retention) ayarlanabilir
- **Tek deste, otomatik güncelleme:** Kelime listesi bu repodan çekilir; liste güncellendiğinde uygulama güncellemesi beklemeden yeni kelimeler gelir, ilerleme korunur
- **Çevrimdışı çalışma:** Tekrarlar internet olmadan yapılabilir
- **Senkron ve yedek:** Google hesabıyla giriş yaparak ilerlemeyi cihazlar arasında senkronlama; ilerlemeyi dosya olarak dışa/içe aktarma
- **Anki'den geçiş:** Anki'deki çalışma geçmişini içe aktarma

Alınan tasarım kararları ve yol haritası için: [docs/TASARIM.md](docs/TASARIM.md)

## Teknoloji

- [FSRS](https://github.com/open-spaced-repetition) aralıklı tekrar algoritması
- Senkron: Cloudflare Pages Functions + D1
- Deste verisi üretimi: Python

## Deste

Destenin güncel hali her zaman [`data/kelimeler.apkg`](data/kelimeler.apkg) dosyasında tutulur. Uygulamanın kullandığı veri bu dosyadan üretilir.

## Anki kullanıcıları için

Desteyi Anki'de kullanmak isteyenler için AnkiWeb'deki paylaşılan deste yayında kalmaya devam edecek. En güncel sürümü doğrudan [`data/kelimeler.apkg`](data/kelimeler.apkg) dosyasından indirip Anki'ye aktarabilirsiniz; kart kimlikleri sabit olduğu için içe aktarma çalışma geçmişini korur.

## Kaynaklar

- **Kur'an metni:** [Tanzil Project](https://tanzil.net) — Creative Commons Attribution 3.0. Metin değiştirilmeden kullanılmıştır.
- **Morfoloji verisi:** [Quranic Arabic Corpus](https://corpus.quran.com) — GNU General Public License. Düzeltilmiş ve Arapça harfe çevrilmiş sürümü [mustafa0x/quran-morphology](https://github.com/mustafa0x/quran-morphology) kullanılmıştır.
- **Meal:** Elmalılı Muhammed Hamdi Yazır, *Hak Dini Kur'an Dili* meali (sadeleştirilmiş). Metin [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api) üzerinden (`tur-elmallsadelesti1`) alınmıştır.
- **Kelime açıklamaları:** Râğıb el-İsfahânî, *el-Müfredât fî Garîbi'l-Kur'ân*, thk. Safvân Adnân ed-Dâvûdî, Dâru'l-Kalem / ed-Dâru'ş-Şâmiyye, Dımaşk–Beyrut, 1412 h.
- **Fontlar:** Amiri ve Amiri Quran (Khaled Hosny) — SIL Open Font License 1.1. Desteye [Fontsource](https://fontsource.org) paketlerinden gömülmüştür.
- Kelimelerin Türkçe anlamları, çoğul/çekim bilgileri ve el-Müfredât alıntılarının Türkçe çevirileri yapay zekâ (Claude) ile hazırlanmıştır.

## Lisans

[GNU GPLv3](LICENSE)
