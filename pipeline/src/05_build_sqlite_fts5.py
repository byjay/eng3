"""
Step 5: Normalized 3-Table SQLite + External-Content FTS5 Binary Compiler
- Compiles tiered word JSONs + normalized sentences & occurrences into a compact SQLite DB
  with External-Content FTS5 (tokenize='porter unicode61') and gzip output (csat_vocab_v1.sqlite.gz).
- Compatible 1:1 with Android's `CorpusSqliteFtsStore` BYOD importer.
"""
from __future__ import annotations

import argparse
import gzip
import json
import re
import shutil
import sqlite3
from pathlib import Path
from typing import Any, Dict, List

FTS_SPECIAL_RE = re.compile(r"[\"'*():^~-]+")


def sanitize_fts_query(raw: str) -> str:
    tokens = [t for t in FTS_SPECIAL_RE.sub(" ", raw.strip()).split() if t]
    if not tokens:
        return ""
    return " AND ".join(f'"{t}"*' for t in tokens)


SCHEMA_SQL = """
CREATE TABLE words (
  word_id TEXT PRIMARY KEY,
  lemma TEXT NOT NULL,
  tier TEXT NOT NULL,
  difficulty_rank INTEGER NOT NULL,
  difficulty_score REAL NOT NULL,
  priority_score REAL NOT NULL,
  total_freq INTEGER NOT NULL,
  csat_freq INTEGER NOT NULL,
  uniformity REAL NOT NULL,
  mean_gap_yr REAL,
  overdue_yr INTEGER,
  listening_ratio REAL NOT NULL,
  is_new_recent_3yr INTEGER NOT NULL DEFAULT 0,
  is_polysemous INTEGER NOT NULL DEFAULT 0,
  payload_json TEXT NOT NULL
);

CREATE INDEX idx_words_tier_prio ON words(tier, priority_score DESC);
CREATE INDEX idx_words_lemma ON words(lemma COLLATE NOCASE);

CREATE TABLE sentences (
  rowid INTEGER PRIMARY KEY AUTOINCREMENT,
  sent_id TEXT UNIQUE NOT NULL,
  year INTEGER NOT NULL,
  exam_type TEXT NOT NULL,
  grade INTEGER NOT NULL,
  month INTEGER NOT NULL,
  section TEXT NOT NULL,
  item_no INTEGER NOT NULL,
  item_type TEXT NOT NULL,
  text TEXT NOT NULL,
  translation_ko TEXT,
  grammar_tags_json TEXT
);

CREATE TABLE occurrences (
  occ_id TEXT PRIMARY KEY,
  word_id TEXT NOT NULL,
  sent_id TEXT NOT NULL,
  surface_form TEXT NOT NULL,
  pos TEXT NOT NULL,
  span_start INTEGER NOT NULL,
  span_end INTEGER NOT NULL,
  is_underlined INTEGER NOT NULL DEFAULT 0,
  is_answer_key INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (word_id) REFERENCES words(word_id),
  FOREIGN KEY (sent_id) REFERENCES sentences(sent_id)
);

CREATE INDEX idx_occ_word_lookup ON occurrences(word_id, sent_id);
CREATE INDEX idx_occ_word_surface ON occurrences(word_id, surface_form);

CREATE VIRTUAL TABLE sentences_fts USING fts5(
  text,
  content='sentences',
  content_rowid='rowid',
  tokenize='porter unicode61'
);
"""


def build_sqlite_fts5(
    words: List[Dict[str, Any]],
    sentences: List[Dict[str, Any]],
    occurrences: List[Dict[str, Any]],
    sqlite_path: Path,
    gzip_out_path: Path | None = None,
) -> Dict[str, Any]:
    sqlite_path.parent.mkdir(parents=True, exist_ok=True)
    if sqlite_path.exists():
        sqlite_path.unlink()

    conn = sqlite3.connect(str(sqlite_path))
    try:
        conn.executescript(SCHEMA_SQL)
        lemma_to_word_id: Dict[str, str] = {}

        word_rows = []
        for w in words:
            st = w["stats"]
            fl = w["flags"]
            rec = st["recurrence"]
            lemma_to_word_id[w["lemma"].lower()] = w["word_id"]
            word_rows.append((
                w["word_id"],
                w["lemma"],
                w["tier"],
                int(w.get("difficulty_rank", 2)),
                float(w.get("difficulty_score", 50.0)),
                float(w.get("priority_score", 50.0)),
                int(st["total_freq"]),
                len(st.get("csat_years", [])),
                float(st["dispersion"]["uniformity"]),
                rec.get("mean_gap_yr"),
                rec.get("overdue_yr"),
                float(st.get("listening_ratio", 0.0)),
                1 if fl.get("is_new_recent_3yr") else 0,
                1 if fl.get("is_polysemous") else 0,
                json.dumps(w, ensure_ascii=False, separators=(",", ":")),
            ))
        conn.executemany(
            """INSERT INTO words (
                 word_id, lemma, tier, difficulty_rank, difficulty_score, priority_score,
                 total_freq, csat_freq, uniformity, mean_gap_yr, overdue_yr,
                 listening_ratio, is_new_recent_3yr, is_polysemous, payload_json
               ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            word_rows,
        )

        sent_rows = []
        for s in sentences:
            sent_rows.append((
                s["sent_id"],
                int(s["year"]),
                s["exam_type"],
                int(s.get("grade", 3)),
                int(s.get("month", 11)),
                s.get("section", "reading"),
                int(s.get("item_no", 29)),
                s.get("item_type", "독해 지문"),
                s["text"],
                s.get("translation_ko", ""),
                json.dumps(s.get("grammar_tags", []), ensure_ascii=False),
            ))
        conn.executemany(
            """INSERT OR IGNORE INTO sentences (
                 sent_id, year, exam_type, grade, month, section,
                 item_no, item_type, text, translation_ko, grammar_tags_json
               ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            sent_rows,
        )

        occ_rows = []
        for o in occurrences:
            wid = o.get("word_id") or lemma_to_word_id.get(str(o.get("lemma", "")).lower())
            if not wid:
                continue
            span = o.get("char_span") or [0, 0]
            occ_rows.append((
                o["occ_id"],
                wid,
                o["sent_id"],
                o.get("surface_form", ""),
                o.get("pos", "NOUN"),
                int(span[0]),
                int(span[1]),
                1 if o.get("is_underlined") else 0,
                1 if o.get("is_answer_key_token") else 0,
            ))
        conn.executemany(
            """INSERT OR IGNORE INTO occurrences (
                 occ_id, word_id, sent_id, surface_form, pos,
                 span_start, span_end, is_underlined, is_answer_key
               ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            occ_rows,
        )

        conn.execute("INSERT INTO sentences_fts(sentences_fts) VALUES('rebuild')")
        conn.commit()
        conn.execute("VACUUM")
    finally:
        conn.close()

    gz_bytes = 0
    if gzip_out_path is not None:
        gzip_out_path.parent.mkdir(parents=True, exist_ok=True)
        with sqlite_path.open("rb") as src, gzip.open(gzip_out_path, "wb", compresslevel=9) as dst:
            shutil.copyfileobj(src, dst)
        gz_bytes = gzip_out_path.stat().st_size

    return {
        "words_inserted": len(word_rows),
        "sentences_inserted": len(sent_rows),
        "occurrences_inserted": len(occ_rows),
        "sqlite_bytes": sqlite_path.stat().st_size,
        "gzip_bytes": gz_bytes,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Step 5: Build normalized SQLite + External-Content FTS5 DB")
    parser.add_argument("--data-dir", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    words: List[Dict[str, Any]] = []
    words_dir = args.data_dir / "words"
    for p in sorted(words_dir.glob("tier_*.json")):
        doc = json.loads(p.read_text(encoding="utf-8"))
        words.extend(doc.get("items", []))

    sentences_path = args.data_dir / "corpus" / "sentences.json"
    occurrences_path = args.data_dir / "corpus" / "occurrences.json"
    sentences = json.loads(sentences_path.read_text(encoding="utf-8")) if sentences_path.exists() else []
    occurrences = json.loads(occurrences_path.read_text(encoding="utf-8")) if occurrences_path.exists() else []

    raw_sqlite = args.out.with_suffix("") if args.out.suffix == ".gz" else args.out
    gz_target = args.out if args.out.suffix == ".gz" else None
    summary = build_sqlite_fts5(words, sentences, occurrences, raw_sqlite, gz_target)
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
