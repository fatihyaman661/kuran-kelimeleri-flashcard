#!/usr/bin/env python3
"""Anki destesini (data/kelimeler.apkg) uygulamanın okuduğu SQLite dosyasına dönüştürür.

Çıktılar:
  data/deck.db.gz         Bütün kelimeler (gzip'li SQLite). Uygulama hem ilk kurulumda hem güncellemede
                          bunu satır satır kendi veritabanına kopyalar; cihazda JSON ayrıştırılmaz.
  data/deck-version.json  Uygulamanın güncelleme kontrolünde indirdiği küçük dosya.

Kelime kimliği Anki not GUID'idir. AnkiWeb güncellemelerinde ilerleme korunsun diye GUID'ler
zaten sabit tutuluyor; uygulama da ilerlemeyi bu kimliğe bağlar. Sürüm, içeriğin özetidir:
içerik değişmeden betik yeniden çalıştırılırsa sürüm de değişmez.

Kullanım: python3 scripts/build_deck.py [--apkg data/kelimeler.apkg] [--out data]
Yalnızca standart kütüphane kullanır.
"""

import argparse
import gzip
import hashlib
import os
import html
import json
import re
import sqlite3
import sys
import tempfile
import zipfile
from html.parser import HTMLParser
from pathlib import Path

FORMAT = 1
SCHEMA = """
CREATE TABLE meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);
CREATE TABLE words (
    id TEXT PRIMARY KEY NOT NULL,   -- Anki not GUID'i
    rank INTEGER NOT NULL,          -- Kur'an'daki sıklık sırası
    word TEXT NOT NULL,
    vocalized TEXT NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    meaning TEXT NOT NULL,
    root TEXT,
    frequency INTEGER NOT NULL,
    forms TEXT NOT NULL,            -- çekimlerin Arapçası, boşlukla ayrılmış (arama için)
    details TEXT NOT NULL           -- JSON: forms, table, examples, mufredat, tags (kartın arka yüzü)
);
"""
DETAIL_KEYS = ("forms", "table", "examples", "mufredat", "tags")
NOTE_TYPE = "Kur'an Kelime (TR)"
FIELDS = ["Kelime", "Tur", "Harekeli", "Tip", "Cekim", "Anlam", "Kok", "Siklik", "Ornekler", "MufAr", "MufTr", "Sira"]


def text(value):
    """HTML varlıklarını çözer, fazla boşlukları sadeleştirir."""
    return re.sub(r"\s+", " ", html.unescape(value)).strip()


def utf16_len(value):
    return len(value.encode("utf-16-le")) // 2


class Node:
    def __init__(self, tag, attrs, parent=None):
        self.tag = tag
        self.attrs = dict(attrs)
        self.classes = set((self.attrs.get("class") or "").split())
        self.parent = parent
        self.children = []

    def find_all(self, predicate):
        for child in self.children:
            if isinstance(child, Node):
                if predicate(child):
                    yield child
                yield from child.find_all(predicate)

    def text(self):
        return "".join(c if isinstance(c, str) else c.text() for c in self.children)


class TreeBuilder(HTMLParser):
    VOID = {"br", "hr", "img"}

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node("root", [])
        self.current = self.root

    def handle_starttag(self, tag, attrs):
        node = Node(tag, attrs, self.current)
        self.current.children.append(node)
        if tag == "br":
            self.current.children.append("\n")
        if tag not in self.VOID:
            self.current = node

    def handle_endtag(self, tag):
        node = self.current
        while node is not self.root and node.tag != tag:
            node = node.parent
        if node is not self.root:
            self.current = node.parent

    def handle_data(self, data):
        self.current.children.append(data)


def parse_html(fragment):
    builder = TreeBuilder()
    builder.feed(fragment)
    builder.close()
    return builder.root


def has_class(name):
    return lambda node: name in node.classes


def parse_verse(node):
    """Ayet metnini ve vurgulanan kelimenin UTF-16 aralıklarını çıkarır (Kotlin String indeksleri)."""
    parts, highlights, offset = [], [], 0
    for child in node.children:
        piece = child if isinstance(child, str) else child.text()
        if isinstance(child, Node) and "hl" in child.classes:
            highlights.append([offset, offset + utf16_len(piece)])
        parts.append(piece)
        offset += utf16_len(piece)
    return "".join(parts), highlights


def parse_examples(fragment):
    examples = []
    for ex in parse_html(fragment).find_all(has_class("ex")):
        verse = next(ex.find_all(has_class("q")), None)
        translation = next(ex.find_all(has_class("tr")), None)
        if verse is None or translation is None:
            raise ValueError("örnekte ayet ya da meal yok")
        ar, highlights = parse_verse(verse)
        source = next(translation.find_all(has_class("src")), None)
        tr = text("".join(c if isinstance(c, str) else c.text() for c in translation.children if c is not source))
        item = {"ar": ar.strip(), "highlights": highlights, "tr": tr}
        if source is not None:
            item["source"] = text(source.text()).strip("()")
        form = next(ex.find_all(has_class("pl")), None)
        if form is not None:
            form_ar = next(form.find_all(has_class("ar")), None)
            label = text("".join(c if isinstance(c, str) else c.text() for c in form.children if c is not form_ar))
            item["form"] = {"ar": text(form_ar.text()) if form_ar else "", "label": label.lstrip("· ").strip()}
        examples.append(item)
    return examples


SIMPLE_FORM = re.compile(r'<div><b>([^<]+):</b> <span class="ar">([^<]*)</span></div>')


def parse_conjugation(fragment):
    """Çekim alanı ya 'Etiket: değer' satırlarıdır ya da (zamirler, mevsuller için) bir tablodur."""
    if not fragment:
        return {}
    if "<table" not in fragment:
        forms = [{"label": text(label), "ar": text(ar)} for label, ar in SIMPLE_FORM.findall(fragment)]
        if SIMPLE_FORM.sub("", fragment).strip():
            raise ValueError(f"tanınmayan çekim biçimi: {fragment[:120]}")
        return {"forms": forms}

    root = parse_html(fragment)
    rows = []
    for tr in root.find_all(lambda n: n.tag == "tr"):
        cells = []
        for cell in tr.children:
            if not isinstance(cell, Node) or cell.tag not in ("th", "td"):
                continue
            small = next(cell.find_all(lambda n: n.tag == "small"), None)
            main = "".join(c if isinstance(c, str) else c.text() for c in cell.children if c is not small)
            if cell.tag == "th":
                kind = "header"
            elif "cur" in cell.classes:
                kind = "current"
            elif "ar" in cell.classes:
                kind = "arabic"
            elif "pn" in cell.classes:
                kind = "muted"
            else:
                kind = "text"
            item = {"text": text(main), "kind": kind}
            if small is not None:
                item["sub"] = text(small.text())
            cells.append(item)
        rows.append(cells)
    table = {"rows": rows}
    note = text(re.sub(r"<table.*?</table>", "", fragment, flags=re.S).replace("</div>", " "))
    note = text(re.sub(r"<[^>]+>", "", note))
    if note:
        table["note"] = note
    return {"table": table}


def open_collection(apkg, workdir):
    with zipfile.ZipFile(apkg) as archive:
        names = set(archive.namelist())
        for candidate in ("collection.anki21", "collection.anki2"):
            if candidate in names:
                return sqlite3.connect(archive.extract(candidate, workdir))
        if "collection.anki21b" in names:
            sys.exit("Deste yeni Anki biçiminde (anki21b). Anki'den dışa aktarırken "
                     "'Eski Anki sürümleriyle uyumlu' seçeneğini işaretleyin.")
    sys.exit(f"{apkg} içinde Anki koleksiyonu bulunamadı.")


def build_words(db):
    models = json.loads(db.execute("select models from col").fetchone()[0])
    model = next((m for m in models.values() if m["name"] == NOTE_TYPE), None)
    if model is None:
        sys.exit(f"'{NOTE_TYPE}' not tipi bulunamadı.")
    names = [f["name"] for f in sorted(model["flds"], key=lambda f: f["ord"])]
    if names != FIELDS:
        sys.exit(f"Not tipinin alanları değişmiş: {names}")

    words = []
    for guid, flds, tags in db.execute("select guid, flds, tags from notes where mid = ?", (int(model["id"]),)):
        f = dict(zip(FIELDS, flds.split("\x1f")))
        word = {
            "id": guid,
            "rank": int(f["Sira"]),
            "word": text(f["Kelime"]),
            "vocalized": text(f["Harekeli"]),
            "type": text(f["Tur"]),
            "category": text(f["Tip"]),
            "meaning": text(f["Anlam"]),
            "frequency": int(f["Siklik"] or 0),
        }
        if f["Kok"]:
            word["root"] = text(f["Kok"])
        if tags.split():
            word["tags"] = tags.split()
        word.update(parse_conjugation(f["Cekim"]))
        if f["Ornekler"]:
            word["examples"] = parse_examples(f["Ornekler"])
        if f["MufAr"] or f["MufTr"]:
            word["mufredat"] = {"ar": text(f["MufAr"]), "tr": text(f["MufTr"])}
        words.append(word)

    words.sort(key=lambda w: w["rank"])
    ids = [w["id"] for w in words]
    if len(set(ids)) != len(ids):
        sys.exit("Aynı GUID'e sahip birden fazla not var.")
    return words


def write_db(words, version, path):
    if path.exists():
        path.unlink()
    db = sqlite3.connect(path)
    db.executescript(SCHEMA)
    db.executemany("INSERT INTO meta VALUES (?, ?)", [("format", str(FORMAT)), ("version", version)])
    db.executemany(
        "INSERT INTO words VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        [
            (
                w["id"], w["rank"], w["word"], w["vocalized"], w["type"], w["category"], w["meaning"],
                w.get("root"), w["frequency"],
                " ".join(f["ar"] for f in w.get("forms", [])),
                json.dumps({k: w[k] for k in DETAIL_KEYS if k in w}, ensure_ascii=False, separators=(",", ":")),
            )
            for w in words
        ],
    )
    db.commit()
    db.execute("VACUUM")
    db.close()


def main():
    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apkg", type=Path, default=root / "data" / "kelimeler.apkg")
    parser.add_argument("--out", type=Path, default=root / "data")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory() as workdir:
        db = open_collection(args.apkg, workdir)
        words = build_words(db)
        db.close()

        canonical = "\n".join(json.dumps(w, ensure_ascii=False, sort_keys=True, separators=(",", ":")) for w in words)
        version = hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:12]

        plain = Path(workdir) / "deck.db"
        write_db(words, version, plain)
        args.out.mkdir(parents=True, exist_ok=True)
        target = args.out / "deck.db.gz"
        # mtime=0: içerik aynıysa dosya da bayt bayt aynı kalsın.
        with open(plain, "rb") as src, open(target, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0, filename="") as out:
            out.write(src.read())

    meta = {"format": FORMAT, "version": version, "words": len(words), "bytes": os.path.getsize(target)}
    (args.out / "deck-version.json").write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    print(f"{len(words)} kelime, sürüm {version}, {meta['bytes'] / 1e6:.1f} MB → {target}")


if __name__ == "__main__":
    main()
