# Tasarım kararları ve yol haritası

Bu dosya projede şimdiye kadar alınan kararları ve planlanan özellikleri toplar. Geliştirme yaparken buradaki kararlara uyulmalı; bir karar değişirse bu dosya da güncellenmeli.

## Amaç

Anki'ye bağımlı olmadan, tek bir özenle hazırlanmış desteyi (Kur'an kelimeleri) aralıklı tekrarla çalıştıran bir mobil uygulama. Çıkış noktası: AnkiWeb'de paylaşılan destenin her güncellemesi yayına girmeden önce bir gün bekliyor; deste güncellemelerinin anında kullanıcıya ulaşması isteniyor.

## Platform

- İlk sürüm Android: Kotlin, Jetpack Compose (Material 3), Hilt, Room. Paket adı `com.mevlutcelik.kelimeler`.

## Deste verisi

- Kaynak: `data/kelimeler.apkg` (4.858 kart). Bu dosya destenin **güncel hali** olarak tutulur.
- `.apkg` dönüştürme aracı Python ile yazıldı: `scripts/build_deck.py`. Deste her güncellendiğinde çalıştırılıp `data/deck.db.gz` ve `data/deck-version.json` yeniden üretilir.
- Uygulamanın okuduğu deste **JSON değil, gzip'li SQLite**: 12.8 MB'lık JSON'u cihazda ayrıştırmak ilk açılışta 9–30 saniye sürüyordu. SQLite dosyası satır satır kopyalanır (birkaç saniye); kartın arka yüzü (çekim, örnekler, el-Müfredât) her satırda JSON olarak durur ve yalnızca kart gösterilirken çözülür.
- Uygulama güncellemeyi önce küçük `deck-version.json` dosyasına bakarak kontrol eder; sürüm değişmişse desteyi indirir. Listeden çıkan kelimeler silinmez, ilerlemeleri saklanır.
- **Her kelimenin kimliği sabittir** (Anki not GUID'i). Kullanıcı ilerlemesi bu kimliğe bağlıdır; kimlikler sürümler arasında asla değişmemeli.
- Uygulama desteyi GitHub'dan çeker. Kelime/içerik güncellemeleri uygulama güncellemesi ve mağaza incelemesi beklemeden kullanıcıya ulaşır. Güncellemede yeni kelimeler eklenir, değişenlerin içeriği güncellenir, ilerleme korunur.
- Kartlarda Arapça yazı için Amiri / Amiri Quran fontları kullanılıyor (deste içinde gömülü).

## Zamanlama: FSRS

- FSRS-6, `fsrs/` modülünde saf Kotlin olarak uygulandı; formüller, öğrenme adımları ve varsayılan ağırlıklar py-fsrs 6 / ts-fsrs ile aynı, birim testli.
  - Hazır kütüphane yerine kendi uygulamamız: kart durumu tekrar kayıtlarından yeniden hesaplandığı için her hesap aynı sonucu vermeli. Bunun için fuzz kelime kimliği + tekrar sayısından tohumlanır, geçen süre de 04:00'te dönen günlerle ölçülür (Anki gibi).
  - Alternatif: [java-fsrs](https://github.com/open-spaced-repetition/java-fsrs) (MIT, Maven Central). `FSRS-Kotlin` deposunun lisansı olmadığı için kullanılamaz.
- Hedef hatırlama oranı (desired retention) kullanıcı tarafından ayarlanabilir, varsayılan 0.90.
- İlk sürümde varsayılan FSRS parametreleri kullanılır. Kişiye özel parametre optimizasyonu sonradan eklenebilir; bunun için tekrar kayıtlarının **baştan eksiksiz tutulması şart**.
- Günlük yeni kart limiti ayarlanabilir.

## Veri modeli

- **Kart durumu değil, tekrar kayıtları saklanır:** `{id, kart_id, zaman, cevap (1–4)}`. Kayıtlar güncellenmez, sadece yeni kayıt eklenir.
- Kart durumu (zorluk, kararlılık, sonraki tekrar tarihi) bu kayıtlar FSRS'ten geçirilerek hesaplanır; performans için cihazda önbelleğe alınabilir.
- Bu yapı sayesinde senkron çakışmasızdır: iki cihazın kayıtları `id`'ye göre birleştirilir.
- Uygulama tamamen çevrimdışı çalışabilmeli.

## Senkron ve yedek

- **Senkron:** Cloudflare Pages Functions + D1. Giriş Google hesabıyla; sunucu tarafında Google ID token'ı doğrulanır, ayrı şifre sistemi yok.
- **Yedek:** İlerlemeyi JSON olarak dışa/içe aktarma.
- İlk sürümde Google Drive entegrasyonu yok.
- Sunucuda e-posta ya da ad tutulmaz; kullanıcı Google hesap kimliğinin (`sub`) SHA-256 özetiyle tanınır, oturum jetonlarının da yalnızca özeti saklanır. Aydınlatma metni / gizlilik politikası: `server/public/gizlilik.html`.
- Google Play gereği uygulamada "Sunucudaki verilerimi sil" var (`DELETE /api/account`).
- Android'in otomatik yedeği yalnızca ilerleme dosyasını (`files/auto-backup/progress.json`) yedekler; yeniden kurulumda ilerleme buradan döner.

## Anki'den ilerleme aktarma

- Kullanıcı Anki'den desteyi **zamanlama bilgileriyle birlikte** `.apkg` olarak dışa aktarır ve uygulamaya yükler.
- `.apkg` içindeki SQLite veritabanından `revlog` (tekrar geçmişi) okunur; `cards` ve `notes` tablolarıyla kart → not GUID eşleşmesi yapılır.
- Anki'nin 1–4 cevapları FSRS notlarıyla (Again/Hard/Good/Easy) birebir eşleşir. Kayıtlar bizim tekrar kayıtlarına çevrilir, kart durumları FSRS ile yeniden hesaplanır.
- Yeni Anki sürümleri veritabanını zstd ile sıkıştırıyor (`collection.anki21b`); eski format (`collection.anki21` / `.anki2`) da desteklenmeli.
- Bizim GUID'lerimizle eşleşmeyen notlar (kullanıcının eklediği kartlar vb.) **atlanır**.

## Kapsam dışı (ilk sürüm)

- **Kullanıcının kart eklemesi yok.** Uygulama tek, özenle hazırlanmış bir deste etrafında kurulu. Gerekirse ileride ana desteden tamamen ayrı bir "kendi kartlarım" destesi olarak eklenebilir.
- Google Drive senkronu.

## Planlanan küçük özellikler

- **Kartı askıya alma:** "Bu kelimeyi biliyorum, gösterme" (özellikle ال, و, مِن gibi baştaki sık kelimeler için).
- **Kişisel not:** Kullanıcının bir karta kendi notunu eklemesi.
- **Hata bildirme:** Karttaki bir hatayı geliştiriciye iletme.
- **Hakkında ekranı:** Kaynaklar ve lisanslar (Tanzil, Quranic Arabic Corpus, GPLv3). *(Yapıldı: Ayarlar → Hakkında.)*

## Lisans ve kaynaklar

- Repo GPLv3. Eklenecek kütüphanelerin lisansı GPLv3 ile uyumlu olmalı (FSRS kütüphaneleri genelde MIT, uyumlu).
- Tanzil metni (Uthmani 1.0.2) değiştirilmeden kullanılmalı ve kaynak gösterilmeli (CC BY-ND 3.0).
- Quranic Arabic Corpus verisi GPL; kaynak gösterilmeli.
- Destede ayrıca Elmalılı meali (sadeleştirilmiş), el-Müfredât (Safvân ed-Dâvûdî tahkiki) alıntıları ve Amiri / Amiri Quran fontları (SIL OFL 1.1) kullanılıyor; README'deki kaynaklar listesi güncel tutulmalı. Hakkında ekranında da bu kaynaklar gösterilmeli.

## Çalışma düzeni

- Repoda birden fazla kişi (ve Claude oturumu) çalışıyor. **Her çalışmaya başlamadan önce `git pull`** yapılmalı.
