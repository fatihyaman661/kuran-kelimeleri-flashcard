export interface Env {
  DB: D1Database;
  /** Google Cloud'daki Web istemci kimliği; uygulamadaki sync.googleClientId ile aynı olmalı. */
  GOOGLE_CLIENT_ID: string;
}

export class HttpError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}

/** İşleyicideki HttpError'ları JSON yanıta çevirir, beklenmeyen hataları 500 yapar. */
export async function handle(run: () => Promise<Response>): Promise<Response> {
  try {
    return await run();
  } catch (e) {
    if (e instanceof HttpError) return json({ error: e.message }, e.status);
    console.error(e);
    return json({ error: "internal" }, 500);
  }
}

export async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export function base64UrlDecode(value: string): Uint8Array {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
}

export function randomToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Authorization: Bearer <jeton> başlığından oturumu bulur; kullanıcı kimliğini döner. */
export async function requireUser(request: Request, env: Env): Promise<string> {
  const header = request.headers.get("authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7).trim() : "";
  if (!token) throw new HttpError(401, "unauthorized");
  const tokenHash = await sha256Hex(token);
  const row = await env.DB.prepare("SELECT user_id FROM sessions WHERE token_hash = ?").bind(tokenHash).first<{ user_id: string }>();
  if (!row) throw new HttpError(401, "unauthorized");
  await env.DB.prepare("UPDATE sessions SET last_used_at = ? WHERE token_hash = ?").bind(Date.now(), tokenHash).run();
  return row.user_id;
}
