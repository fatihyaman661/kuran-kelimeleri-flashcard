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
- Yazı boyutu Arapça (%80–160) ve Latin harfli yazı (%85–130) için ayrı ayarlanır. Latin çarpanı yazı ölçeğine (fontScale) uygulanır, Arapça stiller (`ArabicStyles`) kendi çarpanıyla ölçeklenir; yeni Arapça metin eklerken bu stiller ya da `ArabicStyles.span` kullanılmalı.

## Zamanlama: FSRS

- FSRS-6, `fsrs/` modülünde saf Kotlin olarak uygulandı; formüller, öğrenme adımları ve varsayılan ağırlıklar py-fsrs 6 / ts-fsrs ile aynı, birim testli.
  - Hazır kütüphane yerine kendi uygulamamız: kart durumu tekrar kayıtlarından yeniden hesaplandığı için her hesap aynı sonucu vermeli. Bunun için fuzz kelime kimliği + tekrar sayısından tohumlanır ve `kotlin.random.Random` yerine SplitMix64 ile üretilir (`Random`'ın aynı tohumla aynı diziyi vermesi yalnızca aynı Kotlin sürümünde garanti; SplitMix64 sürümden ve platformdan bağımsız). Geçen süre de 04:00'te dönen günlerle ölçülür (Anki gibi).
  - Alternatif: [java-fsrs](https://github.com/open-spaced-repetition/java-fsrs) (MIT, Maven Central). `FSRS-Kotlin` deposunun lisansı olmadığı için kullanılamaz.
- Hedef hatırlama oranı (desired retention) kullanıcı tarafından ayarlanabilir, varsayılan 0.90.
- İlk sürümde varsayılan FSRS parametreleri kullanılır. Kişiye özel parametre optimizasyonu sonradan eklenebilir; bunun için tekrar kayıtlarının **baştan eksiksiz tutulması şart**.
- Günlük yeni kart limiti ayarlanabilir.
- **Yapı kelimeleri seyreltilir:** Destenin başı harf, zamir eki, işaret ismi ve mevsulle dolu (ilk 50 kartın 40'ı, ilk 100'ün 62'si); arka arkaya gelince yeni başlayanı bunaltıyordu. Yeni kelime seçilirken bir yapı kelimesinden (fiil, isim ya da özel isim olmayan her tür) sonra en az 3 başka kelime gelir: الْ, اللّٰه, قَالَ, كَانَ, وَ, رَبّ… Sıklık sırası korunur; ilk 100'deki yapı kelimeleri yaklaşık 245. yeni kartta biter. Kural `data/study/WordKind.kt`'de, son tanıtılan kelimelerin türüne bakar. Ayrıca yalnızca o gün için artırılabilir (ana sayfada ve oturum sonunda "Bugün daha fazla yeni kelime": +10 / +25 / +50); ertesi gün sınır yine ayardaki değer olur.
- Kart sırası (Anki'deki "Yeni/tekrar sırası"): vakti gelen öğrenme adımları her zaman önce; sonra yeni kelimeler ile tekrarlar ayara göre **tekrarlarla karışık** (varsayılan, az olan tür çok olanın arasına eşit aralıkla serpiştirilir), **tekrarlardan sonra** ya da **tekrarlardan önce**.

## Veri modeli

- **Kart durumu değil, tekrar kayıtları saklanır:** `{id, kart_id, zaman, cevap (1–4)}`. Kayıtlar güncellenmez, sadece yeni kayıt eklenir.
- Askıya alma da aynı kayıtlarla tutulur: cevap alanında `-1` askıya al, `-2` askıdan çıkar. Böylece yedek, senkron ve "geri al" askıya almayı da kapsar; FSRS yalnızca 1–4 cevaplarını görür. Askı durumu son olaydan türetilip `suspended` tablosunda önbelleğe alınır.
- Kart durumu (zorluk, kararlılık, sonraki tekrar tarihi) bu kayıtlar FSRS'ten geçirilerek hesaplanır; performans için cihazda önbelleğe alınabilir.
- Bu yapı sayesinde senkron çakışmasızdır: iki cihazın kayıtları `id`'ye göre birleştirilir.
- Uygulama tamamen çevrimdışı çalışabilmeli.

## Senkron ve yedek

- **Senkron:** Cloudflare Pages Functions + D1. Giriş Google hesabıyla; sunucu tarafında Google ID token'ı doğrulanır, ayrı şifre sistemi yok.
- **Yedek:** İlerlemeyi JSON olarak dışa/içe aktarma.
- İlk sürümde Google Drive entegrasyonu yok.
- Sunucuda e-posta ya da ad tutulmaz; kullanıcı Google hesap kimliğinin (`sub`) SHA-256 özetiyle tanınır, oturum jetonlarının da yalnızca özeti saklanır. Aydınlatma metni / gizlilik politikası: https://mevlut-celik.github.io/kuran-kelimeleri/gizlilik.html (senkron yayına alınınca bu bölüm eklenir).
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

- **Kartı askıya alma:** "Bu kelimeyi biliyorum, gösterme" (özellikle ال, و, مِن gibi baştaki sık kelimeler için). *(Yapıldı: çalışma ekranının üst çubuğu ve kelime sayfası. Askıdaki kelime çalışmaya gelmez; ilerleme yüzdesinde ve metin kapsamında bilinen kelime sayılır, listede ayrı "Askıda" filtresi var.)*
- **Zorlanan kelimeler (leech):** 8 kez unutulan kelime işaretlenir, otomatik askıya alınmaz. Kartın önünde "Zorlanıyor" rozeti ve meal olmadan ayet ipucu, arkasında açıklama ve "Biliyorum, askıya al" seçeneği; listede "Zorlanan" filtresi. *(Yapıldı.)*
- **Kişisel not:** Kullanıcının bir karta kendi notunu eklemesi.
- **Hata bildirme:** Karttaki bir hatayı geliştiriciye iletme.
- **Hakkında ekranı:** Kaynaklar ve lisanslar (Tanzil, Quranic Arabic Corpus, GPLv3). *(Yapıldı: Ayarlar → Hakkında.)*
- **Nasıl kullanılır rehberi:** Anki'yi bilmeyen kullanıcılar zorlandığı için eklendi (`presentation/guide/`). İlk açılışta bir kez kendiliğinden açılır (`guide_seen` ayarı), menüde "Nasıl kullanılır" olarak durur. İki bölüm: *Temel bilgiler* (amaç, sıklık sırası, aralıklı tekrar ve unutma eğrisi, kartın akışı, dört buton, butonların altındaki süreler, FSRS ve hedef oran, günlük düzen) ve *Ekranları tanı* (üst çubuk, kartın ön ve arka yüzü, zorlanan kelimeler ve askıya alma, menü). Rehberdeki süreler uydurma değil, uygulamanın kendi FSRS zamanlayıcısıyla (fuzz kapalı) hesaplanır; örnek kelime (عَلِمَ) `GuideSample.kt`'de gömülüdür. Çalışma ekranında bir şey değişirse (buton, simge, kart düzeni) rehberdeki taklidi de güncellenmeli.

## Seviye tespiti

Daha önce Arapça çalışmış kullanıcı baştaki bildiği kelimelerle uğraşmasın diye. Rehberin "Her gün birkaç dakika" sayfasında ("Daha önce Arapça çalıştın mı?") ve Ayarlar → Çalışma → Seviye tespiti'nde sunulur; tekrar yapılabilir, sorulmamış kelimeler geri alınabilir. Kod: `scripts/placement.py`, `data/placement/`, `presentation/placement/`.

**Soru havuzu (önceden hazırlanır):**
- `scripts/build_deck.py`, `scripts/placement.py` ile deste dosyasına bir `placement` tablosu yazar: kelime, bant, doğru şık ve 3 çeldirici. Eski uygulamalar tabloyu okumaz; biçim numarası değişmez. Havuz sürüm özetine girer: havuz değişince uygulamalar desteyi yeniden indirir.
- Uygulama deste yüklenirken havuzu `files/placement.json`'a yazar (veritabanı şeması değişmez). Dosya yoksa (uygulama güncellenmiş, deste değişmemiş) APK'daki desteden okunur.
- Gözden geçirme listesi: `data/placement-review.md` (betik üretir). Sorunlu soru ya da çeldirici, sırası `data/placement-exclude.txt`'ye yazılıp betik yeniden çalıştırılarak çıkarılır.
- Sıklık bantları logaritmik: 1–50, 51–100, 101–200, 201–400, 401–800, 801–1600, 1601–3200, 3201+ (Kotlin'deki `PlacementBands` ile aynı olmalı). Her banttan en fazla 20 soru, bant boyunca eşit aralıklı.
- Soru olmayanlar: harfler, zamirler, işaret isimleri, ism-i mevsuller, özel isimler, anlamı çok uzun ya da ünlemli kelimeler. Bunlar kesme noktasının altındaysa diğer kelimelerle birlikte öğrenilmiş sayılır. Ayrıca testin başında en sık 18 harf ve ek anlamlarıyla gösterilip "Hepsini biliyor musun?" diye sorulur (öz beyan): "Evet" denirse ilk 500 kelimedeki bütün yapı kelimeleri (fiil, isim ya da özel isim olmayanlar; şu an 115) kesme noktasından bağımsız olarak bilinen sayılır. Harf ve ekleri bilip kelime bilgisi az olan (kesme noktası 0'a yakın çıkan) kullanıcı için.
- Şık metni anlamın ilk kısmıdır (ilk `;`'ye kadar, parantezler atılmış).
- Çeldiriciler: şıkkın görünüşüne göre aynı grup ("-mek" ile biten eylemler / sıfatlar / isimler), aynı ya da komşu bant, aynı kökten değil, doğru cevapla ortak anlam kelimesi taşımayan. Seçim kelimenin GUID'iyle tohumlanır; aynı deste aynı havuzu verir.

**Test (uyarlanabilir, `PlacementTest`):**
- Kelime kartın önündeki gibi gösterilir (harekesiz + tür); 4 anlam şıkkı ve "Bilmiyorum". Yanlışta doğru cevap kısa süre gösterilir.
- 101–200 bandından başlar, her ziyarette 6 soru. Puan: doğru +1, "Bilmiyorum" 0, yanlış −⅓ (şans düzeltmesi). Ortalama puan 0,7 ve üstüyse (6'da 5 doğru 1 yanlış geçer) üst banda, altındaysa alt banda. Geçilen ve geçilemeyen komşu bantlar bulununca geçilemeyende 6 soru daha; kesme noktası o bandın içinde, puana göre logaritmik ölçekte. En fazla 45 soru.
- Gürültülü cevaplarla yapılan benzetimde 6 soruluk ziyaret, 5'e göre belirgin daha isabetli çıktı (ortalama ~26 soru); 8 soru isabeti artırmadan testi uzattı.
- Sonuç ekranında "Tahminen ilk N kelimeyi biliyorsun"; kullanıcı N'yi düzeltebilir, kaç kelimenin bilinen sayılacağını ve kaç günde sorulacağını görür.

**Bilinen sayılan kelimeler (askıya alınmaz):**
- N'ye kadar hiç görülmemiş, askıda olmayan her kelime için `PLACEMENT` (−3) kaydı yazılır; bu kayıtta `durationMs` ilk tekrara kalan süredir. Replay bu olayı görünce kartı doğrudan Review aşamasına koyar (`Scheduler.placed`): **başlangıç kararlılığı 30 gün**, zorluk ilk kez "İyi" denmiş kelimeninki, tekrar sayısı 0. Daha önce çalışılmış kelimeye test dokunmaz.
- Kartın `introducedAt` değeri 0'dır: günlük yeni kelime sınırından yemez. Yeni kelimeler N'den sonraki ilk görülmemiş kelimeden başlar; ilerleme ve metin kapsamı bu kelimeleri hemen sayar, "Kalıcı kelimeler" henüz sorulmamışları saymaz.
- İlk tekrarlar serpiştirilir: **bu kelimelerden günde en fazla 3 tanesi** (önceki testten bekleyenlerle birlikte). Sıra kesme noktasından geriye doğrudur (en belirsizler önce; و, مِنْ gibi en sık kelimeler en sona).
- İlk tekrarda İyi/Kolay → FSRS geçen uzun süreyi hesaba katıp aralığı büyük açar. Tekrar → yeniden öğrenme; testin hatası böyle kendiliğinden düzelir.
- Geri alma: henüz sorulmamış (Review, reps = 0) kelimelere `PLACEMENT_UNDO` (−4) kaydı yazılır, kelime yeniden yeni olur. Sorulmuş olanlar kalır. Kayıtlar silinmediği için geri alma senkronla diğer cihazlara da geçer.
- Kişiye özel FSRS optimizasyonu eklenirse bu olaylar gerçek tekrar sayılmamalı.
- Güvenilirlik ölçüsü: testle bilinen sayılan kelimelerin ilk cevabındaki "Tekrar" oranı (seviye tespiti ekranında gösterilir). %15–20'yi geçerse eşik (`PASS`) ya da başlangıç kararlılığı gözden geçirilmeli.
- **Sunucu:** `server/functions/api/sync.ts` −3 ve −4'ü kabul eder. Bu sürümü yayımlamadan önce sunucu yeniden yayına alınmalı; eski sunucu bilinmeyen değerli bir gönderimi bütünüyle reddeder ve senkron takılır.

## İstatistik

- Yeni veri tutulmaz; hepsi tekrar kayıtlarından ve kart durumlarından hesaplanır (`StatsRepository`).
- Çalışma takvimi: son 53 hafta, renk o günkü tekrar sayısına göre (en yoğun güne göre dört kademe). Seri, bugün çalışılmadıysa dünden sayılır.
- Tahmin (Anki'deki "Future Due"): askıda olmayan kartların vadeleri gün gün sayılır, vakti geçmişler bugüne eklenir. Cevaplar ve yeni kelimeler simüle edilmez; FSRS simülasyonu (Anki'nin gelişmiş tahmini gibi) sonraki adım olabilir.
- Tahmini bitiş: görülmemiş kelime sayısı ÷ günlük yeni kelime sınırı.
- Kalıcı kelime: FSRS kararlılığı 21 günü geçen öğrenilmiş kelime (Anki'deki "olgun" kart).

## Lisans ve kaynaklar

- Repo GPLv3. Eklenecek kütüphanelerin lisansı GPLv3 ile uyumlu olmalı (FSRS kütüphaneleri genelde MIT, uyumlu).
- Tanzil metni (Uthmani 1.0.2) değiştirilmeden kullanılmalı ve kaynak gösterilmeli (CC BY-ND 3.0).
- Quranic Arabic Corpus verisi GPL; kaynak gösterilmeli.
- Destede ayrıca Elmalılı meali (sadeleştirilmiş), el-Müfredât (Safvân ed-Dâvûdî tahkiki) alıntıları ve Amiri / Amiri Quran fontları (SIL OFL 1.1) kullanılıyor; README'deki kaynaklar listesi güncel tutulmalı. Hakkında ekranında da bu kaynaklar gösterilmeli.

## Çalışma düzeni

- Repoda birden fazla kişi (ve Claude oturumu) çalışıyor. **Her çalışmaya başlamadan önce `git pull`** yapılmalı.
