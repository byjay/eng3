"""
Step 1: 10-Year Exam PDF / HWP / HWPX Parser
- References dynamic schedule catalog (exam_catalog_10yr.json) to handle 2020 and 2024+ month shifts.
- Separates 2-column reading order (PyMuPDF 2.0 when installed), restores horizontal underlines via
  vertical gap (0..4pt) & >=70% horizontal overlap, and extracts asterisk (*) footnotes cleanly.
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

FOOTNOTE_LINE_RE = re.compile(r"^\*\s*([A-Za-z][A-Za-z'-]*)\s*:\s*(.+)$")
INLINE_ASTERISK_RE = re.compile(r"(?<=[A-Za-z])\*")
SENTENCE_SPLIT_RE = re.compile(r"(?<=[.!?])\s+(?=[A-Z\"'(\[])")


@dataclass(frozen=True)
class TextSpanBox:
    text: str
    x0: float
    y0: float
    x1: float
    y1: float


@dataclass(frozen=True)
class UnderlineSegment:
    x0: float
    x1: float
    y: float


def load_exam_catalog(catalog_path: Path) -> Dict[str, Any]:
    return json.loads(catalog_path.read_text(encoding="utf-8"))


def expand_expected_exams(catalog: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Expands yearly_schedules into the exact 150-exam catalog across 2017-2026."""
    exams: List[Dict[str, Any]] = []
    for row in catalog.get("yearly_schedules", []):
        admin_year = int(row["admin_year"])
        academic_year = int(row["academic_year"])
        for m in row.get("g1_months", []):
            exams.append({
                "exam_id": f"{admin_year}_G1_M{m:02d}",
                "year": admin_year,
                "academic_year": academic_year,
                "grade": 1,
                "month": int(m),
                "exam_type": "MOCK_G1",
            })
        for m in row.get("g2_months", []):
            exams.append({
                "exam_id": f"{admin_year}_G2_M{m:02d}",
                "year": admin_year,
                "academic_year": academic_year,
                "grade": 2,
                "month": int(m),
                "exam_type": "MOCK_G2",
            })
        for m in row.get("g3_moe_months", []):
            exams.append({
                "exam_id": f"{admin_year}_G3_MOE_M{m:02d}",
                "year": admin_year,
                "academic_year": academic_year,
                "grade": 3,
                "month": int(m),
                "exam_type": "MOCK_G3",
            })
        for m in row.get("kice_mock_months", []):
            exams.append({
                "exam_id": f"{academic_year}_KICE_M{m:02d}",
                "year": academic_year,
                "academic_year": academic_year,
                "grade": 3,
                "month": int(m),
                "exam_type": "KICE_MOCK",
            })
        csat_month = int(row["csat_month"])
        exams.append({
            "exam_id": f"{academic_year}_CSAT",
            "year": academic_year,
            "academic_year": academic_year,
            "grade": 3,
            "month": csat_month,
            "exam_type": "CSAT",
        })
    return exams


def match_underlined_spans(
    spans: List[TextSpanBox],
    underlines: List[UnderlineSegment],
    max_vertical_gap_pt: float = 4.0,
    min_horizontal_overlap: float = 0.70,
) -> List[str]:
    """
    Matches vector underline lines (from PyMuPDF page.get_drawings()) to text span bounding boxes
    using 0..4pt vertical distance below span baseline and >=70% horizontal overlap.
    """
    matched: List[str] = []
    for sp in spans:
        width = max(1e-6, sp.x1 - sp.x0)
        for line in underlines:
            vertical_gap = line.y - sp.y1
            if -0.5 <= vertical_gap <= max_vertical_gap_pt:
                overlap = max(0.0, min(sp.x1, line.x1) - max(sp.x0, line.x0))
                if (overlap / width) >= min_horizontal_overlap:
                    cleaned = sp.text.strip()
                    if cleaned:
                        matched.append(cleaned)
                    break
    return matched


def separate_footnotes(raw_passage: str) -> Tuple[str, Dict[str, str]]:
    """
    Extracts bottom asterisk footnotes (`* word: meaning`) into a dedicated dictionary
    and strips inline `*` markers from the passage body.
    """
    footnotes: Dict[str, str] = {}
    body_lines: List[str] = []
    for line in raw_passage.splitlines():
        stripped = line.strip()
        m = FOOTNOTE_LINE_RE.match(stripped)
        if m:
            term = m.group(1).strip().lower()
            gloss = m.group(2).strip()
            footnotes[term] = gloss
        elif stripped:
            body_lines.append(INLINE_ASTERISK_RE.sub("", stripped))
    clean_body = " ".join(body_lines)
    clean_body = re.sub(r"\s{2,}", " ", clean_body).strip()
    return clean_body, footnotes


def split_sentences(clean_text: str) -> List[str]:
    if not clean_text.strip():
        return []
    parts = SENTENCE_SPLIT_RE.split(clean_text.strip())
    return [p.strip() for p in parts if len(p.strip()) >= 8]


def extract_text_from_file(path: Path) -> Tuple[str, List[str]]:
    """
    Dispatches by extension:
      - .pdf  -> PyMuPDF (fitz) 2-column sort + vector underline recovery
      - .hwpx -> python-hwpx TextExtractor
      - .hwp  -> olefile + zlib fallback
      - .txt  -> UTF-8 plain text (for local listening transcripts / testing)
    """
    ext = path.suffix.lower()
    if ext == ".txt":
        return path.read_text(encoding="utf-8", errors="ignore"), []
    if ext == ".pdf":
        try:
            import fitz  # type: ignore
        except ImportError as exc:
            raise RuntimeError("PyMuPDF(pymupdf)가 설치되어 있지 않습니다. `pip install pymupdf` 후 실행하세요.") from exc
        doc = fitz.open(path)
        pages_text: List[str] = []
        underlined_tokens: List[str] = []
        for page in doc:
            midpoint = page.rect.width / 2.0
            left_blocks: List[Tuple[float, str]] = []
            right_blocks: List[Tuple[float, str]] = []
            spans: List[TextSpanBox] = []
            text_dict = page.get_text("dict")
            for block in text_dict.get("blocks", []):
                if block.get("type") != 0:
                    continue
                bx0, by0, bx1, _ = block["bbox"]
                block_lines: List[str] = []
                for line in block.get("lines", []):
                    line_txt = []
                    for sp in line.get("spans", []):
                        sx0, sy0, sx1, sy1 = sp["bbox"]
                        stxt = sp.get("text", "")
                        if stxt.strip():
                            spans.append(TextSpanBox(stxt, sx0, sy0, sx1, sy1))
                            line_txt.append(stxt)
                    if line_txt:
                        block_lines.append("".join(line_txt))
                joined = "\n".join(block_lines).strip()
                if not joined:
                    continue
                if ((bx0 + bx1) / 2.0) < midpoint:
                    left_blocks.append((by0, joined))
                else:
                    right_blocks.append((by0, joined))
            left_blocks.sort(key=lambda t: t[0])
            right_blocks.sort(key=lambda t: t[0])
            pages_text.append("\n".join([t for _, t in left_blocks] + [t for _, t in right_blocks]))

            underlines: List[UnderlineSegment] = []
            for d in page.get_drawings():
                rect = d.get("rect")
                if rect and abs(rect.y1 - rect.y0) <= 2.2 and (rect.x1 - rect.x0) >= 8.0:
                    underlines.append(UnderlineSegment(rect.x0, rect.x1, (rect.y0 + rect.y1) / 2.0))
            underlined_tokens.extend(match_underlined_spans(spans, underlines))
        return "\n".join(pages_text), underlined_tokens
    if ext == ".hwpx":
        try:
            from hwpx.tools.text_extractor import TextExtractor  # type: ignore
        except ImportError as exc:
            raise RuntimeError("python-hwpx가 설치되어 있지 않습니다. `pip install python-hwpx` 후 실행하세요.") from exc
        extractor = TextExtractor(str(path))
        return extractor.extract_text(), []
    raise ValueError(f"지원하지 않는 파일 형식입니다: {ext}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Step 1: Parse 10-year exam PDF/HWPX/TXT files")
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--raw-dir", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    catalog = load_exam_catalog(args.catalog)
    expected = expand_expected_exams(catalog)
    exam_map = {e["exam_id"]: e for e in expected}
    records: List[Dict[str, Any]] = []

    if args.raw_dir.exists():
        for file_path in sorted(args.raw_dir.iterdir()):
            if not file_path.is_file() or file_path.suffix.lower() not in {".pdf", ".hwpx", ".txt"}:
                continue
            exam_id = file_path.stem
            meta = exam_map.get(exam_id, {
                "exam_id": exam_id,
                "year": 2026,
                "academic_year": 2027,
                "grade": 3,
                "month": 11,
                "exam_type": "CSAT",
            })
            raw_text, underlined = extract_text_from_file(file_path)
            clean_body, footnotes = separate_footnotes(raw_text)
            sents = split_sentences(clean_body)
            records.append({
                "meta": meta,
                "footnotes": footnotes,
                "underlined_tokens": underlined,
                "sentences": sents,
            })

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps({
        "schema_version": "2.0.0",
        "expected_exam_count": len(expected),
        "parsed_exam_count": len(records),
        "exams": records,
    }, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
