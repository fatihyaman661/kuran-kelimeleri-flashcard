import { HttpError, base64UrlDecode } from "./common";

const CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
const ISSUERS = ["accounts.google.com", "https://accounts.google.com"];

interface GoogleKey extends JsonWebKey {
  kid: string;
}

interface IdTokenPayload {
  iss: string;
  aud: string;
  sub: string;
  exp: number;
}

function decodeJson<T>(part: string): T {
  return JSON.parse(new TextDecoder().decode(base64UrlDecode(part))) as T;
}

/**
 * Uygulamanın Credential Manager'dan aldığı Google kimlik jetonunu doğrular: RS256 imzası
 * Google'ın açık anahtarlarıyla, sonra yayıncı, hedef kitle ve süre. Geçerliyse "sub" döner.
 */
export async function verifyGoogleIdToken(token: string, clientId: string): Promise<string> {
  const [headerPart, payloadPart, signaturePart] = token.split(".");
  if (!headerPart || !payloadPart || !signaturePart) throw new HttpError(401, "invalid token");

  const header = decodeJson<{ alg: string; kid: string }>(headerPart);
  if (header.alg !== "RS256") throw new HttpError(401, "invalid token");

  // Google anahtarları saatler boyunca değişmez; Cloudflare önbelleği her istekte indirmeyi önler.
  const response = await fetch(CERTS_URL, { cf: { cacheTtl: 3600, cacheEverything: true } });
  if (!response.ok) throw new HttpError(503, "google certs unavailable");
  const { keys } = (await response.json()) as { keys: GoogleKey[] };
  const jwk = keys.find((k) => k.kid === header.kid);
  if (!jwk) throw new HttpError(401, "invalid token");

  const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const valid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    key,
    base64UrlDecode(signaturePart),
    new TextEncoder().encode(`${headerPart}.${payloadPart}`),
  );
  if (!valid) throw new HttpError(401, "invalid token");

  const payload = decodeJson<IdTokenPayload>(payloadPart);
  if (!ISSUERS.includes(payload.iss)) throw new HttpError(401, "invalid issuer");
  if (payload.aud !== clientId) throw new HttpError(401, "invalid audience");
  if (typeof payload.exp !== "number" || payload.exp * 1000 < Date.now()) throw new HttpError(401, "token expired");
  if (!payload.sub) throw new HttpError(401, "invalid token");
  return payload.sub;
}
