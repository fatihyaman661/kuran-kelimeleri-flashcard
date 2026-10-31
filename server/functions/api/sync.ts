import { Env, HttpError, handle, json, requireUser } from "../../lib/common";

const MAX_PUSH = 500;
const PAGE = 1000;
/** 1–4 cevaplar (Tekrar, Zor, İyi, Kolay); -1 askıya alma, -2 askıdan çıkarma. */
const RATINGS = new Set([1, 2, 3, 4, -1, -2]);

interface Review {
  id: string;
  wordId: string;
  reviewedAt: number;
  rating: number;
  durationMs: number;
}

function parseReview(value: unknown): Review {
  const r = value as Partial<Review> | null;
  const valid =
    r != null &&
    typeof r.id === "string" && r.id.length > 0 && r.id.length <= 64 &&
    typeof r.wordId === "string" && r.wordId.length > 0 && r.wordId.length <= 64 &&
    Number.isSafeInteger(r.reviewedAt) &&
    Number.isInteger(r.rating) && RATINGS.has(r.rating as number);
  if (!valid) throw new HttpError(400, "invalid review");
  const durationMs = Number.isSafeInteger(r.durationMs) ? Math.max(0, r.durationMs as number) : 0;
  return { id: r.id!, wordId: r.wordId!, reviewedAt: r.reviewedAt!, rating: r.rating!, durationMs };
}

/**
 * POST /api/sync  { cursor, reviews[] }  →  { cursor, reviews[], more }
 * Gelen kayıtlar eklenir (aynı kimlik ikinci kez eklenmez), sonra imleçten sonraki kayıtlar döner.
 * Kayıtlar yalnızca eklendiği için iki cihazın birleşmesinde çakışma olmaz.
 */
export const onRequestPost: PagesFunction<Env> = ({ request, env }) =>
  handle(async () => {
    const userId = await requireUser(request, env);
    const body = (await request.json().catch(() => null)) as { cursor?: unknown; reviews?: unknown } | null;
    const cursor = Number.isSafeInteger(body?.cursor) ? (body!.cursor as number) : 0;
    const incoming = Array.isArray(body?.reviews) ? body!.reviews : [];
    if (incoming.length > MAX_PUSH) throw new HttpError(413, "too many reviews");

    const reviews = incoming.map(parseReview);
    if (reviews.length > 0) {
      const insert = env.DB.prepare(
        "INSERT OR IGNORE INTO reviews (user_id, id, word_id, reviewed_at, rating, duration_ms) VALUES (?, ?, ?, ?, ?, ?)",
      );
      await env.DB.batch(reviews.map((r) => insert.bind(userId, r.id, r.wordId, r.reviewedAt, r.rating, r.durationMs)));
    }

    const { results } = await env.DB.prepare(
      "SELECT seq, id, word_id, reviewed_at, rating, duration_ms FROM reviews WHERE user_id = ? AND seq > ? ORDER BY seq LIMIT ?",
    )
      .bind(userId, cursor, PAGE)
      .all<{ seq: number; id: string; word_id: string; reviewed_at: number; rating: number; duration_ms: number }>();

    return json({
      cursor: results.length > 0 ? results[results.length - 1].seq : cursor,
      reviews: results.map((r) => ({ id: r.id, wordId: r.word_id, reviewedAt: r.reviewed_at, rating: r.rating, durationMs: r.duration_ms })),
      more: results.length === PAGE,
    });
  });
