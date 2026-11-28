export interface Env {
  DB: D1Database;
  AI: Ai;
  SESSION_SIGNING_SECRET: string;
  GOOGLE_CLIENT_ID?: string;
}

interface AuthSession {
  uid: string;
  displayName: string;
  email: string;
  exp: number;
}

const JSON_HEADERS = {
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), { status, headers: JSON_HEADERS });
}

function base64UrlEncode(input: Uint8Array | string): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function base64UrlDecode(str: string): Uint8Array {
  const padded = str.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (str.length % 4)) % 4);
  const binary = atob(padded);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

async function signSessionToken(payload: AuthSession, secret: string): Promise<string> {
  const headerB64 = base64UrlEncode(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payloadB64 = base64UrlEncode(JSON.stringify(payload));
  const data = `${headerB64}.${payloadB64}`;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(data));
  return `${data}.${base64UrlEncode(new Uint8Array(sig))}`;
}

async function verifySessionToken(token: string, secret: string): Promise<AuthSession | null> {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [headerB64, payloadB64, sigB64] = parts;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["verify"]
  );
  const valid = await crypto.subtle.verify(
    "HMAC",
    key,
    base64UrlDecode(sigB64),
    new TextEncoder().encode(`${headerB64}.${payloadB64}`)
  );
  if (!valid) return null;
  const parsed = JSON.parse(new TextDecoder().decode(base64UrlDecode(payloadB64))) as AuthSession;
  if (!parsed.uid || Date.now() > parsed.exp) return null;
  return parsed;
}

async function requireAuth(request: Request, env: Env): Promise<AuthSession | null> {
  const authHeader = request.headers.get("Authorization") ?? "";
  if (!authHeader.startsWith("Bearer ")) return null;
  const token = authHeader.slice(7).trim();
  if (!token || !env.SESSION_SIGNING_SECRET) return null;
  return verifySessionToken(token, env.SESSION_SIGNING_SECRET);
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method.toUpperCase();

    if (path === "/api/health" && method === "GET") {
      return jsonResponse({ ok: true, backend: "cloudflare-workers-d1-ai" });
    }

    // 1. Google ID Token -> Cloudflare HMAC Session Token
    if (path === "/api/auth/google" && method === "POST") {
      const body = (await request.json().catch(() => ({}))) as { idToken?: string };
      if (!body.idToken) return jsonResponse({ error: "Missing idToken" }, 400);

      const verifyResp = await fetch(
        `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(body.idToken)}`
      );
      if (!verifyResp.ok) return jsonResponse({ error: "Invalid Google ID token" }, 401);
      const tokenInfo = (await verifyResp.json()) as {
        sub?: string;
        aud?: string;
        name?: string;
        email?: string;
      };
      if (!tokenInfo.sub) return jsonResponse({ error: "Token missing sub" }, 401);
      if (env.GOOGLE_CLIENT_ID && tokenInfo.aud !== env.GOOGLE_CLIENT_ID) {
        return jsonResponse({ error: "Audience mismatch" }, 401);
      }

      const session: AuthSession = {
        uid: `google_${tokenInfo.sub}`,
        displayName: (tokenInfo.name ?? "Explorer").slice(0, 80),
        email: (tokenInfo.email ?? "").slice(0, 120),
        exp: Date.now() + 1000 * 60 * 60 * 24 * 30, // 30 days
      };
      const sessionToken = await signSessionToken(session, env.SESSION_SIGNING_SECRET);
      return jsonResponse({ ...session, sessionToken });
    }

    // 2. Cloudflare Workers AI Chat (replaces Firebase AI Logic text generation)
    if (path === "/api/ai/chat" && method === "POST") {
      const body = (await request.json().catch(() => ({}))) as {
        prompt?: string;
        model?: string;
        stream?: boolean;
      };
      const prompt = (body.prompt ?? "").trim();
      if (!prompt) return jsonResponse({ error: "Empty prompt" }, 400);

      const allowedModels = new Set([
        "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
        "@cf/meta/llama-3.1-8b-instruct-fast",
      ]);
      const model = body.model && allowedModels.has(body.model)
        ? body.model
        : "@cf/meta/llama-3.3-70b-instruct-fp8-fast";

      const result = (await env.AI.run(model as any, {
        messages: [{ role: "user", content: prompt }],
        max_tokens: 768,
        temperature: 0.7,
      })) as { response?: string };

      return jsonResponse({ text: result?.response ?? "" });
    }

    // 3. Cloudflare Workers AI Image Generation (replaces Nano Banana image endpoint)
    if (path === "/api/ai/image" && method === "POST") {
      const body = (await request.json().catch(() => ({}))) as { prompt?: string };
      const prompt = (body.prompt ?? "").trim();
      if (!prompt) return jsonResponse({ error: "Empty image prompt" }, 400);

      const img = (await env.AI.run("@cf/black-forest-labs/flux-1-schnell" as any, {
        prompt,
        num_steps: 4,
      })) as { image?: string };

      return jsonResponse({ imageBase64: img?.image ?? "" });
    }

    // All D1 user & leaderboard routes require verified Cloudflare session token
    const session = await requireAuth(request, env);
    if (!session) return jsonResponse({ error: "Unauthorized" }, 401);

    const now = Date.now();

    if (path === "/api/user/progress" && method === "GET") {
      const progressRow = await env.DB.prepare("SELECT * FROM users WHERE user_id = ?")
        .bind(session.uid)
        .first();
      const masteryRows = await env.DB.prepare("SELECT * FROM word_mastery WHERE user_id = ?")
        .bind(session.uid)
        .all();
      const scenarioRows = await env.DB.prepare("SELECT * FROM scenario_progress WHERE user_id = ?")
        .bind(session.uid)
        .all();

      return jsonResponse({
        progress: progressRow
          ? {
              userId: progressRow.user_id,
              displayName: progressRow.display_name,
              selectedGrade: progressRow.selected_grade,
              selectedTeacherId: progressRow.selected_teacher_id,
              dailyGoal: progressRow.daily_goal,
              streakCount: progressRow.streak_count,
              totalXp: progressRow.total_xp,
              coins: progressRow.coins,
              hearts: progressRow.hearts,
              todayWordsCompleted: progressRow.today_words_completed,
              lastStudyDate: progressRow.last_study_date,
              unlockedItems: JSON.parse(String(progressRow.unlocked_items_json ?? "[]")),
              kakaoUserId: progressRow.kakao_user_id,
              connectedKakaoIds: JSON.parse(String(progressRow.connected_kakao_ids_json ?? "[]")),
              createdAt: progressRow.created_at,
              updatedAt: progressRow.updated_at,
            }
          : null,
        wordMastery: (masteryRows.results ?? []).map((r: any) => ({
          wordId: r.word_id,
          grade: r.grade,
          correctCount: r.correct_count,
          missCount: r.miss_count,
          speakingBestScore: r.speaking_best_score,
          mastered: Boolean(r.mastered),
          createdAt: r.created_at,
          updatedAt: r.updated_at,
        })),
        scenarioProgress: (scenarioRows.results ?? []).map((r: any) => ({
          scenarioId: r.scenario_id,
          zoneId: r.zone_id,
          completedSentencesCount: r.completed_sentences_count,
          twistCleared: Boolean(r.twist_cleared),
          bestFluencyScore: r.best_fluency_score,
          createdAt: r.created_at,
          updatedAt: r.updated_at,
        })),
      });
    }

    if (path === "/api/user/progress" && method === "POST") {
      const b = (await request.json()) as Record<string, any>;
      await env.DB.prepare(
        `INSERT INTO users (
          user_id, display_name, selected_grade, selected_teacher_id, daily_goal,
          streak_count, total_xp, coins, hearts, today_words_completed,
          last_study_date, unlocked_items_json, kakao_user_id, connected_kakao_ids_json,
          created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(user_id) DO UPDATE SET
          display_name = excluded.display_name,
          selected_grade = excluded.selected_grade,
          selected_teacher_id = excluded.selected_teacher_id,
          daily_goal = excluded.daily_goal,
          streak_count = excluded.streak_count,
          total_xp = excluded.total_xp,
          coins = excluded.coins,
          hearts = excluded.hearts,
          today_words_completed = excluded.today_words_completed,
          last_study_date = excluded.last_study_date,
          unlocked_items_json = excluded.unlocked_items_json,
          kakao_user_id = excluded.kakao_user_id,
          connected_kakao_ids_json = excluded.connected_kakao_ids_json,
          updated_at = excluded.updated_at`
      )
        .bind(
          session.uid,
          String(b.displayName ?? session.displayName).slice(0, 80),
          Number(b.selectedGrade ?? 1),
          String(b.selectedTeacherId ?? "emma_nyc").slice(0, 40),
          Number(b.dailyGoal ?? 30),
          Number(b.streakCount ?? 1),
          Number(b.totalXp ?? 0),
          Number(b.coins ?? 150),
          Number(b.hearts ?? 5),
          Number(b.todayWordsCompleted ?? 0),
          String(b.lastStudyDate ?? "").slice(0, 20),
          JSON.stringify(Array.isArray(b.unlockedItems) ? b.unlockedItems.slice(0, 30) : ["badge_starter"]),
          String(b.kakaoUserId ?? "").slice(0, 64),
          JSON.stringify(Array.isArray(b.connectedKakaoIds) ? b.connectedKakaoIds.slice(0, 30) : []),
          now,
          now
        )
        .run();
      return jsonResponse({ ok: true });
    }

    if (path === "/api/user/word-mastery" && method === "POST") {
      const b = (await request.json()) as Record<string, any>;
      const wordId = String(b.wordId ?? "").trim().slice(0, 64);
      if (!wordId) return jsonResponse({ error: "Missing wordId" }, 400);
      await env.DB.prepare(
        `INSERT INTO word_mastery (user_id, word_id, grade, correct_count, miss_count, speaking_best_score, mastered, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(user_id, word_id) DO UPDATE SET
           grade = excluded.grade,
           correct_count = excluded.correct_count,
           miss_count = excluded.miss_count,
           speaking_best_score = excluded.speaking_best_score,
           mastered = excluded.mastered,
           updated_at = excluded.updated_at`
      )
        .bind(
          session.uid,
          wordId,
          Number(b.grade ?? 1),
          Number(b.correctCount ?? 0),
          Number(b.missCount ?? 0),
          Number(b.speakingBestScore ?? 0),
          b.mastered ? 1 : 0,
          now,
          now
        )
        .run();
      return jsonResponse({ ok: true });
    }

    if (path === "/api/user/scenario-progress" && method === "POST") {
      const b = (await request.json()) as Record<string, any>;
      const scenarioId = String(b.scenarioId ?? "").trim().slice(0, 64);
      if (!scenarioId) return jsonResponse({ error: "Missing scenarioId" }, 400);
      await env.DB.prepare(
        `INSERT INTO scenario_progress (user_id, scenario_id, zone_id, completed_sentences_count, twist_cleared, best_fluency_score, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(user_id, scenario_id) DO UPDATE SET
           zone_id = excluded.zone_id,
           completed_sentences_count = excluded.completed_sentences_count,
           twist_cleared = excluded.twist_cleared,
           best_fluency_score = excluded.best_fluency_score,
           updated_at = excluded.updated_at`
      )
        .bind(
          session.uid,
          scenarioId,
          Number(b.zoneId ?? 1),
          Number(b.completedSentencesCount ?? 0),
          b.twistCleared ? 1 : 0,
          Number(b.bestFluencyScore ?? 0),
          now,
          now
        )
        .run();
      return jsonResponse({ ok: true });
    }

    if (path === "/api/user/sessions" && method === "POST") {
      const b = (await request.json()) as Record<string, any>;
      await env.DB.prepare(
        `INSERT INTO study_sessions (user_id, mode, teacher_id, accuracy_percent, wpm, xp_earned, coins_earned, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
      )
        .bind(
          session.uid,
          String(b.mode ?? "AI_TUTOR").slice(0, 32),
          String(b.teacherId ?? "emma_nyc").slice(0, 40),
          Number(b.accuracyPercent ?? 0),
          Number(b.wpm ?? 0),
          Number(b.xpEarned ?? 0),
          Number(b.coinsEarned ?? 0),
          now
        )
        .run();
      return jsonResponse({ ok: true });
    }

    if (path === "/api/leaderboard/family/sync" && method === "POST") {
      const b = (await request.json()) as Record<string, any>;
      const entryId = String(b.entryId ?? `uid_${session.uid}`).slice(0, 80);
      await env.DB.prepare(
        `INSERT INTO family_streaks (entry_id, user_id, kakao_user_id, display_name, avatar_url, streak_count, today_words_completed, total_xp, last_study_date, share_with_friends, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(entry_id) DO UPDATE SET
           kakao_user_id = excluded.kakao_user_id,
           display_name = excluded.display_name,
           avatar_url = excluded.avatar_url,
           streak_count = excluded.streak_count,
           today_words_completed = excluded.today_words_completed,
           total_xp = excluded.total_xp,
           last_study_date = excluded.last_study_date,
           share_with_friends = excluded.share_with_friends,
           updated_at = excluded.updated_at
         WHERE family_streaks.user_id = excluded.user_id`
      )
        .bind(
          entryId,
          session.uid,
          String(b.kakaoUserId ?? "").slice(0, 64),
          String(b.displayName ?? session.displayName).slice(0, 40),
          String(b.avatarUrl ?? "").slice(0, 256),
          Number(b.streakCount ?? 0),
          Number(b.todayWordsCompleted ?? 0),
          Number(b.totalXp ?? 0),
          String(b.lastStudyDate ?? "").slice(0, 20),
          b.shareWithFriends ? 1 : 0,
          now,
          now
        )
        .run();
      return jsonResponse({ ok: true });
    }

    if (path === "/api/leaderboard/family/query" && method === "POST") {
      const b = (await request.json()) as { ownEntryId?: string; friendKakaoIds?: string[] };
      const ownEntryId = String(b.ownEntryId ?? `uid_${session.uid}`).slice(0, 80);
      const friendIds = Array.isArray(b.friendKakaoIds)
        ? b.friendKakaoIds.map((x) => String(x).slice(0, 64)).slice(0, 30)
        : [];
      const placeholders = friendIds.map(() => "?").join(", ");
      const sql = friendIds.length > 0
        ? `SELECT * FROM family_streaks WHERE entry_id = ? OR (entry_id IN (${placeholders}) AND share_with_friends = 1)`
        : `SELECT * FROM family_streaks WHERE entry_id = ?`;
      const rows = await env.DB.prepare(sql).bind(ownEntryId, ...friendIds).all();
      return jsonResponse({
        entries: (rows.results ?? []).map((r: any) => ({
          id: r.entry_id,
          userId: r.user_id,
          kakaoUserId: r.kakao_user_id,
          displayName: r.display_name,
          avatarUrl: r.avatar_url,
          streakCount: r.streak_count,
          todayWordsCompleted: r.today_words_completed,
          totalXp: r.total_xp,
          lastStudyDate: r.last_study_date,
          shareWithFriends: Boolean(r.share_with_friends),
          createdAt: r.created_at,
          updatedAt: r.updated_at,
        })),
      });
    }

    if (path === "/api/levels" && method === "POST") {
      const b = (await request.json()) as Record<string, any>;
      await env.DB.prepare(
        `INSERT INTO levels (user_id, daughter_name, vocab_grade_tier, princess_level, castle_hp_percent, mastered_word_count, accumulated_api_word_count, boss_victories, total_xp, registered_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(user_id) DO UPDATE SET
           daughter_name = excluded.daughter_name,
           vocab_grade_tier = excluded.vocab_grade_tier,
           princess_level = excluded.princess_level,
           castle_hp_percent = excluded.castle_hp_percent,
           mastered_word_count = excluded.mastered_word_count,
           accumulated_api_word_count = excluded.accumulated_api_word_count,
           boss_victories = excluded.boss_victories,
           total_xp = excluded.total_xp,
           registered_at = excluded.registered_at`
      )
        .bind(
          session.uid,
          String(b.daughterName ?? "원삐").slice(0, 32),
          Number(b.vocabGradeTier ?? 3),
          Number(b.princessLevel ?? 1),
          Number(b.castleHpPercent ?? 100),
          Number(b.masteredWordCount ?? 0),
          Number(b.accumulatedApiWordCount ?? 0),
          Number(b.bossVictories ?? 0),
          Number(b.totalXp ?? 0),
          now
        )
        .run();
      return jsonResponse({ ok: true });
    }

    if (path === "/api/user/data" && method === "DELETE") {
      await env.DB.batch([
        env.DB.prepare("DELETE FROM family_streaks WHERE user_id = ?").bind(session.uid),
        env.DB.prepare("DELETE FROM word_mastery WHERE user_id = ?").bind(session.uid),
        env.DB.prepare("DELETE FROM scenario_progress WHERE user_id = ?").bind(session.uid),
        env.DB.prepare("DELETE FROM study_sessions WHERE user_id = ?").bind(session.uid),
        env.DB.prepare("DELETE FROM levels WHERE user_id = ?").bind(session.uid),
        env.DB.prepare("DELETE FROM users WHERE user_id = ?").bind(session.uid),
      ]);
      return jsonResponse({ ok: true, deleted: true });
    }

    return jsonResponse({ error: "Not found" }, 404);
  },
};
