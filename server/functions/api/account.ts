import { Env, handle, json, requireUser } from "../../lib/common";

/**
 * DELETE /api/account  →  { deleted }
 * Kullanıcının sunucudaki bütün tekrar kayıtlarını ve oturumlarını siler. Google Play, hesap
 * oluşturan uygulamalardan uygulama içinden hesap/veri silme imkânı ister.
 */
export const onRequestDelete: PagesFunction<Env> = ({ request, env }) =>
  handle(async () => {
    const userId = await requireUser(request, env);
    const [reviews] = await env.DB.batch([
      env.DB.prepare("DELETE FROM reviews WHERE user_id = ?").bind(userId),
      env.DB.prepare("DELETE FROM sessions WHERE user_id = ?").bind(userId),
    ]);
    return json({ deleted: reviews.meta.changes ?? 0 });
  });
