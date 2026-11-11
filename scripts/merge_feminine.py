"""Yalnızca cinsiyet farkı olan dişil kartları eril kartlara katar (bir kerelik düzeltme).

Quranic Arabic Corpus bazı ism-i fâil ve sıfatların dişil hâlini (صَابِرَة, مُؤْمِنَة…) ayrı kelime sayıyor;
deste bunları ayrı kart yapmıştı. Desteyi kurarken konan kural ise "kalıp anlamı değiştirmiyorsa
çekimli hâller ayrı kart olmasın" idi. Hangi çiftlerin birleşeceği data/disil-birlestirme.tsv'de
(karar = birlestir); anlamı ayrışanlar (أُمّ / أُمَّة, كَبِير / كَبِيرَة…) ayrı kalır.

Birleştirmede eril kart kalır:
- Çekim kutusuna dişil hâller "Müennes" etiketiyle eklenir (sayılarda müennes/müzekker sayılan).
- Dişil kartın ilk örnekleri eril kartın örneklerine katılır (toplam en fazla 10, kısa olan önce);
  dişil örneklerde kelimenin dişil hâli belirtilir.
- Sıklıklar toplanır, bütün kartların sırası sıklığa göre yeniden hesaplanır.
- Dişil kart desteden kalkar (uygulamada "retired" olur).

Kullanım: python3 scripts/merge_feminine.py   (data/kelimeler.apkg'yi yerinde günceller)
Sonra:    python3 scripts/build_deck.py
"""

import re
import shutil
import sqlite3
import sys
import tempfile
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APKG = ROOT / "data" / "kelimeler.apkg"
PAIRS = ROOT / "data" / "disil-birlestirme.tsv"
EXCLUDE = ROOT / "data" / "placement-exclude.txt"

FIELDS = ["Kelime", "Tur", "Harekeli", "Tip", "Cekim", "Anlam", "Kok", "Siklik", "Ornekler", "MufAr", "MufTr", "Sira"]
F = {name: i for i, name in enumerate(FIELDS)}
MAX_EXAMPLES = 10
FEMININE_EXAMPLES = 3
FORM_ROW = re.compile(r'<div><b>([^<]+):</b> <span class="ar">([^<]*)</span></div>')
TAGS = re.compile(r"<[^>]+>")


def read_pairs():
    pairs = []
    for line in PAIRS.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        if cols[4] == "birlestir":
            pairs.append((cols[0], cols[1], cols[2], cols[3]))
    return pairs


def split_examples(html):
    """Ornekler alanı art arda <div class="ex">…</div> bloklarıdır."""
    starts = [m.start() for m in re.finditer(r'<div class="ex">', html)]
    return [html[a:b] for a, b in zip(starts, starts[1:] + [len(html)])]


def verse_length(example):
    verse = re.search(r'<div class="ar q">(.*?)</div>', example, re.S)
    return len(TAGS.sub("", verse.group(1))) if verse else 0


def source_of(example):
    found = re.search(r'<span class="src">\((.*?)\)</span>', example)
    return found.group(1) if found else None


def mark_feminine(example, form, label):
    """Dişil örnekte, kelimenin hangi hâliyle geçtiğini yazan satır (zamir kartlarındaki gibi)."""
    if 'class="pl"' in example:
        return example
    return example.replace('</div><div class="tr">', f'</div><div class="pl"><span class="ar">{form}</span> · {label}</div><div class="tr">', 1)


def feminine_rows(masc, fem):
    rows = FORM_ROW.findall(fem[F["Cekim"]])
    if fem[F["Tur"]].endswith("sayı"):
        # Sayılarda dişil yazılış müzekker sayılanla kullanılır (ثَلَاثَةُ رِجَالٍ), eril yazılış müennesle.
        return (f'<div><b>Müennes sayılanla:</b> <span class="ar">{masc[F["Harekeli"]]}</span></div>'
                f'<div><b>Müzekker sayılanla:</b> <span class="ar">{fem[F["Harekeli"]]}</span></div>'), True
    if not rows:
        # Eril kartta da çekim satırı yoksa (وَاحِد gibi) ikisi yan yana yazılır.
        masc_row = "" if FORM_ROW.search(masc[F["Cekim"]]) else f'<div><b>Müzekker:</b> <span class="ar">{masc[F["Harekeli"]]}</span></div>'
        return masc_row + f'<div><b>Müennes:</b> <span class="ar">{fem[F["Harekeli"]]}</span></div>', False
    out = []
    for label, ar in rows:
        match label:
            case "Tekil":
                label = "Müennes"
            case "Çoğul":
                label = "Müennes çoğul"
            case _:
                label = f"Müennes {label[0].lower()}{label[1:]}"
        out.append(f'<div><b>{label}:</b> <span class="ar">{ar}</span></div>')
    return "".join(out), False


def merge(masc, fem):
    rows, replace = feminine_rows(masc, fem)
    masc[F["Cekim"]] = rows if replace else masc[F["Cekim"]] + rows

    masc_examples = split_examples(masc[F["Ornekler"]])
    seen = {source_of(e) for e in masc_examples}
    label = "müzekker sayılanla" if fem[F["Tur"]].endswith("sayı") else "müennes"
    fem_examples = [mark_feminine(e, fem[F["Harekeli"]], label) for e in split_examples(fem[F["Ornekler"]]) if source_of(e) not in seen]
    taken = fem_examples[:FEMININE_EXAMPLES]
    chosen = masc_examples[: MAX_EXAMPLES - len(taken)] + taken
    if len(chosen) < MAX_EXAMPLES:
        chosen += fem_examples[len(taken): len(taken) + MAX_EXAMPLES - len(chosen)]
    masc[F["Ornekler"]] = "".join(sorted(chosen, key=verse_length))

    masc[F["Siklik"]] = str(int(masc[F["Siklik"]]) + int(fem[F["Siklik"]]))
    if not masc[F["MufAr"]] and fem[F["MufAr"]]:
        masc[F["MufAr"]], masc[F["MufTr"]] = fem[F["MufAr"]], fem[F["MufTr"]]


def main():
    work = Path(tempfile.mkdtemp())
    with zipfile.ZipFile(APKG) as archive:
        archive.extractall(work)
        names = archive.namelist()
    db = sqlite3.connect(work / "collection.anki2")
    notes = {nid: flds.split("\x1f") for nid, flds in db.execute("select id, flds from notes")}
    by_rank = {f[F["Sira"]]: nid for nid, f in notes.items()}
    old_rank = {nid: int(f[F["Sira"]]) for nid, f in notes.items()}

    removed = []
    for masc_rank, masc_word, fem_rank, fem_word in read_pairs():
        masc, fem = notes[by_rank[masc_rank]], notes[by_rank[fem_rank]]
        if masc[F["Harekeli"]] != masc_word or fem[F["Harekeli"]] != fem_word:
            sys.exit(f"Çift tutmuyor: #{masc_rank} {masc[F['Harekeli']]} / #{fem_rank} {fem[F['Harekeli']]}")
        merge(masc, fem)
        removed.append(by_rank[fem_rank])

    for nid in removed:
        del notes[nid]
    # Sıra = sıklık sırası; eşitlikte eski sıra korunur.
    order = sorted(notes, key=lambda nid: (-int(notes[nid][F["Siklik"]]), old_rank[nid]))
    new_rank = {nid: i + 1 for i, nid in enumerate(order)}
    for nid in order:
        notes[nid][F["Sira"]] = str(new_rank[nid])

    now = int(time.time())
    with db:
        db.executemany("delete from cards where nid = ?", [(nid,) for nid in removed])
        db.executemany("delete from notes where id = ?", [(nid,) for nid in removed])
        db.executemany(
            "update notes set flds = ?, mod = ?, usn = -1 where id = ?",
            [("\x1f".join(f), now, nid) for nid, f in notes.items()],
        )
        # Anki'de yeni kartların sırası "due" alanıdır.
        db.executemany("update cards set due = ? where nid = ? and type = 0", [(new_rank[nid], nid) for nid in order])
    db.execute("VACUUM")
    db.close()

    # Hariç tutma listesindeki sıralar da yeni sıraya taşınır.
    rank_map = {str(old_rank[nid]): str(new_rank[nid]) for nid in order}
    lines = []
    for line in EXCLUDE.read_text(encoding="utf-8").splitlines():
        head, sep, comment = line.partition("#")
        value = head.strip()
        if value.isdigit() and value in rank_map:
            line = f"{rank_map[value]:<5} {sep}{comment}".rstrip()
        lines.append(line)
    EXCLUDE.write_text("\n".join(lines) + "\n", encoding="utf-8")

    tmp = work / "kelimeler.apkg"
    with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as archive:
        for name in names:
            archive.write(work / name, name)
    shutil.copy(tmp, APKG)
    print(f"{len(removed)} dişil kart birleştirildi; {len(notes)} kart kaldı → {APKG}")


if __name__ == "__main__":
    main()
