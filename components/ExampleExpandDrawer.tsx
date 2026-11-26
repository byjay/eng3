import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';

/**
 * Production-grade TypeScript / React Reference Implementation of `ExampleExpandDrawer`.
 *
 * Capabilities:
 * - Lazy pagination of 15 sentences at a time (`LIMIT 15 OFFSET ?`) from any SQLite data source adapter
 * - Race-safe request generation sequencing to prevent stale filter responses overwriting latest UI
 * - Cloze (blank-fill) mode toggle with per-sentence interactive reveal
 * - Multi-role `grammar_spans` color underline + soft tint highlighting + interactive legend toggle
 * - Conflict-free atomic interval partitioning (Cloze + overlapping grammar spans never corrupt DOM)
 * - Zero `dangerouslySetInnerHTML`: renders strictly typed React elements with accessible ARIA roles
 */

export type GrammarSpanRole =
  | 'SUBJECT_CHUNK'
  | 'MAIN_VERB'
  | 'RELATIVE_CLAUSE'
  | 'PARTICIPLE_PHRASE'
  | 'PARALLEL_UNIT'
  | 'PREPOSITIONAL_MODIFIER';

export type ExamTypeCode = 'CSAT' | 'KICE_MOCK' | 'MOCK_G3' | 'MOCK_G2' | 'MOCK_G1';
export type ExamSectionCode = 'reading' | 'listening';

export interface GrammarSpan {
  role: GrammarSpanRole;
  char_span: [number, number];
}

export interface OccurrenceRow {
  occ_id: string;
  sent_id: string;
  year: number;
  month: number;
  exam_type: ExamTypeCode;
  section: ExamSectionCode;
  item_no: number;
  item_type: string;
  surface_form: string;
  text: string;
  translation_ko?: string;
  span_start: number;
  span_end: number;
  is_underlined?: boolean;
  grammar_tags: string[];
  grammar_spans: GrammarSpan[];
}

export interface SurfaceVariantStat {
  form: string;
  count: number;
}

export interface ExampleExpandQueryParams {
  wordId: string;
  lemma: string;
  examType?: ExamTypeCode | null;
  section?: ExamSectionCode | null;
  surfaceForm?: string | null;
  onlyGrammarCore?: boolean;
  limit: number;
  offset: number;
}

export interface ExampleExpandPageResult {
  items: OccurrenceRow[];
  totalCount: number;
  hasMore: boolean;
  sourceLabel?: string;
}

/**
 * Pluggable SQLite query adapter (e.g., sql.js / OPFS SQLite Worker / expo-sqlite / Better-SQLite3 IPC).
 * Implementations MUST bind all query parameters (`LIMIT ? OFFSET ?`) without string interpolation.
 */
export interface SqliteExampleDataSource {
  queryOccurrencesPaged(params: ExampleExpandQueryParams): Promise<ExampleExpandPageResult>;
}

export interface ExampleExpandDrawerProps {
  wordId: string;
  lemma: string;
  meaningKo?: string;
  surfaceVariants?: SurfaceVariantStat[];
  isOpen: boolean;
  dataSource: SqliteExampleDataSource;
  pageSize?: number;
  onClose?: () => void;
  onSpeakSentence?: (sentenceEn: string) => void;
  onJumpToGrammarTag?: (grammarTag: string) => void;
}

export interface RenderedTextSegment {
  start: number;
  end: number;
  text: string;
  primaryRole: GrammarSpanRole | null;
  allRoles: GrammarSpanRole[];
  isTargetToken: boolean;
  isClozeMasked: boolean;
  isClozeRevealed: boolean;
}

interface RoleVisualTheme {
  labelKo: string;
  underlineColor: string;
  backgroundColor: string;
  textColor: string;
  priority: number;
}

export const GRAMMAR_ROLE_THEMES: Record<GrammarSpanRole, RoleVisualTheme> = {
  SUBJECT_CHUNK: {
    labelKo: '주어구',
    underlineColor: '#245CE5',
    backgroundColor: 'rgba(36, 92, 229, 0.10)',
    textColor: '#1B43A8',
    priority: 2,
  },
  MAIN_VERB: {
    labelKo: '본동사',
    underlineColor: '#0E9F6E',
    backgroundColor: 'rgba(14, 159, 110, 0.12)',
    textColor: '#086F4B',
    priority: 6,
  },
  RELATIVE_CLAUSE: {
    labelKo: '관계절',
    underlineColor: '#7C3AED',
    backgroundColor: 'rgba(124, 58, 237, 0.10)',
    textColor: '#5B21B6',
    priority: 5,
  },
  PARTICIPLE_PHRASE: {
    labelKo: '분사구문',
    underlineColor: '#D97706',
    backgroundColor: 'rgba(217, 119, 6, 0.12)',
    textColor: '#92400E',
    priority: 4,
  },
  PARALLEL_UNIT: {
    labelKo: '병렬구조',
    underlineColor: '#0891B2',
    backgroundColor: 'rgba(8, 145, 178, 0.11)',
    textColor: '#155E75',
    priority: 3,
  },
  PREPOSITIONAL_MODIFIER: {
    labelKo: '전치사구',
    underlineColor: '#E11D48',
    backgroundColor: 'rgba(225, 29, 72, 0.09)',
    textColor: '#9F1239',
    priority: 1,
  },
};

const ALL_GRAMMAR_ROLES: GrammarSpanRole[] = [
  'SUBJECT_CHUNK',
  'MAIN_VERB',
  'RELATIVE_CLAUSE',
  'PARTICIPLE_PHRASE',
  'PARALLEL_UNIT',
  'PREPOSITIONAL_MODIFIER',
];

const EXAM_TYPE_LABELS: Record<ExamTypeCode, string> = {
  CSAT: '수능',
  KICE_MOCK: '평가원 모평',
  MOCK_G3: '고3 학평',
  MOCK_G2: '고2 학평',
  MOCK_G1: '고1 학평',
};

const DEFAULT_PAGE_SIZE = 15;

/**
 * Resolves a safe [start, end) character span for `surfaceForm` inside `text`
 * when explicit SQLite offsets are absent or out of range.
 */
export function resolveTargetSpan(
  text: string,
  surfaceForm: string,
  spanStart: number,
  spanEnd: number,
): [number, number] | null {
  if (!text) return null;
  if (spanStart >= 0 && spanEnd > spanStart && spanEnd <= text.length) {
    return [spanStart, spanEnd];
  }
  const cleanSurface = surfaceForm.trim();
  if (!cleanSurface) return null;
  const escaped = cleanSurface.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const wordMatch = new RegExp(`\\b${escaped}\\b`, 'i').exec(text);
  if (wordMatch) {
    return [wordMatch.index, wordMatch.index + wordMatch[0].length];
  }
  const fallbackIdx = text.toLowerCase().indexOf(cleanSurface.toLowerCase());
  if (fallbackIdx >= 0) {
    return [fallbackIdx, Math.min(text.length, fallbackIdx + cleanSurface.length)];
  }
  return null;
}

/**
 * Conflict-free Atomic Interval Partitioning:
 * Merges active `grammar_spans` and optional Cloze target range into contiguous,
 * non-overlapping text segments clamped strictly to [0, text.length].
 */
export function partitionSentenceSegments(
  text: string,
  grammarSpans: GrammarSpan[],
  activeRoles: ReadonlySet<GrammarSpanRole>,
  targetSpan: [number, number] | null,
  showGrammarSpans: boolean,
  clozeMode: boolean,
  clozeRevealed: boolean,
): RenderedTextSegment[] {
  if (!text) return [];
  const len = text.length;

  const validTarget: [number, number] | null =
    targetSpan &&
    targetSpan[0] >= 0 &&
    targetSpan[1] > targetSpan[0] &&
    targetSpan[1] <= len
      ? targetSpan
      : null;

  const filteredSpans = showGrammarSpans
    ? grammarSpans
        .filter((s) => activeRoles.has(s.role))
        .map((s) => {
          const start = Math.max(0, Math.min(len, Math.trunc(s.char_span[0])));
          const end = Math.max(0, Math.min(len, Math.trunc(s.char_span[1])));
          return { role: s.role, start, end };
        })
        .filter((s) => s.start < s.end)
    : [];

  const boundaries = new Set<number>([0, len]);
  if (validTarget) {
    boundaries.add(validTarget[0]);
    boundaries.add(validTarget[1]);
  }

  const isMasked = Boolean(validTarget && clozeMode && !clozeRevealed);
  for (const span of filteredSpans) {
    if (!isMasked || !validTarget || span.start <= validTarget[0] || span.start >= validTarget[1]) {
      boundaries.add(span.start);
    }
    if (!isMasked || !validTarget || span.end <= validTarget[0] || span.end >= validTarget[1]) {
      boundaries.add(span.end);
    }
  }

  const pts = Array.from(boundaries).sort((a, b) => a - b);
  const segments: RenderedTextSegment[] = [];

  for (let i = 0; i < pts.length - 1; i++) {
    const start = pts[i];
    const end = pts[i + 1];
    if (start >= end) continue;

    const isTarget = Boolean(validTarget && start >= validTarget[0] && end <= validTarget[1]);
    const overlappingRoles = Array.from(
      new Set(
        filteredSpans
          .filter((s) => s.start < end && s.end > start)
          .map((s) => s.role),
      ),
    ).sort((a, b) => GRAMMAR_ROLE_THEMES[b].priority - GRAMMAR_ROLE_THEMES[a].priority);

    segments.push({
      start,
      end,
      text: text.slice(start, end),
      primaryRole: overlappingRoles[0] ?? null,
      allRoles: overlappingRoles,
      isTargetToken: isTarget,
      isClozeMasked: isTarget && clozeMode && !clozeRevealed,
      isClozeRevealed: isTarget && clozeMode && clozeRevealed,
    });
  }

  return segments;
}

export const ExampleExpandDrawer: React.FC<ExampleExpandDrawerProps> = ({
  wordId,
  lemma,
  meaningKo,
  surfaceVariants = [],
  isOpen,
  dataSource,
  pageSize = DEFAULT_PAGE_SIZE,
  onClose,
  onSpeakSentence,
  onJumpToGrammarTag,
}) => {
  const safePageSize = Math.max(1, Math.min(50, pageSize));

  const [examFilter, setExamFilter] = useState<ExamTypeCode | null>(null);
  const [sectionFilter, setSectionFilter] = useState<ExamSectionCode | null>(null);
  const [surfaceFilter, setSurfaceFilter] = useState<string | null>(null);
  const [onlyGrammarCore, setOnlyGrammarCore] = useState<boolean>(false);

  const [clozeMode, setClozeMode] = useState<boolean>(false);
  const [showGrammarSpans, setShowGrammarSpans] = useState<boolean>(true);
  const [activeRoles, setActiveRoles] = useState<Set<GrammarSpanRole>>(
    () => new Set(ALL_GRAMMAR_ROLES),
  );
  const [revealedIds, setRevealedIds] = useState<Record<string, boolean>>({});

  const [items, setItems] = useState<OccurrenceRow[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [hasMore, setHasMore] = useState<boolean>(false);
  const [sourceLabel, setSourceLabel] = useState<string>('SQLite 로컬 코퍼스');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isLoadingMore, setIsLoadingMore] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const requestSeqRef = useRef<number>(0);

  const loadPage = useCallback(
    async (offset: number, append: boolean) => {
      const seq = ++requestSeqRef.current;
      if (append) {
        setIsLoadingMore(true);
      } else {
        setIsLoading(true);
      }
      setErrorMessage(null);

      try {
        const result = await dataSource.queryOccurrencesPaged({
          wordId,
          lemma,
          examType: examFilter,
          section: sectionFilter,
          surfaceForm: surfaceFilter,
          onlyGrammarCore,
          limit: safePageSize,
          offset,
        });
        if (seq !== requestSeqRef.current) return;

        setItems((prev) => (append ? [...prev, ...result.items] : result.items));
        setTotalCount(result.totalCount);
        setHasMore(result.hasMore);
        if (result.sourceLabel) {
          setSourceLabel(result.sourceLabel);
        }
      } catch (err) {
        if (seq !== requestSeqRef.current) return;
        const message = err instanceof Error ? err.message : '예문을 불러오지 못했습니다.';
        setErrorMessage(message);
      } finally {
        if (seq === requestSeqRef.current) {
          setIsLoading(false);
          setIsLoadingMore(false);
        }
      }
    },
    [dataSource, examFilter, lemma, onlyGrammarCore, safePageSize, sectionFilter, surfaceFilter, wordId],
  );

  // Reset and fetch initial 15 items whenever drawer opens or filters change
  useEffect(() => {
    if (!isOpen) return;
    setRevealedIds({});
    void loadPage(0, false);
  }, [isOpen, loadPage]);

  const handleLoadMore = useCallback(() => {
    if (isLoading || isLoadingMore || !hasMore) return;
    void loadPage(items.length, true);
  }, [hasMore, isLoading, isLoadingMore, items.length, loadPage]);

  const toggleRole = useCallback((role: GrammarSpanRole) => {
    setActiveRoles((prev) => {
      const next = new Set(prev);
      if (next.has(role)) {
        next.delete(role);
      } else {
        next.add(role);
      }
      return next;
    });
  }, []);

  const toggleRevealSentence = useCallback((occId: string) => {
    setRevealedIds((prev) => ({ ...prev, [occId]: !prev[occId] }));
  }, []);

  const activeRoleCount = useMemo(() => activeRoles.size, [activeRoles]);

  if (!isOpen) return null;

  return (
    <section
      role="region"
      aria-label={`${lemma} 예문 확장 드로어`}
      data-testid={`example-expand-drawer-${wordId}`}
      style={{
        borderRadius: 18,
        border: '1px solid #CBD5E1',
        backgroundColor: '#F8FAFC',
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
        color: '#0F172A',
      }}
    >
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
        <div>
          <div style={{ fontSize: 16, fontWeight: 800 }}>
            {lemma}
            {meaningKo ? <span style={{ marginLeft: 8, fontWeight: 600, color: '#475569', fontSize: 13 }}>{meaningKo}</span> : null}
          </div>
          <div style={{ fontSize: 12, color: '#64748B', marginTop: 2 }}>
            {sourceLabel} · 조건 일치 {totalCount}건 (페이지당 {safePageSize}건)
          </div>
        </div>
        {onClose ? (
          <button
            type="button"
            onClick={onClose}
            aria-label="예문 드로어 닫기"
            style={{
              minHeight: 40,
              minWidth: 40,
              borderRadius: 10,
              border: '1px solid #CBD5E1',
              backgroundColor: '#FFFFFF',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            닫기
          </button>
        ) : null}
      </div>

      {/* Primary Learning Mode Toggles: Cloze + grammar_spans */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        <button
          type="button"
          aria-pressed={clozeMode}
          data-testid={`toggle-cloze-${wordId}`}
          onClick={() => {
            setClozeMode((prev) => !prev);
            setRevealedIds({});
          }}
          style={{
            minHeight: 40,
            padding: '6px 14px',
            borderRadius: 999,
            border: '1px solid #6366F1',
            backgroundColor: clozeMode ? '#4F46E5' : '#EEF2FF',
            color: clozeMode ? '#FFFFFF' : '#4338CA',
            fontWeight: 800,
            fontSize: 12,
            cursor: 'pointer',
          }}
        >
          {clozeMode ? '빈칸(Cloze) ON' : '빈칸(Cloze) OFF'}
        </button>

        <button
          type="button"
          aria-pressed={showGrammarSpans}
          data-testid={`toggle-grammar-spans-${wordId}`}
          onClick={() => setShowGrammarSpans((prev) => !prev)}
          style={{
            minHeight: 40,
            padding: '6px 14px',
            borderRadius: 999,
            border: '1px solid #0E9F6E',
            backgroundColor: showGrammarSpans ? '#0E9F6E' : '#ECFDF5',
            color: showGrammarSpans ? '#FFFFFF' : '#047857',
            fontWeight: 800,
            fontSize: 12,
            cursor: 'pointer',
          }}
        >
          {showGrammarSpans ? `구문 분석 ON (${activeRoleCount}/6)` : '구문 분석 OFF'}
        </button>
      </div>

      {/* Interactive Grammar Role Legend */}
      {showGrammarSpans ? (
        <div
          role="group"
          aria-label="구문 역할 범례 필터"
          style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}
        >
          {ALL_GRAMMAR_ROLES.map((role) => {
            const theme = GRAMMAR_ROLE_THEMES[role];
            const selected = activeRoles.has(role);
            return (
              <button
                key={role}
                type="button"
                aria-pressed={selected}
                onClick={() => toggleRole(role)}
                style={{
                  minHeight: 34,
                  padding: '4px 10px',
                  borderRadius: 999,
                  border: `1.5px solid ${selected ? theme.underlineColor : '#CBD5E1'}`,
                  backgroundColor: selected ? theme.backgroundColor : '#FFFFFF',
                  color: selected ? theme.textColor : '#64748B',
                  fontSize: 11,
                  fontWeight: 800,
                  textDecoration: selected ? 'underline' : 'none',
                  textDecorationColor: theme.underlineColor,
                  textUnderlineOffset: 3,
                  cursor: 'pointer',
                }}
              >
                {theme.labelKo}
              </button>
            );
          })}
        </div>
      ) : null}

      {/* Multi-Filter Bar */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        <FilterPill
          label="전체"
          selected={!examFilter && !sectionFilter && !surfaceFilter && !onlyGrammarCore}
          onClick={() => {
            setExamFilter(null);
            setSectionFilter(null);
            setSurfaceFilter(null);
            setOnlyGrammarCore(false);
          }}
        />
        <FilterPill
          label="수능만"
          selected={examFilter === 'CSAT'}
          onClick={() => setExamFilter((prev) => (prev === 'CSAT' ? null : 'CSAT'))}
        />
        <FilterPill
          label="평가원 모평"
          selected={examFilter === 'KICE_MOCK'}
          onClick={() => setExamFilter((prev) => (prev === 'KICE_MOCK' ? null : 'KICE_MOCK'))}
        />
        <FilterPill
          label="독해"
          selected={sectionFilter === 'reading'}
          onClick={() => setSectionFilter((prev) => (prev === 'reading' ? null : 'reading'))}
        />
        <FilterPill
          label="듣기"
          selected={sectionFilter === 'listening'}
          onClick={() => setSectionFilter((prev) => (prev === 'listening' ? null : 'listening'))}
        />
        <FilterPill
          label="문법 핵심만"
          selected={onlyGrammarCore}
          onClick={() => setOnlyGrammarCore((prev) => !prev)}
        />
        {surfaceVariants.slice(0, 5).map((v) => (
          <FilterPill
            key={v.form}
            label={`${v.form} (${v.count})`}
            selected={surfaceFilter?.toLowerCase() === v.form.toLowerCase()}
            onClick={() =>
              setSurfaceFilter((prev) =>
                prev?.toLowerCase() === v.form.toLowerCase() ? null : v.form,
              )
            }
          />
        ))}
      </div>

      {/* Status / Error States */}
      {errorMessage ? (
        <div
          role="alert"
          style={{
            padding: 12,
            borderRadius: 12,
            backgroundColor: '#FEF2F2',
            border: '1px solid #FECACA',
            color: '#991B1B',
            fontSize: 13,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 8,
          }}
        >
          <span>{errorMessage}</span>
          <button
            type="button"
            onClick={() => void loadPage(0, false)}
            style={{
              minHeight: 36,
              padding: '4px 10px',
              borderRadius: 8,
              border: '1px solid #DC2626',
              backgroundColor: '#FFFFFF',
              color: '#991B1B',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            다시 시도
          </button>
        </div>
      ) : null}

      {/* Sentence Cards */}
      <div aria-live="polite" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {isLoading ? (
          <div style={{ padding: 20, textAlign: 'center', color: '#64748B', fontSize: 13 }}>
            SQLite에서 예문 15건을 불러오는 중입니다...
          </div>
        ) : items.length === 0 ? (
          <div
            style={{
              padding: 16,
              borderRadius: 12,
              backgroundColor: '#FFFFFF',
              border: '1px dashed #CBD5E1',
              color: '#64748B',
              fontSize: 13,
            }}
          >
            선택한 조건에 해당하는 예문이 없습니다. 필터를 '전체'로 초기화해 보세요.
          </div>
        ) : (
          items.map((occ) => {
            const isRevealed = Boolean(revealedIds[occ.occ_id]);
            const targetSpan = resolveTargetSpan(occ.text, occ.surface_form, occ.span_start, occ.span_end);
            const segments = partitionSentenceSegments(
              occ.text,
              occ.grammar_spans,
              activeRoles,
              targetSpan,
              showGrammarSpans,
              clozeMode,
              isRevealed,
            );

            return (
              <article
                key={occ.occ_id}
                data-testid={`occurrence-card-${occ.occ_id}`}
                style={{
                  borderRadius: 14,
                  backgroundColor: '#FFFFFF',
                  border: clozeMode && !isRevealed ? '1.5px solid #6366F1' : '1px solid #E2E8F0',
                  padding: 12,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 6,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, fontSize: 11, fontWeight: 800 }}>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: 999,
                        backgroundColor: '#EEF2FF',
                        color: '#3730A3',
                      }}
                    >
                      {occ.year} {occ.month}월 {EXAM_TYPE_LABELS[occ.exam_type] ?? occ.exam_type}
                    </span>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: 999,
                        backgroundColor: occ.section === 'listening' ? '#ECFDF5' : '#EFF6FF',
                        color: occ.section === 'listening' ? '#047857' : '#1D4ED8',
                      }}
                    >
                      {occ.section === 'listening' ? '듣기' : '독해'} {occ.item_no}번 · {occ.item_type}
                    </span>
                  </div>
                  {onSpeakSentence ? (
                    <button
                      type="button"
                      onClick={() => onSpeakSentence(occ.text)}
                      aria-label="문장 발음 듣기"
                      style={{
                        minHeight: 34,
                        padding: '4px 10px',
                        borderRadius: 8,
                        border: '1px solid #CBD5E1',
                        backgroundColor: '#F8FAFC',
                        fontSize: 11,
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      듣기
                    </button>
                  ) : null}
                </div>

                {/* Highlighted Sentence Body (Atomic Segments — Zero Raw HTML) */}
                <p style={{ margin: 0, fontSize: 15, lineHeight: 1.65, fontWeight: 500 }}>
                  {segments.map((seg) => {
                    const roleTheme = seg.primaryRole ? GRAMMAR_ROLE_THEMES[seg.primaryRole] : null;
                    const key = `${occ.occ_id}_${seg.start}_${seg.end}`;

                    if (seg.isClozeMasked) {
                      return (
                        <button
                          key={key}
                          type="button"
                          onClick={() => toggleRevealSentence(occ.occ_id)}
                          aria-label="빈칸 정답 확인"
                          style={{
                            display: 'inline',
                            padding: '1px 8px',
                            margin: '0 2px',
                            borderRadius: 6,
                            border: `1.5px solid ${roleTheme ? roleTheme.underlineColor : '#4F46E5'}`,
                            backgroundColor: '#EEF2FF',
                            color: '#4338CA',
                            fontWeight: 800,
                            fontSize: '0.95em',
                            cursor: 'pointer',
                          }}
                        >
                          [ ______ ]
                        </button>
                      );
                    }

                    const baseStyle: React.CSSProperties = {
                      borderRadius: 4,
                      padding: roleTheme || seg.isTargetToken ? '0 2px' : undefined,
                      backgroundColor: seg.isClozeRevealed
                        ? 'rgba(14, 159, 110, 0.18)'
                        : seg.isTargetToken
                          ? 'rgba(36, 92, 229, 0.16)'
                          : roleTheme?.backgroundColor,
                      color: seg.isClozeRevealed
                        ? '#065F46'
                        : seg.isTargetToken
                          ? '#1E3A8A'
                          : roleTheme?.textColor,
                      fontWeight: seg.isTargetToken || seg.isClozeRevealed ? 800 : undefined,
                      borderBottom: roleTheme
                        ? `2px solid ${roleTheme.underlineColor}`
                        : seg.isTargetToken && occ.is_underlined
                          ? '2px solid #D97706'
                          : undefined,
                    };

                    if (seg.isClozeRevealed) {
                      return (
                        <button
                          key={key}
                          type="button"
                          onClick={() => toggleRevealSentence(occ.occ_id)}
                          title="탭하여 다시 빈칸으로 가리기"
                          style={{
                            ...baseStyle,
                            borderTop: 0,
                            borderLeft: 0,
                            borderRight: 0,
                            display: 'inline',
                            cursor: 'pointer',
                            font: 'inherit',
                            fontWeight: 800,
                          }}
                        >
                          {seg.text}
                        </button>
                      );
                    }

                    return (
                      <span
                        key={key}
                        title={
                          seg.allRoles.length > 0
                            ? seg.allRoles.map((r) => GRAMMAR_ROLE_THEMES[r].labelKo).join(' · ')
                            : undefined
                        }
                        style={baseStyle}
                      >
                        {seg.text}
                      </span>
                    );
                  })}
                </p>

                {occ.translation_ko ? (
                  <p style={{ margin: 0, fontSize: 13, color: '#475569', lineHeight: 1.5 }}>
                    {occ.translation_ko}
                  </p>
                ) : null}

                {clozeMode ? (
                  <div style={{ fontSize: 11, fontWeight: 700, color: isRevealed ? '#047857' : '#4F46E5' }}>
                    {isRevealed
                      ? `정답 어형: ${occ.surface_form} (단어를 다시 탭해 숨기기)`
                      : '빈칸 버튼을 탭하여 정답 어형을 확인하세요.'}
                  </div>
                ) : null}

                {occ.grammar_tags.length > 0 ? (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 2 }}>
                    {occ.grammar_tags.map((tag) => (
                      <button
                        key={tag}
                        type="button"
                        onClick={() => onJumpToGrammarTag?.(tag)}
                        style={{
                          minHeight: 28,
                          padding: '2px 8px',
                          borderRadius: 999,
                          border: '1px solid #C7D2FE',
                          backgroundColor: '#EEF2FF',
                          color: '#4338CA',
                          fontSize: 11,
                          fontWeight: 700,
                          cursor: onJumpToGrammarTag ? 'pointer' : 'default',
                        }}
                      >
                        {tag} →
                      </button>
                    ))}
                  </div>
                ) : null}
              </article>
            );
          })
        )}
      </div>

      {/* 15-Item Lazy Load Pagination Button */}
      {hasMore ? (
        <button
          type="button"
          disabled={isLoadingMore}
          data-testid={`load-more-${wordId}`}
          onClick={handleLoadMore}
          style={{
            minHeight: 48,
            width: '100%',
            borderRadius: 12,
            border: '1.5px solid #245CE5',
            backgroundColor: '#EFF6FF',
            color: '#1D4ED8',
            fontWeight: 800,
            fontSize: 13,
            cursor: isLoadingMore ? 'wait' : 'pointer',
          }}
        >
          {isLoadingMore
            ? '추가 예문을 불러오는 중...'
            : `기출 용례 ${safePageSize}건 더 보기 (${items.length}/${totalCount})`}
        </button>
      ) : items.length > 0 ? (
        <div style={{ textAlign: 'center', fontSize: 11, color: '#64748B', fontWeight: 700 }}>
          모든 조건 일치 예문 {items.length}건을 불러왔습니다.
        </div>
      ) : null}
    </section>
  );
};

interface FilterPillProps {
  label: string;
  selected: boolean;
  onClick: () => void;
}

const FilterPill: React.FC<FilterPillProps> = ({ label, selected, onClick }) => (
  <button
    type="button"
    aria-pressed={selected}
    onClick={onClick}
    style={{
      minHeight: 34,
      padding: '4px 11px',
      borderRadius: 999,
      border: `1px solid ${selected ? '#245CE5' : '#CBD5E1'}`,
      backgroundColor: selected ? '#245CE5' : '#FFFFFF',
      color: selected ? '#FFFFFF' : '#334155',
      fontSize: 12,
      fontWeight: 700,
      cursor: 'pointer',
    }}
  >
    {label}
  </button>
);

export default ExampleExpandDrawer;
