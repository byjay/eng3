"""
Automated QA Verification Suite for the 10-Year CSAT Corpus Pipeline:
- 150-exam catalog expansion & 2024+ month shift verification
- Vector underline geometry matching (0..4pt gap, >=70% horizontal overlap)
- Asterisk (*) footnote separation & zero contamination into Tier S/A
- Gries DP / DP_norm / uniformity & recurrence gap math
- 5-tier JSON export + normalized 3-table SQLite & External-Content FTS5 search/sanitizer
"""
from __future__ import annotations

import importlib.util
import sqlite3
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def _load_module(name: str, file_path: Path):
    spec = importlib.util.spec_from_file_location(name, str(file_path))
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    import sys
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


parse_mod = _load_module("parse_exams", ROOT / "src" / "01_parse_exams.py")
nlp_mod = _load_module("nlp_annotate", ROOT / "src" / "02_nlp_annotate.py")
stats_mod = _load_module("compute_stats", ROOT / "src" / "03_compute_stats.py")
tier_mod = _load_module("export_tiered", ROOT / "src" / "04_export_tiered_json.py")
db_mod = _load_module("build_sqlite", ROOT / "src" / "05_build_sqlite_fts5.py")


class PipelineQaTest(unittest.TestCase):
    def test_catalog_expands_150_exams_and_reflects_2024_shifts(self) -> None:
        catalog = parse_mod.load_exam_catalog(ROOT / "config" / "exam_catalog_10yr.json")
        exams = parse_mod.expand_expected_exams(catalog)
        self.assertEqual(len(exams), 150)
        g3_2024 = [e["month"] for e in exams if e["year"] == 2024 and e["exam_type"] == "MOCK_G3"]
        g1_2024 = [e["month"] for e in exams if e["year"] == 2024 and e["exam_type"] == "MOCK_G1"]
        self.assertIn(5, g3_2024)
        self.assertNotIn(4, g3_2024)
        self.assertIn(10, g1_2024)
        self.assertNotIn(11, g1_2024)

    def test_underline_geometry_and_footnote_separation(self) -> None:
        spans = [
            parse_mod.TextSpanBox("thinking", 100.0, 200.0, 150.0, 212.0),
            parse_mod.TextSpanBox("reflectively", 155.0, 200.0, 220.0, 212.0),
        ]
        lines = [parse_mod.UnderlineSegment(102.0, 148.0, 214.0)]
        matched = parse_mod.match_underlined_spans(spans, lines)
        self.assertEqual(matched, ["thinking"])

        raw = "The archaic* manuscript revealed a pragmatic shift.\n* archaic: 고대의, 낡은"
        clean, footnotes = parse_mod.separate_footnotes(raw)
        self.assertEqual(footnotes, {"archaic": "고대의, 낡은"})
        self.assertNotIn("*", clean)

    def test_gries_dp_norm_and_recurrence_math(self) -> None:
        uniform = stats_mod.compute_gries_dp([10, 10, 10, 10], [1000, 1000, 1000, 1000])
        self.assertAlmostEqual(uniform["gries_dp"], 0.0, places=4)
        self.assertAlmostEqual(uniform["uniformity"], 1.0, places=4)

        bursty = stats_mod.compute_gries_dp([40, 0, 0, 0], [1000, 1000, 1000, 1000])
        self.assertAlmostEqual(bursty["gries_dp_norm"], 1.0, places=4)
        self.assertAlmostEqual(bursty["uniformity"], 0.0, places=4)

        rec = stats_mod.compute_recurrence([2019, 2021, 2024, 2027], latest_year=2027)
        self.assertEqual(rec["mean_gap_yr"], 2.67)
        self.assertEqual(rec["median_gap_yr"], 3.0)
        self.assertEqual(rec["max_gap_yr"], 3)
        self.assertEqual(rec["overdue_yr"], 0)

    def test_5_tier_classification_prevents_footnote_contamination_and_builds_fts5(self) -> None:
        sample_words = [
            {
                "word_id": "w_001", "lemma": "conversation", "word_family_head": "converse",
                "flags": {"is_polysemous": False, "is_listening_heavy": True, "is_new_recent_3yr": False, "is_overdue": False, "is_footnote_ever": False},
                "curriculum": {"kr_2022_group": "G1_ELEM_800", "cefr": "A2", "is_awl": False},
                "stats": {
                    "total_freq": 42, "reading_freq": 15, "listening_freq": 27, "listening_ratio": 0.643,
                    "csat_years": [2019, 2021, 2024, 2027],
                    "recurrence": {"mean_gap_yr": 2.67, "median_gap_yr": 3.0, "max_gap_yr": 3, "last_csat_year": 2027, "overdue_yr": 0},
                    "dispersion": {"gries_dp": 0.24, "gries_dp_norm": 0.27, "uniformity": 0.73},
                    "mock_to_csat_transfer_hits": 4, "first_appearance_year": 2018, "keyness_recent_3yr": 1.2,
                },
            },
            {
                "word_id": "w_002", "lemma": "archaic", "word_family_head": "archaic",
                "flags": {"is_polysemous": False, "is_listening_heavy": False, "is_new_recent_3yr": False, "is_overdue": False, "is_footnote_ever": True},
                "curriculum": {"kr_2022_group": "OOV", "cefr": "C1", "is_awl": False},
                "stats": {
                    "total_freq": 30, "reading_freq": 30, "listening_freq": 0, "listening_ratio": 0.0,
                    "csat_years": [2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025],
                    "recurrence": {"mean_gap_yr": 1.0, "median_gap_yr": 1.0, "max_gap_yr": 1, "last_csat_year": 2025, "overdue_yr": 2},
                    "dispersion": {"gries_dp": 0.10, "gries_dp_norm": 0.12, "uniformity": 0.88},
                    "mock_to_csat_transfer_hits": 5, "first_appearance_year": 2018, "keyness_recent_3yr": 0.0,
                },
            },
        ]
        with tempfile.TemporaryDirectory() as td:
            out_dir = Path(td)
            manifest = tier_mod.export_tiered_json(sample_words, out_dir)
            self.assertEqual(manifest["tiers"]["TIER_S_CORE"]["item_count"], 1)
            self.assertEqual(manifest["tiers"]["TIER_D_FOOTNOTE"]["item_count"], 1)

            classified = [tier_mod.classify_tier_and_difficulty(w) for w in sample_words]
            sentences = [{
                "sent_id": "2027_CSAT_L05_S02", "year": 2027, "exam_type": "CSAT", "grade": 3, "month": 11,
                "section": "listening", "item_no": 5, "item_type": "듣기 대화",
                "text": "The conversation about renewable energy helped students refine their ideas.",
                "translation_ko": "재생 에너지에 대한 대화는 학생들이 생각을 다듬도록 도왔다.",
                "grammar_tags": ["SUBJECT_VERB_AGREE"],
            }]
            occurrences = [{
                "occ_id": "occ_001", "word_id": "w_001", "sent_id": "2027_CSAT_L05_S02",
                "surface_form": "conversation", "pos": "NOUN", "char_span": [4, 16],
                "is_underlined": False, "is_answer_key_token": False,
            }]
            db_path = out_dir / "test.sqlite"
            gz_path = out_dir / "test.sqlite.gz"
            res = db_mod.build_sqlite_fts5(classified, sentences, occurrences, db_path, gz_path)
            self.assertEqual(res["words_inserted"], 2)
            self.assertTrue(gz_path.exists() and gz_path.stat().st_size > 0)

            sanitized = db_mod.sanitize_fts_query('self-driving "energy": (test)')
            self.assertEqual(sanitized, '"self"* AND "driving"* AND "energy"* AND "test"*')

            conn = sqlite3.connect(str(db_path))
            try:
                rows = conn.execute(
                    "SELECT s.sent_id FROM sentences_fts f JOIN sentences s ON s.rowid = f.rowid WHERE sentences_fts MATCH ?",
                    (db_mod.sanitize_fts_query("convers"),),
                ).fetchall()
                self.assertEqual(len(rows), 1)
                self.assertEqual(rows[0][0], "2027_CSAT_L05_S02")
            finally:
                conn.close()


if __name__ == "__main__":
    unittest.main()
