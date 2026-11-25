/**
 * Suneung Vocab Unlimited Distributed Accumulator — Cloudflare Workers + R2
 *
 * - Unlimited 500-word R2 sharding: shards/shard-0001.json, shard-0002.json ...
 * - 5x daily non-uniform Cron runs (10 words/run = 50 words/day) with random Jitter
 * - 3 Free APIs combined with retry/backoff:
 *     1) Free Dictionary API (IPA, POS, real examples, synonyms/antonyms)
 *     2) Datamuse API (synonyms, antonyms, frequency, topic seed replenishment)
 *     3) MyMemory Free Translation API (en -> ko natural meaning/example translation)
 * - Union-Find synonym clustering (SYN-0001, SYN-0002...) + antonym group links
 * - Auto-queue replenishment whenever queue.json drops below 50 words
 * - Backward-compatible POST /api/levels endpoint for app progress sync
 */

const DEFAULT_SHARD_SIZE = 500;
const DEFAULT_WORDS_PER_CRON = 10;
const DEFAULT_MIN_QUEUE = 50;

const INITIAL_CSAT_SEEDS = [
  "alleviate", "pragmatic", "ubiquitous", "resilient", "ambiguous",
  "corroborate", "discrepancy", "fluctuate", "inevitable", "sustainable",
  "crucial", "attribute", "coherent", "consequence", "controversy",
  "cultivate", "deteriorate", "diminish", "elaborate", "emerge",
  "emphasize", "enhance", "enormous", "equivalent", "evaluate",
  "evident", "evolve", "exaggerate", "explicit", "facilitate",
  "fundamental", "generate", "hypothesis", "implement", "implication",
  "incentive", "incorporate", "indigenous", "indispensable", "infer",
  "inherent", "innovative", "insight", "integrate", "integrity",
  "interpret", "intervene", "intrinsic", "justify", "legitimate",
  "maintain", "manipulate", "mechanism", "mediate", "metaphor",
  "migrate", "minimize", "modify", "monitor", "motivate",
  "mutual", "neglect", "notion", "objective", "obscure",
  "obstacle", "obtain", "obvious", "occupy", "occur",
  "offset", "optimize", "orient", "outcome", "overcome",
  "paradigm", "paradox", "parallel", "parameter", "participate",
  "perceive", "persistent", "perspective", "persuade", "phenomenon",
  "plausible", "policy", "potential", "precede", "precise",
  "predict", "predominant", "prejudice", "preliminary", "preserve",
  "prevail", "primary", "principle", "prioritize", "profound"
];

const REPLENISH_TOPICS = [
  "academic,education,research,logic",
  "science,technology,environment,climate",
  "economy,society,policy,global",
  "psychology,emotion,behavior,cognition",
  "travel,communication,culture,daily",
  "argument,evidence,analysis,ethics"
];

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const jitterMs = (min = 280, max = 950) =>
  Math.floor(min + Math.random() * (max - min));

function normalizeWord(raw) {
  if (!raw || typeof raw !== "string") return "";
  const cleaned = raw.trim().toLowerCase();
  if (cleaned.length < 4 || cleaned.length > 20) return "";
  if (!/^[a-z]+$/.test(cleaned)) return "";
  return cleaned;
}

async function fetchJsonWithRetry(url, { retries = 2, timeoutMs = 8000 } = {}) {
  let lastError = null;
  for (let attempt = 0; attempt <= retries; attempt++) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const resp = await fetch(url, {
        headers: { Accept: "application/json" },
        signal: controller.signal
      });
      clearTimeout(timer);
      if (resp.status === 429 || resp.status >= 500) {
        if (attempt < retries) {
          await sleep(450 * (attempt + 1) + jitterMs(100, 350));
          continue;
        }
        throw new Error(`HTTP ${resp.status} for ${url}`);
      }
      if (!resp.ok) {
        return null;
      }
      return await resp.json();
    } catch (err) {
      clearTimeout(timer);
      lastError = err;
      if (attempt < retries) {
        await sleep(400 * (attempt + 1));
      }
    }
  }
  console.warn(`fetchJsonWithRetry exhausted for ${url}:`, lastError?.message);
  return null;
}

async function readR2Json(r2, key, fallback) {
  try {
    const obj = await r2.get(key);
    if (!obj) return fallback;
    const text = await obj.text();
    return text ? JSON.parse(text) : fallback;
  } catch (err) {
    console.warn(`readR2Json fallback for ${key}:`, err?.message);
    return fallback;
  }
}

async function writeR2Json(r2, key, data) {
  await r2.put(key, JSON.stringify(data), {
    httpMetadata: { contentType: "application/json; charset=utf-8" }
  });
}

function shardKeyForNumber(num) {
  return `shards/shard-${String(num).padStart(4, "0")}.json`;
}

// === Union-Find Disjoint Set for Synonym & Antonym Clustering ===
class UnionFind {
  constructor() {
    this.parent = new Map();
  }
  add(x) {
    if (!this.parent.has(x)) this.parent.set(x, x);
  }
  find(x) {
    this.add(x);
    let root = x;
    while (this.parent.get(root) !== root) {
      root = this.parent.get(root);
    }
    let cur = x;
    while (cur !== root) {
      const next = this.parent.get(cur);
      this.parent.set(cur, root);
      cur = next;
    }
    return root;
  }
  union(a, b) {
    const ra = this.find(a);
    const rb = this.find(b);
    if (ra === rb) return;
    // Deterministic lexicographical root so SYN IDs stay stable
    if (ra < rb) this.parent.set(rb, ra);
    else this.parent.set(ra, rb);
  }
}

function rebuildSynonymGroups(existingGroupsDoc, newlyAddedWords) {
  const uf = new UnionFind();
  const antonymPairs = [];

  // Rehydrate existing clusters
  for (const group of Object.values(existingGroupsDoc.groups || {})) {
    const members = Array.isArray(group.members) ? group.members : [];
    for (let i = 0; i < members.length; i++) {
      const m = normalizeWord(members[i]);
      if (!m) continue;
      uf.add(m);
      if (i > 0) uf.union(normalizeWord(members[0]), m);
    }
  }
  for (const pair of existingGroupsDoc.antonymEdges || []) {
    if (Array.isArray(pair) && pair.length === 2) {
      antonymPairs.push(pair);
    }
  }

  // Merge newly harvested words + synonyms + antonyms
  for (const item of newlyAddedWords) {
    const w = normalizeWord(item.word);
    if (!w) continue;
    uf.add(w);
    for (const syn of item.synonyms || []) {
      const ns = normalizeWord(syn);
      if (ns && ns !== w) uf.union(w, ns);
    }
    for (const ant of item.antonyms || []) {
      const na = normalizeWord(ant);
      if (na && na !== w) {
        uf.add(na);
        antonymPairs.push(w < na ? [w, na] : [na, w]);
      }
    }
  }

  // Group all members by deterministic root
  const rootToMembers = new Map();
  for (const word of uf.parent.keys()) {
    const root = uf.find(word);
    if (!rootToMembers.has(root)) rootToMembers.set(root, new Set());
    rootToMembers.get(root).add(word);
  }

  const sortedRoots = Array.from(rootToMembers.keys()).sort();
  const rootToGroupId = new Map();
  const wordToGroupId = new Map();
  const groups = {};

  sortedRoots.forEach((root, idx) => {
    const groupId = `SYN-${String(idx + 1).padStart(4, "0")}`;
    const members = Array.from(rootToMembers.get(root)).sort();
    rootToGroupId.set(root, groupId);
    members.forEach((m) => wordToGroupId.set(m, groupId));
    groups[groupId] = {
      id: groupId,
      members,
      antonymGroupIds: []
    };
  });

  // Cross-link opposing antonym group IDs
  const uniqueAntEdges = new Set();
  const cleanEdges = [];
  for (const [a, b] of antonymPairs) {
    const edgeKey = `${a}|${b}`;
    if (uniqueAntEdges.has(edgeKey)) continue;
    uniqueAntEdges.add(edgeKey);
    cleanEdges.push([a, b]);
    const ga = wordToGroupId.get(a);
    const gb = wordToGroupId.get(b);
    if (ga && gb && ga !== gb) {
      if (!groups[ga].antonymGroupIds.includes(gb)) groups[ga].antonymGroupIds.push(gb);
      if (!groups[gb].antonymGroupIds.includes(ga)) groups[gb].antonymGroupIds.push(ga);
    }
  }

  // Stamp synGroupId back onto newly added words
  for (const item of newlyAddedWords) {
    const w = normalizeWord(item.word);
    item.synGroupId = wordToGroupId.get(w) || "";
  }

  return {
    updatedAt: new Date().toISOString(),
    totalGroups: Object.keys(groups).length,
    groups,
    antonymEdges: cleanEdges.slice(-4000)
  };
}

// === 3 Free APIs Pipeline (Free Dictionary + Datamuse + MyMemory) ===
async function translateToKoreanMyMemory(text) {
  if (!text || text.trim().length < 2) return "";
  const clipped = text.trim().slice(0, 180);
  const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(clipped)}&langpair=en|ko`;
  const data = await fetchJsonWithRetry(url, { retries: 1, timeoutMs: 6500 });
  const translated = data?.responseData?.translatedText;
  if (!translated || typeof translated !== "string") return "";
  const clean = translated.replace(/&#39;/g, "'").replace(/&quot;/g, '"').trim();
  if (/MYMEMORY WARNING|QUERY LENGTH LIMIT/i.test(clean)) return "";
  return clean;
}

async function fetchSingleWordProfile(rawWord) {
  const word = normalizeWord(rawWord);
  if (!word) return null;

  const [dictRes, synRes, antRes, metaRes] = await Promise.all([
    fetchJsonWithRetry(`https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(word)}`),
    fetchJsonWithRetry(`https://api.datamuse.com/words?rel_syn=${encodeURIComponent(word)}&max=8`),
    fetchJsonWithRetry(`https://api.datamuse.com/words?rel_ant=${encodeURIComponent(word)}&max=6`),
    fetchJsonWithRetry(`https://api.datamuse.com/words?sp=${encodeURIComponent(word)}&md=dfp&max=1`)
  ]);

  const dictEntry = Array.isArray(dictRes) ? dictRes[0] : null;
  const dmEntry = Array.isArray(metaRes) ? metaRes[0] : null;

  let ipa = dictEntry?.phonetic || "";
  if (!ipa && Array.isArray(dictEntry?.phonetics)) {
    const withText = dictEntry.phonetics.find((p) => p?.text);
    ipa = withText?.text || "";
  }
  if (!ipa) ipa = `/${word}/`;

  let pos = "n.";
  let englishDef = "";
  const examplePool = [];
  const synSet = new Set();
  const antSet = new Set();

  if (Array.isArray(dictEntry?.meanings)) {
    for (const m of dictEntry.meanings) {
      if (!englishDef && m.partOfSpeech) {
        if (m.partOfSpeech.startsWith("verb")) pos = "v.";
        else if (m.partOfSpeech.startsWith("adj")) pos = "adj.";
        else if (m.partOfSpeech.startsWith("adv")) pos = "adv.";
        else pos = "n.";
      }
      (m.synonyms || []).forEach((s) => {
        const ns = normalizeWord(s);
        if (ns && ns !== word) synSet.add(ns);
      });
      (m.antonyms || []).forEach((a) => {
        const na = normalizeWord(a);
        if (na && na !== word) antSet.add(na);
      });
      for (const d of m.definitions || []) {
        if (!englishDef && d.definition) englishDef = d.definition;
        if (d.example && examplePool.length < 2) examplePool.push(d.example);
      }
    }
  }

  if (!englishDef && Array.isArray(dmEntry?.defs) && dmEntry.defs.length > 0) {
    const parts = dmEntry.defs[0].split("\t");
    if (parts[0] === "v") pos = "v.";
    else if (parts[0] === "adj") pos = "adj.";
    else if (parts[0] === "adv") pos = "adv.";
    englishDef = (parts[1] || parts[0] || "").trim();
  }

  if (!englishDef) return null;

  (Array.isArray(synRes) ? synRes : []).forEach((item) => {
    const ns = normalizeWord(item?.word);
    if (ns && ns !== word) synSet.add(ns);
  });
  (Array.isArray(antRes) ? antRes : []).forEach((item) => {
    const na = normalizeWord(item?.word);
    if (na && na !== word) antSet.add(na);
  });

  // Extract corpus frequency tag from Datamuse ("f:14.2")
  let freq = 10.0;
  for (const tag of dmEntry?.tags || []) {
    if (typeof tag === "string" && tag.startsWith("f:")) {
      const parsed = parseFloat(tag.slice(2));
      if (!Number.isNaN(parsed)) freq = parsed;
    }
  }
  const csatFrequency = freq >= 25 ? 5 : freq >= 8 ? 4 : 3;
  const csatDifficulty = word.length >= 10 || freq < 5 ? 3 : word.length >= 7 || freq < 18 ? 2 : 1;

  // Natural Korean meaning + example translation via MyMemory
  const primaryExampleEn =
    examplePool[0] ||
    (pos === "v."
      ? `We should ${word} this strategy carefully before the exam.`
      : pos === "adj."
        ? `A ${word} mindset helps you solve difficult problems faster.`
        : `Understanding ${word} is essential for reading academic passages.`);

  const [meaningKoRaw, exampleKoRaw] = await Promise.all([
    translateToKoreanMyMemory(`${word}: ${englishDef.slice(0, 90)}`),
    translateToKoreanMyMemory(primaryExampleEn)
  ]);

  const meaningKo = meaningKoRaw || englishDef.slice(0, 56);
  const exampleKo = exampleKoRaw || `'${word}'(${meaningKo})를 실전 문장에서 자연스럽게 말해 보세요.`;

  return {
    id: `r2_${word}`,
    word,
    ipa,
    partOfSpeech: pos,
    grade: csatDifficulty,
    csatFrequency,
    csatDifficulty,
    topic: csatDifficulty === 3 ? "LOGIC_DEBATE" : csatDifficulty === 2 ? "SCIENCE_AI" : "SCHOOL_EXAM",
    meaningKo,
    englishDef: englishDef.slice(0, 140),
    exampleEn: primaryExampleEn,
    exampleKo,
    examples: [
      { labelKo: "사전 실전 예문", english: primaryExampleEn, korean: exampleKo, ipaHint: ipa },
      ...(examplePool[1]
        ? [{ labelKo: "확장 예문", english: examplePool[1], korean: `'${word}' 확장 쓰임 연습`, ipaHint: ipa }]
        : [])
    ],
    synonyms: Array.from(synSet).slice(0, 6),
    antonyms: Array.from(antSet).slice(0, 4),
    synGroupId: "",
    collectedAt: new Date().toISOString()
  };
}

// === Auto Queue Replenishment when queue.json < 50 ===
async function replenishQueueIfNeeded(queue, existingSet, candidatePool, minThreshold) {
  const qSet = new Set(queue.map(normalizeWord).filter(Boolean));
  const nextQueue = Array.from(qSet);

  // 1) Prioritize newly discovered synonyms & antonyms from collected words
  for (const cand of candidatePool) {
    const nw = normalizeWord(cand);
    if (nw && !existingSet.has(nw) && !qSet.has(nw)) {
      nextQueue.push(nw);
      qSet.add(nw);
    }
  }

  // 2) If still below threshold, harvest fresh academic/CSAT topic words from Datamuse
  if (nextQueue.length < minThreshold) {
    const topicIdx = Math.floor(Date.now() / 3600000) % REPLENISH_TOPICS.length;
    const topics = REPLENISH_TOPICS[topicIdx];
    const dmList = await fetchJsonWithRetry(
      `https://api.datamuse.com/words?topics=${encodeURIComponent(topics)}&md=dfp&max=80`
    );
    for (const entry of Array.isArray(dmList) ? dmList : []) {
      const nw = normalizeWord(entry?.word);
      if (nw && !existingSet.has(nw) && !qSet.has(nw)) {
        nextQueue.push(nw);
        qSet.add(nw);
      }
      if (nextQueue.length >= minThreshold + 60) break;
    }
  }

  return nextQueue;
}

// === Core Batch Runner (10 words/run, 5x daily = 50 words/day) ===
async function runScheduledBatch(env, maxWordsOverride) {
  const r2 = env.VOCAB_R2;
  const shardSize = parseInt(env.SHARD_SIZE || String(DEFAULT_SHARD_SIZE), 10);
  const batchLimit = maxWordsOverride || parseInt(env.WORDS_PER_CRON || String(DEFAULT_WORDS_PER_CRON), 10);
  const minQueue = parseInt(env.MIN_QUEUE_THRESHOLD || String(DEFAULT_MIN_QUEUE), 10);

  const meta = await readR2Json(r2, "meta.json", {
    totalWords: 0,
    currentShard: 1,
    totalShards: 1,
    totalGroups: 0,
    lastRunAt: null
  });

  const existingIndex = await readR2Json(r2, "index/word-set.json", []);
  const existingSet = new Set(existingIndex.map(normalizeWord).filter(Boolean));

  let queue = await readR2Json(r2, "queue.json", []);
  if (queue.length === 0 && existingSet.size === 0) {
    queue = [...INITIAL_CSAT_SEEDS];
  }

  queue = await replenishQueueIfNeeded(queue, existingSet, [], minQueue);

  const newlyCollected = [];
  const discoveredRelated = [];
  let cursor = 0;

  while (cursor < queue.length && newlyCollected.length < batchLimit) {
    const candidate = normalizeWord(queue[cursor++]);
    if (!candidate || existingSet.has(candidate)) continue;

    // Gentle random jitter between words to stay well under free API rate limits
    if (newlyCollected.length > 0) {
      await sleep(jitterMs(260, 820));
    }

    const profile = await fetchSingleWordProfile(candidate);
    if (profile) {
      newlyCollected.push(profile);
      existingSet.add(profile.word);
      discoveredRelated.push(...profile.synonyms, ...profile.antonyms);
    }
  }

  const remainingQueue = await replenishQueueIfNeeded(
    queue.slice(cursor),
    existingSet,
    discoveredRelated,
    minQueue
  );

  // Merge Union-Find synonym/antonym clusters
  const existingGroupsDoc = await readR2Json(r2, "groups/synonym-groups.json", {
    updatedAt: null,
    totalGroups: 0,
    groups: {},
    antonymEdges: []
  });
  const updatedGroupsDoc = rebuildSynonymGroups(existingGroupsDoc, newlyCollected);

  // Append into 500-word sharded R2 files
  let shardNum = meta.currentShard || 1;
  let shardItems = await readR2Json(r2, shardKeyForNumber(shardNum), []);
  for (const wordItem of newlyCollected) {
    if (shardItems.length >= shardSize) {
      await writeR2Json(r2, shardKeyForNumber(shardNum), shardItems);
      shardNum += 1;
      shardItems = await readR2Json(r2, shardKeyForNumber(shardNum), []);
    }
    shardItems.push(wordItem);
  }
  await writeR2Json(r2, shardKeyForNumber(shardNum), shardItems);

  const nextMeta = {
    totalWords: existingSet.size,
    currentShard: shardNum,
    totalShards: shardNum,
    totalGroups: updatedGroupsDoc.totalGroups,
    queueRemaining: remainingQueue.length,
    lastBatchAdded: newlyCollected.length,
    lastRunAt: new Date().toISOString()
  };

  await Promise.all([
    writeR2Json(r2, "meta.json", nextMeta),
    writeR2Json(r2, "index/word-set.json", Array.from(existingSet)),
    writeR2Json(r2, "queue.json", remainingQueue),
    writeR2Json(r2, "groups/synonym-groups.json", updatedGroupsDoc)
  ]);

  return { meta: nextMeta, addedCount: newlyCollected.length };
}

function jsonResponse(body, status = 200, env = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN || "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
      "Cache-Control": "public, max-age=120"
    }
  });
}

export default {
  async scheduled(_event, env, ctx) {
    ctx.waitUntil(runScheduledBatch(env));
  },

  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN || "*",
          "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization"
        }
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const r2 = env.VOCAB_R2;

    if (request.method === "GET" && path === "/api/vocab/meta") {
      const meta = await readR2Json(r2, "meta.json", { totalWords: 0, currentShard: 1, totalShards: 1, totalGroups: 0 });
      return jsonResponse(meta, 200, env);
    }

    if (request.method === "GET" && path === "/api/vocab/groups") {
      const groups = await readR2Json(r2, "groups/synonym-groups.json", { totalGroups: 0, groups: {} });
      return jsonResponse(groups, 200, env);
    }

    if (request.method === "GET" && path.startsWith("/api/vocab/shard/")) {
      const num = parseInt(path.split("/").pop() || "1", 10);
      if (Number.isNaN(num) || num < 1) {
        return jsonResponse({ error: "Invalid shard number" }, 400, env);
      }
      const items = await readR2Json(r2, shardKeyForNumber(num), []);
      return jsonResponse({ shard: num, count: items.length, words: items }, 200, env);
    }

    if (request.method === "GET" && path === "/api/vocab/latest") {
      const limit = Math.min(200, Math.max(1, parseInt(url.searchParams.get("limit") || "50", 10)));
      const meta = await readR2Json(r2, "meta.json", { currentShard: 1, totalWords: 0 });
      const latest = [];
      for (let s = meta.currentShard || 1; s >= 1 && latest.length < limit; s--) {
        const items = await readR2Json(r2, shardKeyForNumber(s), []);
        for (let i = items.length - 1; i >= 0 && latest.length < limit; i--) {
          latest.push(items[i]);
        }
      }
      return jsonResponse({ meta, count: latest.length, words: latest }, 200, env);
    }

    if (request.method === "POST" && path === "/api/vocab/bootstrap") {
      if (env.ADMIN_TOKEN) {
        const auth = request.headers.get("Authorization") || "";
        if (auth !== `Bearer ${env.ADMIN_TOKEN}`) {
          return jsonResponse({ error: "Unauthorized" }, 401, env);
        }
      }
      const body = await request.json().catch(() => ({}));
      const count = Math.min(25, Math.max(1, parseInt(body.batchSize || "10", 10)));
      const result = await runScheduledBatch(env, count);
      return jsonResponse({ ok: true, ...result }, 200, env);
    }

    // Backward-compatible app level registration endpoint
    if (request.method === "POST" && (path === "/api/levels" || path === "/levels")) {
      const payload = await request.json().catch(() => null);
      if (!payload || !payload.daughterName) {
        return jsonResponse({ error: "Invalid level payload" }, 400, env);
      }
      const safeName = String(payload.daughterName).replace(/[^a-zA-Z0-9가-힣_-]/g, "_").slice(0, 32);
      await writeR2Json(r2, `levels/${safeName}.json`, payload);
      return jsonResponse({ ok: true, saved: safeName }, 200, env);
    }

    return jsonResponse({ error: "Not found" }, 404, env);
  }
};
