# Kur'an Kelimeleri Flashcard

Kur'an-ı Kerim'de geçen kelimeleri aralıklı tekrar (spaced repetition) yöntemiyle öğrenmek için Android uygulaması.

> 🚧 Geliştirme aşamasında.

## Özellikler (planlanan)

- **FSRS zamanlama:** Anki'nin de kullandığı FSRS algoritmasıyla tekrar planlama, hedef hatırlama oranı (desired retention) ayarlanabilir
- **Tek deste, otomatik güncelleme:** Kelime listesi bu repodan çekilir; liste güncellendiğinde uygulama güncellemesi beklemeden yeni kelimeler gelir, ilerleme korunur
- **Çevrimdışı çalışma:** Tekrarlar internet olmadan yapılabilir
- **Senkron ve yedek:** Google hesabıyla giriş yaparak ilerlemeyi cihazlar arasında senkronlama; ilerlemeyi dosya olarak dışa/içe aktarma

## Teknoloji

- [Expo](https://expo.dev) / React Native, TypeScript
- [ts-fsrs](https://github.com/open-spaced-repetition/ts-fsrs)
- Cihazda SQLite
- Senkron: Cloudflare Pages Functions + D1
- Deste verisi üretimi: Python

## Anki kullanıcıları için

Desteyi Anki'de kullanmak isteyenler için AnkiWeb'deki paylaşılan deste yayında kalmaya devam edecek.

## Kaynaklar

- **Kur'an metni:** [Tanzil Project](https://tanzil.net) — Creative Commons Attribution 3.0. Metin değiştirilmeden kullanılmıştır.
- **Morfoloji verisi:** [Quranic Arabic Corpus](https://corpus.quran.com) — GNU General Public License.

## Lisans

[GNU GPLv3](LICENSE)
