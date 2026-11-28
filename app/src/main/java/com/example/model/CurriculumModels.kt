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
    CSAT_FREQUENCY("핵심 중요도순", "★★★★★ 필수 어휘"),
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
    val distractors: List<String>,
    val synonymGroupId: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
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
    val dadName: String = "멘토",
    val momName: String = "코치",
    val daughterName: String = "학습자",
    val parentPin: String = "",
    val isPinConfigured: Boolean = false
)

data class GuestInviteSession(
    val isGuestMode: Boolean = false,
    val inviteCode: String = "",
    val hostNickname: String = "학습자",
    val guestNickname: String = "",
    val myGeneratedCode: String = "",
    val kakaoLinked: Boolean = false
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
    val favorite: Boolean = false,
    val kakaoId: String = ""
)

data class KakaoSessionState(
    val isLoggedIn: Boolean = false,
    val isConfigured: Boolean = false,
    val kakaoUserId: String = "",
    val nickname: String = "",
    val profileImageUrl: String = "",
    val accessToken: String = "",
    val friends: List<KakaoFriend> = emptyList(),
    val statusMessage: String = "테스트 모드: 로그인 없이 모든 기능 즉시 사용 중"
)

data class WrongNoteRecord(
    val wordId: String,
    val savedDateIso: String,
    val lastStudiedDateIso: String,
    val lastStudiedAtMs: Long = 0L,
    val isMastered: Boolean = false
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
    FREE_OPEN_SOURCE_AUTO(
        "무료 오픈소스+AI 자동 (추천)",
        "키 없는 무료 오픈소스 LLM → 10슬롯(Groq/OpenRouter) → Gemini → LLM 없는 스킬엔진 4중 무중단"
    ),
    BUILT_IN_GEMINI(
        "내장 Gemini + 무료 폴백",
        "내장 Gemini AI 우선 호출, 한도 초과 시 무료 오픈소스·온디바이스 스킬로 즉시 전환"
    ),
    EXTERNAL_10_SLOT_ROTATOR(
        "10슬롯 무료 LLM 순환",
        "Groq·OpenRouter·Cerebras·HuggingFace·Ollama 등 최대 10개 무료 오픈소스 API 라운드로빈"
    ),
    OFFLINE_ONLY(
        "LLM 없이 0ms 스킬 엔진",
        "API·인터넷·LLM 0%! 멀티턴 롤플레이·문법교정·55+ 실전표현 온디바이스 즉시 회화"
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
        "장면연상법",
        "장면연상",
        "의미와 연결된 시각 장면·스토리·간격 복습(10분/1일/3일)으로 선명하게 기억"
    ),
    WORD_ASSOCIATION(
        "단어연상법",
        "어근·발음연상",
        "검증된 어근 분석 + 기억 보조 발음 고리로 뜻을 빠르게 떠올리는 학습"
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
    val playerName: String, // "원삐", "투삐", or "손님"
    val activityTypeKo: String,
    val methodKo: String,
    val durationSec: Int,
    val wordsStudied: Int,
    val knownCount: Int,
    val unknownCount: Int
)

