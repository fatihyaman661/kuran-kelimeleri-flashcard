# Kur'an Kelimeleri Flashcard

Kur'an-ı Kerim'de geçen kelimeleri aralıklı tekrar (spaced repetition) yöntemiyle öğrenmek için mobil uygulama.

> 🚧 Geliştirme aşamasında.

## Özellikler (planlanan)

- **FSRS zamanlama:** Anki'nin de kullandığı FSRS algoritmasıyla tekrar planlama, hedef hatırlama oranı (desired retention) ayarlanabilir
- **Tek deste, otomatik güncelleme:** Kelime listesi bu repodan çekilir; liste güncellendiğinde uygulama güncellemesi beklemeden yeni kelimeler gelir, ilerleme korunur
- **Çevrimdışı çalışma:** Tekrarlar internet olmadan yapılabilir
- **Senkron ve yedek:** Google hesabıyla giriş yaparak ilerlemeyi cihazlar arasında senkronlama; ilerlemeyi dosya olarak dışa/içe aktarma
- **Anki'den geçiş:** Anki'deki çalışma geçmişini içe aktarma
- **İlerleme:** Öğrenilen kelime yüzdesi, Kur'an metnindeki kelime geçişlerinin ne kadarının tanındığı (metin kapsamı), son 30 günün tekrar grafiği ve çalışma serisi

Alınan tasarım kararları ve yol haritası için: [docs/TASARIM.md](docs/TASARIM.md)

## Teknoloji

- [FSRS](https://github.com/open-spaced-repetition) aralıklı tekrar algoritması (FSRS-6, `fsrs/` modülünde Kotlin)
- Android: Kotlin, Jetpack Compose (Material 3), Hilt, Room (cihazda SQLite), DataStore
- Senkron: Cloudflare Pages Functions + D1
- Deste verisi üretimi: Python

## Proje yapısı

| Klasör | İçerik |
|---|---|
| `app/` | Android uygulaması |
| `fsrs/` | FSRS-6 zamanlayıcısı (saf Kotlin/JVM, birim testli) |
| `scripts/build_deck.py` | `data/kelimeler.apkg` → `data/deck.db.gz` + `data/deck-version.json` |
| `data/` | Anki destesi ve uygulamanın okuduğu deste dosyaları |
| `server/` | Senkron sunucusu (Cloudflare Pages Functions + D1) ve gizlilik politikası sayfası |
| `docs/` | Tasarım kararları |

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

## Deste

Destenin güncel hali her zaman [`data/kelimeler.apkg`](data/kelimeler.apkg) dosyasında tutulur. Uygulamanın kullandığı veri bu dosyadan üretilir:

```bash
python3 scripts/build_deck.py
```

Betik `data/deck.db.gz` (gzip'li SQLite) ve `data/deck-version.json` dosyalarını üretir. Bu dosyalar `main`'e gönderilince
uygulamalar yeni sürümü kendileri indirir (açılışta en fazla 12 saatte bir, ya da Ayarlar → Deste → Güncellemeleri kontrol et);
yeni bir derleme de aynı dosyayı APK'nın içine koyar, ilk açılışta internet gerekmez. Not GUID'leri sabit kalmalı; ilerleme bunlara bağlı.

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

Desteyi Anki'de kullanmak isteyenler için AnkiWeb'deki paylaşılan deste yayında kalmaya devam edecek. En güncel sürümü doğrudan [`data/kelimeler.apkg`](data/kelimeler.apkg) dosyasından indirip Anki'ye aktarabilirsiniz; kart kimlikleri sabit olduğu için içe aktarma çalışma geçmişini korur.

## Kaynaklar

- **Kur'an metni:** [Tanzil Project](https://tanzil.net) — Tanzil Quran Text (Uthmani, sürüm 1.0.2), Creative Commons Attribution-NoDerivs 3.0 (CC BY-ND 3.0). Metin değiştirilmeden kullanılmıştır.
- **Morfoloji verisi:** [Quranic Arabic Corpus](https://corpus.quran.com) — GNU General Public License. Düzeltilmiş ve Arapça harfe çevrilmiş sürümü [mustafa0x/quran-morphology](https://github.com/mustafa0x/quran-morphology) kullanılmıştır.
- **Meal:** Elmalılı Muhammed Hamdi Yazır, *Hak Dini Kur'an Dili* meali (sadeleştirilmiş). Metin [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api) üzerinden (`tur-elmallsadelesti1`) alınmıştır.
- **Kelime açıklamaları:** Râğıb el-İsfahânî, *el-Müfredât fî Garîbi'l-Kur'ân*, thk. Safvân Adnân ed-Dâvûdî, Dâru'l-Kalem / ed-Dâru'ş-Şâmiyye, Dımaşk–Beyrut, 1412 h.
- **Fontlar:** Amiri ve Amiri Quran (Khaled Hosny) — SIL Open Font License 1.1. Desteye [Fontsource](https://fontsource.org) paketlerinden gömülmüştür.
- Kelimelerin Türkçe anlamları, çoğul/çekim bilgileri ve el-Müfredât alıntılarının Türkçe çevirileri yapay zekâ (Claude) ile hazırlanmıştır.

## Lisans

[GNU GPLv3](LICENSE)
