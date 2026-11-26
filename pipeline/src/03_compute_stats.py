"""
Step 3: Quantitative Corpus Statistics Engine
- Gries DP, DP_norm, and Uniformity (1 - DP_norm) handling variable corpus part sizes.
- CSAT recurrence metrics: mean_gap_yr, median_gap_yr, max_gap_yr, last_csat_year, overdue_yr.
- Mock -> CSAT same-year carryover transfer hits, listening ratio, recent 3-year emergent flag,
  and log-likelihood keyness score.
"""
from __future__ import annotations

import argparse
import json
import math
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Dict, List


def compute_gries_dp(counts_per_part: List[int], tokens_per_part: List[int]) -> Dict[str, float]:
    """
    Computes Gries's Deviation of Proportions (DP), normalized DP_norm, and Uniformity (1 - DP_norm).
    Pure Python implementation with exact mathematical parity to numpy version.
    """
    if not counts_per_part or len(counts_per_part) != len(tokens_per_part):
        return {"gries_dp": 1.0, "gries_dp_norm": 1.0, "uniformity": 0.0}
    total_af = float(sum(counts_per_part))
    total_tok = float(sum(tokens_per_part))
    if total_af <= 0.0 or total_tok <= 0.0:
        return {"gries_dp": 1.0, "gries_dp_norm": 1.0, "uniformity": 0.0}

    shares = [t / total_tok for t in tokens_per_part]
    observed = [c / total_af for c in counts_per_part]
    dp = 0.5 * sum(abs(v - s) for v, s in zip(observed, shares))
    min_share = min(shares)
    denom = max(1e-9, 1.0 - min_share)
    dp_norm = min(1.0, max(0.0, dp / denom))
    uniformity = max(0.0, min(1.0, 1.0 - dp_norm))
    return {
        "gries_dp": round(dp, 4),
        "gries_dp_norm": round(dp_norm, 4),
        "uniformity": round(uniformity, 4),
    }


def compute_recurrence(csat_years: List[int], latest_year: int = 2027) -> Dict[str, Any]:
    yrs = sorted(set(int(y) for y in csat_years))
    if not yrs:
        return {
            "mean_gap_yr": None,
            "median_gap_yr": None,
            "max_gap_yr": None,
            "last_csat_year": None,
            "overdue_yr": None,
        }
    last_yr = yrs[-1]
    overdue = max(0, latest_year - last_yr)
    if len(yrs) < 2:
        return {
            "mean_gap_yr": None,
            "median_gap_yr": None,
            "max_gap_yr": None,
            "last_csat_year": last_yr,
            "overdue_yr": overdue,
        }
    gaps = [yrs[i + 1] - yrs[i] for i in range(len(yrs) - 1)]
    sorted_gaps = sorted(gaps)
    mid = len(sorted_gaps) // 2
    if len(sorted_gaps) % 2 == 1:
        median_gap = float(sorted_gaps[mid])
    else:
        median_gap = (sorted_gaps[mid - 1] + sorted_gaps[mid]) / 2.0
    return {
        "mean_gap_yr": round(sum(gaps) / len(gaps), 2),
        "median_gap_yr": round(median_gap, 2),
        "max_gap_yr": int(max(gaps)),
        "last_csat_year": last_yr,
        "overdue_yr": overdue,
    }


def compute_log_likelihood_keyness(
    recent_freq: int, recent_total: int, prior_freq: int, prior_total: int
) -> float:
    """Dunning log-likelihood G2 for recent 3-year surge detection (>= 6.63 corresponds to p < 0.01)."""
    if recent_total <= 0 or prior_total <= 0 or (recent_freq + prior_freq) <= 0:
        return 0.0
    total = float(recent_total + prior_total)
    e1 = recent_total * (recent_freq + prior_freq) / total
    e2 = prior_total * (recent_freq + prior_freq) / total
    g2 = 0.0
    if recent_freq > 0 and e1 > 0:
        g2 += 2.0 * recent_freq * math.log(recent_freq / e1)
    if prior_freq > 0 and e2 > 0:
        g2 += 2.0 * prior_freq * math.log(prior_freq / e2)
    return round(g2 if (recent_freq / max(1, recent_total)) >= (prior_freq / max(1, prior_total)) else 0.0, 2)


def compute_corpus_word_stats(annotated: Dict[str, Any], latest_year: int = 2027) -> List[Dict[str, Any]]:
    occurrences = annotated.get("occurrences", [])
    exam_tokens: Counter[str] = Counter(o["exam_id"] for o in occurrences)
    all_exam_ids = sorted(exam_tokens.keys())
    tokens_per_part = [exam_tokens[eid] for eid in all_exam_ids]

    by_lemma: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
    recent_total = 0
    prior_total = 0
    for occ in occurrences:
        by_lemma[occ["lemma"]].append(occ)
        if int(occ["year"]) >= (latest_year - 2):
            recent_total += 1
        else:
            prior_total += 1

    words_out: List[Dict[str, Any]] = []
    for idx, lemma in enumerate(sorted(by_lemma.keys()), start=1):
        occs = by_lemma[lemma]
        per_exam = Counter(o["exam_id"] for o in occs)
        counts_per_part = [per_exam.get(eid, 0) for eid in all_exam_ids]
        dispersion = compute_gries_dp(counts_per_part, tokens_per_part)

        csat_years = sorted({int(o["year"]) for o in occs if o["exam_type"] == "CSAT"})
        mock_years = {int(o["year"]) for o in occs if o["exam_type"] in {"KICE_MOCK", "MOCK_G3"}}
        transfer_hits = len(set(csat_years) & mock_years)
        recurrence = compute_recurrence(csat_years, latest_year=latest_year)

        listening_freq = sum(1 for o in occs if o.get("section") == "listening")
        total_freq = len(occs)
        reading_freq = total_freq - listening_freq
        listening_ratio = round(listening_freq / max(1, total_freq), 3)

        all_years = sorted({int(o["year"]) for o in occs})
        first_year = all_years[0] if all_years else latest_year
        recent_freq = sum(1 for o in occs if int(o["year"]) >= (latest_year - 2))
        prior_freq = total_freq - recent_freq
        keyness = compute_log_likelihood_keyness(recent_freq, recent_total, prior_freq, prior_total)

        surface_counts = Counter(o["surface_form"].lower() for o in occs)
        pos_counts = Counter(o.get("pos", "NOUN") for o in occs)
        kr_group = occs[0].get("kr_2022_group", "OOV")
        is_awl = any(o.get("is_awl", False) for o in occs)
        is_footnote = any(o.get("is_footnote", False) for o in occs)
        is_excluded = any(o.get("is_excluded_entity", False) for o in occs)

        mean_gap = recurrence["mean_gap_yr"]
        overdue_yr = recurrence["overdue_yr"]
        is_overdue = bool(mean_gap is not None and overdue_yr is not None and overdue_yr >= mean_gap)

        words_out.append({
            "word_id": f"w_{idx:06d}",
            "lemma": lemma,
            "word_family_head": lemma,
            "flags": {
                "is_polysemous": len(pos_counts) >= 2,
                "is_listening_heavy": listening_ratio >= 0.45,
                "is_new_recent_3yr": first_year >= (latest_year - 2),
                "is_overdue": is_overdue,
                "is_footnote_ever": is_footnote,
                "is_excluded_entity": is_excluded,
            },
            "curriculum": {
                "kr_2022_group": kr_group,
                "cefr": "A2" if kr_group == "G1_ELEM_800" else ("B1" if kr_group == "G2_MID_H1_1200" else ("B2" if kr_group == "G3_H2_H3_1000" else "C1")),
                "is_awl": is_awl,
            },
            "pos_distribution": dict(pos_counts),
            "surface_variants": [{"form": form, "count": cnt} for form, cnt in surface_counts.most_common(8)],
            "stats": {
                "total_freq": total_freq,
                "reading_freq": reading_freq,
                "listening_freq": listening_freq,
                "listening_ratio": listening_ratio,
                "exam_range_150": len(per_exam),
                "csat_years": csat_years,
                "recurrence": recurrence,
                "dispersion": dispersion,
                "mock_to_csat_transfer_hits": transfer_hits,
                "first_appearance_year": first_year,
                "keyness_recent_3yr": keyness,
            },
            "top_sentence_ids": list(dict.fromkeys(o["sent_id"] for o in occs))[:5],
        })
    return words_out


def main() -> None:
    parser = argparse.ArgumentParser(description="Step 3: Compute Gries DP_norm, Recurrence, Transfer & Keyness")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    annotated = json.loads(args.input.read_text(encoding="utf-8"))
    words = compute_corpus_word_stats(annotated)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps({
        "schema_version": "2.0.0",
        "word_count": len(words),
        "words": words,
    }, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
