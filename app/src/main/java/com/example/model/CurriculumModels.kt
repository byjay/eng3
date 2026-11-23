package com.example.model

import androidx.annotation.DrawableRes
import java.util.Locale

enum class VocabTopic(val labelKo: String) {
    SCHOOL_EXAM("학교·시험"),
    TRAVEL_DAILY("여행·일상"),
    SCIENCE_AI("과학·AI"),
    SOCIETY_ECONOMY("사회·경제"),
    EMOTION_MIND("감정·심리"),
    LOGIC_DEBATE("논리·토론")
}

enum class VocabStudyTrack(val labelKo: String, val badgeKo: String) {
    CSAT_FREQUENCY("수능 빈출순", "★★★★★ 최다빈출"),
    CSAT_DIFFICULTY("수능 난이도별", "기초→킬러"),
    RANDOM_MIX("완전 랜덤", "인터리빙 셔플"),
    SMART_REVIEW("약점 복습", "망각곡선 SM-2"),
    TOPIC_SITUATION("상황별 단어", "주제별 연관")
}

enum class LoopRepeatMode(val labelKo: String, val count: Int) {
    ONCE("1회", 1),
    THREE("3회 반복", 3),
    FIVE("5회 반복", 5),
    INFINITE("무한 반복", -1)
}

data class VocabExample(
    val labelKo: String,
    val english: String,
    val korean: String,
    val ipaHint: String = ""
)

data class VocabWord(
    val id: String,
    val grade: Int, // 1 = 고1 공통영어, 2 = 고2 영어Ⅰ·Ⅱ, 3 = 고3 수능·심화
    val textbookUnitKo: String,
    val csatFrequency: Int, // 3..5 (5 = 최다빈출)
    val csatDifficulty: Int, // 1..3 (1 = 내신기초, 2 = 수능핵심, 3 = 1등급킬러)
    val topic: VocabTopic,
    val word: String,
    val ipa: String,
    val partOfSpeech: String,
    val meaningKo: String,
    val exampleEn: String,
    val exampleKo: String,
    val examples: List<VocabExample>, // 4 sentences per word (Read, Listen & Speak)
    val pronunciationTipKo: String,
    val distractors: List<String>
)

data class PatternSlot(
    val slotKey: String,
    val labelKo: String,
    val options: List<Pair<String, String>> // Pair(English, Korean)
)

data class PatternDrillItem(
    val id: String,
    val stageTagKo: String, // "자기소개", "1문장 집중", "2문장 연결", "3문장 실전"
    val sentenceCount: Int, // 1..3
    val titleKo: String,
    val templateEn: String, // Uses {slot1}, {slot2}
    val templateKo: String,
    val coachingTipKo: String,
    val slots: List<PatternSlot>
)

data class ParentProfiles(
    val dadName: String = "아빠 국왕",
    val momName: String = "엄마 왕비",
    val daughterName: String = "빅뚱",
    val parentPin: String = "1234"
)

data class GiftRequestItem(
    val id: String,
    val targetParent: String, // "아빠" or "엄마"
    val giftTitleKo: String,
    val reasonKo: String,
    val requiredLevel: Int = 1,
    val isApproved: Boolean = false,
    val createdDateLabel: String = ""
)

data class KingdomProgressStats(
    val castleHpPercent: Int = 40,
    val missionClearCount: Int = 0, // Every 7 clears -> difficulty tier up + Boss Battle!
    val difficultyTier: Int = 1,
    val bossPending: Boolean = false,
    val bossVictories: Int = 0,
    val previewCount: Int = 0,
    val reviewCount: Int = 0,
    val perfectScoreCount: Int = 0,
    val princessLevel: Int = 1,
    val giftTickets: Int = 1,
    val totalAllowanceWon: Int = 0
)

data class DadAllowanceMission(
    val id: String,
    val titleKo: String,
    val descriptionKo: String,
    val rewardWon: Int,
    val castleHpBonus: Int,
    val categoryKo: String,
    val isCompletedByDaughter: Boolean = false,
    val isApprovedByDad: Boolean = false
)

data class ScorecardEntry(
    val id: String,
    val examTitleKo: String,
    val subjectKo: String,
    val score: Int,
    val targetScore: Int,
    val dateLabel: String
)

data class StudyScheduleItem(
    val id: String,
    val dayLabelKo: String,
    val timeLabel: String,
    val titleKo: String,
    val detailKo: String,
    val isCompleted: Boolean = false
)

data class KakaoFriend(
    val uuid: String,
    val nickname: String,
    val thumbnailUrl: String = "",
    val favorite: Boolean = false
)

data class KakaoSessionState(
    val isLoggedIn: Boolean = false,
    val isConfigured: Boolean = false,
    val nickname: String = "",
    val profileImageUrl: String = "",
    val accessToken: String = "",
    val friends: List<KakaoFriend> = emptyList(),
    val statusMessage: String = "테스트 모드: 로그인 없이 모든 기능 즉시 사용 중"
)

data class TravelZone(
    val id: Int,
    val titleKo: String,
    val titleEn: String,
    val subtitleKo: String,
    val accentHex: Long,
    val shadowHex: Long
)

data class TravelSentence(
    val index: Int, // 1..30
    val phaseKo: String,
    val english: String,
    val korean: String,
    val ipaHint: String,
    val speakingTipKo: String
)

data class TravelTwist(
    val id: String,
    val titleKo: String,
    val staffPromptEn: String,
    val staffPromptKo: String,
    val strategyKo: String,
    val keyPhrases: List<String>,
    val expectedKeywords: List<String>
)

data class TravelScenario(
    val id: String,
    val number: Int, // 1..100
    val zoneId: Int, // 1..10
    val titleKo: String,
    val titleEn: String,
    val locationTag: String,
    val difficulty: Int, // 1..3
    val coinReward: Int,
    val xpReward: Int,
    val sentences: List<TravelSentence>,
    val twists: List<TravelTwist>
)

data class AiHumanTutor(
    val id: String,
    val name: String,
    val koreanNickname: String,
    val cityCountry: String,
    val accentLabel: String,
    val locale: Locale,
    val ttsPitch: Float,
    val ttsRate: Float,
    val specialtyKo: String,
    val inspiredBy: String,
    val greetingEn: String,
    val greetingKo: String,
    val friendPersonaPrompt: String,
    @DrawableRes val portraitRes: Int,
    val themeColorHex: Long,
    val badgeTag: String
)

data class CollectibleItem(
    val id: String,
    val titleKo: String,
    val descriptionKo: String,
    val categoryKo: String,
    val costCoins: Int,
    val colorHex: Long,
    val bonusLabel: String
)

enum class AiEngineMode(val labelKo: String, val descKo: String) {
    BUILT_IN_GEMINI(
        "실제 Gemini AI (기본)",
        "API 키 없이 즉시 원어민 선생님과 실시간 영상통화"
    ),
    EXTERNAL_10_SLOT_ROTATOR(
        "10슬롯 LLM 자동 순환",
        "Groq 무료 API 등 최대 10개 키를 라운드 로빈·자동 폴백 순환"
    ),
    OFFLINE_ONLY(
        "100% 오프라인 초경량 엔진",
        "인터넷·LLM 없이 기기 내장 발음/패턴 코치로 초고속 반응"
    )
}

data class LlmSlotConfig(
    val slotIndex: Int, // 0..9
    val label: String,
    val providerName: String,
    val baseUrl: String,
    val modelId: String,
    val apiKey: String,
    val enabled: Boolean,
    val requestCount: Int = 0,
    val lastStatus: String = "대기"
)

enum class TokenStatus {
    EXACT,
    CLOSE,
    MISSED
}

data class WordPronunciationToken(
    val targetWord: String,
    val spokenWord: String?,
    val status: TokenStatus,
    val similarityPercent: Int,
    val phoneticTipKo: String?
)

data class PronunciationReport(
    val targetText: String,
    val recognizedText: String,
    val overallScore: Int, // 0..100
    val fluencyGrade: String, // S, A+, A, B, C
    val estimatedWpm: Int,
    val tokens: List<WordPronunciationToken>,
    val phoneticFeedbackKo: String,
    val mouthTipKo: String,
    val passed: Boolean
)

data class TutorChatMessage(
    val id: String,
    val fromTutor: Boolean,
    val textEn: String,
    val translationKo: String? = null,
    val coachingTipKo: String? = null,
    val naturalRephraseEn: String? = null,
    val pronunciationScore: Int? = null,
    val engineBadge: String? = null,
    val isStreaming: Boolean = false
)

enum class VocabLearningMethod(
    val labelKo: String,
    val shortKo: String,
    val descKo: String
) {
    HIPPOCAMPUS(
        "해마학습법",
        "해마연상",
        "강렬한 장면·스토리·감정 결합 + 10분/1일/3일 골든타임 복습으로 장기기억 해마 각인"
    ),
    WORD_ASSOCIATION(
        "단어연상법",
        "어근·발음연상",
        "접두사·어근·접미사 분해 + 한국어 발음 이미지 연상 고리로 3초 암기"
    ),
    EBBINGHAUS_SM2(
        "망각곡선 SM-2",
        "망각곡선",
        "모르는 단어·애매한 단어를 망각 직전에 자동 재출제하는 간격 반복법"
    ),
    ACTIVE_RECALL(
        "능동적 회상",
        "능동회상",
        "뜻을 가리고 머릿속에서 먼저 꺼내본 뒤 알아요/몰라요로 즉시 점검"
    ),
    CONTEXT_SPEAKING(
        "맥락·스피킹",
        "맥락입트임",
        "4개 실전 예문을 읽고·듣고·입으로 말하며 원어민 문맥 그대로 체화"
    )
}

enum class WordKnowledgeStatus(val labelKo: String) {
    KNOWN("완전 알아요"),
    FUZZY("애매해요"),
    UNKNOWN("몰라요")
}

data class StudyTimeLogEntry(
    val id: String,
    val dateIso: String,
    val startTimeLabel: String,
    val endTimeLabel: String,
    val playerName: String, // "빅뚱" or "스몰뚱"
    val activityTypeKo: String,
    val methodKo: String,
    val durationSec: Int,
    val wordsStudied: Int,
    val knownCount: Int,
    val unknownCount: Int
)

