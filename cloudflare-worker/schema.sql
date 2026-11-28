-- Cloudflare D1 Schema for SpeakQuest (replaces Firebase Firestore collections)

CREATE TABLE IF NOT EXISTS users (
  user_id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL DEFAULT 'Explorer',
  selected_grade INTEGER NOT NULL DEFAULT 1,
  selected_teacher_id TEXT NOT NULL DEFAULT 'emma_nyc',
  daily_goal INTEGER NOT NULL DEFAULT 30,
  streak_count INTEGER NOT NULL DEFAULT 1,
  total_xp INTEGER NOT NULL DEFAULT 0,
  coins INTEGER NOT NULL DEFAULT 150,
  hearts INTEGER NOT NULL DEFAULT 5,
  today_words_completed INTEGER NOT NULL DEFAULT 0,
  last_study_date TEXT NOT NULL DEFAULT '',
  unlocked_items_json TEXT NOT NULL DEFAULT '["badge_starter"]',
  kakao_user_id TEXT NOT NULL DEFAULT '',
  connected_kakao_ids_json TEXT NOT NULL DEFAULT '[]',
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS word_mastery (
  user_id TEXT NOT NULL,
  word_id TEXT NOT NULL,
  grade INTEGER NOT NULL DEFAULT 1,
  correct_count INTEGER NOT NULL DEFAULT 0,
  miss_count INTEGER NOT NULL DEFAULT 0,
  speaking_best_score INTEGER NOT NULL DEFAULT 0,
  mastered INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, word_id)
);

CREATE TABLE IF NOT EXISTS scenario_progress (
  user_id TEXT NOT NULL,
  scenario_id TEXT NOT NULL,
  zone_id INTEGER NOT NULL DEFAULT 1,
  completed_sentences_count INTEGER NOT NULL DEFAULT 0,
  twist_cleared INTEGER NOT NULL DEFAULT 0,
  best_fluency_score INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, scenario_id)
);

CREATE TABLE IF NOT EXISTS study_sessions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  mode TEXT NOT NULL,
  teacher_id TEXT NOT NULL,
  accuracy_percent INTEGER NOT NULL DEFAULT 0,
  wpm INTEGER NOT NULL DEFAULT 0,
  xp_earned INTEGER NOT NULL DEFAULT 0,
  coins_earned INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS family_streaks (
  entry_id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  kakao_user_id TEXT NOT NULL DEFAULT '',
  display_name TEXT NOT NULL DEFAULT 'Explorer',
  avatar_url TEXT NOT NULL DEFAULT '',
  streak_count INTEGER NOT NULL DEFAULT 0,
  today_words_completed INTEGER NOT NULL DEFAULT 0,
  total_xp INTEGER NOT NULL DEFAULT 0,
  last_study_date TEXT NOT NULL DEFAULT '',
  share_with_friends INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS levels (
  user_id TEXT PRIMARY KEY,
  daughter_name TEXT NOT NULL,
  vocab_grade_tier INTEGER NOT NULL,
  princess_level INTEGER NOT NULL,
  castle_hp_percent INTEGER NOT NULL,
  mastered_word_count INTEGER NOT NULL,
  accumulated_api_word_count INTEGER NOT NULL,
  boss_victories INTEGER NOT NULL,
  total_xp INTEGER NOT NULL,
  registered_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_family_streaks_user ON family_streaks(user_id);
CREATE INDEX IF NOT EXISTS idx_family_streaks_kakao ON family_streaks(kakao_user_id);
CREATE INDEX IF NOT EXISTS idx_study_sessions_user ON study_sessions(user_id, created_at DESC);
