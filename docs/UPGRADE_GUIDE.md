# docs/UPGRADE_GUIDE.md — 10개년 수능·모의고사 5-Tier 어휘·문법 분석 엔진 및 학습 앱 업그레이드 기술 가이드

## 1. 업그레이드 목표 및 전체 아키텍처

본 업그레이드는 기존 Android(Kotlin · Jetpack Compose) 학습 앱에 최근 10개년(2017~2026년 시행, 2018~2027학년도 수능 및 고1·고2·고3 학력평가·평가원 모의평가 총 150회, 추정 약 92만 2,500 토큰·5만 3,600 문장)의 전수 분석 체계를 통합하여 다음 4가지 핵심 역량을 탑재한다.

1. **난이도(Tier)별 어휘 JSON 분리 저장 (`Tier S ~ Tier D`)**:
   전수 표제어를 누적 빈도, Gries 분산도($DP_{\text{norm}}$ 및 균등도 $1 - DP_{\text{norm}}$), 수능 재출현 주기 및 미출현 경과(`overdue_yr`), 모평→수능 연계 적중, 2022 개정 교육과정 3,000 어휘군, 각주(`*`) 여부에 따라 5개 난이도 티어별 JSON 파일로 분리 저장한다.
2. **정규화 3테이블 SQLite + External-Content FTS5 고속 검색 및 지연 용례 확장**:
   안드로이드 표준 `SQLiteOpenHelper`와 External-Content `FTS5(tokenize='porter unicode61')`로 문장 원문 중복을 제거하고, 단어 카드 확장 버튼 클릭 시에만 15건 단위 페이징으로 역대 용례·변화형·문맥 빈칸(Cloze) 학습을 즉시 로드한다.
3. **6대 핵심 영문법 기출 패턴 자동 선별 및 양방향 연동**:
   10개년 평가원 어법 출제 비중(관계사·접속사 36.7%, 동사·준동사 30.0%, 병렬구조 10.0%, 대명사·대동사 10.0%, 능동·수동태 6.7%, 수일치 6.7%)을 UD 구문 규칙으로 태깅하고, 어휘 카드 ↔ 문법 훈련 화면 간 양방향 점프와 단어 통계 팝오버를 지원한다.
4. **코틀린 네이티브 FSRS 망각곡선 스케줄러 + 수능 D-Day 3단계 가드레일**:
   기존 SM-2 기록(`intervalDays`, `easeFactor`)과 무손실 호환되는 FSRS 안정성($S$)·난이도($D$)·기억률($R$) 스케줄러를 적용하고, D-Day 잔여 일수에 따라 구축기(>40일)·정리유지기(16~40일)·락다운기(≤15일, 신규 투입 0개 차단)를 자동 제어한다.

---

## 2. 저작권 안전 배포 가드레일 (필수)

2024년 7월 대법원 판결(`대법원 2024. 7. 11. 선고 2021다272001`) 및 평가원·EBSi 이용약관에 따라 **어휘 통계 메타데이터**와 **저작권이 있는 기출 지문 원문**의 배포 경로를 반드시 분리한다.

- **공개 저장소 및 앱 기본 번들 허용 범위**:
  - `pipeline/src/*` 파싱·분석·빌드 코드 및 테스트
  - `app/src/main/assets/corpus/manifest.json` 및 `words/tier_*.json` 내 정량 통계 수치(빈도·균등도·재출현 주기·난이도 티어·출처 메타데이터)
  - 앱 자체 창작·패러프레이징 학습 예문 및 6대 문법 훈련 문장(`grammar/golden_sentences.json`)
- **`.gitignore` 격리 및 개인 로컬 BYOD 전용 대상**:
  - `pipeline/raw/*` (원본 PDF/HWP/HWPX/듣기 대본)
  - `data/corpus/sentences.json`, `data/corpus/occurrences.json`, `data/db/csat_vocab_v1.sqlite(.gz)`
  - 앱은 무권한 Android SAF(`ActivityResultContracts.OpenDocument`)로 사용자가 로컬에서 직접 빌드한 `.sqlite`, `.sqlite.gz`, `.json` 파일을 앱 내부 전용 저장소에 안전하게 불러와 결합하는 **하이브리드 BYOD 모드**를 제공한다.

---

## 3. 5단계 난이도 티어 분류 매트릭스

| 출력 파일 | 티어 코드 | 정량 분류 기준 (10개년 150회 / 수능 10회 기준) | 앱 기본 동작 |
|---|---|---|---|
| `tier_s_core.json` | `TIER_S_CORE` | 균등도($1 - DP_{\text{norm}}$) $\ge 0.70$, 또는 교육과정 1·2그룹 중 수능 7개년 이상 & 평균 주기 $\le 1.5$년 | 최우선 필수 암기 트랙 |
| `tier_a_cyclical.json` | `TIER_A_CYCLICAL` | 균등도 $0.40 \sim 0.69$, 수능 3~6개년 출현, 또는 모평→수능 전이 적중 $\ge 2$회(OOV 제외) | 1등급 안정 핵심 트랙 |
| `tier_b_advanced_new.json` | `TIER_B_ADVANCED_NEW` | 교육과정 목록 외(OOV) 중 각주 미제공 & 균등도 $\ge 0.15$, 또는 최근 3개년 신출·Keyness $\ge 6.63$ | 고난도 빈칸·변별 트랙 |
| `tier_c_bursty.json` | `TIER_C_BURSTY` | 균등도 $< 0.15$ (특정 1~2개 지문 편중어), 각주 미제공 | 문맥 추론 보조 트랙 |
| `tier_d_footnote.json` | `TIER_D_FOOTNOTE` | 시험지 하단 별표(`*`) 각주 제공 전문어 또는 고유명사·단위·약어 예외 항목 | 기본 암기 덱 자동 숨김 |

---

## 4. 정규화 3테이블 SQLite + External-Content FTS5 스키마

```sql
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
```

### FTS5 검색 안정화 규칙

- 모든 사용자 입력은 특수문자(`"`, `'`, `*`, `(`, `)`, `:`, `^`, `~`, `-`)를 공백 치환 후 `"token"*` 접두사 `AND` 조합으로 정제(`sanitizeFtsQuery`)한다.
- UI 검색창에는 `120ms` 디바운스 및 이전 쿼리 취소를 적용하고, SQL 쿼리는 100% 바인딩 파라미터(`?`)만 사용한다.

---

## 5. 코틀린 네이티브 FSRS + D-Day 3단계 가드레일 정책

| 단계 | D-Day 잔여 일수 | 일일 신규 단어 상한 | 목표 기억 유지율 ($R$) | 허용 티어 |
|---|---|---|---|---|
| `PHASE_1_BUILD` (구축기) | `> 40일` | `25개/일` | `0.90` | Tier S · A · B |
| `PHASE_2_CONSOLIDATE` (정리유지기) | `16일 ~ 40일` | `5개/일` | `0.92` | Tier S · A |
| `PHASE_3_LOCKDOWN` (락다운기) | `≤ 15일` | **`0개/일` (신규 전면 차단)** | `0.95` | Tier S · A (기존 오답·핵심 문법 집중) |

- 기존 SM-2 기록(`intervalDays`, `easeFactor`)만 보유한 단어는 최초 FSRS 복습 시 안정성 $S = \max(1.0, \text{intervalDays})$, 난이도 $D = \text{clamp}(11.0 - 2.4 \cdot \text{easeFactor}, 1.0, 10.0)$으로 무손실 전환된다.

---

## 6. 오프라인 파이썬 파이프라인 실행 및 QA 검증 방법

```bash
# 1. QA 단위 테스트 실행 (표준 라이브러리만으로 즉시 검증 가능)
python3 -m unittest discover -s pipeline/tests -v

# 2. 전체 파이프라인 실행 (로컬 raw/ 원천 확보 후)
python3 pipeline/src/01_parse_exams.py --catalog pipeline/config/exam_catalog_10yr.json --raw-dir pipeline/raw --out data/parsed_exams.json
python3 pipeline/src/02_nlp_annotate.py --input data/parsed_exams.json --vocab pipeline/config/kr_2022_vocab_3000.json --out data/annotated_corpus.json
python3 pipeline/src/03_compute_stats.py --input data/annotated_corpus.json --out data/word_stats.json
python3 pipeline/src/04_export_tiered_json.py --input data/word_stats.json --out-dir data
python3 pipeline/src/05_build_sqlite_fts5.py --data-dir data --out data/db/csat_vocab_v1.sqlite.gz
```

생성된 `csat_vocab_v1.sqlite.gz` 또는 `tier_*.json`은 앱 내 **`단어암기 → 분석·문법 → 로컬 코퍼스 불러오기`** 버튼(Android SAF)으로 즉시 마운트하여 개인 학습용으로 활용할 수 있다.
