import { Env, HttpError, handle, json, randomToken, sha256Hex } from "../../lib/common";
import { verifyGoogleIdToken } from "../../lib/google";

/**
 * POST /api/auth  { idToken }  →  { token }
 * Google kimlik jetonu bir kez doğrulanır; karşılığında uzun ömürlü bir oturum jetonu verilir.
 * Veritabanında jetonun kendisi değil özeti, kullanıcının da yalnızca Google kimliğinin özeti durur.
 */
export const onRequestPost: PagesFunction<Env> = ({ request, env }) =>
  handle(async () => {
    const body = (await request.json().catch(() => null)) as { idToken?: unknown } | null;
    if (typeof body?.idToken !== "string") throw new HttpError(400, "idToken required");

    const sub = await verifyGoogleIdToken(body.idToken, env.GOOGLE_CLIENT_ID);
    const userId = await sha256Hex(`google:${sub}`);
    const token = randomToken();
    const now = Date.now();
    await env.DB.prepare("INSERT INTO sessions (token_hash, user_id, created_at, last_used_at) VALUES (?, ?, ?, ?)")
      .bind(await sha256Hex(token), userId, now, now)
      .run();
    return json({ token });
  });
