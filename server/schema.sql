-- Kur'an Kelimeleri senkron veritabanı (Cloudflare D1).
-- Kişisel veri en aza indirilir: kullanıcı yalnızca Google hesap kimliğinin SHA-256 özetiyle tutulur,
-- e-posta ya da ad saklanmaz.

CREATE TABLE IF NOT EXISTS sessions (
    token_hash   TEXT PRIMARY KEY,           -- oturum jetonunun SHA-256 özeti (jetonun kendisi saklanmaz)
    user_id      TEXT NOT NULL,              -- Google "sub" değerinin SHA-256 özeti
    created_at   INTEGER NOT NULL,
    last_used_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS sessions_user ON sessions (user_id);

-- Tekrar kayıtları yalnızca eklenir. seq, istemcinin "bundan sonrasını ver" imleci olur.
CREATE TABLE IF NOT EXISTS reviews (
    seq         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     TEXT NOT NULL,
    id          TEXT NOT NULL,               -- istemcinin ürettiği UUID
    word_id     TEXT NOT NULL,               -- deste kimliği (Anki not GUID'i)
    reviewed_at INTEGER NOT NULL,            -- epoch milisaniye
    rating      INTEGER NOT NULL,            -- 1 Tekrar, 2 Zor, 3 İyi, 4 Kolay
    duration_ms INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, id)
);
CREATE INDEX IF NOT EXISTS reviews_user_seq ON reviews (user_id, seq);
