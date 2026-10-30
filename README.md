# Kur'an Kelimeleri Flashcard

Kur'an-ı Kerim'de geçen kelimeleri aralıklı tekrar (spaced repetition) yöntemiyle öğrenmek için Android uygulaması.

> 🚧 Geliştirme aşamasında.

## Özellikler

- **FSRS zamanlama:** Anki'nin de kullandığı FSRS algoritmasıyla (FSRS-6) tekrar planlama, hedef hatırlama oranı (desired retention) ayarlanabilir
- **Tek deste, otomatik güncelleme:** Kelime listesi bu repodan çekilir; liste güncellendiğinde uygulama güncellemesi beklemeden yeni kelimeler gelir, ilerleme korunur
- **Çevrimdışı çalışma:** Tekrarlar internet olmadan yapılabilir
- **Senkron ve yedek:** Google hesabıyla giriş yaparak ilerlemeyi cihazlar arasında senkronlama; ilerlemeyi dosya olarak dışa/içe aktarma
- **İlerleme:** Öğrenilen kelime yüzdesi, Kur'an metnindeki kelime geçişlerinin ne kadarının tanındığı (metin kapsamı), son 30 günün tekrar grafiği ve çalışma serisi

## Teknoloji

- Kotlin, Jetpack Compose (Material 3), Hilt, Navigation Compose
- FSRS-6: `:fsrs` modülünde saf Kotlin, birim testli ([py-fsrs](https://github.com/open-spaced-repetition/py-fsrs) / [ts-fsrs](https://github.com/open-spaced-repetition/ts-fsrs) ile aynı formüller ve varsayılan ağırlıklar)
- Cihazda SQLite (Room) ve DataStore
- Senkron: Cloudflare Pages Functions + D1 (`server/`)
- Deste verisi üretimi: Python

### İlerleme verisi

İlerleme yalnızca **tekrar kayıtlarından** oluşur: `{id, kelime, zaman, cevap}`. Kayıtlar hiç değiştirilmez, yalnızca eklenir;
kartın durumu bu kayıtlar FSRS'ten geçirilerek hesaplanır. Bu sayede:

- senkron, iki cihazın kayıtlarını kimliğe göre birleştirmekten ibarettir, çakışma çıkmaz;
- yedek dosyası yalnızca bu kayıtları taşır, içe aktarılınca durumlar yeniden hesaplanır;
- ileride FSRS parametrelerini kişiye göre eğitmek (optimizer) için gereken geçmiş baştan tutulur.

Kelimeler Anki not GUID'leriyle tanınır; liste güncellenince kelimeler bu kimliğe göre güncellenir, listeden çıkanların
ilerlemesi silinmez.

## Proje yapısı

| Klasör | İçerik |
|---|---|
| `app/` | Android uygulaması |
| `fsrs/` | FSRS-6 zamanlayıcısı (saf Kotlin/JVM) |
| `scripts/build_deck.py` | `data/kelimeler.apkg` → `data/deck.db.gz` + `data/deck-version.json` |
| `data/` | Anki destesi ve uygulamanın okuduğu deste dosyaları |
| `server/` | Senkron sunucusu (Cloudflare Pages Functions + D1) ve gizlilik politikası sayfası |

## Derleme

JDK 21 (Android Studio'nun JBR'si) ve Android SDK gerekir; Gradle sarmalayıcıyla gelir.

```bash
./gradlew :app:assembleDebug                    # debug APK (paket: com.mevlutcelik.kelimeler.dev)
./gradlew :app:assemblePreview                  # küçültülmüş release kodu, debug imzalı (cihazda gerçek hız)
./gradlew :fsrs:test :app:testDebugUnitTest     # birim testleri
./gradlew :app:bundleRelease                    # Google Play AAB (keystore.properties gerekir)
```

`local.properties` içinde `sdk.dir` tanımlı olmalıdır (Android Studio projeyi açınca oluşturur). Play yükleme anahtarı
için `keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp doldurun; bu dosya depoya girmez.

## Desteyi güncellemek

1. Anki'den güncel desteyi `data/kelimeler.apkg` olarak dışa aktarın ("Eski Anki sürümleriyle uyumlu" seçeneği açık).
   Not GUID'leri sabit kalmalı; ilerleme bunlara bağlı.
2. Deste dosyalarını üretin:

   ```bash
   python3 scripts/build_deck.py
   ```

3. `data/` altındaki değişiklikleri `main`'e gönderin. Uygulamalar `data/deck-version.json` dosyasına bakarak yeni sürümü
   kendileri indirir (açılışta en fazla 12 saatte bir, ya da Ayarlar → Deste → Güncellemeleri kontrol et).

Yeni bir derleme de aynı `data/deck.db.gz` dosyasını APK'nın içine koyar; ilk açılışta internet gerekmez.

## Senkron sunucusu (isteğe bağlı)

Sunucu tanımlanmamış bir derlemede uygulama tamamen çevrimdışı çalışır, Ayarlar'da senkron kapalı görünür.

1. Google Cloud Console'da bir proje açın; OAuth izin ekranını yapılandırın.
   - "Web uygulaması" türünde bir istemci kimliği oluşturun. Bu, sunucunun ve uygulamanın kullandığı kimliktir.
   - Uygulamanın paket adı (`com.mevlutcelik.kelimeler`) ve imza SHA-1'leri (debug, yükleme anahtarı, Play uygulama imzası)
     için "Android" türünde istemci kimlikleri oluşturun.
2. Cloudflare'da D1 veritabanını kurun ve yayınlayın (`server/` klasöründe):

   ```bash
   npm install
   npx wrangler d1 create kuran-kelimeleri      # çıkan database_id'yi wrangler.toml'a yazın
   npm run db:init
   npm run deploy
   ```

   `wrangler.toml` içindeki `GOOGLE_CLIENT_ID` değerine Web istemci kimliğini yazın.
3. Uygulamanın `local.properties` dosyasına ekleyin:

   ```properties
   sync.url=https://<proje>.pages.dev
   sync.googleClientId=<Web istemci kimliği>.apps.googleusercontent.com
   ```

Sunucuda e-posta ya da ad tutulmaz; kullanıcı Google hesap kimliğinin SHA-256 özetiyle tanınır. Kullanıcılar uygulamadan
"Sunucudaki verilerimi sil" ile bütün kayıtlarını silebilir. Gizlilik politikası `server/public/gizlilik.html` olarak
aynı adreste yayınlanır; Play Console'a bu adres verilir.

Bunlara ek olarak Android'in otomatik yedeği yalnızca ilerleme dosyasını (`files/auto-backup/progress.json`) Google
hesabına yedekler; uygulama yeniden kurulduğunda ilerleme buradan geri gelir.

## Anki kullanıcıları için

Desteyi Anki'de kullanmak isteyenler için AnkiWeb'deki paylaşılan deste yayında kalmaya devam edecek.

## Kaynaklar

- **Kur'an metni:** [Tanzil Project](https://tanzil.net) — Creative Commons Attribution 3.0. Metin değiştirilmeden kullanılmıştır.
- **Morfoloji verisi:** [Quranic Arabic Corpus](https://corpus.quran.com) — GNU General Public License.
- **Yazı tipi:** [Amiri](https://github.com/aliftype/amiri) — SIL Open Font License 1.1.

## Lisans

[GNU GPLv3](LICENSE)
