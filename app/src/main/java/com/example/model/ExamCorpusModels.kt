package com.example.model

/**
 * Integrated 10-Year Vocabulary, Usage Occurrence & Grammar Analysis Schema (v2.0)
 *
 * Backward-compatible with v1.0 callers/tests while adding:
 * - 5-Tier Difficulty Split (`VocabDifficultyTier`: TIER_S_CORE .. TIER_D_FOOTNOTE)
 * - Gries Dispersion Metrics (`DP`, `DP_norm`, `uniformity = 1 - DP_norm`)
 * - CSAT Recurrence Metrics (`meanGapYr`, `medianGapYr`, `maxGapYr`, `lastCsatYear`, `overdueYr`)
 * - 2022 Revised Curriculum 3,000 Groups (`G1_ELEM_800` .. `OOV`)
 * - Normalized 3-table SQLite + External-Content FTS5 span & section metadata
 * - Copyright-safe Hybrid BYOD runtime mode tracking
 */

enum class ExamType(val code: String, val labelKo: String) {
    CSAT("CSAT", "수능"),
    KICE_MOCK("KICE_MOCK", "평가원 모평"),
    MOCK_G3("MOCK_G3", "고3 학평"),
    MOCK_G2("MOCK_G2", "고2 학평"),
    MOCK_G1("MOCK_G1", "고1 학평");

    companion object {
        fun fromCode(raw: String): ExamType =
            entries.firstOrNull { it.code.equals(raw.trim(), ignoreCase = true) } ?: MOCK_G1
    }
}

enum class ExamSection(val code: String, val labelKo: String) {
    READING("reading", "독해"),
    LISTENING("listening", "듣기");

    companion object {
        fun fromCode(raw: String): ExamSection =
            if (raw.trim().equals("listening", ignoreCase = true)) LISTENING else READING
    }
}

enum class CefrLevel(val code: String, val weight: Int) {
    A2("A2", 1),
    B1("B1", 2),
    B2("B2", 3),
    C1("C1", 4);

    companion object {
        fun fromCode(raw: String): CefrLevel =
            entries.firstOrNull { it.code.equals(raw.trim(), ignoreCase = true) } ?: B1
    }
}

enum class CurriculumGroup2022(val code: String, val labelKo: String, val baseDifficulty: Float) {
    G1_ELEM_800("G1_ELEM_800", "초등 800", 15f),
    G2_MID_H1_1200("G2_MID_H1_1200", "중학·고1 1200", 35f),
    G3_H2_H3_1000("G3_H2_H3_1000", "고2·3 1000", 60f),
    OOV("OOV", "교육과정 외(OOV)", 82f);

    companion object {
        fun fromCode(raw: String): CurriculumGroup2022 =
            entries.firstOrNull { it.code.equals(raw.trim(), ignoreCase = true) } ?: OOV
    }
}

enum class CorpusDataProvenance(val code: String, val badgeKo: String) {
    CURATED_TRAINING_CORPUS("CURATED_TRAINING", "자체 창작·통계"),
    OFFICIAL_IMPORTED_JSON("OFFICIAL_IMPORT", "로컬 BYOD JSON"),
    LOCAL_BYOD_SQLITE("LOCAL_BYOD_SQLITE", "로컬 BYOD SQLite");

    companion object {
        fun fromCode(raw: String): CorpusDataProvenance =
            entries.firstOrNull { it.code.equals(raw.trim(), ignoreCase = true) }
                ?: CURATED_TRAINING_CORPUS
    }
}

enum class CorpusRuntimeMode(val labelKo: String, val descriptionKo: String) {
    CURATED_ORIGINAL(
        labelKo = "안전 창작·통계 모드",
        descriptionKo = "저작권 안전 자체 제작 예문과 5-Tier 정량 통계로 동작합니다."
    ),
    LOCAL_BYOD_SQLITE(
        labelKo = "로컬 BYOD SQLite+FTS5 모드",
        descriptionKo = "개인 기기에 직접 불러온 로컬 SQLite+FTS5 DB와 결합해 동작합니다."
    ),
    LOCAL_BYOD_JSON(
        labelKo = "로컬 BYOD JSON 모드",
        descriptionKo = "개인 기기에 직접 불러온 로컬 기출 JSON 용례와 결합해 동작합니다."
    )
}

/**
 * 5-Tier Difficulty Split Matrix (Tier S .. Tier D)
 */
enum class VocabDifficultyTier(
    val code: String,
    val rank: Int,
    val fileName: String,
    val labelKo: String,
    val shortBadgeKo: String,
    val descriptionKo: String,
    val hiddenInDefaultDeck: Boolean = false
) {
    TIER_S_CORE(
        code = "TIER_S_CORE",
        rank = 1,
        fileName = "tier_s_core.json",
        labelKo = "Tier S 최우선 필수",
        shortBadgeKo = "Tier S",
        descriptionKo = "균등도(1-DP_norm) 0.70 이상 또는 수능 7개년+ 상시 필수어"
    ),
    TIER_A_CYCLICAL(
        code = "TIER_A_CYCLICAL",
        rank = 2,
        fileName = "tier_a_cyclical.json",
        labelKo = "Tier A 주기 반복",
        shortBadgeKo = "Tier A",
        descriptionKo = "균등도 0.40~0.69 또는 모평→수능 연계 반복 핵심어"
    ),
    TIER_B_ADVANCED_NEW(
        code = "TIER_B_ADVANCED_NEW",
        rank = 3,
        fileName = "tier_b_advanced_new.json",
        labelKo = "Tier B 변별·신출",
        shortBadgeKo = "Tier B",
        descriptionKo = "교육과정 외(OOV) 고난도 변별어 및 최근 3개년 신출·급상승어"
    ),
    TIER_C_BURSTY(
        code = "TIER_C_BURSTY",
        rank = 4,
        fileName = "tier_c_bursty.json",
        labelKo = "Tier C 저빈도·편중",
        shortBadgeKo = "Tier C",
        descriptionKo = "특정 1~2개 지문에 편중된 문맥 추론 보조 어휘"
    ),
    TIER_D_FOOTNOTE(
        code = "TIER_D_FOOTNOTE",
        rank = 5,
        fileName = "tier_d_footnote.json",
        labelKo = "Tier D 각주·제외군",
        shortBadgeKo = "Tier D",
        descriptionKo = "별표(*) 각주 제공 전문어·고유명사·단위 (기본 덱 자동 숨김)",
        hiddenInDefaultDeck = true
    );

    companion object {
        fun fromCode(raw: String): VocabDifficultyTier =
            entries.firstOrNull { it.code.equals(raw.trim(), ignoreCase = true) } ?: TIER_A_CYCLICAL
    }
}

enum class ExamPriorityTier(
    val labelKo: String,
    val shortBadgeKo: String,
    val descriptionKo: String
) {
    MUST_KNOW_CORE(
        labelKo = "절대 필수 암기",
        shortBadgeKo = "절대 필수",
        descriptionKo = "누적 빈도·연도 분산도·연계성이 모두 높은 최우선 어휘군"
    ),
    HIGH_DISCRIMINATION(
        labelKo = "고난도 변별 어휘",
        shortBadgeKo = "고난도 변별",
        descriptionKo = "상위권 변별 문항과 고3/C1 수준 독해에 집중되는 어휘군"
    ),
    EVERGREEN_REPEAT(
        labelKo = "상시 반복 출제",
        shortBadgeKo = "상시 반복",
        descriptionKo = "짧은 재출현 주기로 꾸준히 반복 등장하는 핵심 어휘군"
    ),
    NEWLY_EMERGENT(
        labelKo = "최근 신출 어휘",
        shortBadgeKo = "최근 신출",
        descriptionKo = "최근 출제 경향에서 새롭게 부상한 주목 어휘군"
    )
}

enum class GrammarCategory(
    val code: String,
    val udRuleCode: String,
    val kiceSharePercent: Float,
    val labelKo: String,
    val shortLabelKo: String,
    val focusSummaryKo: String
) {
    RELATIVES_CONJUNCTIONS(
        code = "RELATIVES_CONJUNCTIONS",
        udRuleCode = "RELCL_CONJ",
        kiceSharePercent = 36.7f,
        labelKo = "관계사·접속사",
        shortLabelKo = "관계사",
        focusSummaryKo = "관계대명사(which/what)·전치사+which·동격 that·whether 완전/불완전 절 판별"
    ),
    VERBALS_PARTICIPLES(
        code = "VERBALS_PARTICIPLES",
        udRuleCode = "VERB_VS_NONFINITE",
        kiceSharePercent = 30.0f,
        labelKo = "동사·준동사",
        shortLabelKo = "준동사",
        focusSummaryKo = "긴 주어-본동사 판별, 동명사/to부정사 주어, 분사구문 능동·수동 구분"
    ),
    PARALLELISM_COMPARISON(
        code = "PARALLELISM_COMPARISON",
        udRuleCode = "PARALLEL_STRUCT",
        kiceSharePercent = 10.0f,
        labelKo = "병렬구조·비교",
        shortLabelKo = "병렬구조",
        focusSummaryKo = "등위·상관접속사(and/or/not only A but also B)의 원거리 문법 형태 대칭"
    ),
    INVERSION_EMPHASIS(
        code = "INVERSION_EMPHASIS",
        udRuleCode = "PRONOUN_PROVERB",
        kiceSharePercent = 10.0f,
        labelKo = "대명사·도치·강조",
        shortLabelKo = "대명사·도치",
        focusSummaryKo = "재귀대명사·가주어/가목적어 it·대동사 및 부정어 문두 도치 구문"
    ),
    VOICE_TENSE(
        code = "VOICE_TENSE",
        udRuleCode = "VOICE_ACTIVE_PASSIVE",
        kiceSharePercent = 6.7f,
        labelKo = "능동·수동태",
        shortLabelKo = "태·시제",
        focusSummaryKo = "행위 주체에 따른 능동/수동 구분, 자동사 수동 불가 및 과거분사 후치수식"
    ),
    SUBJECT_VERB_AGREEMENT(
        code = "SUBJECT_VERB_AGREEMENT",
        udRuleCode = "SUBJECT_VERB_AGREE",
        kiceSharePercent = 6.7f,
        labelKo = "수일치",
        shortLabelKo = "수일치",
        focusSummaryKo = "주어와 본동사 사이 교란 명사구(Attraction Error)를 넘는 단·복수 호응"
    );

    companion object {
        fun fromCode(raw: String): GrammarCategory? {
            val clean = raw.trim()
            return entries.firstOrNull {
                it.code.equals(clean, ignoreCase = true) ||
                    it.udRuleCode.equals(clean, ignoreCase = true)
            }
        }
    }
}

data class GriesDispersionMetrics(
    val dp: Float = 0f,
    val dpNorm: Float = 0f,
    val uniformity: Float = 1f
)

data class RecurrenceMetrics(
    val meanGapYr: Float? = null,
    val medianGapYr: Float? = null,
    val maxGapYr: Int? = null,
    val lastCsatYear: Int? = null,
    val overdueYr: Int? = null
)

data class WordAnalyticsFlags(
    val isPolysemous: Boolean = false,
    val isListeningHeavy: Boolean = false,
    val isNewRecent3Yr: Boolean = false,
    val isOverdue: Boolean = false,
    val isFootnoteEver: Boolean = false,
    val isExcludedEntity: Boolean = false
)

data class SurfaceVariantCount(
    val form: String,
    val count: Int
)

enum class GrammarSpanRole(
    val code: String,
    val labelKo: String,
    val shortLabelKo: String,
    val descriptionKo: String
) {
    SUBJECT_CHUNK(
        code = "SUBJECT_CHUNK",
        labelKo = "주어구",
        shortLabelKo = "주어",
        descriptionKo = "문장 핵심 주어 및 전치사·수식 포함 주어 덩어리"
    ),
    MAIN_VERB(
        code = "MAIN_VERB",
        labelKo = "본동사",
        shortLabelKo = "본동사",
        descriptionKo = "주어와 수·시제·태가 호응하는 절의 핵심 서술 동사"
    ),
    RELATIVE_CLAUSE(
        code = "RELATIVE_CLAUSE",
        labelKo = "관계절·접속절",
        shortLabelKo = "관계절",
        descriptionKo = "which/that/who/what/where/whether 등으로 이어지는 종속절"
    ),
    PARTICIPLE_PHRASE(
        code = "PARTICIPLE_PHRASE",
        labelKo = "분사구문·준동사",
        shortLabelKo = "분사구문",
        descriptionKo = "V-ing / p.p. / to부정사로 압축된 분사 및 준동사 수식구"
    ),
    PARALLEL_UNIT(
        code = "PARALLEL_UNIT",
        labelKo = "병렬구조",
        shortLabelKo = "병렬",
        descriptionKo = "and / or / but / not only A but also B 등 대칭 구조"
    ),
    PREPOSITIONAL_MODIFIER(
        code = "PREPOSITIONAL_MODIFIER",
        labelKo = "전치사 수식어",
        shortLabelKo = "전치사구",
        descriptionKo = "주어-동사 사이에 끼어들거나 문맥을 한정하는 전치사구"
    );

    companion object {
        fun fromCode(raw: String): GrammarSpanRole? {
            val clean = raw.trim()
            return entries.firstOrNull { it.code.equals(clean, ignoreCase = true) }
        }
    }
}

data class GrammarSpan(
    val role: GrammarSpanRole,
    val start: Int,
    val end: Int
)

data class ExamOccurrenceRecord(
    val id: String,
    val year: Int,
    val month: Int,
    val examType: ExamType,
    val itemTypeKo: String,
    val itemNumber: Int,
    val surfaceForm: String,
    val sentenceEn: String,
    val sentenceKo: String,
    val structureNoteKo: String,
    val grammarCategories: List<GrammarCategory> = emptyList(),
    val provenance: CorpusDataProvenance = CorpusDataProvenance.CURATED_TRAINING_CORPUS,
    val section: ExamSection = ExamSection.READING,
    val charSpanStart: Int = -1,
    val charSpanEnd: Int = -1,
    val isUnderlined: Boolean = false,
    val isAnswerKeyToken: Boolean = false,
    val grammarSpans: List<GrammarSpan> = emptyList()
)

data class CorpusWordAnalytics(
    val totalFrequency: Int,
    val csatFrequency: Int,
    val mockFrequency: Int,
    val hasMockToCsatCarryover: Boolean,
    val mockToCsatCarryoverRate: Float,
    val yearDispersion: Float,
    val avgRecurrenceGapYears: Float,
    val firstSeenYear: Int,
    val lastSeenYear: Int,
    val isEvergreenCore: Boolean,
    val isNewlyEmergent: Boolean,
    val priorityScore: Int,
    val priorityTier: ExamPriorityTier,
    val difficultyTier: VocabDifficultyTier = VocabDifficultyTier.TIER_A_CYCLICAL,
    val difficultyScore: Float = 50f,
    val griesDispersion: GriesDispersionMetrics = GriesDispersionMetrics(),
    val recurrence: RecurrenceMetrics = RecurrenceMetrics(),
    val readingFrequency: Int = totalFrequency,
    val listeningFrequency: Int = 0,
    val listeningRatio: Float = 0f,
    val mockToCsatTransferHits: Int = if (hasMockToCsatCarryover) csatFrequency else 0,
    val flags: WordAnalyticsFlags = WordAnalyticsFlags(),
    val curriculumGroup: CurriculumGroup2022 = CurriculumGroup2022.G2_MID_H1_1200,
    val isAwl: Boolean = false
)

data class ExamCorpusWordEntry(
    val wordId: String,
    val lemma: String,
    val ipa: String,
    val pos: String,
    val meaningKo: String,
    val grade: Int,
    val cefr: CefrLevel,
    val isMoeBasic: Boolean,
    val inflections: List<String>,
    val collocations: List<String>,
    val occurrences: List<ExamOccurrenceRecord>,
    val grammarTags: List<GrammarCategory>,
    val analytics: CorpusWordAnalytics,
    val sourceWord: VocabWord,
    val surfaceVariants: List<SurfaceVariantCount> = emptyList(),
    val wordFamilyHead: String = lemma
)

data class GrammarFocusSentence(
    val id: String,
    val category: GrammarCategory,
    val titleKo: String,
    val patternFormula: String,
    val sentenceWithBlankEn: String,
    val completeSentenceEn: String,
    val sentenceKo: String,
    val targetSegment: String,
    val distractorSegment: String,
    val explanationKo: String,
    val trapTipKo: String,
    val linkedLemmas: List<String>,
    val difficultyTier: Int = 2,
    val provenance: CorpusDataProvenance = CorpusDataProvenance.CURATED_TRAINING_CORPUS
)

data class CorpusSummaryMetrics(
    val totalLemmas: Int,
    val totalOccurrences: Int,
    val avgDispersion: Float,
    val mockToCsatCarryoverPercent: Int,
    val mustKnowCount: Int,
    val highDiscriminationCount: Int,
    val evergreenCount: Int,
    val newlyEmergentCount: Int,
    val grammarSentenceCount: Int,
    val tierCounts: Map<VocabDifficultyTier, Int> = emptyMap(),
    val avgUniformity: Float = avgDispersion
)
