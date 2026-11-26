package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudflareLevelPayload
import com.example.data.CloudflareLevelSyncService
import com.example.data.DadKingdomLocalStore
import com.example.data.DailyWordHarvesterService
import com.example.data.FamilyLeaderboardRepository
import com.example.data.HumanTutorRepository
import com.example.data.KakaoAuthAndFriendsService
import com.example.data.KakaoGuestInviteService
import com.example.data.LearningAchievementResult
import com.example.data.LlmSlotStore
import com.example.data.PatternSentenceRepository
import com.example.data.SquirrelRpgRepository
import com.example.data.TravelCurriculumRepository
import com.example.data.VocabRepository
import com.example.engine.EnGameMathEngine
import com.example.engine.EnGameMode
import com.example.engine.EnGamePhase
import com.example.engine.EnGameUiState
import com.example.engine.HybridAiCoachEngine
import com.example.engine.LearningMission
import com.example.engine.MediaReviewStatus
import com.example.engine.MemoryPalaceEntry
import com.example.engine.MemoryPalaceRoom
import com.example.engine.MissionKind
import com.example.engine.NanoBananaMotionEngine
import com.example.engine.PronunciationAnalyzer
import com.example.engine.QuestCelebrationEvent
import com.example.engine.RecallGrade
import com.example.engine.RewardConfig
import com.example.engine.RewardEventType
import com.example.engine.RewardTransaction
import com.example.engine.SuggestedReplyOption
import com.example.engine.TypedRecallEvaluator
import com.example.engine.TypedRecallResult
import com.example.engine.VocabAnalyticsTracker
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceCommandParser
import com.example.engine.VoiceEngineState
import com.example.engine.VoiceSettingCommand
import com.example.engine.VoiceSpeechEngine
import com.example.engine.WordLearningProgress
import com.example.engine.WordLearningStatus
import com.example.ui.components.FamilyLeaderboardUiState
import com.example.ui.components.MotionPhotoGesture
import com.example.ui.components.StreakMilestoneEvent
import com.example.ui.components.WeeklyStudyReportData
import com.example.model.AiEngineMode
import com.example.model.AiHumanTutor
import com.example.model.CollectibleItem
import com.example.model.DadAllowanceMission
import com.example.model.EmeraldStoryChapter
import com.example.model.GiftRequestItem
import com.example.model.GuestInviteSession
import com.example.model.KakaoSessionState
import com.example.model.KingdomProgressStats
import com.example.model.LlmSlotConfig
import com.example.model.LoopRepeatMode
import com.example.model.MysteryChestReward
import com.example.model.ParentProfiles
import com.example.model.PatternDrillItem
import com.example.model.PronunciationReport
import com.example.model.RpgBattleAction
import com.example.model.RpgGamePhase
import com.example.model.RpgGameUiState
import com.example.model.RpgPhraseChallenge
import com.example.model.RpgStoryChoice
import com.example.model.RpgWordChallenge
import com.example.model.ScenarioProgress
import com.example.model.ScorecardEntry
import com.example.model.SquirrelJobHero
import com.example.model.SquirrelSkillKind
import com.example.model.StudyScheduleItem
import com.example.model.StudyTimeLogEntry
import com.example.model.TravelScenario
import com.example.model.TravelTwist
import com.example.model.TutorChatMessage
import com.example.model.UserProgress
import com.example.model.VocabLearningMethod
import com.example.model.VocabStudyTrack
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.model.WordKnowledgeStatus
import com.example.model.WrongNoteRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SpartaPhase {
    PREVIEW_STUDY,      // 예습 모드 (단어 + 4개 예문 먼저 읽고 듣기)
    MEANING_QUIZ,       // 능동적 회상 퀴즈
    SENTENCE_SHADOWING, // 단어별 3~5개 예문 읽고·듣고·말하기
    LEVEL_TEST,         // 1~5등급 빠른 레벨테스트 (5문항)
    BOSS_BATTLE,        // 7회 미션마다 해금되는 시험 마왕 보스전!
    COMPLETED
}

data class SpartaDrillState(
    val grade: Int = 0, // 0 = 전학년, 1=고1, 2=고2, 3=고3
    val selectedTier1To5: Int = 3, // 1등급(최고) ~ 5등급(기초)
    val track: VocabStudyTrack = VocabStudyTrack.CSAT_FREQUENCY,
    val selectedTopic: VocabTopic = VocabTopic.SCHOOL_EXAM,
    val dailyTarget: Int = 30,
    val queue: List<VocabWord> = emptyList(),
    val retryIds: Set<String> = emptySet(),
    val masteredInSession: Int = 0,
    val currentWord: VocabWord? = null,
    val activeExampleIndex: Int = 0, // 0..3 (4 sentences per word!)
    val spokenExampleIndices: Set<Int> = emptySet(),
    val options: List<String> = emptyList(),
    val phase: SpartaPhase = SpartaPhase.MEANING_QUIZ,
    val timeLeftSec: Int = 18,
    val maxTimeSec: Int = 18,
    val combo: Int = 0,
    val perfectStreakInSession: Boolean = true,
    val sessionCoins: Int = 0,
    val sessionXp: Int = 0,
    val lastFeedbackKo: String = "",
    val lastWasMiss: Boolean = false,
    val latestPronunciation: PronunciationReport? = null,
    val showKoreanHint: Boolean = true,
    val loopMode: LoopRepeatMode = LoopRepeatMode.ONCE,
    val isLoopPlaying: Boolean = false,
    // Learning Method (해마학습법 · 단어연상법 · 망각곡선 · 능동회상 · 맥락스피킹) & Know/Don't Know Diagnostic
    val learningMethod: VocabLearningMethod = VocabLearningMethod.HIPPOCAMPUS,
    val showAnswerRevealed: Boolean = false,
    val sessionKnownCount: Int = 0,
    val sessionFuzzyCount: Int = 0,
    val sessionUnknownCount: Int = 0,
    // 1~5 Grade Level Test
    val levelTestIndex: Int = 0,
    val levelTestCorrectCount: Int = 0,
    val levelTestWords: List<VocabWord> = emptyList(),
    val levelTestResultBanner: String = "",
    // Boss Battle (every 7 missions!)
    val bossHp: Int = 3,
    val bossMaxHp: Int = 3,
    val bossNameKo: String = "어둠의 빈칸추론 마왕"
)

data class PatternStudioState(
    val activeDrillIndex: Int = 0,
    val selectedSlotIndices: Map<String, Int> = emptyMap(),
    val showKorean: Boolean = true,
    val loopMode: LoopRepeatMode = LoopRepeatMode.THREE,
    val isLoopPlaying: Boolean = false,
    val latestPronunciation: PronunciationReport? = null
) {
    val activeDrill: PatternDrillItem
        get() = PatternSentenceRepository.drills[activeDrillIndex.coerceIn(0, PatternSentenceRepository.drills.lastIndex)]

    val renderedEnglish: String
        get() = PatternSentenceRepository.renderEnglish(activeDrill, selectedSlotIndices)

    val renderedKorean: String
        get() = PatternSentenceRepository.renderKorean(activeDrill, selectedSlotIndices)
}

data class TravelPracticeState(
    val selectedZoneId: Int = 1,
    val activeScenario: TravelScenario = TravelCurriculumRepository.allScenarios.first(),
    val activeSentenceIndex: Int = 0,
    val completedIndices: Set<Int> = emptySet(),
    val activeTwistIndex: Int = 0,
    val showKoreanSubtitles: Boolean = true,
    val latestPronunciation: PronunciationReport? = null,
    val twistReplyText: String = "",
    val twistReplyKo: String = "",
    val twistCoachTipKo: String = "",
    val twistLoading: Boolean = false,
    val twistEngineBadge: String = "",
    val proactiveLineEn: String = "",
    val proactiveLineKo: String = "",
    val proactiveThoughtKo: String = "",
    val proactiveMood: String = "happy",
    val proactiveLoading: Boolean = false,
    val proactiveEngineBadge: String = ""
)

data class TutorCallState(
    val selectedTutor: AiHumanTutor = HumanTutorRepository.tutors.first(),
    val messages: List<TutorChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val latestPronunciation: PronunciationReport? = null,
    val showKoreanTranslations: Boolean = true,
    val koreanVoiceInputMode: Boolean = false, // false = EN speaking, true = KR question/voice command!
    val liveSubtitleEn: String = HumanTutorRepository.tutors.first().greetingEn,
    val liveSubtitleKo: String = HumanTutorRepository.tutors.first().greetingKo,
    val liveCoachingCommentKo: String = "마이크를 눌러 회화학습을 시작하세요",
    val liveNaturalRephraseEn: String = "",
    val liveGrammarFixKo: String = "",
    val activeEngineBadgeKo: String = "무료 오픈소스+스킬 코치",
    val dynamicSuggestedReplies: List<SuggestedReplyOption> = HybridAiCoachEngine.defaultSuggestedRepliesForTopic("수다"),
    val collectedUpgrades: List<Pair<String, String>> = emptyList(),
    val pronunciationScoresInCall: List<Int> = emptyList(),
    val liveVoiceCommandNotice: String = "",
    val callSeconds: Int = 0,
    val activeTopicTag: String = "수다",
    val sceneLocationLabelKo: String = "뉴욕 타임스스퀘어",
    val nanoBananaSceneBitmap: Bitmap? = null,
    val nanoBananaBlinkBitmap: Bitmap? = null,
    val isGeneratingScene: Boolean = false,
    val sceneErrorKo: String? = null,
    val turnsInCall: Int = 0,
    val lastUserSpokenEn: String = "",
    val shadowingTargetEn: String? = null,
    val liveThoughtKo: String = "",
    val conversationGoalKo: String = HybridAiCoachEngine.goalForTopic("수다"),
    val aiMood: String = "happy",
    val proactiveModeEnabled: Boolean = true,
    val proactiveNudgeCount: Int = 0
)

class SpeakQuestViewModel(
    private val localStore: DadKingdomLocalStore,
    private val llmSlotStore: LlmSlotStore,
    private val voiceEngine: VoiceSpeechEngine,
    private val aiCoachEngine: HybridAiCoachEngine,
    private val familyLeaderboardRepository: FamilyLeaderboardRepository? = null
) : ViewModel() {

    private val nanoBananaEngine = NanoBananaMotionEngine()
    private var sceneGenJob: Job? = null
    private var tutorReplyJob: Job? = null
    private var twistReplyJob: Job? = null
    private var travelProactiveJob: Job? = null
    private var familyLeaderboardSyncJob: Job? = null

    private val _firebaseUserUid = MutableStateFlow<String?>(null)
    private val _firebaseDisplayName = MutableStateFlow<String?>(null)
    val firebaseDisplayName: StateFlow<String?> = _firebaseDisplayName.asStateFlow()

    private val _familyLeaderboardState =
        MutableStateFlow<FamilyLeaderboardUiState>(FamilyLeaderboardUiState.SignedOut)
    val familyLeaderboardState: StateFlow<FamilyLeaderboardUiState> =
        _familyLeaderboardState.asStateFlow()

    val shareStreakWithFriends: StateFlow<Boolean> = localStore.shareStreakWithFriendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private val _streakMilestoneEvent = MutableStateFlow<StreakMilestoneEvent?>(null)
    val streakMilestoneEvent: StateFlow<StreakMilestoneEvent?> = _streakMilestoneEvent.asStateFlow()

    private val _questCelebrationEvent = MutableStateFlow<QuestCelebrationEvent?>(null)
    val questCelebrationEvent: StateFlow<QuestCelebrationEvent?> = _questCelebrationEvent.asStateFlow()

    private val _vocabCardExamplePronunciation = MutableStateFlow<PronunciationReport?>(null)
    val vocabCardExamplePronunciation: StateFlow<PronunciationReport?> = _vocabCardExamplePronunciation.asStateFlow()

    private val _vocabCardExampleTarget = MutableStateFlow("")
    val vocabCardExampleTarget: StateFlow<String> = _vocabCardExampleTarget.asStateFlow()

    fun dismissStreakMilestoneEvent() {
        _streakMilestoneEvent.value = null
    }

    fun dismissQuestCelebrationEvent() {
        _questCelebrationEvent.value = null
    }

    val voiceState: StateFlow<VoiceEngineState> = voiceEngine.state

    val userProgress: StateFlow<UserProgress> = localStore.userProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserProgress())

    val weeklyStudyReport: StateFlow<WeeklyStudyReportData> = localStore.weeklyStudyReportFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WeeklyStudyReportData())

    val kingdomStats: StateFlow<KingdomProgressStats> = localStore.kingdomStatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), KingdomProgressStats())

    val parentProfiles: StateFlow<ParentProfiles> = localStore.parentProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ParentProfiles())

    // v2 Master Spec StateFlows (SM-2, Idempotent Reward Ledger, Daily Missions, Memory Palace)
    val wordSm2Progress: StateFlow<Map<String, WordLearningProgress>> = localStore.wordSm2ProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val rewardLedger: StateFlow<List<RewardTransaction>> = localStore.rewardLedgerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dailyVocabMissions: StateFlow<List<LearningMission>> = localStore.dailyMissionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val memoryPalaceEntries: StateFlow<Map<String, MemoryPalaceEntry>> = localStore.memoryPalaceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val mediaReviewOverrides: StateFlow<Map<String, MediaReviewStatus>> = localStore.mediaReviewOverridesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val csatDDayDays: StateFlow<Int> = localStore.csatDDayDaysFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            com.example.engine.FsrsSchedulerEngine.DEFAULT_DAYS_UNTIL_CSAT
        )

    val masteredWordIds: StateFlow<Set<String>> = localStore.masteredWordIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val weakWordIds: StateFlow<Set<String>> = localStore.weakWordIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val wrongNoteRecords: StateFlow<Map<String, WrongNoteRecord>> = localStore.wrongNoteRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val harvestedApiWords: StateFlow<List<VocabWord>> = localStore.harvestedWordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val vocabGradeTier: StateFlow<Int> = localStore.vocabGradeTierFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 3)

    val levelTestDone: StateFlow<Boolean> = localStore.levelTestDoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val cloudflareWorkerUrl: StateFlow<String> = localStore.cloudflareWorkerUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    val scenarioProgressState: StateFlow<List<ScenarioProgress>> = localStore.clearedScenarioIdsFlow
        .map { cleared ->
            cleared.map { id ->
                val sc = TravelCurriculumRepository.allScenarios.firstOrNull { it.id == id }
                ScenarioProgress(
                    scenarioId = id,
                    zoneId = sc?.zoneId ?: 1,
                    completedSentencesCount = 10,
                    twistCleared = true,
                    bestFluencyScore = 92
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dadMissions: StateFlow<List<DadAllowanceMission>> = localStore.dadMissionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val giftRequests: StateFlow<List<GiftRequestItem>> = localStore.giftRequestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val scorecards: StateFlow<List<ScorecardEntry>> = localStore.scorecardFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val schedules: StateFlow<List<StudyScheduleItem>> = localStore.scheduleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val kakaoSession: StateFlow<KakaoSessionState> = localStore.kakaoSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), KakaoSessionState())

    val selectedSquirrelHeroId: StateFlow<String> = localStore.selectedSquirrelHeroIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "hero_luna")

    val clearedRpgChapters: StateFlow<Set<String>> = localStore.clearedRpgChaptersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val unopenedChests: StateFlow<Int> = localStore.unopenedChestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val openedTreasures: StateFlow<List<MysteryChestReward>> = localStore.openedTreasuresFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _rpgState = MutableStateFlow(RpgGameUiState())
    val rpgState: StateFlow<RpgGameUiState> = _rpgState.asStateFlow()

    private val _enGameState = MutableStateFlow(EnGameUiState())
    val enGameState: StateFlow<EnGameUiState> = _enGameState.asStateFlow()
    private var enGameTimerJob: Job? = null
    private val enGameRecentWordIds = ArrayDeque<String>()

    val guestInviteSession: StateFlow<GuestInviteSession> = localStore.guestInviteSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GuestInviteSession())

    val engineMode: StateFlow<AiEngineMode> = llmSlotStore.engineModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AiEngineMode.FREE_OPEN_SOURCE_AUTO)

    val llmSlots: StateFlow<List<LlmSlotConfig>> = llmSlotStore.slotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val speechRate: StateFlow<Float> = llmSlotStore.speechRateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 1.0f)

    val handsFreeMode: StateFlow<Boolean> = llmSlotStore.handsFreeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private val _spartaState = MutableStateFlow(SpartaDrillState())
    val spartaState: StateFlow<SpartaDrillState> = _spartaState.asStateFlow()

    private val _patternState = MutableStateFlow(PatternStudioState())
    val patternState: StateFlow<PatternStudioState> = _patternState.asStateFlow()

    private val _travelState = MutableStateFlow(TravelPracticeState())
    val travelState: StateFlow<TravelPracticeState> = _travelState.asStateFlow()

    private val _tutorCallState = MutableStateFlow(TutorCallState())
    val tutorCallState: StateFlow<TutorCallState> = _tutorCallState.asStateFlow()

    private val _bannerToast = MutableStateFlow<String?>(null)
    val bannerToast: StateFlow<String?> = _bannerToast.asStateFlow()

    private val _slotTestResults = MutableStateFlow<Map<Int, String>>(emptyMap())
    val slotTestResults: StateFlow<Map<Int, String>> = _slotTestResults.asStateFlow()

    private val _harvestStatusKo = MutableStateFlow("무료 단어 API 대기 중")
    val harvestStatusKo: StateFlow<String> = _harvestStatusKo.asStateFlow()

    private val _isHarvestingWords = MutableStateFlow(false)
    val isHarvestingWords: StateFlow<Boolean> = _isHarvestingWords.asStateFlow()

    private val _cloudflareStatusKo = MutableStateFlow("미등록 (Cloudflare Worker 연결 가능)")
    val cloudflareStatusKo: StateFlow<String> = _cloudflareStatusKo.asStateFlow()

    val studyLogs: StateFlow<List<StudyTimeLogEntry>> = localStore.studyLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeStudyElapsedSec = MutableStateFlow(0)
    val activeStudyElapsedSec: StateFlow<Int> = _activeStudyElapsedSec.asStateFlow()

    private var activeActivityLabelKo: String = "단어 학습"
    private var activeSessionStartMillis: Long = System.currentTimeMillis()
    private var loggedSessionWords: Int = 0
    private var loggedSessionKnown: Int = 0
    private var loggedSessionUnknown: Int = 0

    private var timerJob: Job? = null
    private var loopJob: Job? = null
    private var callClockJob: Job? = null
    private var studyStopwatchJob: Job? = null

    init {
        viewModelScope.launch {
            val savedTier = localStore.vocabGradeTierFlow.first()
            startSmartVocabSession(
                grade = 0,
                track = VocabStudyTrack.CSAT_FREQUENCY,
                tier1To5 = savedTier
            )
            autoHarvestDailyWordsIfNeeded()
        }
        initTutorConversation(HumanTutorRepository.tutors.first())
        startCallClock()
        startStudyStopwatch()
    }

    private fun startStudyStopwatch() {
        studyStopwatchJob?.cancel()
        activeSessionStartMillis = System.currentTimeMillis()
        studyStopwatchJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _activeStudyElapsedSec.value = _activeStudyElapsedSec.value + 1
            }
        }
    }

    fun onEnterStudyActivity(activityLabelKo: String) {
        if (activeActivityLabelKo == activityLabelKo) return
        flushActiveStudyTimeLog(showConfirmation = false)
        activeActivityLabelKo = activityLabelKo
        activeSessionStartMillis = System.currentTimeMillis()
        _activeStudyElapsedSec.value = 0
        loggedSessionWords = 0
        loggedSessionKnown = 0
        loggedSessionUnknown = 0
    }

    fun flushActiveStudyTimeLog(showConfirmation: Boolean = false) {
        val elapsed = _activeStudyElapsedSec.value
        if (elapsed < 3 && loggedSessionWords == 0 && loggedSessionKnown == 0 && loggedSessionUnknown == 0) {
            if (showConfirmation) {
                showToast("최소 3초 이상 학습 후 시간 로그를 기록할 수 있어요!")
            }
            return
        }
        val endMillis = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        val entry = StudyTimeLogEntry(
            id = UUID.randomUUID().toString(),
            dateIso = todayIso(),
            startTimeLabel = timeFmt.format(Date(activeSessionStartMillis)),
            endTimeLabel = timeFmt.format(Date(endMillis)),
            playerName = parentProfiles.value.daughterName,
            activityTypeKo = activeActivityLabelKo,
            methodKo = _spartaState.value.learningMethod.labelKo,
            durationSec = elapsed.coerceAtLeast(1),
            wordsStudied = loggedSessionWords.coerceAtLeast(loggedSessionKnown + loggedSessionUnknown),
            knownCount = loggedSessionKnown,
            unknownCount = loggedSessionUnknown
        )
        activeSessionStartMillis = endMillis
        _activeStudyElapsedSec.value = 0
        loggedSessionWords = 0
        loggedSessionKnown = 0
        loggedSessionUnknown = 0

        viewModelScope.launch {
            localStore.appendStudyTimeLog(entry)
            if (showConfirmation) {
                val mins = entry.durationSec / 60
                val secs = entry.durationSec % 60
                showToast("학습 시간 로그 저장 완료! (${entry.playerName} · ${mins}분 ${secs}초 · ${entry.methodKo})")
            }
        }
    }

    fun selectVocabLearningMethod(method: VocabLearningMethod) {
        _spartaState.value = _spartaState.value.copy(
            learningMethod = method,
            showAnswerRevealed = method == VocabLearningMethod.HIPPOCAMPUS || method == VocabLearningMethod.WORD_ASSOCIATION,
            lastFeedbackKo = "${method.labelKo} 적용! ${method.descKo}"
        )
        if (method == VocabLearningMethod.EBBINGHAUS_SM2) {
            startSmartVocabSession(track = VocabStudyTrack.SMART_REVIEW)
            _spartaState.value = _spartaState.value.copy(learningMethod = method)
        } else if (method == VocabLearningMethod.CONTEXT_SPEAKING && _spartaState.value.currentWord != null) {
            _spartaState.value = _spartaState.value.copy(phase = SpartaPhase.SENTENCE_SHADOWING)
        }
        showToast("단어 학습법 전환: ${method.labelKo}")
    }

    fun toggleRevealVocabAnswer() {
        _spartaState.value = _spartaState.value.copy(
            showAnswerRevealed = !_spartaState.value.showAnswerRevealed
        )
    }

    fun markWordKnowledge(status: WordKnowledgeStatus) {
        val st = _spartaState.value
        val word = st.currentWord ?: return
        loggedSessionWords += 1
        when (status) {
            WordKnowledgeStatus.KNOWN -> {
                loggedSessionKnown += 1
                _spartaState.value = st.copy(
                    sessionKnownCount = st.sessionKnownCount + 1,
                    showAnswerRevealed = false,
                    lastWasMiss = false,
                    lastFeedbackKo = "'${word.word}' 완전 알아요(O) 확인! 마스터 DB 기록 완료 → 다음 단어!"
                )
                advanceToNextSpartaWord(word, report = null)
            }
            WordKnowledgeStatus.FUZZY -> {
                loggedSessionUnknown += 1
                val remaining = st.queue.drop(1)
                val insertAt = minOf(2, remaining.size)
                val updatedQueue = remaining.toMutableList().apply { add(insertAt, word) }
                _spartaState.value = st.copy(
                    queue = updatedQueue,
                    retryIds = st.retryIds + word.id,
                    sessionFuzzyCount = st.sessionFuzzyCount + 1,
                    showAnswerRevealed = true,
                    lastWasMiss = true,
                    lastFeedbackKo = "'${word.word}' 애매해요(△) — 장면·어근 연상 힌트를 보고 2문제 뒤 다시 확인합니다!"
                )
                viewModelScope.launch {
                    localStore.markWordWeak(word.id)
                    localStore.recordDailyWordOutcome(word.id, word.word, word.meaningKo, known = false)
                }
                speakWithActiveTutor(word.word)
            }
            WordKnowledgeStatus.UNKNOWN -> {
                loggedSessionUnknown += 1
                val remaining = st.queue.drop(1)
                val insertAt = minOf(1, remaining.size)
                val updatedQueue = remaining.toMutableList().apply { add(insertAt, word) }
                _spartaState.value = st.copy(
                    queue = updatedQueue,
                    retryIds = st.retryIds + word.id,
                    sessionUnknownCount = st.sessionUnknownCount + 1,
                    showAnswerRevealed = true,
                    lastWasMiss = true,
                    lastFeedbackKo = "'${word.word}' 몰라요(X) — 장면 연상과 어근을 확인하고 복습 대기열에 넣었어요!"
                )
                viewModelScope.launch {
                    localStore.markWordWeak(word.id)
                    localStore.recordDailyWordOutcome(word.id, word.word, word.meaningKo, known = false)
                }
                speakWithActiveTutor(word.exampleEn)
            }
        }
    }

    private fun startCallClock() {
        callClockJob?.cancel()
        callClockJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _tutorCallState.value = _tutorCallState.value.copy(
                    callSeconds = _tutorCallState.value.callSeconds + 1
                )
            }
        }
    }

    fun clearBannerToast() {
        _bannerToast.value = null
    }

    fun showToast(msg: String) {
        _bannerToast.value = msg
    }

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ==================== DAILY FREE WORD API HARVESTER (50+ WORDS/DAY) ====================

    private fun autoHarvestDailyWordsIfNeeded() {
        viewModelScope.launch {
            val lastDate = localStore.lastHarvestDateFlow.first()
            val today = todayIso()
            if (lastDate != today) {
                harvestDaily50WordsFromFreeApi(preferredTopic = null, silent = true)
            } else {
                val count = localStore.harvestedWordsFlow.first().size
                _harvestStatusKo.value = "오늘 무료 API 수집 완료 · 누적 ${count}단어 DB 보유"
            }
        }
    }

    fun harvestDaily50WordsFromFreeApi(
        preferredTopic: VocabTopic? = null,
        silent: Boolean = false
    ) {
        if (_isHarvestingWords.value) return
        viewModelScope.launch {
            _isHarvestingWords.value = true
            _harvestStatusKo.value = "R2 샤드 · 3종 무료 API에서 새 단어 50개 수집 중..."
            val existingApiWords = localStore.harvestedWordsFlow.first()
            val existingSet = (VocabRepository.allWords + existingApiWords)
                .map { it.word.lowercase() }
                .toSet()
            val daySeed = (System.currentTimeMillis() / 86_400_000L).toInt() + existingApiWords.size

            val result = DailyWordHarvesterService.harvestDaily50PlusWords(
                existingWordLowerSet = existingSet,
                daySeed = daySeed,
                preferredTopic = preferredTopic,
                customCloudflareWorkerUrl = cloudflareWorkerUrl.value
            )
            result.onSuccess { res ->
                val totalCount = localStore.appendHarvestedWords(res.newlyAddedWords)
                _harvestStatusKo.value = "+${res.newlyAddedWords.size}단어 (${res.sourceLabel} · 누적 ${totalCount}개)"
                if (!silent) {
                    showToast("${res.sourceLabel}로 ${res.newlyAddedWords.size}단어(동의어 ${res.synonymGroupCount}그룹) 수집 완료!")
                }
            }.onFailure { err ->
                _harvestStatusKo.value = "오프라인/대기 (${err.message ?: "필수 교과 단어로 학습 가능"})"
                if (!silent) {
                    showToast("단어 수집 연결 실패: ${err.message ?: "네트워크 확인"}")
                }
            }
            _isHarvestingWords.value = false
        }
    }

    fun startVocabCardExampleShadowing(targetSentence: String) {
        if (targetSentence.isBlank()) return
        stopLoopPlayback()
        _vocabCardExampleTarget.value = targetSentence
        _vocabCardExamplePronunciation.value = null
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.startListening(tutor.locale) { transcript, durationMs ->
            val report = PronunciationAnalyzer.analyze(targetSentence, transcript, durationMs)
            _vocabCardExamplePronunciation.value = report
            if (report.passed) {
                viewModelScope.launch {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val spec = RewardConfig.specFor(RewardEventType.LISTENING_QUIZ_CORRECT)
                    localStore.grantIdempotentReward(
                        eventId = "shadow_${today}_${targetSentence.lowercase().hashCode()}",
                        eventType = RewardEventType.LISTENING_QUIZ_CORRECT,
                        xpDelta = spec.xp,
                        coinDelta = spec.coins,
                        reason = "예문 따라 말하기 통과 (${report.overallScore}점)"
                    )
                    localStore.advanceVocabMission(MissionKind.DAILY_LISTENING_SPEAK, 1)
                }
                showToast("예문 발음 ${report.overallScore}점 통과! +${RewardConfig.specFor(RewardEventType.LISTENING_QUIZ_CORRECT).xp} XP")
            }
        }
    }

    fun clearVocabCardExamplePronunciation() {
        _vocabCardExamplePronunciation.value = null
        _vocabCardExampleTarget.value = ""
    }

    fun speakSlowSentence(text: String) {
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.speak(
            text = text,
            locale = tutor.locale,
            pitch = tutor.ttsPitch,
            rate = 0.74f
        )
        viewModelScope.launch {
            localStore.advanceVocabMission(MissionKind.DAILY_LISTENING_SPEAK, 1)
        }
    }

    // ==================== ANTI-ABC 5-TRACK VOCAB + 1~5 GRADE TIER + LEVEL TEST + 7-MISSION BOSS ====================

    private fun combinedWordPool(grade: Int): List<VocabWord> {
        val core = VocabRepository.wordsForGrade(grade)
        val harvested = harvestedApiWords.value.let { list ->
            if (grade == 0) list else list.filter { it.grade == grade }
        }
        return (core + harvested).distinctBy { it.word.lowercase() }
    }

    private fun tier1To5TargetDifficulty(tier: Int): Int = when (tier.coerceIn(1, 5)) {
        1 -> 3 // 1등급 최상위
        2 -> 3 // 2등급 심화
        3 -> 2 // 3등급 핵심
        4 -> 2 // 4등급 기본
        else -> 1 // 5등급 기초
    }

    fun startSmartVocabSession(
        grade: Int = _spartaState.value.grade,
        track: VocabStudyTrack = _spartaState.value.track,
        topic: VocabTopic = _spartaState.value.selectedTopic,
        tier1To5: Int = _spartaState.value.selectedTier1To5,
        previewFirst: Boolean = false,
        count: Int = 30
    ) {
        timerJob?.cancel()
        stopLoopPlayback()
        val pool = combinedWordPool(grade)
        val weakIds = weakWordIds.value
        val masteredIds = masteredWordIds.value
        val targetDiff = tier1To5TargetDifficulty(tier1To5)

        // 100% Anti-Alphabetical ordering across all 5 tracks!
        val ordered: List<VocabWord> = when (track) {
            VocabStudyTrack.CSAT_FREQUENCY -> {
                pool.groupBy { it.csatFrequency }
                    .toSortedMap(compareByDescending { it })
                    .values
                    .flatMap { it.shuffled() }
            }
            VocabStudyTrack.CSAT_DIFFICULTY -> {
                // Prioritize selected 1~5 grade tier first, then build up!
                val exact = pool.filter { it.csatDifficulty == targetDiff }.shuffled()
                val rest = pool.filter { it.csatDifficulty != targetDiff }.shuffled()
                exact + rest
            }
            VocabStudyTrack.RANDOM_MIX -> pool.shuffled()
            VocabStudyTrack.SMART_REVIEW -> {
                val weak = pool.filter { it.id in weakIds }.shuffled()
                val fresh = pool.filter { it.id !in weakIds && it.id !in masteredIds }.shuffled()
                val mastered = pool.filter { it.id in masteredIds && it.id !in weakIds }.shuffled()
                weak + fresh + mastered
            }
            VocabStudyTrack.TOPIC_SITUATION -> {
                val gradeMatched = pool.filter { it.topic == topic }.shuffled()
                val matched = if (gradeMatched.isNotEmpty()) gradeMatched else combinedWordPool(0).filter { it.topic == topic }.shuffled()
                val filler = pool.filter { it.topic != topic }.shuffled()
                matched + filler
            }
        }.take(count.coerceAtMost(pool.size.coerceAtLeast(1)))

        val first = ordered.firstOrNull()
        val difficultyTier = kingdomStats.value.difficultyTier
        val quizTime = (18 - (difficultyTier - 1) * 2).coerceIn(10, 18)
        val gradeLabel = when (grade) {
            1 -> "고1 공통영어"
            2 -> "고2 영어Ⅰ·Ⅱ"
            3 -> "고3 수능·심화"
            else -> "전학년 통합"
        }

        _spartaState.value = SpartaDrillState(
            grade = grade,
            selectedTier1To5 = tier1To5.coerceIn(1, 5),
            track = track,
            selectedTopic = topic,
            dailyTarget = ordered.size,
            queue = ordered,
            currentWord = first,
            activeExampleIndex = 0,
            spokenExampleIndices = emptySet(),
            options = buildOptions(first, pool),
            phase = if (previewFirst) SpartaPhase.PREVIEW_STUDY else SpartaPhase.MEANING_QUIZ,
            timeLeftSec = quizTime,
            maxTimeSec = quizTime,
            learningMethod = _spartaState.value.learningMethod,
            lastFeedbackKo = "$gradeLabel · ${tier1To5}등급 빌드업 · ${track.labelKo} 시작! (4개 예문 읽고·듣고·말하기)"
        )
        viewModelScope.launch {
            localStore.updateSelectedGrade(grade)
            localStore.setVocabGradeTier(tier1To5)
        }
        if (!previewFirst) {
            startSpartaCountdown()
        }
    }

    fun selectVocabGrade(grade: Int) {
        val clamped = grade.coerceIn(0, 3)
        val count = combinedWordPool(clamped).size
        startSmartVocabSession(grade = clamped)
        val label = when (clamped) {
            1 -> "고1 공통영어"
            2 -> "고2 영어Ⅰ·Ⅱ"
            3 -> "고3 수능·심화"
            else -> "전학년 전체"
        }
        showToast("$label 단어 덱으로 전환했어요! (${count}단어)")
    }

    fun selectVocabTier1To5(tier: Int) {
        startSmartVocabSession(
            tier1To5 = tier.coerceIn(1, 5),
            track = VocabStudyTrack.CSAT_DIFFICULTY
        )
        showToast("수능 ${tier}등급 맞춤 난이도로 덱을 빌드업했어요!")
    }

    fun startQuickLevelTest() {
        timerJob?.cancel()
        stopLoopPlayback()
        val pool = combinedWordPool(0)
        // 5 progressive non-alphabetical words from 5등급(easy) -> 1등급(killer)
        val q1 = pool.filter { it.csatDifficulty == 1 }.shuffled().firstOrNull()
        val q2 = pool.filter { it.csatDifficulty == 1 && it.id != q1?.id }.shuffled().firstOrNull()
        val q3 = pool.filter { it.csatDifficulty == 2 }.shuffled().firstOrNull()
        val q4 = pool.filter { it.csatDifficulty == 2 && it.id != q3?.id }.shuffled().firstOrNull()
        val q5 = pool.filter { it.csatDifficulty == 3 }.shuffled().firstOrNull()
        val testWords = listOfNotNull(q1, q2, q3, q4, q5)
        val first = testWords.firstOrNull() ?: return

        _spartaState.value = _spartaState.value.copy(
            phase = SpartaPhase.LEVEL_TEST,
            levelTestIndex = 0,
            levelTestCorrectCount = 0,
            levelTestWords = testWords,
            currentWord = first,
            options = buildOptions(first, pool),
            levelTestResultBanner = "",
            lastFeedbackKo = "1~5등급 빠른 레벨테스트 (1/5) — 5문항으로 딱 맞는 시작 등급을 찾아드려요!"
        )
    }

    fun submitLevelTestAnswer(chosenMeaning: String) {
        val st = _spartaState.value
        val word = st.currentWord ?: return
        val isCorrect = chosenMeaning == word.meaningKo
        val nextCorrect = st.levelTestCorrectCount + if (isCorrect) 1 else 0
        val nextIdx = st.levelTestIndex + 1

        if (nextIdx >= st.levelTestWords.size) {
            // Map 5..0 correct -> Grade 1..5!
            val diagnosedTier = (6 - nextCorrect).coerceIn(1, 5)
            val resultMsg = "레벨테스트 완료 ($nextCorrect/5 정답) → 추천 시작 등급: 수능 ${diagnosedTier}등급! 이제 ${diagnosedTier}등급부터 1등급까지 빌드업합니다!"
            viewModelScope.launch {
                localStore.setVocabGradeTier(diagnosedTier, fromLevelTest = true)
            }
            startSmartVocabSession(
                tier1To5 = diagnosedTier,
                track = VocabStudyTrack.CSAT_DIFFICULTY
            )
            _spartaState.value = _spartaState.value.copy(
                levelTestResultBanner = resultMsg,
                lastFeedbackKo = resultMsg
            )
            showToast("레벨테스트 결과: 수능 ${diagnosedTier}등급! 빌드업 시작!")
        } else {
            val nextWord = st.levelTestWords[nextIdx]
            _spartaState.value = st.copy(
                levelTestIndex = nextIdx,
                levelTestCorrectCount = nextCorrect,
                currentWord = nextWord,
                options = buildOptions(nextWord, combinedWordPool(0)),
                lastFeedbackKo = if (isCorrect) {
                    "정답! (${nextIdx + 1}/5) 점점 높은 등급 단어에 도전합니다!"
                } else {
                    "아쉬워요! 정답은 '${word.meaningKo}'. 다음 문항 (${nextIdx + 1}/5) 도전!"
                }
            )
        }
    }

    fun startBossBattle() {
        timerJob?.cancel()
        stopLoopPlayback()
        val tier = kingdomStats.value.difficultyTier
        val bossNames = listOf(
            "어둠의 빈칸추론 마왕",
            "말썽꾸러기 숙어 드래곤",
            "시험지 도둑 고블린 대장",
            "깜빡증 망각 마법사"
        )
        val bossName = bossNames[(tier - 1) % bossNames.size]
        val bossWords = combinedWordPool(0)
            .filter { it.csatDifficulty >= minOf(tier, 3) }
            .shuffled()
            .take(5)
        val first = bossWords.firstOrNull() ?: return

        _spartaState.value = _spartaState.value.copy(
            phase = SpartaPhase.BOSS_BATTLE,
            queue = bossWords,
            currentWord = first,
            options = buildOptions(first, combinedWordPool(0)),
            bossHp = 3,
            bossMaxHp = 3,
            bossNameKo = "Lv.$tier $bossName",
            timeLeftSec = 14,
            maxTimeSec = 14,
            lastFeedbackKo = "긴급 경보! 7회 미션 달성으로 '$bossName'이 성문 앞에 나타났어요! 3연속 정답으로 물리치세요!"
        )
        startSpartaCountdown()
    }

    fun completePreviewAndStartQuiz() {
        val st = _spartaState.value
        viewModelScope.launch {
            val (boss, lvlUp) = localStore.recordLearningAchievement(
                xpDelta = 15,
                coinsDelta = 10,
                castleHpDelta = 3,
                previewDelta = 1
            )
            notifySpecialMilestones(boss, lvlUp)
        }
        _spartaState.value = st.copy(
            phase = SpartaPhase.MEANING_QUIZ,
            lastFeedbackKo = "예습 완료 (+예습 누적 1회)! 이제 능동적 회상 퀴즈로 실력을 증명해요!"
        )
        startSpartaCountdown()
    }

    private fun buildOptions(word: VocabWord?, pool: List<VocabWord>): List<String> {
        if (word == null) return emptyList()
        val dist = word.distractors.ifEmpty {
            pool.filter { it.meaningKo != word.meaningKo }.map { it.meaningKo }.shuffled().take(3)
        }
        return (dist.take(3) + word.meaningKo).shuffled()
    }

    private fun startSpartaCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val st = _spartaState.value
                if (st.phase == SpartaPhase.COMPLETED ||
                    st.phase == SpartaPhase.PREVIEW_STUDY ||
                    st.phase == SpartaPhase.LEVEL_TEST ||
                    st.currentWord == null
                ) break
                if (st.timeLeftSec > 1) {
                    _spartaState.value = st.copy(timeLeftSec = st.timeLeftSec - 1)
                } else {
                    handleSpartaMiss("시간 초과! 에빙하우스 망각곡선 복습 대기열로 들어갑니다.")
                    break
                }
            }
        }
    }

    fun submitSpartaQuizAnswer(chosenMeaning: String) {
        val st = _spartaState.value
        val word = st.currentWord ?: return

        if (st.phase == SpartaPhase.LEVEL_TEST) {
            submitLevelTestAnswer(chosenMeaning)
            return
        }

        if (st.phase == SpartaPhase.BOSS_BATTLE) {
            if (chosenMeaning == word.meaningKo) {
                val nextHp = st.bossHp - 1
                if (nextHp <= 0) {
                    timerJob?.cancel()
                    viewModelScope.launch {
                        localStore.clearBossBattle()
                    }
                    _spartaState.value = st.copy(
                        bossHp = 0,
                        phase = SpartaPhase.COMPLETED,
                        lastFeedbackKo = "대승리!! '${st.bossNameKo}' 격파! +150 코인 · 성채 HP +20% · 선물 요청권 +1장 획득!"
                    )
                    showToast("보스전 대승리! 아빠·엄마 선물 요청권 +1장 획득!")
                } else {
                    val nextQueue = st.queue.drop(1)
                    val nextWord = nextQueue.firstOrNull() ?: word
                    _spartaState.value = st.copy(
                        bossHp = nextHp,
                        queue = nextQueue,
                        currentWord = nextWord,
                        options = buildOptions(nextWord, combinedWordPool(0)),
                        timeLeftSec = 14,
                        lastFeedbackKo = "치명타 명중! 마왕 체력이 ${nextHp}칸 남았어요!"
                    )
                }
            } else {
                _spartaState.value = st.copy(
                    lastWasMiss = true,
                    lastFeedbackKo = "마왕이 방어했어요! 정답은 '${word.meaningKo}'. 다시 한 번 노려봐요!"
                )
            }
            return
        }

        if (st.phase != SpartaPhase.MEANING_QUIZ) return

        if (chosenMeaning == word.meaningKo) {
            timerJob?.cancel()
            val newCombo = st.combo + 1
            val firstExample = word.examples.firstOrNull()?.english ?: word.exampleEn
            _spartaState.value = st.copy(
                phase = SpartaPhase.SENTENCE_SHADOWING,
                activeExampleIndex = 0,
                spokenExampleIndices = emptySet(),
                timeLeftSec = 30,
                maxTimeSec = 30,
                combo = newCombo,
                lastWasMiss = false,
                latestPronunciation = null,
                lastFeedbackKo = "정답! 이제 '${word.word}'의 4개 예문을 읽고·듣고·말해보세요!"
            )
            speakWithActiveTutor(firstExample)
        } else {
            handleSpartaMiss("오답! 정답은 '${word.meaningKo}'. 약점 복습 대기열에 저장되어 다시 출제됩니다!")
        }
    }

    private fun handleSpartaMiss(message: String) {
        timerJob?.cancel()
        val st = _spartaState.value
        val missedWord = st.currentWord ?: return
        val remaining = st.queue.drop(1)
        val insertAt = minOf(2, remaining.size)
        val updatedQueue = remaining.toMutableList().apply { add(insertAt, missedWord) }
        val nextWord = updatedQueue.firstOrNull()

        val nextHearts = (userProgress.value.hearts - 1).coerceAtLeast(1)
        _spartaState.value = st.copy(
            queue = updatedQueue,
            retryIds = st.retryIds + missedWord.id,
            currentWord = nextWord,
            options = buildOptions(nextWord, combinedWordPool(st.grade)),
            phase = SpartaPhase.MEANING_QUIZ,
            timeLeftSec = st.maxTimeSec,
            combo = 0,
            perfectStreakInSession = false,
            lastWasMiss = true,
            latestPronunciation = null,
            lastFeedbackKo = message
        )
        viewModelScope.launch {
            localStore.markWordWeak(missedWord.id)
            localStore.recordLearningAchievement(xpDelta = 0, coinsDelta = 0, castleHpDelta = -1, newHearts = nextHearts)
        }
        startSpartaCountdown()
    }

    fun selectVocabExample(index: Int) {
        val word = _spartaState.value.currentWord ?: return
        val clamped = index.coerceIn(0, (word.examples.size - 1).coerceAtLeast(0))
        _spartaState.value = _spartaState.value.copy(
            activeExampleIndex = clamped,
            latestPronunciation = null
        )
        val text = word.examples.getOrNull(clamped)?.english ?: word.exampleEn
        playTextWithLoopMode(text, _spartaState.value.loopMode)
    }

    fun setVocabLoopMode(mode: LoopRepeatMode) {
        _spartaState.value = _spartaState.value.copy(loopMode = mode)
        showToast("예문 반복 듣기: ${mode.labelKo}")
    }

    fun listenSpartaWordOnly() {
        val word = _spartaState.value.currentWord ?: return
        speakWithActiveTutor(word.word)
    }

    fun listenSpartaSentence() {
        val st = _spartaState.value
        val word = st.currentWord ?: return
        val sentence = word.examples.getOrNull(st.activeExampleIndex)?.english ?: word.exampleEn
        playTextWithLoopMode(sentence, st.loopMode)
    }

    fun listenAllWordExamplesSequentially() {
        val word = _spartaState.value.currentWord ?: return
        stopLoopPlayback()
        loopJob = viewModelScope.launch {
            _spartaState.value = _spartaState.value.copy(isLoopPlaying = true)
            word.examples.forEachIndexed { idx, ex ->
                _spartaState.value = _spartaState.value.copy(activeExampleIndex = idx)
                speakWithActiveTutor(ex.english)
                delay((ex.english.length * 75L).coerceIn(2800L, 6500L))
            }
            _spartaState.value = _spartaState.value.copy(isLoopPlaying = false)
        }
    }

    fun startSpartaShadowingSpeech() {
        stopLoopPlayback()
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.startListening(tutor.locale) { transcript, durationMs ->
            evaluateSpartaShadowing(transcript, durationMs)
        }
    }

    fun evaluateSpartaShadowing(spokenText: String, durationMs: Long = 3000L) {
        val st = _spartaState.value
        val word = st.currentWord ?: return
        val targetSentence = word.examples.getOrNull(st.activeExampleIndex)?.english ?: word.exampleEn
        val report = PronunciationAnalyzer.analyze(targetSentence, spokenText, durationMs)

        if (report.passed) {
            val updatedSpoken = st.spokenExampleIndices + st.activeExampleIndex
            _spartaState.value = st.copy(
                spokenExampleIndices = updatedSpoken,
                latestPronunciation = report,
                lastWasMiss = false,
                lastFeedbackKo = "예문 ${st.activeExampleIndex + 1} 발음 ${report.overallScore}점(${report.fluencyGrade}) 통과! 다른 예문도 연습하거나 다음 단어로 넘어갈 수 있어요!"
            )
            // Advance automatically once they clear a sentence (or if they click Next Word)
            advanceToNextSpartaWord(word, report)
        } else {
            _spartaState.value = st.copy(
                latestPronunciation = report,
                lastWasMiss = true,
                lastFeedbackKo = "발음 ${report.overallScore}점 (통과 기준 72점) — 빨간 단어를 원어민과 비교해서 다시 한 번!"
            )
        }
    }

    fun advanceToNextSpartaWord(word: VocabWord? = _spartaState.value.currentWord, report: PronunciationReport? = null) {
        val st = _spartaState.value
        val current = word ?: return
        timerJob?.cancel()
        val wasRetry = current.id in st.retryIds || current.id in weakWordIds.value
        val coinGain = 15 + (st.combo * 2).coerceAtMost(20)
        val xpGain = 20 + (st.combo * 3).coerceAtMost(30)
        val nextQueue = st.queue.drop(1)
        val newMastered = st.masteredInSession + 1
        val completed = nextQueue.isEmpty()
        val nextWord = nextQueue.firstOrNull()

        _spartaState.value = st.copy(
            queue = nextQueue,
            retryIds = st.retryIds - current.id,
            masteredInSession = newMastered,
            currentWord = nextWord,
            activeExampleIndex = 0,
            spokenExampleIndices = emptySet(),
            options = buildOptions(nextWord, combinedWordPool(st.grade)),
            phase = if (completed) SpartaPhase.COMPLETED else SpartaPhase.MEANING_QUIZ,
            timeLeftSec = st.maxTimeSec,
            sessionCoins = st.sessionCoins + coinGain,
            sessionXp = st.sessionXp + xpGain,
            lastWasMiss = false,
            latestPronunciation = report,
            lastFeedbackKo = if (completed) {
                "성벽 수호 대성공! 오늘 단어 미션 완료! +100 보너스 코인 지급!"
            } else {
                "'${current.word}' 정복 완료! +${coinGain} 코인 · 성채 수복 중!"
            }
        )

        viewModelScope.launch {
            localStore.markWordMastered(current.id)
            localStore.recordDailyWordOutcome(current.id, current.word, current.meaningKo, known = true)
            val result = localStore.recordLearningAchievement(
                xpDelta = xpGain + if (completed) 150 else 0,
                coinsDelta = coinGain + if (completed) 100 else 0,
                wordsDelta = 1,
                castleHpDelta = if (completed) 10 else 2,
                missionCompleted = (newMastered % 5 == 0) || completed,
                reviewDelta = if (wasRetry) 1 else 0,
                perfectDelta = if (completed && st.perfectStreakInSession) 1 else 0
            )
            notifySpecialMilestones(result)
        }
        if (!completed) {
            startSpartaCountdown()
        }
    }

    // ==================== 1~3 SENTENCE PATTERN WORD-SWAP & LOOP LISTENING STUDIO ====================

    fun selectPatternDrill(index: Int) {
        stopLoopPlayback()
        _patternState.value = _patternState.value.copy(
            activeDrillIndex = index.coerceIn(0, PatternSentenceRepository.drills.lastIndex),
            selectedSlotIndices = emptyMap(),
            latestPronunciation = null
        )
        speakPatternCurrent()
    }

    fun swapPatternSlotWord(slotKey: String, optionIndex: Int) {
        val updated = _patternState.value.selectedSlotIndices + (slotKey to optionIndex)
        _patternState.value = _patternState.value.copy(
            selectedSlotIndices = updated,
            latestPronunciation = null
        )
        speakPatternCurrent()
    }

    fun setPatternLoopMode(mode: LoopRepeatMode) {
        _patternState.value = _patternState.value.copy(loopMode = mode)
        showToast("패턴 반복 듣기: ${mode.labelKo}")
    }

    fun togglePatternKorean() {
        _patternState.value = _patternState.value.copy(showKorean = !_patternState.value.showKorean)
    }

    fun speakPatternCurrent() {
        val st = _patternState.value
        playTextWithLoopMode(st.renderedEnglish, st.loopMode, forPattern = true)
    }

    fun startPatternSpeechPractice() {
        stopLoopPlayback()
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.startListening(tutor.locale) { transcript, durationMs ->
            evaluatePatternSpeech(transcript, durationMs)
        }
    }

    fun evaluatePatternSpeech(spokenText: String, durationMs: Long = 3500L) {
        val st = _patternState.value
        val report = PronunciationAnalyzer.analyze(st.renderedEnglish, spokenText, durationMs)
        _patternState.value = st.copy(latestPronunciation = report)
        if (report.passed) {
            viewModelScope.launch {
                val (boss, lvlUp) = localStore.recordLearningAchievement(
                    xpDelta = 25,
                    coinsDelta = 20,
                    castleHpDelta = 4,
                    missionCompleted = true,
                    perfectDelta = if (report.overallScore >= 90) 1 else 0
                )
                notifySpecialMilestones(boss, lvlUp)
            }
            showToast("패턴 문장 발음 ${report.overallScore}점 통과! +20 코인 · 성채 수복 +4%")
        }
    }

    private fun playTextWithLoopMode(
        text: String,
        mode: LoopRepeatMode,
        forPattern: Boolean = false
    ) {
        stopLoopPlayback()
        if (mode == LoopRepeatMode.ONCE) {
            speakWithActiveTutor(text)
            return
        }
        loopJob = viewModelScope.launch {
            if (forPattern) {
                _patternState.value = _patternState.value.copy(isLoopPlaying = true)
            } else {
                _spartaState.value = _spartaState.value.copy(isLoopPlaying = true)
            }
            val totalLoops = if (mode == LoopRepeatMode.INFINITE) 25 else mode.count
            val gapMs = (text.length * 75L + 1100L).coerceIn(3000L, 7500L)
            repeat(totalLoops) {
                speakWithActiveTutor(text)
                delay(gapMs)
            }
            if (forPattern) {
                _patternState.value = _patternState.value.copy(isLoopPlaying = false)
            } else {
                _spartaState.value = _spartaState.value.copy(isLoopPlaying = false)
            }
        }
    }

    fun stopLoopPlayback() {
        loopJob?.cancel()
        loopJob = null
        _spartaState.value = _spartaState.value.copy(isLoopPlaying = false)
        _patternState.value = _patternState.value.copy(isLoopPlaying = false)
    }

    // ==================== REAL VIDEO-CALL AI TEACHER + VOICE SETTINGS CONTROL ====================

    fun selectHumanTutor(tutor: AiHumanTutor) {
        stopVoiceIo()
        initTutorConversation(tutor)
        viewModelScope.launch {
            localStore.updateSelectedTeacher(tutor.id)
        }
        speakWithActiveTutor(tutor.greetingEn)
    }

    private fun initTutorConversation(tutor: AiHumanTutor) {
        val defaultTopic = if (tutor.cityCountry.contains("Guam", ignoreCase = true)) "괌 바닷가" else "뉴욕 시내"
        val welcomeMsg = TutorChatMessage(
            id = UUID.randomUUID().toString(),
            fromTutor = true,
            textEn = tutor.greetingEn,
            translationKo = tutor.greetingKo,
            coachingTipKo = "${tutor.specialtyKo} · 영어로 수다 떨거나 'KR 질문'으로 한국어 표현·설정을 말로 지시해 보세요!",
            engineBadge = engineMode.value.labelKo
        )
        _tutorCallState.value = TutorCallState(
            selectedTutor = tutor,
            messages = listOf(welcomeMsg),
            showKoreanTranslations = _tutorCallState.value.showKoreanTranslations,
            liveSubtitleEn = tutor.greetingEn,
            liveSubtitleKo = tutor.greetingKo,
            liveCoachingCommentKo = "EN 말하기 · KR 질문/음성설정 · 단어 탭하면 느리게 듣기!",
            activeEngineBadgeKo = engineMode.value.labelKo,
            dynamicSuggestedReplies = HybridAiCoachEngine.defaultSuggestedRepliesForTopic(defaultTopic),
            activeTopicTag = defaultTopic,
            sceneLocationLabelKo = NanoBananaMotionEngine.topicToDisplayLocationKo(defaultTopic)
        )
    }

    fun toggleKoreanVoiceInputMode() {
        val next = !_tutorCallState.value.koreanVoiceInputMode
        _tutorCallState.value = _tutorCallState.value.copy(koreanVoiceInputMode = next)
        showToast(if (next) "한국어 질문·음성 설정 모드 ON (예: 마트에서 이거 빼고 현금으로...)" else "원어민 영어 회화 말하기 모드 ON")
    }

    fun toggleProactiveMode() {
        val next = !_tutorCallState.value.proactiveModeEnabled
        _tutorCallState.value = _tutorCallState.value.copy(
            proactiveModeEnabled = next,
            proactiveNudgeCount = 0
        )
        showToast(if (next) "능동 대화 ON · 상황에 맞춰 먼저 생각하고 말을 걸어요" else "능동 대화 OFF")
    }

    fun updateConversationGoal(newGoalKo: String) {
        val cleaned = newGoalKo.trim()
        if (cleaned.isBlank()) return
        _tutorCallState.value = _tutorCallState.value.copy(
            conversationGoalKo = cleaned.take(60),
            liveThoughtKo = "새 목표 '$cleaned'에 맞춰 대화를 이끌어 갈게요!"
        )
        showToast("대화 목표 변경: $cleaned")
    }

    fun setTutorConversationTopic(
        context: Context,
        topicKo: String,
        starterPromptEn: String
    ) {
        val tutor = _tutorCallState.value.selectedTutor
        val learnerName = parentProfiles.value.daughterName.ifBlank { "원삐" }
        val locationLabel = NanoBananaMotionEngine.topicToDisplayLocationKo(topicKo)
        val goalKo = HybridAiCoachEngine.goalForTopic(topicKo)
        val streamingId = UUID.randomUUID().toString()
        val placeholderMsg = TutorChatMessage(
            id = streamingId,
            fromTutor = true,
            textEn = "...",
            isStreaming = true
        )

        tutorReplyJob?.cancel()
        voiceEngine.stopSpeaking()
        _tutorCallState.value = _tutorCallState.value.copy(
            activeTopicTag = topicKo,
            sceneLocationLabelKo = locationLabel,
            conversationGoalKo = goalKo,
            liveThoughtKo = "${learnerName}님과 '$topicKo' 상황 대화를 능동적으로 구상하는 중...",
            aiMood = "thinking",
            isThinking = true,
            isGeneratingScene = false,
            sceneErrorKo = null,
            nanoBananaSceneBitmap = null,
            nanoBananaBlinkBitmap = null,
            proactiveNudgeCount = 0,
            messages = _tutorCallState.value.messages + placeholderMsg,
            liveGrammarFixKo = "",
            dynamicSuggestedReplies = HybridAiCoachEngine.defaultSuggestedRepliesForTopic(topicKo)
        )

        tutorReplyJob = viewModelScope.launch {
            val reply = try {
                aiCoachEngine.generateProactiveTopicTurn(
                    tutor = tutor,
                    learnerName = learnerName,
                    activeTopicTag = topicKo,
                    conversationGoalKo = goalKo,
                    history = _tutorCallState.value.messages.filterNot { it.id == streamingId },
                    mode = engineMode.value,
                    slots = llmSlots.value,
                    isOpening = true,
                    onStreamChunk = { partial ->
                        _tutorCallState.value = _tutorCallState.value.copy(
                            liveSubtitleEn = partial,
                            messages = _tutorCallState.value.messages.map { m ->
                                if (m.id == streamingId) m.copy(textEn = partial, isStreaming = true) else m
                            }
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                com.example.engine.LocalConversationSkillEngine.generateProactiveOpener(
                    tutor = tutor,
                    learnerName = learnerName,
                    activeTopicTag = topicKo,
                    conversationGoalKo = goalKo,
                    turnCount = _tutorCallState.value.turnsInCall,
                    isOpening = true
                )
            }

            val finalMsg = TutorChatMessage(
                id = streamingId,
                fromTutor = true,
                textEn = reply.replyEn.ifBlank { starterPromptEn },
                translationKo = reply.translationKo,
                coachingTipKo = reply.coachingTipKo,
                naturalRephraseEn = reply.naturalRephraseEn,
                engineBadge = reply.engineBadge,
                isStreaming = false
            )
            val curr = _tutorCallState.value
            _tutorCallState.value = curr.copy(
                isThinking = false,
                messages = curr.messages.map { m -> if (m.id == streamingId) finalMsg else m },
                liveSubtitleEn = finalMsg.textEn,
                liveSubtitleKo = reply.translationKo,
                liveCoachingCommentKo = reply.coachingTipKo,
                liveNaturalRephraseEn = reply.naturalRephraseEn,
                liveGrammarFixKo = reply.grammarFixKo,
                liveThoughtKo = reply.thoughtKo.ifBlank { "${learnerName}님과의 '$topicKo' 대화를 시작했어요!" },
                conversationGoalKo = reply.goalProgressKo.ifBlank { goalKo },
                aiMood = reply.characterMood,
                activeEngineBadgeKo = reply.engineBadge,
                dynamicSuggestedReplies = reply.suggestedReplies.ifEmpty {
                    HybridAiCoachEngine.defaultSuggestedRepliesForTopic(topicKo)
                }
            )
            speakWithActiveTutor(finalMsg.textEn) {
                if (handsFreeMode.value) {
                    startTutorLiveListening()
                }
            }
        }
    }

    fun triggerProactiveCheckIn(manual: Boolean = false) {
        val curr = _tutorCallState.value
        val vState = voiceEngine.state.value
        if (curr.isThinking || vState.isListening || vState.isSpeaking) return
        if (!manual && (!curr.proactiveModeEnabled || curr.proactiveNudgeCount >= 2)) return

        val tutor = curr.selectedTutor
        val learnerName = parentProfiles.value.daughterName.ifBlank { "원삐" }
        val streamingId = UUID.randomUUID().toString()
        val placeholderMsg = TutorChatMessage(
            id = streamingId,
            fromTutor = true,
            textEn = "...",
            isStreaming = true
        )

        tutorReplyJob?.cancel()
        _tutorCallState.value = curr.copy(
            isThinking = true,
            aiMood = "thinking",
            liveThoughtKo = "${learnerName}님께 '${curr.activeTopicTag}' 다음 질문을 능동적으로 떠올리는 중...",
            proactiveNudgeCount = if (manual) 0 else curr.proactiveNudgeCount + 1,
            messages = curr.messages + placeholderMsg
        )

        tutorReplyJob = viewModelScope.launch {
            val reply = try {
                aiCoachEngine.generateProactiveTopicTurn(
                    tutor = tutor,
                    learnerName = learnerName,
                    activeTopicTag = curr.activeTopicTag,
                    conversationGoalKo = curr.conversationGoalKo,
                    history = curr.messages,
                    mode = engineMode.value,
                    slots = llmSlots.value,
                    isOpening = false,
                    onStreamChunk = { partial ->
                        _tutorCallState.value = _tutorCallState.value.copy(
                            liveSubtitleEn = partial,
                            messages = _tutorCallState.value.messages.map { m ->
                                if (m.id == streamingId) m.copy(textEn = partial, isStreaming = true) else m
                            }
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                com.example.engine.LocalConversationSkillEngine.generateProactiveOpener(
                    tutor = tutor,
                    learnerName = learnerName,
                    activeTopicTag = curr.activeTopicTag,
                    conversationGoalKo = curr.conversationGoalKo,
                    turnCount = curr.turnsInCall,
                    isOpening = false
                )
            }

            val finalMsg = TutorChatMessage(
                id = streamingId,
                fromTutor = true,
                textEn = reply.replyEn,
                translationKo = reply.translationKo,
                coachingTipKo = reply.coachingTipKo,
                naturalRephraseEn = reply.naturalRephraseEn,
                engineBadge = reply.engineBadge,
                isStreaming = false
            )
            val latest = _tutorCallState.value
            _tutorCallState.value = latest.copy(
                isThinking = false,
                messages = latest.messages.map { m -> if (m.id == streamingId) finalMsg else m },
                liveSubtitleEn = reply.replyEn,
                liveSubtitleKo = reply.translationKo,
                liveCoachingCommentKo = reply.coachingTipKo,
                liveNaturalRephraseEn = reply.naturalRephraseEn,
                liveGrammarFixKo = reply.grammarFixKo,
                liveThoughtKo = reply.thoughtKo.ifBlank { "${learnerName}님에게 다음 대화 힌트를 건넸어요" },
                conversationGoalKo = reply.goalProgressKo.ifBlank { latest.conversationGoalKo },
                aiMood = reply.characterMood,
                activeEngineBadgeKo = reply.engineBadge,
                dynamicSuggestedReplies = reply.suggestedReplies.ifEmpty { latest.dynamicSuggestedReplies }
            )
            speakWithActiveTutor(reply.replyEn) {
                if (handsFreeMode.value) {
                    startTutorLiveListening()
                }
            }
        }
    }

    fun synthesizeTutorSceneWithNanoBanana(
        context: Context,
        topicTag: String = _tutorCallState.value.activeTopicTag,
        gesture: MotionPhotoGesture = MotionPhotoGesture.WARM_SMILE
    ) {
        val tutor = _tutorCallState.value.selectedTutor
        val locationLabel = NanoBananaMotionEngine.topicToDisplayLocationKo(topicTag)
        val cacheKey = nanoBananaEngine.buildCacheKey(tutor.id, topicTag, gesture)
        val cachedScene = nanoBananaEngine.getCachedScene(cacheKey)
        val cachedBlink = nanoBananaEngine.getCachedBlink(cacheKey)

        if (cachedScene != null) {
            _tutorCallState.value = _tutorCallState.value.copy(
                activeTopicTag = topicTag,
                sceneLocationLabelKo = locationLabel,
                nanoBananaSceneBitmap = cachedScene,
                nanoBananaBlinkBitmap = cachedBlink,
                isGeneratingScene = false,
                sceneErrorKo = null
            )
            return
        }

        sceneGenJob?.cancel()
        val appContext = context.applicationContext
        _tutorCallState.value = _tutorCallState.value.copy(
            activeTopicTag = topicTag,
            sceneLocationLabelKo = locationLabel,
            isGeneratingScene = true,
            sceneErrorKo = null
        )

        sceneGenJob = viewModelScope.launch {
            val result = nanoBananaEngine.synthesizeTopicSceneAndBlink(
                context = appContext,
                basePortraitRes = tutor.portraitRes,
                tutorId = tutor.id,
                tutorName = tutor.name,
                topicTag = topicTag,
                gesture = gesture,
                alsoSynthesizeBlink = true
            )
            if (result.sceneBitmap != null) {
                _tutorCallState.value = _tutorCallState.value.copy(
                    nanoBananaSceneBitmap = result.sceneBitmap,
                    nanoBananaBlinkBitmap = result.blinkBitmap,
                    isGeneratingScene = false,
                    sceneErrorKo = null
                )
            } else {
                _tutorCallState.value = _tutorCallState.value.copy(
                    isGeneratingScene = false,
                    sceneErrorKo = null
                )
                showToast(result.errorMessage ?: "HD 스튜디오 배경으로 자동 표시됩니다.")
            }
        }
    }

    fun toggleTutorKoreanTranslations() {
        _tutorCallState.value = _tutorCallState.value.copy(
            showKoreanTranslations = !_tutorCallState.value.showKoreanTranslations
        )
    }

    fun startTutorLiveListening() {
        stopLoopPlayback()
        val st = _tutorCallState.value
        val listenLocale = if (st.koreanVoiceInputMode) Locale.KOREA else st.selectedTutor.locale
        voiceEngine.startListening(listenLocale) { transcript, durationMs ->
            sendUserMessageToTutor(transcript, durationMs)
        }
    }

    fun clearVoiceErrorNotice() {
        voiceEngine.clearErrorNotice()
    }

    fun stopVoiceIo() {
        stopLoopPlayback()
        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
    }

    fun startTutorShadowingDrill(targetSentence: String) {
        val clean = targetSentence.trim()
        if (clean.isBlank()) return
        stopLoopPlayback()
        _tutorCallState.value = _tutorCallState.value.copy(
            shadowingTargetEn = clean,
            koreanVoiceInputMode = false,
            liveCoachingCommentKo = "따라 말하기 집중 모드! 아래 문장을 원어민 리듬으로 말해 보세요."
        )
        speakWithActiveTutor(clean) {
            startTutorLiveListening()
        }
    }

    fun cancelTutorShadowingDrill() {
        _tutorCallState.value = _tutorCallState.value.copy(
            shadowingTargetEn = null
        )
    }

    fun sendUserMessageToTutor(userText: String, durationMs: Long = 3200L) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        // 1. Intercept hands-free Voice Settings Commands first!
        val voiceCmd = VoiceCommandParser.parse(trimmed)
        if (voiceCmd != null) {
            executeVoiceSettingCommand(voiceCmd)
            return
        }

        val st = _tutorCallState.value
        val tutor = st.selectedTutor

        // 1.5 If active Shadowing Drill target is set, grade pronunciation strictly against that target!
        val activeShadowTarget = st.shadowingTargetEn
        if (!activeShadowTarget.isNullOrBlank()) {
            val shadowReport = PronunciationAnalyzer.analyze(activeShadowTarget, trimmed, durationMs)
            val passed = shadowReport.overallScore >= 75
            val updatedScores = st.pronunciationScoresInCall + shadowReport.overallScore
            _tutorCallState.value = st.copy(
                latestPronunciation = shadowReport,
                lastUserSpokenEn = trimmed,
                shadowingTargetEn = if (passed) null else activeShadowTarget,
                pronunciationScoresInCall = updatedScores,
                liveCoachingCommentKo = if (passed) {
                    "섀도잉 ${shadowReport.overallScore}점 (${shadowReport.fluencyGrade}) 완벽 통과! 이제 자유롭게 대화를 이어가세요!"
                } else {
                    "섀도잉 ${shadowReport.overallScore}점! 빨간/노란 단어를 탭해 느리게 듣고 한 번 더 도전해 보세요."
                }
            )
            if (passed) {
                viewModelScope.launch {
                    val (boss, lvlUp) = localStore.recordLearningAchievement(
                        xpDelta = 18,
                        coinsDelta = 15,
                        castleHpDelta = 3,
                        missionCompleted = false
                    )
                    notifySpecialMilestones(boss, lvlUp)
                }
                showToast("따라 말하기 ${shadowReport.overallScore}점 통과! +15 코인")
            } else {
                showToast("섀도잉 ${shadowReport.overallScore}점 · 틀린 단어를 탭해 다시 들어보세요!")
            }
            return
        }

        val isEnglishSpeech = trimmed.any { it in 'a'..'z' || it in 'A'..'Z' }
        val selfScore = if (isEnglishSpeech) PronunciationAnalyzer.analyze(trimmed, trimmed, durationMs).overallScore else null

        val userMsg = TutorChatMessage(
            id = UUID.randomUUID().toString(),
            fromTutor = false,
            textEn = trimmed,
            pronunciationScore = selfScore
        )
        val streamingId = UUID.randomUUID().toString()
        val streamingPlaceholder = TutorChatMessage(
            id = streamingId,
            fromTutor = true,
            textEn = "...",
            isStreaming = true
        )

        val learnerName = parentProfiles.value.daughterName.ifBlank { "원삐" }
        voiceEngine.clearErrorNotice()
        tutorReplyJob?.cancel()
        _tutorCallState.value = st.copy(
            messages = st.messages + userMsg + streamingPlaceholder,
            isThinking = true,
            aiMood = "thinking",
            liveThoughtKo = "${learnerName}님의 말을 이해하고 '${st.activeTopicTag}' 목표에 맞춰 답을 생각하는 중...",
            proactiveNudgeCount = 0,
            liveVoiceCommandNotice = "",
            lastUserSpokenEn = trimmed
        )

        tutorReplyJob = viewModelScope.launch {
            val reply = try {
                aiCoachEngine.generateFriendReply(
                    tutor = tutor,
                    userSpokenText = trimmed,
                    history = st.messages + userMsg,
                    mode = engineMode.value,
                    slots = llmSlots.value,
                    activeTopicTag = st.activeTopicTag,
                    learnerName = learnerName,
                    conversationGoalKo = st.conversationGoalKo,
                    onStreamChunk = { partial ->
                        _tutorCallState.value = _tutorCallState.value.copy(
                            liveSubtitleEn = partial,
                            messages = _tutorCallState.value.messages.map { m ->
                                if (m.id == streamingId) m.copy(textEn = partial, isStreaming = true) else m
                            }
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                com.example.engine.LocalConversationSkillEngine.generateReply(
                    tutor = tutor,
                    userText = trimmed,
                    history = st.messages + userMsg,
                    activeTopicTag = st.activeTopicTag,
                    activeTwist = null,
                    learnerName = learnerName,
                    conversationGoalKo = st.conversationGoalKo
                )
            }

            val coachingReport = if (isEnglishSpeech) {
                PronunciationAnalyzer.analyze(reply.naturalRephraseEn, trimmed, durationMs)
            } else {
                null
            }

            val finalTutorMsg = TutorChatMessage(
                id = streamingId,
                fromTutor = true,
                textEn = reply.replyEn,
                translationKo = reply.translationKo,
                coachingTipKo = reply.coachingTipKo,
                naturalRephraseEn = reply.naturalRephraseEn,
                engineBadge = reply.engineBadge,
                isStreaming = false
            )

            val currentCall = _tutorCallState.value
            val nextTurns = currentCall.turnsInCall + 1
            val updatedUpgrades = if (reply.naturalRephraseEn.isNotBlank()) {
                (listOf(trimmed to reply.naturalRephraseEn) + currentCall.collectedUpgrades)
                    .distinctBy { it.second }
                    .take(15)
            } else {
                currentCall.collectedUpgrades
            }
            val updatedScores = if (coachingReport != null) {
                currentCall.pronunciationScoresInCall + coachingReport.overallScore
            } else {
                currentCall.pronunciationScoresInCall
            }

            _tutorCallState.value = currentCall.copy(
                messages = currentCall.messages.map { m ->
                    if (m.id == streamingId) finalTutorMsg else m
                },
                isThinking = false,
                latestPronunciation = coachingReport,
                liveSubtitleEn = reply.replyEn,
                liveSubtitleKo = reply.translationKo,
                liveCoachingCommentKo = reply.coachingTipKo,
                liveNaturalRephraseEn = reply.naturalRephraseEn,
                liveGrammarFixKo = reply.grammarFixKo,
                liveThoughtKo = reply.thoughtKo.ifBlank { "${learnerName}님과 '${currentCall.activeTopicTag}' 대화를 이어가는 중" },
                conversationGoalKo = reply.goalProgressKo.ifBlank { currentCall.conversationGoalKo },
                aiMood = reply.characterMood,
                activeEngineBadgeKo = reply.engineBadge,
                dynamicSuggestedReplies = reply.suggestedReplies.ifEmpty {
                    HybridAiCoachEngine.defaultSuggestedRepliesForTopic(currentCall.activeTopicTag)
                },
                collectedUpgrades = updatedUpgrades,
                pronunciationScoresInCall = updatedScores,
                turnsInCall = nextTurns
            )

            speakWithActiveTutor(reply.replyEn) {
                if (handsFreeMode.value) {
                    startTutorLiveListening()
                }
            }

            val (boss, lvlUp) = localStore.recordLearningAchievement(
                xpDelta = 15,
                coinsDelta = 12,
                castleHpDelta = 2,
                missionCompleted = (nextTurns % 5 == 0)
            )
            notifySpecialMilestones(boss, lvlUp)
        }
    }

    private fun executeVoiceSettingCommand(cmd: VoiceSettingCommand) {
        val replyKo = when (cmd) {
            is VoiceSettingCommand.SetSpeechRate -> {
                setSpeechRateMultiplier(cmd.rate)
                cmd.replyKo
            }
            is VoiceSettingCommand.SetSubtitlesVisible -> {
                _tutorCallState.value = _tutorCallState.value.copy(showKoreanTranslations = cmd.visible)
                cmd.replyKo
            }
            is VoiceSettingCommand.SetHandsFree -> {
                setHandsFreeMode(cmd.enabled)
                cmd.replyKo
            }
            is VoiceSettingCommand.SetLoopMode -> {
                setVocabLoopMode(cmd.mode)
                setPatternLoopMode(cmd.mode)
                cmd.replyKo
            }
            is VoiceSettingCommand.SwitchTutor -> {
                selectHumanTutor(cmd.tutor)
                cmd.replyKo
            }
            is VoiceSettingCommand.SwitchGrade -> {
                startSmartVocabSession(grade = cmd.grade)
                cmd.replyKo
            }
            is VoiceSettingCommand.SwitchVocabTrack -> {
                startSmartVocabSession(track = cmd.track)
                cmd.replyKo
            }
        }
        _tutorCallState.value = _tutorCallState.value.copy(
            liveVoiceCommandNotice = "음성 설정 반영: $replyKo",
            liveSubtitleKo = replyKo
        )
        voiceEngine.speak(replyKo, Locale.KOREA, pitch = 1.05f, rate = 1.05f)
        showToast("음성 명령 완료: $replyKo")
    }

    fun speakWithActiveTutor(text: String, onDone: (() -> Unit)? = null) {
        val tutor = _tutorCallState.value.selectedTutor
        val rateMultiplier = speechRate.value
        voiceEngine.speak(
            text = text,
            locale = tutor.locale,
            pitch = tutor.ttsPitch,
            rate = (tutor.ttsRate * rateMultiplier).coerceIn(0.55f, 1.4f),
            onDone = onDone
        )
    }

    fun speakSlowWord(word: String) {
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.speak(
            text = word,
            locale = tutor.locale,
            pitch = tutor.ttsPitch,
            rate = 0.78f
        )
    }

    // ==================== TRAVEL 100 SCENARIOS ====================

    fun selectTravelZone(zoneId: Int) {
        val firstInZone = TravelCurriculumRepository.scenariosForZone(zoneId).firstOrNull()
            ?: TravelCurriculumRepository.allScenarios.first()
        travelProactiveJob?.cancel()
        _travelState.value = _travelState.value.copy(
            selectedZoneId = zoneId,
            activeScenario = firstInZone,
            activeSentenceIndex = 0,
            completedIndices = emptySet(),
            activeTwistIndex = 0,
            latestPronunciation = null,
            twistReplyText = "",
            twistReplyKo = "",
            twistCoachTipKo = "",
            proactiveLineEn = "",
            proactiveLineKo = "",
            proactiveThoughtKo = "",
            proactiveMood = "happy",
            proactiveLoading = false
        )
    }

    fun selectTravelScenario(scenario: TravelScenario) {
        travelProactiveJob?.cancel()
        _travelState.value = _travelState.value.copy(
            selectedZoneId = scenario.zoneId,
            activeScenario = scenario,
            activeSentenceIndex = 0,
            completedIndices = emptySet(),
            activeTwistIndex = 0,
            latestPronunciation = null,
            twistReplyText = "",
            twistReplyKo = "",
            twistCoachTipKo = "",
            proactiveLineEn = "",
            proactiveLineKo = "",
            proactiveThoughtKo = "",
            proactiveMood = "happy",
            proactiveLoading = false
        )
    }

    fun triggerTravelProactiveConversation(isOpening: Boolean = false) {
        val st = _travelState.value
        val tutor = _tutorCallState.value.selectedTutor
        val learnerName = parentProfiles.value.daughterName.ifBlank { "원삐" }
        val streetLoc = com.example.ui.components.TravelStreetDirectory.locationForZone(st.selectedZoneId)
        val topicLabel = "${streetLoc.placeNameKo} · ${st.activeScenario.titleKo}"
        val goalKo = "${streetLoc.cityBadgeEn} 실제 거리에서 '${st.activeScenario.titleKo}' 상황 영어로 자연스럽게 해결하기"

        travelProactiveJob?.cancel()
        _travelState.value = st.copy(proactiveLoading = true)

        travelProactiveJob = viewModelScope.launch {
            val reply = try {
                aiCoachEngine.generateProactiveTopicTurn(
                    tutor = tutor,
                    learnerName = learnerName,
                    activeTopicTag = topicLabel,
                    conversationGoalKo = goalKo,
                    history = emptyList(),
                    mode = engineMode.value,
                    slots = llmSlots.value,
                    isOpening = isOpening
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                com.example.engine.AiCoachReply(
                    replyEn = "Hey $learnerName! We're right here at ${streetLoc.cityBadgeEn}. Want to practice '${st.activeScenario.titleKo}' together?",
                    translationKo = "${learnerName}님! 지금 ${streetLoc.placeNameKo}에 와 있어요. '${st.activeScenario.titleKo}' 상황 같이 말해볼까요?",
                    coachingTipKo = "실제 거리를 둘러보며 첫 문장부터 자신 있게 말해보세요.",
                    naturalRephraseEn = "",
                    engineBadge = "현지 오프라인 코치",
                    thoughtKo = "${streetLoc.placeNameKo} 현지 거리에 맞춰 ${learnerName}님에게 자연스럽게 첫 대화를 건넵니다.",
                    characterMood = "excited"
                )
            }

            _travelState.value = _travelState.value.copy(
                proactiveLoading = false,
                proactiveLineEn = reply.replyEn,
                proactiveLineKo = reply.translationKo,
                proactiveThoughtKo = reply.thoughtKo,
                proactiveMood = reply.characterMood.ifBlank { "happy" },
                proactiveEngineBadge = reply.engineBadge
            )
            speakWithActiveTutor(reply.replyEn)
        }
    }

    fun replayTravelProactiveLine() {
        val st = _travelState.value
        val learnerName = parentProfiles.value.daughterName.ifBlank { "원삐" }
        val loc = com.example.ui.components.TravelStreetDirectory.locationForZone(st.selectedZoneId)
        val text = st.proactiveLineEn.ifBlank {
            "Hey $learnerName! Welcome to ${loc.cityBadgeEn}. Ready to practice ${st.activeScenario.titleKo}?"
        }
        speakWithActiveTutor(text)
    }

    fun selectTravelSentence(index: Int) {
        val sc = _travelState.value.activeScenario
        val clamped = index.coerceIn(0, sc.sentences.lastIndex)
        _travelState.value = _travelState.value.copy(activeSentenceIndex = clamped, latestPronunciation = null)
        speakWithActiveTutor(sc.sentences[clamped].english)
    }

    fun toggleTravelKorean() {
        _travelState.value = _travelState.value.copy(showKoreanSubtitles = !_travelState.value.showKoreanSubtitles)
    }

    fun listenActiveTravelSentence() {
        val st = _travelState.value
        val sentence = st.activeScenario.sentences.getOrNull(st.activeSentenceIndex) ?: return
        speakWithActiveTutor(sentence.english)
    }

    fun startTravelSentenceSpeech() {
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.startListening(tutor.locale) { transcript, durationMs ->
            evaluateTravelSentence(transcript, durationMs)
        }
    }

    fun evaluateTravelSentence(spokenText: String, durationMs: Long = 3200L) {
        val st = _travelState.value
        val sentence = st.activeScenario.sentences.getOrNull(st.activeSentenceIndex) ?: return
        val report = PronunciationAnalyzer.analyze(sentence.english, spokenText, durationMs)
        val updatedCompleted = if (report.passed) st.completedIndices + st.activeSentenceIndex else st.completedIndices
        _travelState.value = st.copy(completedIndices = updatedCompleted, latestPronunciation = report)

        if (report.passed) {
            viewModelScope.launch {
                val (boss, lvlUp) = localStore.recordLearningAchievement(
                    xpDelta = 15,
                    coinsDelta = 10,
                    castleHpDelta = 2,
                    missionCompleted = (updatedCompleted.size % 5 == 0)
                )
                notifySpecialMilestones(boss, lvlUp)
            }
            showToast("문장 #${sentence.index} 발음 ${report.overallScore}점 통과! +10 코인")
        }
    }

    fun selectTravelTwist(twistIdx: Int) {
        val sc = _travelState.value.activeScenario
        val clamped = twistIdx.coerceIn(0, sc.twists.lastIndex)
        _travelState.value = _travelState.value.copy(
            activeTwistIndex = clamped,
            twistReplyText = "",
            twistReplyKo = "",
            twistCoachTipKo = ""
        )
        speakWithActiveTutor(sc.twists[clamped].staffPromptEn)
    }

    fun playActiveTwistPrompt() {
        val st = _travelState.value
        val twist = st.activeScenario.twists.getOrNull(st.activeTwistIndex) ?: return
        speakWithActiveTutor(twist.staffPromptEn)
    }

    fun startTwistVoiceResponse() {
        val tutor = _tutorCallState.value.selectedTutor
        voiceEngine.startListening(tutor.locale) { transcript, durationMs ->
            submitTwistResponse(transcript, durationMs)
        }
    }

    fun submitTwistResponse(userText: String, durationMs: Long = 3500L) {
        if (userText.isBlank()) return
        val st = _travelState.value
        val twist: TravelTwist = st.activeScenario.twists.getOrNull(st.activeTwistIndex) ?: return
        val tutor = _tutorCallState.value.selectedTutor
        val refPhrase = twist.keyPhrases.firstOrNull() ?: userText
        val pronReport = PronunciationAnalyzer.analyze(refPhrase, userText, durationMs)

        twistReplyJob?.cancel()
        _travelState.value = st.copy(twistLoading = true, latestPronunciation = pronReport)

        twistReplyJob = viewModelScope.launch {
            val learnerName = parentProfiles.value.daughterName.ifBlank { "원삐" }
            val reply = try {
                aiCoachEngine.generateFriendReply(
                    tutor = tutor,
                    userSpokenText = userText,
                    history = emptyList(),
                    mode = engineMode.value,
                    slots = llmSlots.value,
                    activeTwist = twist,
                    learnerName = learnerName
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                com.example.engine.LocalConversationSkillEngine.generateReply(
                    tutor = tutor,
                    userText = userText,
                    history = emptyList(),
                    activeTopicTag = st.activeScenario.titleKo,
                    activeTwist = twist
                )
            }
            _travelState.value = _travelState.value.copy(
                twistLoading = false,
                twistReplyText = reply.replyEn,
                twistReplyKo = reply.translationKo,
                twistCoachTipKo = reply.coachingTipKo,
                twistEngineBadge = reply.engineBadge,
                proactiveLineEn = reply.replyEn,
                proactiveLineKo = reply.translationKo,
                proactiveThoughtKo = reply.thoughtKo,
                proactiveMood = reply.characterMood.ifBlank { "happy" },
                proactiveEngineBadge = reply.engineBadge
            )
            speakWithActiveTutor(reply.replyEn)
            localStore.markScenarioCleared(st.activeScenario.id)
            val (boss, lvlUp) = localStore.recordLearningAchievement(
                xpDelta = st.activeScenario.xpReward,
                coinsDelta = st.activeScenario.coinReward,
                castleHpDelta = 6,
                missionCompleted = true
            )
            notifySpecialMilestones(boss, lvlUp)
            showToast("왕국 원정 돌발 변수 클리어! +${st.activeScenario.coinReward} 코인")
        }
    }

    // ==================== PARENTS (DAD/MOM), GIFT REQUESTS, ALLOWANCE, SCORECARD, SCHEDULE, KAKAO, CLOUDFLARE ====================

    fun saveParentProfiles(profiles: ParentProfiles) {
        val rawPin = profiles.parentPin.trim()
        if (rawPin.isNotBlank()) {
            if (!com.example.engine.ParentPinCrypto.isValidPinFormat(rawPin)) {
                showToast("부모님 PIN은 숫자 4~6자리로 입력해 주세요.")
                return
            }
            if (com.example.engine.ParentPinCrypto.isWeakPin(rawPin)) {
                showToast("1234, 0000 등 추측하기 쉬운 번호는 사용할 수 없어요. 다른 PIN을 입력해 주세요.")
                return
            }
        }
        viewModelScope.launch {
            localStore.saveParentProfiles(profiles)
            val pinNotice = if (rawPin.isNotBlank()) " · PIN 암호화 저장 완료" else ""
            showToast("가족 왕국 구성원(${profiles.dadName} · ${profiles.momName} · ${profiles.daughterName}) 저장 완료$pinNotice!")
        }
    }

    fun createGiftRequest(targetParent: String, giftTitleKo: String, reasonKo: String) {
        if (giftTitleKo.isBlank()) return
        viewModelScope.launch {
            val newItem = GiftRequestItem(
                id = UUID.randomUUID().toString(),
                targetParent = targetParent,
                giftTitleKo = giftTitleKo.trim(),
                reasonKo = reasonKo.trim().ifBlank { "열심히 공부해서 성을 구했어요!" },
                requiredLevel = kingdomStats.value.princessLevel,
                isApproved = false,
                createdDateLabel = todayIso()
            )
            localStore.saveGiftRequests(listOf(newItem) + giftRequests.value, consumeTicket = true)
            showToast("${targetParent}에게 '${newItem.giftTitleKo}' 선물 요청을 등록했어요! 카톡으로도 바로 보내보세요!")
        }
    }

    fun selectActivePlayer(playerName: String) {
        viewModelScope.launch {
            localStore.updateActivePlayerName(playerName)
            showToast("현재 애칭: ${playerName}")
        }
    }

    fun generateAndShareGuestInvite(context: Context) {
        viewModelScope.launch {
            val existingCode = guestInviteSession.value.myGeneratedCode
            val code = existingCode.ifBlank { KakaoGuestInviteService.generateInviteCode() }
            localStore.saveGeneratedInviteCode(code)
            val host = parentProfiles.value.daughterName.ifBlank { "원삐" }
            KakaoGuestInviteService.shareGuestInviteViaKakaoTalk(
                context = context,
                code = code,
                hostNickname = host,
                cloudflareWorkerUrl = cloudflareWorkerUrl.value
            )
            showToast("손님 초대코드 [$code] 생성 및 카톡 독립 실행 링크 전송!")
        }
    }

    fun regenerateGuestInviteCode() {
        viewModelScope.launch {
            val code = KakaoGuestInviteService.generateInviteCode()
            localStore.saveGeneratedInviteCode(code)
            showToast("새 손님 초대코드 발급: $code")
        }
    }

    fun acceptGuestInviteCode(rawCode: String, hostNickname: String = "원삐", fromDeepLink: Boolean = false) {
        val clean = KakaoGuestInviteService.normalizeCode(rawCode)
        if (clean.length < 4) {
            showToast("올바른 초대코드를 입력해 주세요 (예: WP-7K2A)")
            return
        }
        viewModelScope.launch {
            localStore.activateGuestByInviteCode(clean, hostNickname)
            showToast(
                if (fromDeepLink) "초대코드 [$clean]로 독립 앱 입장 완료! 별도 로그인 없이 바로 시작!"
                else "손님($clean) 입장 완료! 원하면 내 카톡과 바로 연동할 수 있어요."
            )
        }
    }

    fun approveGiftRequestWithPin(
        giftId: String,
        enteredPin: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (!parentProfiles.value.isPinConfigured) {
            showToast("먼저 '가족' 탭에서 부모님 PIN(4~6자리)을 설정해 주세요.")
            onResult(false)
            return
        }
        viewModelScope.launch {
            when (val result = localStore.verifyParentPinWithLockout(enteredPin)) {
                is com.example.engine.PinVerificationResult.Verified -> {
                    val updated = giftRequests.value.map { g ->
                        if (g.id == giftId) g.copy(isApproved = true) else g
                    }
                    localStore.saveGiftRequests(updated, consumeTicket = false)
                    showToast("왕실 선물 약속 승인 기록 완료! ${parentProfiles.value.daughterName} 최고예요!")
                    onResult(true)
                }
                is com.example.engine.PinVerificationResult.LockedOut -> {
                    showToast("PIN 입력 오류 초과로 ${result.retryAfterSeconds}초 동안 잠겼습니다. 잠시 후 다시 시도해 주세요.")
                    onResult(false)
                }
                is com.example.engine.PinVerificationResult.Invalid -> {
                    showToast("부모님 PIN이 일치하지 않아요. (남은 시도 ${result.remainingAttempts}회)")
                    onResult(false)
                }
                is com.example.engine.PinVerificationResult.NotConfigured -> {
                    showToast("먼저 '가족' 탭에서 부모님 PIN(4~6자리)을 설정해 주세요.")
                    onResult(false)
                }
            }
        }
    }

    fun shareGiftRequestToKakao(context: Context, gift: GiftRequestItem) {
        val parents = parentProfiles.value
        val stats = kingdomStats.value
        val parentCall = if (gift.targetParent == "아빠") parents.dadName else parents.momName
        val message = """
            [아빠가딸들을 위한 학습도구 — 왕실 선물 요청장]
            받는 분: $parentCall
            보내는 분: ${parents.daughterName} (Lv.${stats.princessLevel} · 성채 수복 ${stats.castleHpPercent}%)
            
            요청 선물: ${gift.giftTitleKo}
            사유: ${gift.reasonKo}
            누적 학습: 예습 ${stats.previewCount}회 · 복습 ${stats.reviewCount}회 · 만점 ${stats.perfectScoreCount}회 · 보스전 ${stats.bossVictories}승!
            
            앱에서 부모님 PIN으로 승인해 주세요!
        """.trimIndent()
        KakaoAuthAndFriendsService.shareKingdomReportViaKakaoTalk(context, message)
    }

    fun addDadAllowanceMission(titleKo: String, descKo: String, rewardWon: Int) {
        if (titleKo.isBlank()) return
        viewModelScope.launch {
            val newMission = DadAllowanceMission(
                id = UUID.randomUUID().toString(),
                titleKo = titleKo.trim(),
                descriptionKo = descKo.trim().ifBlank { "성을 지키기 위한 아빠의 특별 용돈 미션!" },
                rewardWon = rewardWon.coerceIn(500, 100_000),
                castleHpBonus = 12,
                categoryKo = "아빠 직접 미션"
            )
            localStore.saveDadMissions(listOf(newMission) + dadMissions.value)
            showToast("아빠의 새 용돈 미션(+${rewardWon}원) 등록 완료!")
        }
    }

    fun markMissionDoneByDaughter(missionId: String) {
        viewModelScope.launch {
            val updated = dadMissions.value.map { m ->
                if (m.id == missionId) m.copy(isCompletedByDaughter = true) else m
            }
            localStore.saveDadMissions(updated)
            showToast("완료 보고 올렸어요! 부모님 PIN 승인을 받으면 가족 용돈 약속이 기록됩니다.")
        }
    }

    fun approveMissionWithPin(
        missionId: String,
        enteredPin: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (!parentProfiles.value.isPinConfigured) {
            showToast("먼저 '가족' 탭에서 부모님 PIN(4~6자리)을 설정해 주세요.")
            onResult(false)
            return
        }
        val target = dadMissions.value.firstOrNull { it.id == missionId }
        if (target == null) {
            onResult(false)
            return
        }
        if (target.isApprovedByDad) {
            onResult(true)
            return
        }
        viewModelScope.launch {
            when (val result = localStore.verifyParentPinWithLockout(enteredPin)) {
                is com.example.engine.PinVerificationResult.Verified -> {
                    val updated = dadMissions.value.map { m ->
                        if (m.id == missionId) m.copy(isCompletedByDaughter = true, isApprovedByDad = true) else m
                    }
                    localStore.saveDadMissions(updated, allowanceDeltaWon = target.rewardWon, castleBonus = target.castleHpBonus)
                    val (boss, lvlUp) = localStore.recordLearningAchievement(
                        xpDelta = 60,
                        coinsDelta = 50,
                        castleHpDelta = target.castleHpBonus,
                        missionCompleted = true
                    )
                    notifySpecialMilestones(boss, lvlUp)
                    showToast("승인 완료! 가족 용돈 약속 +${target.rewardWon}원 기록 (앱 내 송금 없음) & 성채 HP +${target.castleHpBonus}%!")
                    onResult(true)
                }
                is com.example.engine.PinVerificationResult.LockedOut -> {
                    showToast("PIN 입력 오류 초과로 ${result.retryAfterSeconds}초 동안 잠겼습니다. 잠시 후 다시 시도해 주세요.")
                    onResult(false)
                }
                is com.example.engine.PinVerificationResult.Invalid -> {
                    showToast("부모님 PIN이 일치하지 않아요. (남은 시도 ${result.remainingAttempts}회)")
                    onResult(false)
                }
                is com.example.engine.PinVerificationResult.NotConfigured -> {
                    showToast("먼저 '가족' 탭에서 부모님 PIN(4~6자리)을 설정해 주세요.")
                    onResult(false)
                }
            }
        }
    }

    /**
     * Google Play Account & Data Deletion compliance flow:
     * 1) Deletes user's Firestore familyStreaks entries
     * 2) Deletes user's Firestore /users/{uid} profile and subcollections
     * 3) Clears all local DataStore preferences & saved PIN/settings
     * 4) Deletes (or signs out if re-auth required) the Firebase Auth account
     */
    fun deleteAccountAndAllData(
        context: Context,
        onFinished: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val kakaoId = kakaoSession.value.kakaoUserId
            val streakDel = runCatching {
                familyLeaderboardRepository?.deleteOwnFamilyStreakEntries(kakaoId)
            }
            val cloudDel = runCatching {
                com.example.data.UserProgressRepository(context).deleteAllUserCloudData()
            }

            // Always wipe all on-device DataStore records regardless of network state
            localStore.clearAllLocalData()
            llmSlotStore.clearAll()

            var authDeleteNote = ""
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                val authDel = runCatching {
                    currentUser.delete().await()
                }
                if (authDel.isFailure) {
                    authDeleteNote = " (보안상 최근 로그인 확인이 필요해 로그아웃 처리되었습니다. 다시 로그인 후 계정 삭제를 완료할 수 있습니다.)"
                }
                runCatching { Firebase.auth.signOut() }
            }

            _firebaseUserUid.value = null
            _firebaseDisplayName.value = null
            _familyLeaderboardState.value = FamilyLeaderboardUiState.SignedOut

            val cloudOk = streakDel.isSuccess && cloudDel.isSuccess
            val msg = if (cloudOk) {
                "모든 학습 기록과 클라우드·기기 데이터가 안전하게 삭제되었습니다.$authDeleteNote"
            } else {
                "기기 데이터 삭제 및 로그아웃이 완료되었습니다. 네트워크 확인 후 클라우드 동기화 상태를 다시 확인해 주세요.$authDeleteNote"
            }
            showToast(msg)
            onFinished(cloudOk, msg)
        }
    }

    fun addScorecardEntry(examTitleKo: String, subjectKo: String, score: Int, targetScore: Int) {
        if (examTitleKo.isBlank()) return
        viewModelScope.launch {
            val entry = ScorecardEntry(
                id = UUID.randomUUID().toString(),
                examTitleKo = examTitleKo.trim(),
                subjectKo = subjectKo.trim().ifBlank { "고등 영어" },
                score = score.coerceIn(0, 100),
                targetScore = targetScore.coerceIn(1, 100),
                dateLabel = todayIso()
            )
            localStore.saveScorecards(listOf(entry) + scorecards.value)
            val hitGoal = entry.score >= entry.targetScore
            if (hitGoal) {
                val (boss, lvlUp) = localStore.recordLearningAchievement(
                    xpDelta = 80,
                    coinsDelta = 60,
                    castleHpDelta = 10,
                    missionCompleted = true,
                    perfectDelta = 1
                )
                notifySpecialMilestones(boss, lvlUp)
            }
            showToast(if (hitGoal) "목표 점수 돌파!! 만점 보너스 +60 코인!" else "점수표에 '${entry.examTitleKo}' 기록 완료!")
        }
    }

    fun addStudySchedule(dayKo: String, timeLabel: String, titleKo: String, detailKo: String) {
        if (titleKo.isBlank()) return
        viewModelScope.launch {
            val item = StudyScheduleItem(
                id = UUID.randomUUID().toString(),
                dayLabelKo = dayKo.trim().ifBlank { "매일" },
                timeLabel = timeLabel.trim().ifBlank { "오후 8:00" },
                titleKo = titleKo.trim(),
                detailKo = detailKo.trim().ifBlank { "성벽 수호 학습" }
            )
            localStore.saveSchedules(schedules.value + item)
            showToast("학습 일정이 추가되었어요!")
        }
    }

    fun toggleStudyScheduleCompleted(id: String) {
        viewModelScope.launch {
            val target = schedules.value.firstOrNull { it.id == id } ?: return@launch
            val nextDone = !target.isCompleted
            val updated = schedules.value.map { s ->
                if (s.id == id) s.copy(isCompleted = nextDone) else s
            }
            localStore.saveSchedules(updated)
            if (nextDone) {
                val (boss, lvlUp) = localStore.recordLearningAchievement(
                    xpDelta = 25,
                    coinsDelta = 20,
                    castleHpDelta = 5,
                    missionCompleted = true
                )
                notifySpecialMilestones(boss, lvlUp)
                showToast("일정 완료! +20 코인 · 성채 수복 +5%")
            }
        }
    }

    // ==================== KAKAO LOGIN, FRIENDS & CLOUDFLARE LEVEL REGISTRATION ====================

    fun launchKakaoLogin(context: Context) {
        val res = KakaoAuthAndFriendsService.launchKakaoOAuthLogin(context)
        res.onFailure { err ->
            showToast(err.message ?: "카카오 앱 키를 확인해 주세요.")
        }
    }

    fun onFirebaseAuthStateChanged(uid: String?, displayName: String?) {
        _firebaseUserUid.value = uid
        _firebaseDisplayName.value = displayName
        if (uid.isNullOrBlank()) {
            familyLeaderboardSyncJob?.cancel()
            _familyLeaderboardState.value = FamilyLeaderboardUiState.SignedOut
        } else {
            refreshFamilyLeaderboard(manual = false)
        }
    }

    fun refreshFamilyLeaderboard(manual: Boolean = false) {
        val repo = familyLeaderboardRepository
        val uid = _firebaseUserUid.value
        if (repo == null || uid.isNullOrBlank()) {
            _familyLeaderboardState.value = FamilyLeaderboardUiState.SignedOut
            return
        }

        val previousMembers = when (val cur = _familyLeaderboardState.value) {
            is FamilyLeaderboardUiState.Success -> cur.members
            is FamilyLeaderboardUiState.Loading -> cur.previousMembers
            is FamilyLeaderboardUiState.Error -> cur.cachedMembers
            FamilyLeaderboardUiState.SignedOut -> emptyList()
        }

        familyLeaderboardSyncJob?.cancel()
        familyLeaderboardSyncJob = viewModelScope.launch {
            _familyLeaderboardState.value = FamilyLeaderboardUiState.Loading(previousMembers)
            val progress = localStore.userProgressFlow.first()
            val kakao = localStore.kakaoSessionFlow.first()
            val parents = localStore.parentProfilesFlow.first()
            val shareEnabled = localStore.shareStreakWithFriendsFlow.first()

            val resolvedDisplayName = kakao.nickname
                .ifBlank { parents.daughterName }
                .ifBlank { _firebaseDisplayName.value.orEmpty() }
                .ifBlank { "가족 수호기사" }

            val syncResult = repo.syncUserStreakAndKakaoFriends(
                displayName = resolvedDisplayName,
                avatarUrl = kakao.profileImageUrl,
                streakCount = progress.streakCount,
                todayWordsCompleted = progress.todayWordsCompleted,
                totalXp = progress.totalXp,
                coins = progress.coins,
                hearts = progress.hearts,
                dailyGoal = progress.dailyGoal,
                selectedGrade = progress.selectedGrade,
                selectedTeacherId = progress.selectedTeacherId,
                lastStudyDate = progress.lastStudyDate,
                kakaoUserId = kakao.kakaoUserId,
                connectedFriends = kakao.friends,
                shareWithFriends = shareEnabled
            )

            val ownEntry = syncResult.getOrNull()
            val fetchResult = repo.fetchKakaoFriendsLeaderboard(
                ownKakaoUserId = kakao.kakaoUserId,
                connectedFriends = kakao.friends,
                fallbackOwnEntry = ownEntry
            )

            fetchResult.onSuccess { snapshot ->
                _familyLeaderboardState.value = FamilyLeaderboardUiState.Success(
                    members = snapshot.rankedMembers,
                    unlinkedKakaoFriends = snapshot.unlinkedKakaoFriends,
                    shareWithFriends = shareEnabled,
                    syncedAtLabel = snapshot.syncedAtLabel
                )
                if (manual) {
                    showToast("가족 리더보드 동기화 완료 (${snapshot.rankedMembers.size}명 순위 반영)")
                }
            }.onFailure { err ->
                val message = err.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "가족 리더보드를 불러오지 못했습니다. 네트워크와 로그인 상태를 확인해 주세요."
                _familyLeaderboardState.value = FamilyLeaderboardUiState.Error(
                    message = message,
                    cachedMembers = previousMembers
                )
                if (manual) {
                    showToast("리더보드 동기화 실패: $message")
                }
            }
        }
    }

    fun toggleShareStreakWithFriends(enabled: Boolean) {
        viewModelScope.launch {
            localStore.setShareStreakWithFriends(enabled)
            showToast(
                if (enabled) "내 일일 스트릭을 카카오톡 친구들에게 공개합니다."
                else "내 일일 스트릭을 비공개로 전환했습니다."
            )
            if (!_firebaseUserUid.value.isNullOrBlank()) {
                refreshFamilyLeaderboard(manual = false)
            }
        }
    }

    fun handleKakaoOAuthRedirectCode(authCode: String) {
        viewModelScope.launch {
            showToast("카카오톡 계정 및 친구 목록을 불러오는 중...")
            val res = KakaoAuthAndFriendsService.exchangeCodeAndLoadProfileAndFriends(authCode)
            res.onSuccess { auth ->
                localStore.saveKakaoSession(
                    nickname = auth.nickname,
                    profileImageUrl = auth.profileImageUrl,
                    accessToken = auth.accessToken,
                    friends = auth.friends,
                    kakaoUserId = auth.kakaoUserId
                )
                showToast("카카오톡 로그인 성공! ${auth.nickname}님 (친구 ${auth.friends.size}명 연동)")
                if (!_firebaseUserUid.value.isNullOrBlank()) {
                    refreshFamilyLeaderboard(manual = false)
                }
            }.onFailure { err ->
                showToast("카카오 연동 오류: ${err.message ?: "다시 시도해 주세요"}")
            }
        }
    }

    fun refreshKakaoFriends() {
        viewModelScope.launch {
            val token = kakaoSession.value.accessToken
            val res = KakaoAuthAndFriendsService.refreshFriends(token)
            res.onSuccess { list ->
                val cur = kakaoSession.value
                localStore.saveKakaoSession(
                    nickname = cur.nickname,
                    profileImageUrl = cur.profileImageUrl,
                    accessToken = cur.accessToken,
                    friends = list,
                    kakaoUserId = cur.kakaoUserId
                )
                showToast("카카오톡 친구 ${list.size}명 최신 동기화 완료!")
                if (!_firebaseUserUid.value.isNullOrBlank()) {
                    refreshFamilyLeaderboard(manual = false)
                }
            }.onFailure { err ->
                showToast(err.message ?: "먼저 카카오톡 로그인을 진행해 주세요.")
            }
        }
    }

    fun logoutKakao() {
        viewModelScope.launch {
            localStore.clearKakaoSession()
            showToast("카카오톡 연동이 해제되었습니다. (테스트 모드로 계속 이용 가능)")
            if (!_firebaseUserUid.value.isNullOrBlank()) {
                refreshFamilyLeaderboard(manual = false)
            }
        }
    }

    fun shareFamilyReportToKakao(context: Context) {
        val parents = parentProfiles.value
        val stats = kingdomStats.value
        val prog = userProgress.value
        val avgScore = scorecards.value.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val report = """
            [아빠가딸들을 위한 학습도구 — 가족 왕국 보고서]
            도전자: ${parents.daughterName} (Lv.${stats.princessLevel} · 수능 ${vocabGradeTier.value}등급 빌드업)
            왕국 성채 수복률: ${stats.castleHpPercent}% · 보스전 ${stats.bossVictories}승!
            누적 완료: 오늘 ${prog.todayWordsCompleted}단어 · 예습 ${stats.previewCount} · 복습 ${stats.reviewCount} · 만점 ${stats.perfectScoreCount}
            성적표 평균: ${avgScore}점 · 누적 획득 용돈: ${stats.totalAllowanceWon}원
            
            ${parents.dadName} · ${parents.momName}, 오늘도 응원해 주세요!
        """.trimIndent()
        KakaoAuthAndFriendsService.shareKingdomReportViaKakaoTalk(context, report)
    }

    fun saveAndRegisterCloudflareLevel(customUrl: String) {
        viewModelScope.launch {
            if (customUrl.isNotBlank()) {
                localStore.saveCloudflareWorkerUrl(customUrl)
            }
            _cloudflareStatusKo.value = "Cloudflare 서버에 레벨 등록 중..."
            val payload = CloudflareLevelPayload(
                daughterName = parentProfiles.value.daughterName,
                vocabGradeTier = vocabGradeTier.value,
                princessLevel = kingdomStats.value.princessLevel,
                castleHpPercent = kingdomStats.value.castleHpPercent,
                masteredWordCount = masteredWordIds.value.size,
                accumulatedApiWordCount = harvestedApiWords.value.size,
                bossVictories = kingdomStats.value.bossVictories,
                totalXp = userProgress.value.totalXp
            )
            val res = CloudflareLevelSyncService.registerLevelToCloudflare(
                customWorkerUrl = customUrl.ifBlank { cloudflareWorkerUrl.value },
                payload = payload
            )
            res.onSuccess { msg ->
                _cloudflareStatusKo.value = msg
                showToast(msg)
            }.onFailure { err ->
                _cloudflareStatusKo.value = "대기: ${err.message ?: "Worker URL 확인 필요"}"
                showToast(err.message ?: "Cloudflare 서버 등록 실패")
            }
        }
    }

    // ==================== SHOP, SETTINGS & 10-SLOT LLM ====================

    fun buyCollectible(item: CollectibleItem) {
        viewModelScope.launch {
            val res = localStore.buyCollectible(item.id, item.costCoins)
            res.onSuccess { showToast("'${item.titleKo}' 3D 보물 획득! 성채 HP +5%") }
                .onFailure { showToast(it.message ?: "구매 실패") }
        }
    }

    fun refillHearts() {
        viewModelScope.launch {
            val res = localStore.refillHearts(50)
            res.onSuccess { showToast("하트 5개 완충 완료!") }
                .onFailure { showToast(it.message ?: "코인이 부족해요!") }
        }
    }

    fun setAiEngineMode(mode: AiEngineMode) {
        viewModelScope.launch {
            llmSlotStore.setEngineMode(mode)
            showToast("AI 엔진 모드: ${mode.labelKo}")
        }
    }

    fun setSpeechRateMultiplier(rate: Float) {
        viewModelScope.launch { llmSlotStore.setSpeechRate(rate) }
    }

    fun setHandsFreeMode(enabled: Boolean) {
        viewModelScope.launch { llmSlotStore.setHandsFree(enabled) }
    }

    fun saveLlmSlot(slot: LlmSlotConfig) {
        viewModelScope.launch {
            llmSlotStore.saveSlot(slot)
            showToast("LLM 슬롯 #${slot.slotIndex + 1} 저장 완료!")
        }
    }

    fun testLlmSlot(slot: LlmSlotConfig) {
        viewModelScope.launch {
            _slotTestResults.value = _slotTestResults.value + (slot.slotIndex to "연결 테스트 중...")
            val result = aiCoachEngine.testExternalSlot(slot)
            val msg = result.fold(
                onSuccess = { "연결 성공: \"${it.take(55)}\"" },
                onFailure = { "실패: ${it.message ?: "네트워크 오류"}" }
            )
            _slotTestResults.value = _slotTestResults.value + (slot.slotIndex to msg)
        }
    }

    // ==================== WHITE PYGMY SQUIRREL STORY RPG & MYSTERY CHEST ====================

    private val secretTreasurePool: List<Triple<String, String, String>> = listOf(
        Triple("황금 도토리 왕관 유물", "전설", "비밀 유물"),
        Triple("에메랄드 별빛 망토", "에픽", "비밀 유물"),
        Triple("아빠의 깜짝 간식 비밀권", "스페셜", "비밀 쿠폰"),
        Triple("엄마의 주말 데이트 비밀권", "스페셜", "비밀 쿠폰"),
        Triple("크리스탈 발음 마법봉", "희귀", "비밀 유물"),
        Triple("피그미 다람쥐 황금 방패", "에픽", "비밀 유물"),
        Triple("아이스크림·디저트 깜짝권", "스페셜", "비밀 쿠폰"),
        Triple("별빛 서고 고대 마법서", "전설", "비밀 유물"),
        Triple("영화 관람 패밀리 비밀권", "스페셜", "비밀 쿠폰"),
        Triple("무지개 날개 에메랄드 브로치", "희귀", "비밀 유물")
    )

    fun openSquirrelHeroPicker() {
        _rpgState.value = _rpgState.value.copy(phase = RpgGamePhase.HERO_PICKER)
    }

    fun selectSquirrelHero(hero: SquirrelJobHero) {
        viewModelScope.launch {
            localStore.saveSelectedSquirrelHero(hero.id)
            _rpgState.value = _rpgState.value.copy(phase = RpgGamePhase.IDLE)
            showToast("${hero.nameKo} (${hero.jobTitleKo}) 흰색 피그미 다람쥐 장착!")
        }
    }

    fun closeRpgOverlay() {
        stopVoiceIo()
        _rpgState.value = RpgGameUiState(phase = RpgGamePhase.IDLE)
    }

    fun startRpgChapter(chapterNumber: Int) {
        val chapter = SquirrelRpgRepository.chapters.firstOrNull { it.number == chapterNumber }
            ?: SquirrelRpgRepository.chapters.first()
        val hero = SquirrelRpgRepository.findHeroById(selectedSquirrelHeroId.value)
        onEnterStudyActivity("왕국 RPG 배틀")
        _rpgState.value = RpgGameUiState(
            phase = RpgGamePhase.STORY_DIALOGUE,
            activeChapter = chapter,
            heroHp = hero.maxHp,
            heroMaxHp = hero.maxHp,
            heroMp = 40,
            heroShieldActive = hero.skillKind == SquirrelSkillKind.SHIELD_GUARD,
            heroReviveAvailable = hero.skillKind == SquirrelSkillKind.ROYAL_FEAST,
            monsterHp = chapter.monster.maxHp,
            monsterMaxHp = chapter.monster.maxHp,
            turnNumber = 1,
            comboCount = 0,
            floatingBannerKo = "${chapter.titleKo} 입장!",
            lastBattleLogKo = "${hero.nameKo} (${hero.jobTitleKo}) 모험 시작!"
        )
    }

    fun chooseRpgStoryOption(choice: RpgStoryChoice) {
        val cur = _rpgState.value
        val newHp = (cur.heroHp + choice.hpBonus).coerceAtMost(cur.heroMaxHp + choice.hpBonus)
        val newMaxHp = maxOf(cur.heroMaxHp, newHp)
        val newMp = (cur.heroMp + choice.mpBonus).coerceAtMost(100)
        if (choice.bonusCoins > 0) {
            viewModelScope.launch { localStore.grantBonusCoins(choice.bonusCoins) }
        }
        _rpgState.value = cur.copy(
            phase = RpgGamePhase.STORY_OUTCOME,
            selectedChoiceOutcomeKo = choice.outcomeKo,
            heroHp = newHp,
            heroMaxHp = newMaxHp,
            heroMp = newMp,
            heroAtkBuffPercent = cur.heroAtkBuffPercent + choice.atkBuffPercent,
            heroShieldActive = cur.heroShieldActive || choice.startShield,
            floatingBannerKo = choice.tagKo
        )
    }

    fun startChapterBattle() {
        val cur = _rpgState.value
        val monster = cur.activeChapter?.monster ?: return
        _rpgState.value = cur.copy(
            phase = RpgGamePhase.BATTLE_ACTION,
            floatingBannerKo = "${monster.nameKo} 출현!",
            lastBattleLogKo = "수능 빈출 단어+문장 또는 실전 회화로 공격하세요!"
        )
    }

    fun chooseRpgBattleAction(action: RpgBattleAction) {
        val cur = _rpgState.value
        val hero = SquirrelRpgRepository.findHeroById(selectedSquirrelHeroId.value)
        when (action) {
            RpgBattleAction.WORD_STRIKE -> {
                val challenge = buildCsatWordChallenge(
                    forceHint = hero.skillKind == SquirrelSkillKind.TIME_REVEAL,
                    skillAmplified = false
                )
                _rpgState.value = cur.copy(
                    phase = RpgGamePhase.BATTLE_WORD_CHALLENGE,
                    wordChallenge = challenge,
                    heroGuarding = false
                )
            }
            RpgBattleAction.PHRASE_CAST -> {
                val challenge = buildConversationPhraseChallenge(skillAmplified = false)
                _rpgState.value = cur.copy(
                    phase = RpgGamePhase.BATTLE_PHRASE_CHALLENGE,
                    phraseChallenge = challenge,
                    heroGuarding = false
                )
            }
            RpgBattleAction.CLASS_SKILL -> {
                if (cur.heroMp < hero.skillMpCost) return
                val spentMp = (cur.heroMp - hero.skillMpCost).coerceAtLeast(0)
                when (hero.skillKind) {
                    SquirrelSkillKind.SHIELD_GUARD -> {
                        val challenge = buildCsatWordChallenge(forceHint = false, skillAmplified = true)
                        _rpgState.value = cur.copy(
                            heroMp = spentMp,
                            heroShieldActive = true,
                            phase = RpgGamePhase.BATTLE_WORD_CHALLENGE,
                            wordChallenge = challenge,
                            floatingBannerKo = "${hero.skillNameKo} 발동!"
                        )
                    }
                    SquirrelSkillKind.SUN_HEAL, SquirrelSkillKind.ROYAL_FEAST -> {
                        val healAmt = if (hero.skillKind == SquirrelSkillKind.ROYAL_FEAST) 50 else 45
                        val newHp = (cur.heroHp + healAmt).coerceAtMost(cur.heroMaxHp)
                        val buffAdd = if (hero.skillKind == SquirrelSkillKind.ROYAL_FEAST) 20 else 0
                        val challenge = buildCsatWordChallenge(forceHint = false, skillAmplified = true)
                        _rpgState.value = cur.copy(
                            heroHp = newHp,
                            heroMp = spentMp,
                            heroAtkBuffPercent = cur.heroAtkBuffPercent + buffAdd,
                            phase = RpgGamePhase.BATTLE_WORD_CHALLENGE,
                            wordChallenge = challenge,
                            floatingBannerKo = "HP +$healAmt 회복!"
                        )
                    }
                    SquirrelSkillKind.MELODY_BOOST, SquirrelSkillKind.COMBO_ARROW -> {
                        val healAmt = if (hero.skillKind == SquirrelSkillKind.MELODY_BOOST) 20 else 0
                        val challenge = buildConversationPhraseChallenge(skillAmplified = true)
                        _rpgState.value = cur.copy(
                            heroHp = (cur.heroHp + healAmt).coerceAtMost(cur.heroMaxHp),
                            heroMp = spentMp,
                            phase = RpgGamePhase.BATTLE_PHRASE_CHALLENGE,
                            phraseChallenge = challenge,
                            floatingBannerKo = "${hero.skillNameKo} 발동!"
                        )
                    }
                    SquirrelSkillKind.TIME_REVEAL -> {
                        val challenge = buildCsatWordChallenge(forceHint = true, skillAmplified = true)
                        _rpgState.value = cur.copy(
                            heroMp = spentMp,
                            monsterStunned = true,
                            phase = RpgGamePhase.BATTLE_WORD_CHALLENGE,
                            wordChallenge = challenge,
                            floatingBannerKo = "시간 정지! 적 1턴 기절!"
                        )
                    }
                    else -> {
                        val extraBuff = if (hero.skillKind == SquirrelSkillKind.GOLDEN_BREW) 25 else 0
                        val challenge = buildCsatWordChallenge(forceHint = false, skillAmplified = true)
                        _rpgState.value = cur.copy(
                            heroMp = spentMp,
                            heroAtkBuffPercent = cur.heroAtkBuffPercent + extraBuff,
                            phase = RpgGamePhase.BATTLE_WORD_CHALLENGE,
                            wordChallenge = challenge,
                            floatingBannerKo = "${hero.skillNameKo} 준비!"
                        )
                    }
                }
            }
            RpgBattleAction.GUARD_HINT -> {
                val mpGain = (cur.heroMp + 30).coerceAtMost(100)
                val guardedState = cur.copy(
                    heroMp = mpGain,
                    heroGuarding = true,
                    floatingBannerKo = "방어 태세 · MP +30!",
                    lastBattleLogKo = "피해를 절반으로 줄이고 마력을 모았어요!"
                )
                resolveMonsterCounterTurn(guardedState, hero)
            }
        }
    }

    private fun buildCsatWordChallenge(forceHint: Boolean, skillAmplified: Boolean): RpgWordChallenge {
        val tier = vocabGradeTier.value
        val allPool = (VocabRepository.allWords + harvestedApiWords.value).distinctBy { it.id }
        // Prioritize high-frequency CSAT words matching current grade tier
        val tierWords = allPool.filter { it.csatFrequency >= 4 || it.grade == ((6 - tier).coerceIn(1, 3)) }
            .ifEmpty { allPool }
        val target = tierWords.random()
        val sentenceObj = target.examples.firstOrNull()
        val sentenceEn = sentenceObj?.english ?: target.exampleEn.ifBlank { "Let's master the word ${target.word} in a real sentence." }
        val sentenceKo = sentenceObj?.korean ?: target.exampleKo.ifBlank { target.meaningKo }
        val distractors = allPool.filter { it.meaningKo != target.meaningKo }
            .shuffled()
            .take(3)
            .map { it.meaningKo }
        val options = (distractors + target.meaningKo).shuffled()
        return RpgWordChallenge(
            word = target,
            exampleSentenceEn = sentenceEn,
            exampleSentenceKo = sentenceKo,
            options = options,
            showHint = forceHint,
            isSkillAmplified = skillAmplified,
            startedAtMs = System.currentTimeMillis()
        )
    }

    private fun buildConversationPhraseChallenge(skillAmplified: Boolean): RpgPhraseChallenge {
        val scenarios = TravelCurriculumRepository.allScenarios
        val targetScenario = scenarios.random()
        val targetSentence = targetScenario.sentences.random()
        val distractors = scenarios.flatMap { it.sentences }
            .filter { it.english != targetSentence.english }
            .shuffled()
            .take(2)
            .map { it.english }
        val options = (distractors + targetSentence.english).shuffled()
        return RpgPhraseChallenge(
            promptKorean = targetSentence.korean,
            targetEnglish = targetSentence.english,
            options = options,
            speakingTipKo = targetSentence.speakingTipKo,
            isSkillAmplified = skillAmplified,
            startedAtMs = System.currentTimeMillis()
        )
    }

    fun listenRpgEnglish(text: String) {
        voiceEngine.speak(text = text, locale = Locale.US, rate = speechRate.value)
    }

    fun recordMainFocusWordResult(wordId: String, known: Boolean) {
        val matchedWord = (VocabRepository.allWords + harvestedApiWords.value).firstOrNull { it.id == wordId }
        val wordText = matchedWord?.word ?: wordId
        val meaningText = matchedWord?.meaningKo ?: "수능 빈출 영단어"
        recordVocabCardSm2Recall(
            wordId = wordId,
            wordText = wordText,
            meaningKo = meaningText,
            grade = if (known) RecallGrade.GOOD else RecallGrade.AGAIN,
            fromMnemonic = false
        )
    }

    fun setCsatDDayDays(daysRemaining: Int) {
        viewModelScope.launch {
            localStore.setCsatDDayDays(daysRemaining)
            val policy = com.example.engine.FsrsSchedulerEngine.resolveDDayPolicy(daysRemaining)
            showToast("수능 D-${policy.daysUntilCsat} (${policy.phase.shortBadgeKo}) · 목표 기억률 ${(policy.desiredRetention * 100).toInt()}%")
        }
    }

    /**
     * v2 Master Spec SM-2 Recall + Idempotent Reward + Daily Mission Progress.
     * Repeated taps on the same word on the same day never double-pay XP/Coins.
     */
    fun recordVocabCardSm2Recall(
        wordId: String,
        wordText: String,
        meaningKo: String,
        grade: RecallGrade,
        fromMnemonic: Boolean = false
    ) {
        val known = grade == RecallGrade.GOOD || grade == RecallGrade.EASY
        if (known) {
            _spartaState.value = _spartaState.value.copy(
                sessionKnownCount = _spartaState.value.sessionKnownCount + 1
            )
            VocabAnalyticsTracker.log("recall_correct", wordId = wordId, meta = grade.name)
        } else {
            _spartaState.value = _spartaState.value.copy(
                sessionUnknownCount = _spartaState.value.sessionUnknownCount + 1
            )
            VocabAnalyticsTracker.log("recall_wrong", wordId = wordId, meta = grade.name)
        }

        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val existingSm2 = wordSm2Progress.value[wordId]
            val isFirstTimeWord = existingSm2 == null
            val wasWeakOrDue = wordId in weakWordIds.value || (existingSm2?.dueAtMs ?: 0L) in 1L..System.currentTimeMillis()
            val updatedSm2 = localStore.recordWordSm2Recall(wordId = wordId, grade = grade)
            localStore.recordDailyWordOutcome(wordId, wordText, meaningKo, known = known)
            VocabAnalyticsTracker.log("word_recall_graded", wordId = wordId, meta = grade.name)

            val matchedWord = (VocabRepository.allWords + harvestedApiWords.value).firstOrNull { it.id == wordId }

            if (known) {
                // Anti-farming: true first-time word learning gets a one-time ever eventId
                val (eventId, eventType) = when {
                    isFirstTimeWord -> "new_word_$wordId" to RewardEventType.NEW_WORD_LEARNED
                    wasWeakOrDue -> "review_${today}_$wordId" to RewardEventType.DAILY_REVIEW_COMPLETED
                    else -> "recall_${today}_$wordId" to RewardEventType.ACTIVE_RECALL_CORRECT
                }
                val spec = RewardConfig.specFor(eventType)
                val reward = localStore.grantIdempotentReward(
                    eventId = eventId,
                    eventType = eventType,
                    xpDelta = spec.xp,
                    coinDelta = spec.coins,
                    reason = "단어 회상 완료 ($wordText · ${grade.labelKo})",
                    wordId = wordId
                )
                if (reward.granted) {
                    localStore.advanceVocabMission(MissionKind.DAILY_NEW_WORDS, 1)
                    localStore.advanceVocabMission(MissionKind.WEEKLY_UNIQUE_WORDS, 1)
                }
                if (wasWeakOrDue) {
                    localStore.advanceVocabMission(MissionKind.DAILY_REVIEW_DUE, 1)
                    localStore.advanceVocabMission(MissionKind.DAILY_WRONG_RETRY, 1)
                }
                if (fromMnemonic) {
                    localStore.advanceVocabMission(MissionKind.DAILY_MNEMONIC_RECALL, 1)
                }
                // Advance Story/Exploration missions based on word topic & verified morpheme
                if (matchedWord != null && reward.granted) {
                    if (matchedWord.topic == VocabTopic.TRAVEL_DAILY) {
                        localStore.advanceVocabMission(MissionKind.STORY_TRAVEL_WORDS, 1)
                    }
                    if (matchedWord.topic == VocabTopic.EMOTION_MIND) {
                        localStore.advanceVocabMission(MissionKind.STORY_EMOTION_WORDS, 1)
                    }
                    if (VocabMnemonicEngine.buildStructuredGuide(matchedWord).hasVerifiedMorpheme) {
                        localStore.advanceVocabMission(MissionKind.STORY_VERIFIED_ROOTS, 1)
                    }
                }

                val streakResult = localStore.recordLearningAchievement(
                    xpDelta = 0,
                    coinsDelta = 0,
                    wordsDelta = 1,
                    castleHpDelta = if (reward.granted) 1 else 0,
                    missionCompleted = false
                )

                // Session daily goal milestone bonus (idempotent once per day)
                val dailyGoal = userProgress.value.dailyGoal.coerceIn(5, 30)
                if (streakResult.todayWordsCompleted >= dailyGoal) {
                    val sessionSpec = RewardConfig.specFor(RewardEventType.TEN_WORD_SESSION_COMPLETED)
                    localStore.grantIdempotentReward(
                        eventId = "daily_goal_session_$today",
                        eventType = RewardEventType.TEN_WORD_SESSION_COMPLETED,
                        xpDelta = sessionSpec.xp,
                        coinDelta = sessionSpec.coins,
                        reason = "오늘 목표 ${dailyGoal}단어 세션 완료 보너스"
                    )
                }

                if (reward.leveledUpTo != null) {
                    _questCelebrationEvent.value = QuestCelebrationEvent(
                        titleKo = "레벨 업 달성!",
                        xpEarned = spec.xp,
                        coinsEarned = spec.coins,
                        streakCount = userProgress.value.streakCount,
                        leveledUpTitleKo = "Lv.${reward.leveledUpTo.level} ${reward.leveledUpTo.titleKo}"
                    )
                } else if (reward.granted) {
                    val nextLabel = if (updatedSm2.intervalDays > 0) "${updatedSm2.intervalDays}일 뒤 복습" else "잠시 뒤 복습"
                    showToast("+$wordText · +${spec.xp} XP +${spec.coins} 코인 ($nextLabel)")
                }
                notifySpecialMilestones(streakResult)
            } else {
                if (grade == RecallGrade.HARD) {
                    localStore.advanceVocabMission(MissionKind.DAILY_REVIEW_DUE, 1)
                }
                val retryLabel = if (grade == RecallGrade.AGAIN) "10분 뒤 다시 복습" else "1일 뒤 다시 복습"
                showToast("오답·복습 큐 등록 ($wordText · $retryLabel)")
            }
        }
    }

    /**
     * Section 3.3 & 12: Inline typed active recall evaluation with 1-char typo tolerance.
     */
    fun submitTypedVocabRecall(word: VocabWord, rawInput: String): TypedRecallResult {
        val result = TypedRecallEvaluator.evaluate(rawInput, word.word)
        if (result.isPassed) {
            VocabAnalyticsTracker.log("typing_correct", wordId = word.id, meta = result.outcome.name)
            viewModelScope.launch {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val spec = RewardConfig.specFor(RewardEventType.TYPING_SPELLING_CORRECT)
                val reward = localStore.grantIdempotentReward(
                    eventId = "typing_${today}_${word.id}",
                    eventType = RewardEventType.TYPING_SPELLING_CORRECT,
                    xpDelta = spec.xp,
                    coinDelta = spec.coins,
                    reason = "철자 직접 입력 회상 성공 (${word.word})",
                    wordId = word.id
                )
                localStore.recordWordSm2Recall(wordId = word.id, grade = RecallGrade.GOOD)
                localStore.recordDailyWordOutcome(word.id, word.word, word.meaningKo, known = true)
                if (reward.granted) {
                    localStore.advanceVocabMission(MissionKind.DAILY_NEW_WORDS, 1)
                    localStore.advanceVocabMission(MissionKind.WEEKLY_UNIQUE_WORDS, 1)
                    if (VocabMnemonicEngine.buildStructuredGuide(word).hasVerifiedMorpheme) {
                        localStore.advanceVocabMission(MissionKind.STORY_VERIFIED_ROOTS, 1)
                    }
                    showToast("철자 정답! ${word.word} (+${spec.xp} XP · +${spec.coins} 코인)")
                }
            }
        } else {
            VocabAnalyticsTracker.log("recall_wrong", wordId = word.id, meta = "TYPING_MISS")
        }
        return result
    }

    fun setDailyWordGoal(goal: Int) {
        viewModelScope.launch {
            localStore.setDailyWordGoal(goal)
            showToast("오늘 단어 목표를 ${goal.coerceIn(5, 30)}개로 설정했어요!")
        }
    }

    fun toggleSuspendWord(wordId: String, wordLabel: String) {
        viewModelScope.launch {
            val updated = localStore.toggleSuspendWord(wordId)
            val msg = if (updated.learningStatus == WordLearningStatus.SUSPENDED) {
                "'$wordLabel' 복습 대기열에서 제외(숨김)했어요."
            } else {
                "'$wordLabel' 복습 대기열에 다시 포함했어요."
            }
            showToast(msg)
        }
    }

    fun setMediaReviewStatus(wordId: String, status: MediaReviewStatus) {
        viewModelScope.launch {
            localStore.setMediaReviewStatus(wordId, status)
            val label = if (status == MediaReviewStatus.APPROVED) "승인" else "보류"
            showToast("미디어·연상 검수 상태를 '${label}'로 저장했어요.")
        }
    }

    fun claimVocabMissionReward(missionId: String) {
        viewModelScope.launch {
            val (mission, reward) = localStore.claimVocabMission(missionId)
            if (mission != null && reward != null && reward.granted) {
                _questCelebrationEvent.value = QuestCelebrationEvent(
                    titleKo = mission.titleKo,
                    xpEarned = mission.rewardXp,
                    coinsEarned = mission.rewardCoins,
                    streakCount = userProgress.value.streakCount,
                    leveledUpTitleKo = reward.leveledUpTo?.let { "Lv.${it.level} ${it.titleKo}" }
                )
            } else {
                showToast("이미 수령했거나 진행 중인 미션입니다.")
            }
        }
    }

    fun saveWordToMemoryPalace(wordId: String, room: MemoryPalaceRoom, memoKo: String) {
        viewModelScope.launch {
            localStore.saveMemoryPalaceEntry(wordId, room, memoKo)
            localStore.advanceVocabMission(MissionKind.DAILY_MNEMONIC_RECALL, 1)
            localStore.advanceVocabMission(MissionKind.WEEKLY_PALACE_ROOMS, 1)
            VocabAnalyticsTracker.log("memory_palace_saved", wordId = wordId, meta = room.name)
            showToast("'${room.titleKo}' 기억의 궁전에 배치했어요!")
        }
    }

    fun logVocabAudioListen() {
        VocabAnalyticsTracker.log("pronunciation_played")
        viewModelScope.launch {
            localStore.advanceVocabMission(MissionKind.DAILY_LISTENING_SPEAK, 1)
        }
    }

    fun submitRpgWordAnswer(selectedMeaning: String) {
        val cur = _rpgState.value
        val ch = cur.wordChallenge ?: return
        val hero = SquirrelRpgRepository.findHeroById(selectedSquirrelHeroId.value)
        val correct = selectedMeaning == ch.word.meaningKo
        val elapsedMs = System.currentTimeMillis() - ch.startedAtMs
        resolvePlayerAttackResult(
            cur = cur,
            hero = hero,
            isCorrect = correct,
            isPhrase = false,
            isVoiceSpoken = false,
            isSkillAmplified = ch.isSkillAmplified,
            elapsedMs = elapsedMs,
            correctFeedbackKo = "${ch.word.word} = ${ch.word.meaningKo}!"
        )
    }

    fun submitRpgPhraseAnswer(selectedEnglish: String) {
        val cur = _rpgState.value
        val ch = cur.phraseChallenge ?: return
        val hero = SquirrelRpgRepository.findHeroById(selectedSquirrelHeroId.value)
        val correct = selectedEnglish == ch.targetEnglish
        val elapsedMs = System.currentTimeMillis() - ch.startedAtMs
        resolvePlayerAttackResult(
            cur = cur,
            hero = hero,
            isCorrect = correct,
            isPhrase = true,
            isVoiceSpoken = false,
            isSkillAmplified = ch.isSkillAmplified,
            elapsedMs = elapsedMs,
            correctFeedbackKo = "회화 성공! \"${ch.targetEnglish}\""
        )
    }

    fun startRpgVoicePhraseAttack() {
        val cur = _rpgState.value
        val ch = cur.phraseChallenge ?: return
        val hero = SquirrelRpgRepository.findHeroById(selectedSquirrelHeroId.value)
        voiceEngine.startListening(
            locale = Locale.US,
            onResult = { spoken, durationMs ->
                val report = PronunciationAnalyzer.analyze(
                    targetText = ch.targetEnglish,
                    recognizedText = spoken,
                    durationMs = durationMs
                )
                val passed = report.overallScore >= 45 || spoken.trim().split(Regex("\\s+")).size >= 2
                resolvePlayerAttackResult(
                    cur = _rpgState.value,
                    hero = hero,
                    isCorrect = passed,
                    isPhrase = true,
                    isVoiceSpoken = true,
                    isSkillAmplified = ch.isSkillAmplified,
                    elapsedMs = durationMs,
                    correctFeedbackKo = "스피킹 크리티컬 (${report.overallScore}점)!"
                )
            }
        )
    }

    private fun resolvePlayerAttackResult(
        cur: RpgGameUiState,
        hero: SquirrelJobHero,
        isCorrect: Boolean,
        isPhrase: Boolean,
        isVoiceSpoken: Boolean,
        isSkillAmplified: Boolean,
        elapsedMs: Long,
        correctFeedbackKo: String
    ) {
        val chapter = cur.activeChapter ?: return
        if (isCorrect) {
            val newCombo = cur.comboCount + 1
            var dmg = hero.baseAtk.toFloat()
            if (hero.skillKind == SquirrelSkillKind.WORD_METEOR && !isPhrase) dmg *= 1.25f
            if (hero.skillKind == SquirrelSkillKind.MELODY_BOOST && isPhrase) dmg *= 1.30f
            if (hero.skillKind == SquirrelSkillKind.DRAGON_BREAKER && chapter.monster.isBoss) dmg *= 1.30f
            if (hero.skillKind == SquirrelSkillKind.COMBO_ARROW && newCombo >= 2) dmg *= 1.35f
            if (hero.skillKind == SquirrelSkillKind.SHADOW_DOUBLE && elapsedMs <= 5000L) dmg *= 1.65f
            if (isVoiceSpoken) dmg *= 1.35f
            if (isSkillAmplified) dmg *= 1.65f
            if (cur.heroAtkBuffPercent > 0) dmg *= (1f + cur.heroAtkBuffPercent / 100f)

            val finalDmg = dmg.toInt().coerceAtLeast(18)
            val newMonsterHp = (cur.monsterHp - finalDmg).coerceAtLeast(0)
            val healPassive = if (hero.skillKind == SquirrelSkillKind.SUN_HEAL) 12 else 0
            val newHeroHp = (cur.heroHp + healPassive).coerceAtMost(cur.heroMaxHp)
            val newHeroMp = (cur.heroMp + 22).coerceAtMost(100)
            val newWordsCount = cur.wordsClearedInBattle + 1

            _spartaState.value = _spartaState.value.copy(
                sessionKnownCount = _spartaState.value.sessionKnownCount + 1
            )

            if (newMonsterHp <= 0) {
                val coinMultiplier = if (hero.skillKind == SquirrelSkillKind.GOLDEN_BREW) 1.35f else 1f
                val earnedCoins = (chapter.rewardCoins * coinMultiplier).toInt()
                val earnedXp = chapter.rewardXp
                val earnedCastle = chapter.castleHpBonus
                viewModelScope.launch {
                    val streakHit = localStore.recordRpgChapterVictory(
                        chapterId = chapter.id,
                        xpDelta = earnedXp,
                        coinsDelta = earnedCoins,
                        castleHpBonus = earnedCastle,
                        wordsCount = newWordsCount
                    )
                    if (streakHit != null) {
                        val todayWords = localStore.userProgressFlow.first().todayWordsCompleted
                        _streakMilestoneEvent.value = StreakMilestoneEvent(
                            newStreakCount = streakHit,
                            todayWordsCompleted = todayWords
                        )
                    }
                    flushActiveStudyTimeLog(showConfirmation = false)
                }
                _rpgState.value = cur.copy(
                    phase = RpgGamePhase.VICTORY,
                    heroHp = newHeroHp,
                    heroMp = newHeroMp,
                    monsterHp = 0,
                    comboCount = newCombo,
                    monsterHitTick = cur.monsterHitTick + 1,
                    wordsClearedInBattle = newWordsCount,
                    earnedCoins = earnedCoins,
                    earnedXp = earnedXp,
                    earnedCastleHp = earnedCastle,
                    floatingBannerKo = "-$finalDmg 격파!!",
                    lastBattleLogKo = correctFeedbackKo,
                    mysteryChestPendingOpen = true,
                    revealedChestReward = null,
                    wordChallenge = null,
                    phraseChallenge = null
                )
                return
            }

            val postAttack = cur.copy(
                heroHp = newHeroHp,
                heroMp = newHeroMp,
                monsterHp = newMonsterHp,
                comboCount = newCombo,
                monsterHitTick = cur.monsterHitTick + 1,
                wordsClearedInBattle = newWordsCount,
                floatingBannerKo = if (isSkillAmplified || isVoiceSpoken) "-$finalDmg CRITICAL!" else "-$finalDmg 명중!",
                lastBattleLogKo = correctFeedbackKo,
                wordChallenge = null,
                phraseChallenge = null
            )
            resolveMonsterCounterTurn(postAttack, hero)
        } else {
            _spartaState.value = _spartaState.value.copy(
                sessionUnknownCount = _spartaState.value.sessionUnknownCount + 1
            )
            val missState = cur.copy(
                comboCount = 0,
                floatingBannerKo = "빗나감! 정답 확인",
                lastBattleLogKo = "정답: $correctFeedbackKo",
                wordChallenge = null,
                phraseChallenge = null
            )
            resolveMonsterCounterTurn(missState, hero)
        }
    }

    private fun resolveMonsterCounterTurn(state: RpgGameUiState, hero: SquirrelJobHero) {
        val monster = state.activeChapter?.monster ?: return
        if (state.monsterStunned) {
            _rpgState.value = state.copy(
                phase = RpgGamePhase.BATTLE_ACTION,
                monsterStunned = false,
                turnNumber = state.turnNumber + 1,
                lastBattleLogKo = "${state.lastBattleLogKo} · 적이 기절해 공격 못함!"
            )
            return
        }
        if (state.heroShieldActive) {
            _rpgState.value = state.copy(
                phase = RpgGamePhase.BATTLE_ACTION,
                heroShieldActive = false,
                heroGuarding = false,
                turnNumber = state.turnNumber + 1,
                floatingBannerKo = "방어막으로 피해 0!",
                lastBattleLogKo = "${monster.nameKo}의 공격을 방어막이 막았어요!"
            )
            return
        }
        val rawDmg = monster.atk
        val actualDmg = if (state.heroGuarding) (rawDmg / 2).coerceAtLeast(4) else rawDmg
        val remainingHp = (state.heroHp - actualDmg).coerceAtLeast(0)

        if (remainingHp <= 0 && state.heroReviveAvailable) {
            val reviveHp = (state.heroMaxHp * 0.4f).toInt().coerceAtLeast(35)
            _rpgState.value = state.copy(
                phase = RpgGamePhase.BATTLE_ACTION,
                heroHp = reviveHp,
                heroReviveAvailable = false,
                heroGuarding = false,
                heroHitTick = state.heroHitTick + 1,
                turnNumber = state.turnNumber + 1,
                floatingBannerKo = "왕실 요리사 부활! HP +$reviveHp",
                lastBattleLogKo = "위기에서 특제 도토리 요리로 부활했어요!"
            )
            return
        }

        if (remainingHp <= 0) {
            _rpgState.value = state.copy(
                phase = RpgGamePhase.DEFEAT,
                heroHp = 0,
                heroGuarding = false,
                heroHitTick = state.heroHitTick + 1,
                floatingBannerKo = "체력 소진!",
                lastBattleLogKo = "${monster.nameKo}에게 밀렸어요. 다시 도전해 보세요!"
            )
            return
        }

        _rpgState.value = state.copy(
            phase = RpgGamePhase.BATTLE_ACTION,
            heroHp = remainingHp,
            heroGuarding = false,
            heroHitTick = state.heroHitTick + 1,
            turnNumber = state.turnNumber + 1,
            lastBattleLogKo = "${state.lastBattleLogKo} · ${monster.nameKo} 반격 -$actualDmg"
        )
    }

    fun openMysteryTreasureChest() {
        val pick = secretTreasurePool.random()
        val today = SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(Date())
        val reward = MysteryChestReward(
            id = "chest_${UUID.randomUUID().toString().take(6)}",
            titleKo = pick.first,
            rarityKo = pick.second,
            categoryKo = pick.third,
            openedDateLabel = today
        )
        viewModelScope.launch {
            localStore.consumeMysteryChestAndSaveReward(reward)
            _rpgState.value = _rpgState.value.copy(
                mysteryChestPendingOpen = false,
                revealedChestReward = reward
            )
            showToast("미스터리 보물상자 오픈! [${reward.rarityKo}] ${reward.titleKo} 획득!")
        }
    }

    fun advanceOrReplayRpgChapter() {
        val curNum = _rpgState.value.activeChapter?.number ?: 1
        val nextNum = if (curNum < 6) curNum + 1 else 1
        startRpgChapter(nextNum)
    }

    fun retryCurrentRpgChapter() {
        val curNum = _rpgState.value.activeChapter?.number ?: 1
        startRpgChapter(curNum)
    }

    // ==================== ENGAME 2.0 4-MODE ARCADE ENGINE ====================

    fun openEnGameHub() {
        enGameTimerJob?.cancel()
        stopVoiceIo()
        val cur = _enGameState.value
        _enGameState.value = cur.copy(
            phase = EnGamePhase.MODE_HUB,
            lastFeedbackKo = ""
        )
    }

    fun closeEnGameOverlay() {
        enGameTimerJob?.cancel()
        stopVoiceIo()
        _enGameState.value = _enGameState.value.copy(phase = EnGamePhase.CLOSED)
    }

    fun backToEnGameHub() {
        enGameTimerJob?.cancel()
        stopVoiceIo()
        _enGameState.value = _enGameState.value.copy(
            phase = EnGamePhase.MODE_HUB,
            lastFeedbackKo = ""
        )
    }

    fun startEnGameMode(mode: EnGameMode) {
        enGameTimerJob?.cancel()
        stopVoiceIo()
        enGameRecentWordIds.clear()
        onEnterStudyActivity("EnGame ${mode.titleKo}")

        val cur = _enGameState.value
        val firstQuestion = nextEnGameQuestion(mode = mode, elo = cur.eloRating)
        _enGameState.value = EnGameUiState(
            phase = EnGamePhase.PLAYING,
            mode = mode,
            eloRating = cur.eloRating,
            highScore = cur.highScore,
            lives = 3,
            maxLives = 3,
            timeLeftSec = mode.durationSec,
            totalTimeSec = mode.durationSec,
            waveNumber = 1,
            bossHp = 100,
            bossMaxHp = 100,
            currentQuestion = firstQuestion,
            lastFeedbackKo = "${mode.titleKo} 시작! 빠르게 맞혀 피버 게이지를 채우세요!"
        )

        if (mode == EnGameMode.VOICE_BOSS_RUSH) {
            listenRpgEnglish(firstQuestion.audioTextEn)
        }

        enGameTimerJob = viewModelScope.launch {
            while (_enGameState.value.phase == EnGamePhase.PLAYING) {
                delay(1000L)
                val now = _enGameState.value
                if (now.phase != EnGamePhase.PLAYING) break
                val nextSec = (now.timeLeftSec - 1).coerceAtLeast(0)
                if (nextSec <= 0) {
                    finishEnGameSession(now.copy(timeLeftSec = 0))
                    break
                } else {
                    _enGameState.value = now.copy(timeLeftSec = nextSec)
                }
            }
        }
    }

    fun submitEnGameChoice(selected: String) {
        val cur = _enGameState.value
        val q = cur.currentQuestion ?: return
        if (cur.phase != EnGamePhase.PLAYING) return
        val correct = selected.equals(q.correctAnswer, ignoreCase = true)
        val elapsedMs = System.currentTimeMillis() - q.startedAtMs
        applyEnGameEvaluation(
            cur = cur,
            isCorrect = correct,
            elapsedMs = elapsedMs,
            isVoiceCritical = false
        )
    }

    fun tapEnGameScrambledToken(tokenIndex: Int) {
        val cur = _enGameState.value
        val q = cur.currentQuestion ?: return
        val isTileMode = q.mode == EnGameMode.SENTENCE_BUILDER || q.mode == EnGameMode.SPELLING_TILES
        if (cur.phase != EnGamePhase.PLAYING || !isTileMode) return
        if (tokenIndex !in q.scrambledTokens.indices || tokenIndex in cur.selectedTokens) return
        _enGameState.value = cur.copy(selectedTokens = cur.selectedTokens + tokenIndex)
    }

    fun removeEnGameSelectedToken(positionInTray: Int) {
        val cur = _enGameState.value
        if (positionInTray !in cur.selectedTokens.indices) return
        val updated = cur.selectedTokens.toMutableList().apply { removeAt(positionInTray) }
        _enGameState.value = cur.copy(selectedTokens = updated)
    }

    fun clearEnGameSentenceTokens() {
        _enGameState.value = _enGameState.value.copy(selectedTokens = emptyList())
    }

    fun submitEnGameSentenceBuilder() {
        val cur = _enGameState.value
        val q = cur.currentQuestion ?: return
        val isTileMode = q.mode == EnGameMode.SENTENCE_BUILDER || q.mode == EnGameMode.SPELLING_TILES
        if (cur.phase != EnGamePhase.PLAYING || !isTileMode) return
        val separator = if (q.mode == EnGameMode.SPELLING_TILES) "" else " "
        val assembled = cur.selectedTokens.mapNotNull { q.scrambledTokens.getOrNull(it) }.joinToString(separator)
        val correct = assembled.equals(q.correctAnswer, ignoreCase = true)
        val elapsedMs = System.currentTimeMillis() - q.startedAtMs
        applyEnGameEvaluation(
            cur = cur,
            isCorrect = correct,
            elapsedMs = elapsedMs,
            isVoiceCritical = false
        )
    }

    /**
     * P6 Section 8.2: Interactive 4-pair Memory Flip matching game.
     * Matches English word card with Korean meaning card, updates SM-2 on each matched pair,
     * and advances to a fresh board when all 4 pairs are cleared.
     */
    fun tapEnGameMemoryFlipCard(cardIndex: Int) {
        val cur = _enGameState.value
        val q = cur.currentQuestion ?: return
        if (cur.phase != EnGamePhase.PLAYING || q.mode != EnGameMode.MEMORY_FLIP) return
        val card = q.memoryFlipCards.getOrNull(cardIndex) ?: return
        if (card.pairId in cur.matchedPairIds) return

        // If 2 mismatched cards were already open, start fresh from the newly tapped card
        val open = if (cur.flippedCardIndices.size >= 2) emptyList() else cur.flippedCardIndices
        if (cardIndex in open) return

        val nextOpen = open + cardIndex
        if (card.isEnglish) {
            listenRpgEnglish(card.label)
        }

        if (nextOpen.size < 2) {
            _enGameState.value = cur.copy(flippedCardIndices = nextOpen)
            return
        }

        val first = q.memoryFlipCards[nextOpen[0]]
        val second = q.memoryFlipCards[nextOpen[1]]
        val isMatch = first.pairId == second.pairId && first.isEnglish != second.isEnglish
        if (isMatch) {
            val nextMatched = cur.matchedPairIds + first.pairId
            val allPairsCount = q.memoryFlipCards.map { it.pairId }.distinct().size
            viewModelScope.launch {
                localStore.recordWordSm2Recall(wordId = first.wordId, grade = RecallGrade.GOOD)
                localStore.advanceVocabMission(MissionKind.DAILY_NEW_WORDS, 1)
                localStore.advanceVocabMission(MissionKind.WEEKLY_UNIQUE_WORDS, 1)
            }
            if (nextMatched.size >= allPairsCount) {
                val elapsedMs = System.currentTimeMillis() - q.startedAtMs
                applyEnGameEvaluation(
                    cur = cur.copy(flippedCardIndices = emptyList(), matchedPairIds = emptySet()),
                    isCorrect = true,
                    elapsedMs = elapsedMs.coerceAtMost(4500L),
                    isVoiceCritical = false
                )
            } else {
                _enGameState.value = cur.copy(
                    flippedCardIndices = emptyList(),
                    matchedPairIds = nextMatched,
                    score = cur.score + 45,
                    combo = cur.combo + 1,
                    maxCombo = maxOf(cur.maxCombo, cur.combo + 1),
                    hitPulseTick = cur.hitPulseTick + 1,
                    lastFeedbackKo = "짝 일치! +45점 (${nextMatched.size}/$allPairsCount 쌍 완료)",
                    lastSpeedTagKo = "MATCH!"
                )
            }
        } else {
            _enGameState.value = cur.copy(
                flippedCardIndices = nextOpen,
                combo = 0,
                missPulseTick = cur.missPulseTick + 1,
                lastFeedbackKo = "짝이 달라요! 위치를 기억하고 다시 뒤집어 보세요.",
                lastSpeedTagKo = "RETRY"
            )
        }
    }

    fun startEnGameVoiceBossAttack() {
        val cur = _enGameState.value
        val q = cur.currentQuestion ?: return
        if (cur.phase != EnGamePhase.PLAYING) return
        voiceEngine.startListening(
            locale = Locale.US,
            onResult = { spoken, durationMs ->
                val report = PronunciationAnalyzer.analyze(
                    targetText = q.audioTextEn,
                    recognizedText = spoken,
                    durationMs = durationMs
                )
                val passed = report.overallScore >= 45 || spoken.trim().split(Regex("\\s+")).size >= 2
                applyEnGameEvaluation(
                    cur = _enGameState.value,
                    isCorrect = passed,
                    elapsedMs = durationMs,
                    isVoiceCritical = true
                )
            }
        )
    }

    private fun applyEnGameEvaluation(
        cur: EnGameUiState,
        isCorrect: Boolean,
        elapsedMs: Long,
        isVoiceCritical: Boolean
    ) {
        val q = cur.currentQuestion ?: return
        val eval = EnGameMathEngine.evaluateAnswer(
            state = cur,
            isCorrect = isCorrect,
            elapsedMs = elapsedMs,
            isVoiceCritical = isVoiceCritical,
            correctLabel = q.correctAnswer
        )

        val nextScore = cur.score + eval.scoreDelta
        val nextHighScore = maxOf(cur.highScore, nextScore)
        val nextMaxCombo = maxOf(cur.maxCombo, eval.nextCombo)
        val nextLives = if (isCorrect) cur.lives else (cur.lives - 1).coerceAtLeast(0)
        val nextFeverTurns = when {
            eval.feverJustActivated -> 5
            cur.feverTurnsLeft > 0 && isCorrect -> (cur.feverTurnsLeft - 1).coerceAtLeast(0)
            !isCorrect -> 0
            else -> 0
        }
        val nextFeverGauge = if (nextFeverTurns == 0 && cur.feverTurnsLeft > 0) 0 else eval.nextFeverGauge

        var nextWave = cur.waveNumber
        var nextBossHp = cur.bossHp
        var nextBossMaxHp = cur.bossMaxHp
        var waveBonusFeedback = eval.feedbackKo
        if (cur.mode == EnGameMode.VOICE_BOSS_RUSH && isCorrect) {
            nextBossHp = (cur.bossHp - eval.bossDamage).coerceAtLeast(0)
            if (nextBossHp <= 0) {
                nextWave += 1
                nextBossMaxHp = 100 + (nextWave - 1) * 35
                nextBossHp = nextBossMaxHp
                waveBonusFeedback = "WAVE ${nextWave - 1} 보스 격파!! 다음 보스 출현! (+${eval.scoreDelta}점)"
            }
        }

        val updatedMissed = if (!isCorrect) {
            cur.missedWords + (q.audioTextEn to q.correctAnswer)
        } else {
            cur.missedWords
        }

        // Persist SM-2 recall, daily missions & wrong-answer notebook automatically
        val wordId = q.wordId
        if (!wordId.isNullOrBlank()) {
            viewModelScope.launch {
                val grade = if (isCorrect) RecallGrade.GOOD else RecallGrade.AGAIN
                localStore.recordWordSm2Recall(wordId = wordId, grade = grade)
                if (isCorrect) {
                    localStore.recordDailyWordOutcome(wordId, q.audioTextEn, q.correctAnswer, known = true)
                    localStore.advanceVocabMission(MissionKind.DAILY_NEW_WORDS, 1)
                    if (cur.mode == EnGameMode.VOICE_BOSS_RUSH) {
                        localStore.advanceVocabMission(MissionKind.DAILY_LISTENING_SPEAK, 1)
                    }
                } else {
                    localStore.recordDailyWordOutcome(wordId, q.audioTextEn, q.correctAnswer, known = false)
                }
            }
        }

        val updatedState = cur.copy(
            eloRating = eval.nextElo,
            score = nextScore,
            highScore = nextHighScore,
            combo = eval.nextCombo,
            maxCombo = nextMaxCombo,
            lives = nextLives,
            feverGauge = nextFeverGauge,
            feverTurnsLeft = nextFeverTurns,
            waveNumber = nextWave,
            bossHp = nextBossHp,
            bossMaxHp = nextBossMaxHp,
            totalAnswered = cur.totalAnswered + 1,
            correctCount = cur.correctCount + (if (isCorrect) 1 else 0),
            selectedTokens = emptyList(),
            lastFeedbackKo = waveBonusFeedback,
            lastSpeedTagKo = eval.speedTagKo,
            lastScoreDelta = eval.scoreDelta,
            hitPulseTick = if (isCorrect) cur.hitPulseTick + 1 else cur.hitPulseTick,
            missPulseTick = if (!isCorrect) cur.missPulseTick + 1 else cur.missPulseTick,
            missedWords = updatedMissed
        )

        if (nextLives <= 0) {
            finishEnGameSession(updatedState)
            return
        }

        val nextQuestion = nextEnGameQuestion(mode = updatedState.mode, elo = updatedState.eloRating)
        _enGameState.value = updatedState.copy(currentQuestion = nextQuestion)
        if (updatedState.mode == EnGameMode.VOICE_BOSS_RUSH) {
            listenRpgEnglish(nextQuestion.audioTextEn)
        }
    }

    private fun nextEnGameQuestion(mode: EnGameMode, elo: Int): com.example.engine.EnGameQuestion {
        val vocabPool = (VocabRepository.allWords + harvestedApiWords.value).distinctBy { it.id }
        val travelSentences = TravelCurriculumRepository.allScenarios.flatMap { it.sentences }
        val q = EnGameMathEngine.generateQuestion(
            mode = mode,
            eloRating = elo,
            vocabPool = vocabPool,
            weakWordIds = weakWordIds.value,
            travelSentences = travelSentences,
            recentIds = enGameRecentWordIds.toSet()
        )
        q.wordId?.let { id ->
            enGameRecentWordIds.addLast(id)
            while (enGameRecentWordIds.size > 12) enGameRecentWordIds.removeFirst()
        }
        return q
    }

    private fun finishEnGameSession(finalState: EnGameUiState) {
        if (finalState.phase == EnGamePhase.RESULT) return
        enGameTimerJob?.cancel()
        stopVoiceIo()
        val baseSpec = RewardConfig.specFor(RewardEventType.MINIGAME_COMPLETED)
        val coins = (baseSpec.coins + finalState.score / 30).coerceIn(5, 80)
        val xp = (baseSpec.xp + finalState.correctCount * 3).coerceIn(12, 120)
        val castleBonus = if (finalState.correctCount >= 5) 5 else 2
        val sessionId = "engame_${finalState.mode.name}_${finalState.score}_${finalState.totalAnswered}_${System.currentTimeMillis() / 10000L}"

        _enGameState.value = finalState.copy(
            phase = EnGamePhase.RESULT,
            earnedCoins = coins,
            earnedXp = xp,
            currentQuestion = null
        )

        viewModelScope.launch {
            val ledgerResult = localStore.grantIdempotentReward(
                eventId = sessionId,
                eventType = RewardEventType.MINIGAME_COMPLETED,
                xpDelta = xp,
                coinDelta = coins,
                reason = "EnGame ${finalState.mode.titleKo} 완료 (${finalState.score}점)",
                sessionId = sessionId
            )
            if (finalState.correctCount >= 3) {
                localStore.advanceVocabMission(MissionKind.DAILY_REVIEW_DUE, 1)
            }
            val result = localStore.recordLearningAchievement(
                xpDelta = 0,
                coinsDelta = 0,
                wordsDelta = finalState.correctCount.coerceAtLeast(1),
                castleHpDelta = castleBonus,
                missionCompleted = finalState.correctCount >= 5
            )
            if (ledgerResult.leveledUpTo != null) {
                showToast("레벨 업! Lv.${ledgerResult.leveledUpTo.level} ${ledgerResult.leveledUpTo.titleKo}!")
            }
            notifySpecialMilestones(result)
            flushActiveStudyTimeLog(showConfirmation = false)
        }
    }

    private fun notifySpecialMilestones(result: LearningAchievementResult) {
        if (result.streakMilestoneHit != null) {
            _streakMilestoneEvent.value = StreakMilestoneEvent(
                newStreakCount = result.streakMilestoneHit,
                todayWordsCompleted = result.todayWordsCompleted
            )
        }
        if (!_firebaseUserUid.value.isNullOrBlank()) {
            refreshFamilyLeaderboard(manual = false)
        }
        notifySpecialMilestones(result.unlockedBoss, result.leveledUp)
    }

    private fun notifySpecialMilestones(bossUnlocked: Boolean, leveledUp: Boolean) {
        when {
            bossUnlocked -> showToast("7회 미션 돌파!! 난이도 상승 + 시험 마왕 보스전이 열렸습니다!")
            leveledUp -> showToast("레벨 업!! 미스터리 보물상자 기운이 모였습니다!")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        loopJob?.cancel()
        callClockJob?.cancel()
        studyStopwatchJob?.cancel()
        enGameTimerJob?.cancel()
        familyLeaderboardSyncJob?.cancel()
        voiceEngine.release()
    }
}
