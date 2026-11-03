"""Seviye tespiti soru havuzu (build_deck.py çağırır).

Her soru bir kelime, onun kısa anlamı (doğru şık) ve üç çeldiriciden oluşur. Havuz önceden ve
deterministik üretilir: aynı deste aynı havuzu verir. Uygulama testte bu havuzdan rastgele ve
uyarlanarak soru çeker (bkz. docs/TASARIM.md, "Seviye tespiti").

Gözden geçirme: betik data/placement-review.md listesini yazar. Sorunlu bir soru
data/placement-exclude.txt dosyasına sırası (ör. "48") ya da GUID'i yazılarak havuzdan çıkarılır.
"""

import random
import re
from pathlib import Path

# Sıra numarasına göre logaritmik bantlar: (alt, üst) dahil. Sonuncusu destenin sonuna kadar.
BANDS = [(1, 50), (51, 100), (101, 200), (201, 400), (401, 800), (801, 1600), (1601, 3200), (3201, 10**9)]
PER_BAND = 20
MAX_GLOSS = 34

# Kelime bilgisi ölçmeyen ya da anlam şıkkı tuhaf duran türler soru olmaz. Bunlar kesme noktasının
# altındaysa yine de diğer kelimelerle birlikte öğrenilmiş sayılır.
QUESTION_TYPES = ("Fiil", "İsim")
EXCLUDED_TYPES = ("Özel isim",)

# Ortak anlam kelimesi ararken sayılmayan kısa, genel sözcükler.
STOPWORDS = {
    "ve", "ile", "bir", "bu", "şu", "o", "de", "da", "ki", "gibi", "için", "olan", "olmak", "etmek",
    "eden", "olarak", "çok", "en", "her", "hale", "hâle", "kendi",
}


# --- Türkçede de bilinen kelimeler -------------------------------------------------------------------
# "ihtilafa düşmek", "takva", "emanet" gibi Türkçeye geçmiş kelimeleri Türkçe bilen herkes tanır; bunlar
# Arapça kelime bilgisini ölçmez. Arapça kelimenin (ya da kökünün) ünsüzleri Türkçe anlamdaki bir
# sözcükte sırasıyla geçiyorsa kelime Türkçede de var sayılır. Ses denkliği kabadır: Türkçedeki
# sertleşme yüzünden b/p, d/t, c/ç, g/k aynı sınıftır; ayın, hemze ve elif atlanır.
AR_SOUND = {
    "ب": "B", "ت": "D", "ث": "S", "ج": "C", "ح": "H", "خ": "H", "د": "D", "ذ": "Z", "ر": "R", "ز": "Z",
    "س": "S", "ش": "S", "ص": "S", "ض": "ZD", "ط": "D", "ظ": "Z", "غ": "K", "ف": "F", "ق": "K", "ك": "K",
    "ل": "L", "م": "M", "ن": "N", "ه": "H", "ة": "D", "و": "V", "ي": "Y",
}
TR_SOUND = {
    "b": "B", "p": "B", "d": "D", "t": "D", "c": "C", "ç": "C", "j": "C", "g": "K", "k": "K", "s": "S",
    "ş": "S", "z": "Z", "h": "H", "f": "F", "r": "R", "l": "L", "m": "M", "n": "N", "v": "V", "y": "Y", "ğ": "K",
}
HARAKAT = re.compile("[\u064B-\u065F\u0670\u0640]")


def subsequence(needle, haystack):
    i = 0
    for sound in haystack:
        if i < len(needle) and sound in needle[i]:
            i += 1
    return i == len(needle)


def turkish_cognate(word):
    """Türkçe anlamda, Arapça kelimeden ya da kökünden gelmiş bir sözcük var mı ("kitap", "ihtilaf")."""
    needles = []
    if word.get("root"):
        root = [AR_SOUND.get(c, "") for c in word["root"].split()]
        root = [c for c in root if c and c not in ("V", "Y")]
        if len(root) >= 2:
            needles.append(root)
    letters = [AR_SOUND[c] for c in HARAKAT.sub("", word["word"]) if c in AR_SOUND]
    if len(letters) >= 2:
        needles.append(letters)
    for token in re.findall(r"[a-zçğıöşüâîû]+", word["meaning"].lower()):
        sounds = [TR_SOUND[c] for c in token if c in TR_SOUND]
        for needle in needles:
            # Sözcüğün ünsüzlerinin çoğu Arapça kelimeden gelmeli: "yeryüzü"ndeki r–z tesadüftür.
            if len(sounds) >= len(needle) and subsequence(needle, sounds) and len(needle) / len(sounds) >= 0.6:
                return True
    return False


# --- Ezberden bilinen yerler --------------------------------------------------------------------------
# Yalnızca çok ezberlenen yerlerde geçen nadir bir kelimeyi (Âdiyât'taki مُورِيَات gibi) bilmek, o sıradaki
# kelimeleri bilmek demek değildir. Fâtiha, Yâsîn, Mülk, Amme cüzü, Âyetü'l-Kürsî, Âmenerrasûlü ve Haşr'in
# sonu.
MEMORIZED_SURAS = {1, 36, 67, *range(78, 115)}
MEMORIZED_VERSES = {(2, 255), (2, 285), (2, 286), (59, 22), (59, 23), (59, 24)}
SOURCE = re.compile(r"(\d+)/(\d+)")


def only_in_memorized(word):
    """Kelimenin bütün geçtiği yerler ezberlenen yerlerde mi. Örnekler 10'da kesildiyse bilinemez: hayır."""
    examples = word.get("examples", [])
    if not examples or len(examples) >= 10:
        return False
    for example in examples:
        found = SOURCE.search(example.get("source", ""))
        if not found:
            return False
        sura, verse = int(found.group(1)), int(found.group(2))
        if sura not in MEMORIZED_SURAS and (sura, verse) not in MEMORIZED_VERSES:
            return False
    return True


def band_of(rank):
    for index, (low, high) in enumerate(BANDS):
        if low <= rank <= high:
            return index
    raise ValueError(rank)


def eligible(word):
    kind = word["type"]
    if kind in EXCLUDED_TYPES:
        return False
    return kind == "Fiil" or kind.startswith("İsim")


SIFAT_TYPES = {
    "İsim – ism-i fâil", "İsim – ism-i mef'ûl", "İsim – sıfat", "İsim – sıfat-ı müşebbehe",
    "İsim – mübalağa sîgası", "İsim – ism-i tafdîl", "İsim / sıfat", "İsim / ism-i tafdîl",
}


def group_of(word, short):
    """Çeldiriciler aynı gruptan seçilir; yoksa '-mek' ekinden ya da sıfat kalıbından doğru şık sezilir.

    Grup türe değil şıkkın görünüşüne bakar: bazı mastarların anlamı isim gibi yazılmış ("iş, durum").
    """
    parts = [p.strip() for p in short.split(",")]
    match parts:
        case _ if any(re.search(r"m[ae]k$", p) for p in parts):
            return "eylem"
        case _ if word["type"] in SIFAT_TYPES:
            return "sıfat"
        case _:
            return "isim"


def gloss(meaning):
    """Anlamın ilk kısmı: ilk ';'ye kadar, parantezler atılmış. Çok uzunsa ilk virgüle kadar."""
    first = meaning.split(";")[0]
    first = re.sub(r"\([^)]*\)", "", first)
    first = re.sub(r"\s+", " ", first).strip(" ,:")
    if len(first) > MAX_GLOSS:
        first = first.split(",")[0].strip()
    # Ünlemli anlamlar ("ne kötü!", "gel!") şık olarak öne çıkıp doğru cevabı ele verir.
    if not first or len(first) > MAX_GLOSS or ":" in first or "!" in first:
        return None
    return first


def stems(value):
    """Kaba Türkçe kök: sözcüğün ilk beş harfi. Ortak anlam kelimesini yakalamak için yeterli."""
    words = re.findall(r"[a-zçğıöşüâîû]+", value.lower())
    return {w[:5] for w in words if len(w) >= 3 and w not in STOPWORDS}


def clashes(a, b):
    """İki kelimenin anlamları örtüşüyor mu: ortak anlam kelimesi, aynı kök ya da aynı kısa anlam."""
    if a.get("root") and a.get("root") == b.get("root"):
        return True
    if a["gloss"] == b["gloss"]:
        return True
    return bool(stems(a["meaning"]) & stems(b["meaning"]))


def read_exclusions(path):
    if not path.exists():
        return set()
    items = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.split("#")[0].strip()
        if line:
            items.add(line)
    return items


def build_pool(words, exclude_path):
    excluded = read_exclusions(exclude_path)
    candidates = []
    for word in words:
        if not eligible(word) or str(word["rank"]) in excluded or word["id"] in excluded:
            continue
        short = gloss(word["meaning"])
        if short is None:
            continue
        candidates.append({**word, "gloss": short, "band": band_of(word["rank"]), "group": group_of(word, short)})

    # Kökünden Türkçede bir kelime bulunan kökler: o kökten gelen kelimeler soru olarak ikinci tercih.
    familiar_roots = {w["root"] for w in words if w.get("root") and turkish_cognate(w)}

    def askable(c):
        return not turkish_cognate(c) and not only_in_memorized(c)

    def preferred(c):
        return c.get("root") not in familiar_roots

    pool = []
    for band in range(len(BANDS)):
        # Şık olarak her kelime kullanılabilir; soru olarak yalnızca Türkçede olmayan, ezberden
        # gelmeyenler. Önce kökü Türkçede de bilinmeyenler, yetmezse diğerleri.
        in_band = [c for c in candidates if c["band"] == band and askable(c)]
        first = [c for c in in_band if preferred(c)]
        second = [c for c in in_band if not preferred(c)]
        chosen = spread(first, PER_BAND)
        if len(chosen) < PER_BAND:
            chosen += spread(second, PER_BAND - len(chosen))
        chosen.sort(key=lambda c: c["rank"])
        for item in chosen:
            distractors = pick_distractors(item, candidates)
            if distractors is None:
                continue
            pool.append({
                "id": item["id"],
                "rank": item["rank"],
                "band": band,
                "word": item["word"],
                "type": item["type"],
                "answer": item["gloss"],
                "options": [d["gloss"] for d in distractors],
                "distractor_ranks": [d["rank"] for d in distractors],
            })
    return pool


def spread(items, count):
    """Bant boyunca eşit aralıklı seçim: bandın başını da sonunu da temsil etsin."""
    step = max(1, len(items) / count) if count else 1
    return [items[int(i * step)] for i in range(min(count, len(items)))]


def pick_distractors(item, candidates):
    """Aynı gruptan, aynı ya da komşu banttan, anlamı örtüşmeyen üç kelime. Seçim kelimeye göre sabittir."""
    rng = random.Random(item["id"])
    for spread in (0, 1, 2):
        near = [
            c for c in candidates
            if c["id"] != item["id"] and c["group"] == item["group"] and abs(c["band"] - item["band"]) <= spread
        ]
        rng.shuffle(near)
        chosen = []
        for c in near:
            if clashes(item, c) or any(clashes(c, d) for d in chosen):
                continue
            chosen.append(c)
            if len(chosen) == 3:
                return chosen
    return None


def write_review(pool, path):
    lines = [
        "# Seviye tespiti soru havuzu",
        "",
        "`scripts/build_deck.py` üretir; elle düzenlemeyin. Sorunlu bir soruyu çıkarmak için sırasını",
        "`data/placement-exclude.txt` dosyasına yazıp betiği yeniden çalıştırın. Çeldiricinin kelimesini",
        "çıkarmak için de onun sırasını yazmak yeter.",
        "",
        f"Toplam {len(pool)} soru.",
    ]
    for band, (low, high) in enumerate(BANDS):
        items = [q for q in pool if q["band"] == band]
        title = f"{low}–{high}" if high < 10**9 else f"{low}+"
        lines += ["", f"## Bant {band + 1}: sıra {title} ({len(items)} soru)", "",
                  "| Sıra | Kelime | Tür | Doğru | Çeldiriciler (sıra) |", "|---|---|---|---|---|"]
        for q in items:
            others = "; ".join(f"{o} (#{r})" for o, r in zip(q["options"], q["distractor_ranks"]))
            lines.append(f"| {q['rank']} | {q['word']} | {q['type']} | **{q['answer']}** | {others} |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
