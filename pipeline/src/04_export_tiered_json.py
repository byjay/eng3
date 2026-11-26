"""
Step 4: 5-Tier Difficulty Classifier & Partitioned JSON Exporter
- Splits vocabulary master into 5 difficulty tier files:
    tier_s_core.json | tier_a_cyclical.json | tier_b_advanced_new.json |
    tier_c_bursty.json | tier_d_footnote.json
- Generates manifest.json with item counts, byte sizes, and schema metadata.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any, Dict, List

TIER_FILE_MAP = {
    "TIER_S_CORE": ("tier_s_core.json", 1),
    "TIER_A_CYCLICAL": ("tier_a_cyclical.json", 2),
    "TIER_B_ADVANCED_NEW": ("tier_b_advanced_new.json", 3),
    "TIER_C_BURSTY": ("tier_c_bursty.json", 4),
    "TIER_D_FOOTNOTE": ("tier_d_footnote.json", 5),
}


def classify_tier_and_difficulty(word: Dict[str, Any]) -> Dict[str, Any]:
    """
    Classifies a lemma into 5 tiers (TIER_S_CORE .. TIER_D_FOOTNOTE) and computes
    0..100 difficulty_score and priority_score.
    Guarantees zero contamination of footnote/excluded terms into Tier S/A.
    """
    st = word["stats"]
    cur = word["curriculum"]
    fl = word["flags"]
    unif = float(st["dispersion"]["uniformity"])
    csat_cnt = len(st.get("csat_years", []))
    mean_gap = st["recurrence"].get("mean_gap_yr")
    kr_grp = cur.get("kr_2022_group", "OOV")

    # 1) Tier D: asterisk footnote or excluded entity (proper noun / unit / abbreviation)
    if fl.get("is_footnote_ever") or fl.get("is_excluded_entity"):
        tier = "TIER_D_FOOTNOTE"
        diff_rank = 5
    # 2) Tier S: evergreen absolute core
    elif (unif >= 0.70) or (
        kr_grp in {"G1_ELEM_800", "G2_MID_H1_1200"}
        and csat_cnt >= 7
        and mean_gap is not None
        and mean_gap <= 1.5
    ):
        tier = "TIER_S_CORE"
        diff_rank = 1
    # 3) Tier A: cyclical core & KICE mock-to-CSAT carryover
    elif (0.40 <= unif < 0.70) or (3 <= csat_cnt <= 6) or (
        st.get("mock_to_csat_transfer_hits", 0) >= 2 and kr_grp != "OOV"
    ):
        tier = "TIER_A_CYCLICAL"
        diff_rank = 2
    # 4) Tier B: high-discrimination OOV or recent 3-year emergent/surging word
    elif (kr_grp == "OOV" and unif >= 0.15) or fl.get("is_new_recent_3yr") or (
        float(st.get("keyness_recent_3yr", 0.0)) >= 6.63
    ):
        tier = "TIER_B_ADVANCED_NEW"
        diff_rank = 3
    # 5) Tier C: low-dispersion bursty word concentrated in 1-2 passages
    else:
        tier = "TIER_C_BURSTY"
        diff_rank = 4

    base_map = {
        "G1_ELEM_800": 15.0,
        "G2_MID_H1_1200": 35.0,
        "G3_H2_H3_1000": 60.0,
        "OOV": 82.0,
    }
    diff_score = min(
        100.0,
        round(base_map.get(kr_grp, 75.0) + (1.0 - unif) * 18.0 + (5.0 if fl.get("is_polysemous") else 0.0), 1),
    )

    freq_norm = min(1.0, math.log1p(int(st["total_freq"])) / math.log1p(500))
    prio_score = min(
        100.0,
        round(
            unif * 45.0
            + freq_norm * 25.0
            + min(csat_cnt, 10) * 2.0
            + (10.0 if fl.get("is_polysemous") else 0.0),
            1,
        ),
    )

    enriched = dict(word)
    enriched["tier"] = tier
    enriched["difficulty_rank"] = diff_rank
    enriched["difficulty_score"] = diff_score
    enriched["priority_score"] = prio_score
    return enriched


def export_tiered_json(words: List[Dict[str, Any]], out_dir: Path, schema_version: str = "2.0.0") -> Dict[str, Any]:
    words_dir = out_dir / "words"
    words_dir.mkdir(parents=True, exist_ok=True)
    buckets: Dict[str, List[Dict[str, Any]]] = {k: [] for k in TIER_FILE_MAP}

    for w in words:
        classified = classify_tier_and_difficulty(w)
        buckets[classified["tier"]].append(classified)

    manifest_tiers: Dict[str, Any] = {}
    for tier_key, (filename, diff_rank) in TIER_FILE_MAP.items():
        items = sorted(
            buckets[tier_key],
            key=lambda x: (-x["priority_score"], -x["stats"]["total_freq"], x["lemma"]),
        )
        payload = {
            "schema_version": schema_version,
            "tier": tier_key,
            "difficulty_rank": diff_rank,
            "item_count": len(items),
            "items": items,
        }
        target_path = words_dir / filename
        target_path.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        manifest_tiers[tier_key] = {
            "file": f"words/{filename}",
            "difficulty_rank": diff_rank,
            "item_count": len(items),
            "bytes": target_path.stat().st_size,
        }

    manifest = {
        "schema_version": schema_version,
        "total_words": sum(v["item_count"] for v in manifest_tiers.values()),
        "tiers": manifest_tiers,
    }
    (out_dir / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(description="Step 4: Export 5-Tier Split JSON files & manifest.json")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()

    stats_doc = json.loads(args.input.read_text(encoding="utf-8"))
    export_tiered_json(stats_doc.get("words", []), args.out_dir)


if __name__ == "__main__":
    main()
