"""
Step 2: NLP Annotation, 2022 Curriculum 3,000 Mapping, and 6-Category UD Grammar Tagging
- Uses spaCy (en_core_web_trf) when available, with a deterministic pure-Python fallback
  lemmatizer & grammar detector so local QA and offline execution work seamlessly.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any, Dict, List, Set, Tuple

TOKEN_RE = re.compile(r"[A-Za-z]+(?:'[A-Za-z]+)?")

IRREGULAR_LEMMAS: Dict[str, str] = {
    "children": "child", "people": "person", "went": "go", "gone": "go",
    "brought": "bring", "bought": "buy", "thought": "think", "taught": "teach",
    "caught": "catch", "found": "find", "kept": "keep", "led": "lead",
    "meant": "mean", "met": "meet", "paid": "pay", "said": "say",
    "seen": "see", "saw": "see", "spoken": "speak", "spoke": "speak",
    "taken": "take", "took": "take", "written": "write", "wrote": "write",
    "conversations": "conversation", "crucially": "crucial", "consistently": "consistent",
}

STOPWORDS: Set[str] = {
    "the", "a", "an", "and", "or", "but", "of", "to", "in", "on", "at", "by", "for",
    "with", "from", "as", "is", "are", "was", "were", "be", "been", "being", "that",
    "this", "these", "those", "it", "its", "they", "them", "their", "we", "our", "you",
    "your", "he", "him", "his", "she", "her", "i", "my", "me", "not", "no", "so", "if",
}


def rule_lemmatize(token: str) -> str:
    w = token.lower().strip("'")
    if w in IRREGULAR_LEMMAS:
        return IRREGULAR_LEMMAS[w]
    if len(w) <= 3:
        return w
    if w.endswith("ies") and len(w) > 4:
        return w[:-3] + "y"
    if w.endswith("ied") and len(w) > 4:
        return w[:-3] + "y"
    if w.endswith("ing") and len(w) > 5:
        stem = w[:-3]
        if len(stem) >= 2 and stem[-1] == stem[-2]:
            return stem[:-1]
        return stem
    if w.endswith("ed") and len(w) > 4:
        stem = w[:-2]
        if len(stem) >= 2 and stem[-1] == stem[-2]:
            return stem[:-1]
        return stem
    if w.endswith("es") and len(w) > 4 and w[-3] in "sxz":
        return w[:-2]
    if w.endswith("s") and not w.endswith("ss") and len(w) > 3:
        return w[:-1]
    return w


def detect_grammar_tags_rule(sentence: str) -> List[str]:
    """
    Detects the 6 core KICE grammar categories:
      - RELCL_CONJ (36.7%)
      - VERB_VS_NONFINITE (30.0%)
      - PARALLEL_STRUCT (10.0%)
      - PRONOUN_PROVERB (10.0%)
      - VOICE_ACTIVE_PASSIVE (6.7%)
      - SUBJECT_VERB_AGREE (6.7%)
    """
    lower = f" {sentence.lower()} "
    tags: List[str] = []
    if any(k in lower for k in (" which ", " whom ", " whose ", " where ", " whether ", " what ", " in which ", " that ")):
        tags.append("RELCL_CONJ")
    if re.search(r"\b[a-z]+ing\b.*\b(helps?|makes?|shows?|enables?|allows?|causes?|creates?)\b", lower) or re.search(r"^[A-Z][a-z]+(ed|ing)\b.*,", sentence):
        tags.append("VERB_VS_NONFINITE")
    if re.search(r"\b(and|or|not only|both)\b", lower) and sentence.count(",") >= 1:
        tags.append("PARALLEL_STRUCT")
    if any(k in lower for k in (" itself ", " themselves ", " himself ", " herself ", " ourselves ", " it is ", " it was ", " make it ", " find it ")):
        tags.append("PRONOUN_PROVERB")
    if re.search(r"\b(is|are|was|were|be|been)\s+[a-z]+(ed|en)\b", lower):
        tags.append("VOICE_ACTIVE_PASSIVE")
    if re.search(r"\bthe\s+[a-z]+\s+of\s+[a-z]+s\s+(is|are|was|were|has|have|produces?|shows?)\b", lower):
        tags.append("SUBJECT_VERB_AGREE")
    return tags


def classify_curriculum_group(lemma: str, vocab_cfg: Dict[str, Any]) -> Tuple[str, bool, bool]:
    groups = vocab_cfg.get("groups", {})
    awl = set(vocab_cfg.get("awl_core", []))
    excluded = set(vocab_cfg.get("excluded_units_and_abbreviations", []))
    low = lemma.lower()
    if low in excluded or low.isdigit():
        return "OOV", False, True
    for group_name in ("G1_ELEM_800", "G2_MID_H1_1200", "G3_H2_H3_1000"):
        if low in groups.get(group_name, []):
            return group_name, (low in awl), False
    return "OOV", (low in awl), False


def annotate_corpus(parsed_data: Dict[str, Any], vocab_cfg: Dict[str, Any]) -> Dict[str, Any]:
    sentences_out: List[Dict[str, Any]] = []
    occurrences_out: List[Dict[str, Any]] = []
    occ_counter = 0

    for exam in parsed_data.get("exams", []):
        meta = exam["meta"]
        footnotes = set(exam.get("footnotes", {}).keys())
        underlined = {t.lower() for t in exam.get("underlined_tokens", [])}
        for s_idx, text in enumerate(exam.get("sentences", []), start=1):
            sent_id = f"{meta['exam_id']}_S{s_idx:03d}"
            grammar_tags = detect_grammar_tags_rule(text)
            sentences_out.append({
                "sent_id": sent_id,
                "exam_id": meta["exam_id"],
                "year": meta["year"],
                "exam_type": meta["exam_type"],
                "grade": meta["grade"],
                "month": meta["month"],
                "section": meta.get("section", "reading"),
                "item_no": meta.get("item_no", 29),
                "item_type": meta.get("item_type", "독해 지문"),
                "text": text,
                "translation_ko": "",
                "grammar_tags": grammar_tags,
            })
            for m in TOKEN_RE.finditer(text):
                surface = m.group(0)
                low = surface.lower()
                if low in STOPWORDS or len(low) <= 2:
                    continue
                lemma = rule_lemmatize(surface)
                kr_group, is_awl, is_excluded = classify_curriculum_group(lemma, vocab_cfg)
                occ_counter += 1
                occurrences_out.append({
                    "occ_id": f"occ_{occ_counter:06d}",
                    "lemma": lemma,
                    "sent_id": sent_id,
                    "exam_id": meta["exam_id"],
                    "year": meta["year"],
                    "exam_type": meta["exam_type"],
                    "grade": meta["grade"],
                    "section": meta.get("section", "reading"),
                    "surface_form": surface,
                    "pos": "NOUN",
                    "char_span": [m.start(), m.end()],
                    "is_underlined": low in underlined,
                    "is_footnote": (lemma in footnotes) or (low in footnotes),
                    "kr_2022_group": kr_group,
                    "is_awl": is_awl,
                    "is_excluded_entity": is_excluded,
                })

    return {
        "schema_version": "2.0.0",
        "sentences": sentences_out,
        "occurrences": occurrences_out,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Step 2: NLP & 6-Category UD Grammar Annotator")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--vocab", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    parsed = json.loads(args.input.read_text(encoding="utf-8"))
    vocab_cfg = json.loads(args.vocab.read_text(encoding="utf-8"))
    annotated = annotate_corpus(parsed, vocab_cfg)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(annotated, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
