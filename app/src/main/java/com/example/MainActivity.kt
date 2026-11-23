package com.example

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DadKingdomLocalStore
import com.example.data.LlmSlotStore
import com.example.data.SquirrelRpgRepository
import com.example.engine.HybridAiCoachEngine
import com.example.engine.VoiceSpeechEngine
import com.example.firebase.configureAppCheck
import com.example.model.RpgGamePhase
import com.example.ui.components.StreakMilestoneConfettiOverlay
import com.example.ui.screens.AiTutorCallScreen
import com.example.ui.screens.HubAndShopScreen
import com.example.ui.screens.SpartaVocabScreen
import com.example.ui.screens.SquirrelHeroPickerOverlay
import com.example.ui.screens.SquirrelStoryAndBattleOverlay
import com.example.ui.screens.TravelScenariosScreen
import com.example.ui.screens.WorldMapScreen
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.SpeakQuestTheme
import com.example.viewmodel.SpeakQuestViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FamilyKingdomTab(
    val route: String,
    val labelKo: String,
    val activityLogName: String,
    val icon: ImageVector,
    val tag: String
) {
    KINGDOM("kingdom_home", "왕국홈", "왕국 홈", Icons.Default.Castle, "nav_kingdom_home"),
    VOCAB("vocab_pattern", "단어·패턴", "단어·패턴 학습", Icons.Default.LocalFireDepartment, "nav_vocab_pattern"),
    TRAVEL("travel_100", "여행100", "여행 회화 100", Icons.Default.FlightTakeoff, "nav_travel"),
    VIDEO_CALL("video_tutor", "회화학습", "원어민 회화학습", Icons.Default.RecordVoiceOver, "nav_video_call"),
    FAMILY_HUB("family_hub", "통계·보물", "통계·보물 허브", Icons.Default.CardGiftcard, "nav_family_hub")
}

class MainActivity : ComponentActivity() {

    private val _pendingKakaoAuthCode = MutableStateFlow<String?>(null)
    val pendingKakaoAuthCode: StateFlow<String?> = _pendingKakaoAuthCode.asStateFlow()

    private val _pendingGuestInvite = MutableStateFlow<Pair<String, String>?>(null)
    val pendingGuestInvite: StateFlow<Pair<String, String>?> = _pendingGuestInvite.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // MUST configure Firebase App Check before setContent or any Firebase AI Logic calls
        configureAppCheck(this, intent)
        extractIncomingDeepLinks(intent)

        setContent {
            SpeakQuestTheme {
                FamilyKingdomApp(
                    pendingKakaoAuthCodeFlow = pendingKakaoAuthCode,
                    onConsumeKakaoCode = { _pendingKakaoAuthCode.value = null },
                    pendingGuestInviteFlow = pendingGuestInvite,
                    onConsumeGuestInvite = { _pendingGuestInvite.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingDeepLinks(intent)
    }

    private fun extractIncomingDeepLinks(intent: Intent?) {
        val data = intent?.data ?: return
        // 1. Kakao OAuth redirect
        if (data.scheme == "dadquest" && data.host == "kakao-oauth") {
            val code = data.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                _pendingKakaoAuthCode.value = code
            }
            return
        }
        // 2. Standalone Guest Invite deep link (dadquest://invite or https://dadquest.app/invite)
        val isCustomInvite = data.scheme == "dadquest" && data.host == "invite"
        val isWebInvite = (data.scheme == "https" || data.scheme == "http") &&
            (data.path?.startsWith("/invite") == true)
        if (isCustomInvite || isWebInvite) {
            val inviteCode = data.getQueryParameter("code")
            val hostNickname = data.getQueryParameter("host").orEmpty().ifBlank { "원삐" }
            if (!inviteCode.isNullOrBlank()) {
                _pendingGuestInvite.value = inviteCode to hostNickname
            }
        }
    }
}

@Composable
private fun FamilyKingdomApp(
    pendingKakaoAuthCodeFlow: StateFlow<String?>,
    onConsumeKakaoCode: () -> Unit,
    pendingGuestInviteFlow: StateFlow<Pair<String, String>?>,
    onConsumeGuestInvite: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: SpeakQuestViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val localStore = DadKingdomLocalStore(app)
                val slotStore = LlmSlotStore(app)
                val voiceEngine = VoiceSpeechEngine(app)
                val aiCoachEngine = HybridAiCoachEngine(slotStore)
                SpeakQuestViewModel(
                    localStore = localStore,
                    llmSlotStore = slotStore,
                    voiceEngine = voiceEngine,
                    aiCoachEngine = aiCoachEngine
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableStateOf(FamilyKingdomTab.KINGDOM) }
    val snackbarHostState = remember { SnackbarHostState() }

    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val kingdomStats by viewModel.kingdomStats.collectAsStateWithLifecycle()
    val parents by viewModel.parentProfiles.collectAsStateWithLifecycle()
    val masteredWordIds by viewModel.masteredWordIds.collectAsStateWithLifecycle()
    val harvestedApiWords by viewModel.harvestedApiWords.collectAsStateWithLifecycle()
    val vocabGradeTier by viewModel.vocabGradeTier.collectAsStateWithLifecycle()
    val scenarioProgress by viewModel.scenarioProgressState.collectAsStateWithLifecycle()
    val dadMissions by viewModel.dadMissions.collectAsStateWithLifecycle()
    val giftRequests by viewModel.giftRequests.collectAsStateWithLifecycle()
    val scorecards by viewModel.scorecards.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val studyLogs by viewModel.studyLogs.collectAsStateWithLifecycle()
    val activeStudySec by viewModel.activeStudyElapsedSec.collectAsStateWithLifecycle()
    val kakaoSession by viewModel.kakaoSession.collectAsStateWithLifecycle()
    val guestInviteSession by viewModel.guestInviteSession.collectAsStateWithLifecycle()
    val cloudflareWorkerUrl by viewModel.cloudflareWorkerUrl.collectAsStateWithLifecycle()
    val cloudflareStatusKo by viewModel.cloudflareStatusKo.collectAsStateWithLifecycle()
    val harvestStatusKo by viewModel.harvestStatusKo.collectAsStateWithLifecycle()
    val isHarvestingWords by viewModel.isHarvestingWords.collectAsStateWithLifecycle()

    val spartaState by viewModel.spartaState.collectAsStateWithLifecycle()
    val patternState by viewModel.patternState.collectAsStateWithLifecycle()
    val travelState by viewModel.travelState.collectAsStateWithLifecycle()
    val tutorCallState by viewModel.tutorCallState.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val engineMode by viewModel.engineMode.collectAsStateWithLifecycle()
    val llmSlots by viewModel.llmSlots.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val handsFreeMode by viewModel.handsFreeMode.collectAsStateWithLifecycle()
    val slotTestResults by viewModel.slotTestResults.collectAsStateWithLifecycle()
    val bannerToast by viewModel.bannerToast.collectAsStateWithLifecycle()
    val pendingKakaoCode by pendingKakaoAuthCodeFlow.collectAsStateWithLifecycle()
    val pendingGuestInvite by pendingGuestInviteFlow.collectAsStateWithLifecycle()

    val selectedSquirrelHeroId by viewModel.selectedSquirrelHeroId.collectAsStateWithLifecycle()
    val clearedRpgChapters by viewModel.clearedRpgChapters.collectAsStateWithLifecycle()
    val unopenedChests by viewModel.unopenedChests.collectAsStateWithLifecycle()
    val openedTreasures by viewModel.openedTreasures.collectAsStateWithLifecycle()
    val rpgState by viewModel.rpgState.collectAsStateWithLifecycle()
    val streakMilestoneEvent by viewModel.streakMilestoneEvent.collectAsStateWithLifecycle()
    val activeSquirrelHero = remember(selectedSquirrelHeroId) {
        SquirrelRpgRepository.findHeroById(selectedSquirrelHeroId)
    }

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val totalStudySec = remember(studyLogs, activeStudySec) {
        studyLogs.sumOf { it.durationSec } + activeStudySec
    }
    val todayStudySec = remember(studyLogs, activeStudySec) {
        studyLogs.filter { it.dateIso == todayIso }.sumOf { it.durationSec } + activeStudySec
    }

    LaunchedEffect(currentTab) {
        viewModel.onEnterStudyActivity(currentTab.activityLogName)
    }

    LaunchedEffect(pendingKakaoCode) {
        val code = pendingKakaoCode
        if (!code.isNullOrBlank()) {
            onConsumeKakaoCode()
            viewModel.handleKakaoOAuthRedirectCode(code)
            currentTab = FamilyKingdomTab.FAMILY_HUB
        }
    }

    LaunchedEffect(pendingGuestInvite) {
        val invite = pendingGuestInvite
        if (invite != null) {
            onConsumeGuestInvite()
            viewModel.acceptGuestInviteCode(
                rawCode = invite.first,
                hostNickname = invite.second,
                fromDeepLink = true
            )
            currentTab = FamilyKingdomTab.KINGDOM
        }
    }

    LaunchedEffect(bannerToast) {
        val message = bannerToast
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBannerToast()
        }
    }

    if (currentTab != FamilyKingdomTab.KINGDOM) {
        BackHandler {
            viewModel.stopVoiceIo()
            currentTab = FamilyKingdomTab.KINGDOM
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                FamilyKingdomTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentTab != tab) {
                                viewModel.stopVoiceIo()
                                currentTab = tab
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.labelKo
                            )
                        },
                        label = {
                            Text(
                                text = tab.labelKo,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AdventureEmerald,
                            selectedTextColor = AdventureEmerald,
                            indicatorColor = AdventureEmerald.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                FamilyKingdomTab.KINGDOM -> {
                    WorldMapScreen(
                        progress = progress,
                        kingdomStats = kingdomStats,
                        parents = parents,
                        vocabGradeTier = vocabGradeTier,
                        masteredWordsCount = masteredWordIds.size,
                        harvestedApiCount = harvestedApiWords.size,
                        harvestStatusKo = harvestStatusKo,
                        todayStudySeconds = todayStudySec,
                        totalStudySeconds = totalStudySec,
                        studyLogCount = studyLogs.size,
                        scenarioProgress = scenarioProgress,
                        activeTutor = tutorCallState.selectedTutor,
                        activeSquirrelHero = activeSquirrelHero,
                        clearedRpgChapters = clearedRpgChapters,
                        unopenedChests = unopenedChests,
                        onSwitchPlayer = { name -> viewModel.selectActivePlayer(name) },
                        onOpenHeroPicker = { viewModel.openSquirrelHeroPicker() },
                        onStartRpgChapter = { chapterNum -> viewModel.startRpgChapter(chapterNum) },
                        onOpenMysteryChest = { viewModel.openMysteryTreasureChest() },
                        onStartVocabTrack = { track ->
                            viewModel.startSmartVocabSession(track = track, tier1To5 = vocabGradeTier)
                            currentTab = FamilyKingdomTab.VOCAB
                        },
                        onStartLevelTest = {
                            viewModel.startQuickLevelTest()
                            currentTab = FamilyKingdomTab.VOCAB
                        },
                        onStartBossBattle = {
                            viewModel.startBossBattle()
                            currentTab = FamilyKingdomTab.VOCAB
                        },
                        onOpenTravelZone = { zoneId ->
                            viewModel.selectTravelZone(zoneId)
                            currentTab = FamilyKingdomTab.TRAVEL
                        },
                        onOpenTutorVideoCall = {
                            currentTab = FamilyKingdomTab.VIDEO_CALL
                        },
                        onOpenFamilyGiftHub = {
                            currentTab = FamilyKingdomTab.FAMILY_HUB
                        },
                        onHarvestDailyWords = {
                            viewModel.harvestDaily50WordsFromFreeApi()
                        },
                        onRefillHearts = {
                            viewModel.refillHearts()
                        },
                        onSpeakFocusEnglish = { text ->
                            viewModel.listenRpgEnglish(text)
                        },
                        onRecordFocusWordResult = { wordId, known ->
                            viewModel.recordMainFocusWordResult(wordId, known)
                        }
                    )
                }

                FamilyKingdomTab.VOCAB -> {
                    SpartaVocabScreen(
                        state = spartaState,
                        patternState = patternState,
                        voiceState = voiceState,
                        activeStudySec = activeStudySec,
                        harvestedApiCount = harvestedApiWords.size,
                        harvestStatusKo = harvestStatusKo,
                        isHarvestingWords = isHarvestingWords,
                        onSelectTier1To5 = { tier -> viewModel.selectVocabTier1To5(tier) },
                        onStartLevelTest = { viewModel.startQuickLevelTest() },
                        onSelectTrack = { track -> viewModel.startSmartVocabSession(track = track) },
                        onSelectTopic = { topic ->
                            viewModel.startSmartVocabSession(
                                track = com.example.model.VocabStudyTrack.TOPIC_SITUATION,
                                topic = topic
                            )
                        },
                        onSelectLearningMethod = { method -> viewModel.selectVocabLearningMethod(method) },
                        onToggleRevealAnswer = { viewModel.toggleRevealVocabAnswer() },
                        onMarkWordKnowledge = { status -> viewModel.markWordKnowledge(status) },
                        onFlushStudyLog = { viewModel.flushActiveStudyTimeLog(showConfirmation = true) },
                        onStartPreviewMode = {
                            viewModel.startSmartVocabSession(previewFirst = true)
                        },
                        onCompletePreview = { viewModel.completePreviewAndStartQuiz() },
                        onSubmitAnswer = { meaning -> viewModel.submitSpartaQuizAnswer(meaning) },
                        onSelectExampleIndex = { idx -> viewModel.selectVocabExample(idx) },
                        onSetVocabLoopMode = { mode -> viewModel.setVocabLoopMode(mode) },
                        onListenWord = { viewModel.listenSpartaWordOnly() },
                        onListenSentence = { viewModel.listenSpartaSentence() },
                        onListenAllExamples = { viewModel.listenAllWordExamplesSequentially() },
                        onStartVoiceShadowing = { viewModel.startSpartaShadowingSpeech() },
                        onStopVoice = { viewModel.stopVoiceIo() },
                        onEvaluateTypedShadowing = { spoken -> viewModel.evaluateSpartaShadowing(spoken) },
                        onNextWord = { viewModel.advanceToNextSpartaWord() },
                        onRestartSession = { viewModel.startSmartVocabSession() },
                        onHarvestDaily50Words = { viewModel.harvestDaily50WordsFromFreeApi() },
                        onSelectPatternDrill = { idx -> viewModel.selectPatternDrill(idx) },
                        onSwapPatternWord = { key, idx -> viewModel.swapPatternSlotWord(key, idx) },
                        onSetPatternLoopMode = { mode -> viewModel.setPatternLoopMode(mode) },
                        onTogglePatternKorean = { viewModel.togglePatternKorean() },
                        onSpeakPattern = { viewModel.speakPatternCurrent() },
                        onStartPatternSpeech = { viewModel.startPatternSpeechPractice() },
                        onEvaluateTypedPattern = { spoken -> viewModel.evaluatePatternSpeech(spoken) }
                    )
                }

                FamilyKingdomTab.TRAVEL -> {
                    TravelScenariosScreen(
                        state = travelState,
                        scenarioProgress = scenarioProgress,
                        voiceState = voiceState,
                        speechRate = speechRate,
                        onSelectZone = { zoneId -> viewModel.selectTravelZone(zoneId) },
                        onSelectScenario = { sc -> viewModel.selectTravelScenario(sc) },
                        onSelectSentence = { idx -> viewModel.selectTravelSentence(idx) },
                        onToggleKorean = { viewModel.toggleTravelKorean() },
                        onSetSpeechRate = { rate -> viewModel.setSpeechRateMultiplier(rate) },
                        onListenSentence = { viewModel.listenActiveTravelSentence() },
                        onStartSentenceSpeech = { viewModel.startTravelSentenceSpeech() },
                        onEvaluateTypedSentence = { spoken -> viewModel.evaluateTravelSentence(spoken) },
                        onSelectTwist = { idx -> viewModel.selectTravelTwist(idx) },
                        onPlayTwistPrompt = { viewModel.playActiveTwistPrompt() },
                        onStartTwistSpeech = { viewModel.startTwistVoiceResponse() },
                        onSubmitTypedTwist = { text -> viewModel.submitTwistResponse(text) },
                        onStopVoice = { viewModel.stopVoiceIo() }
                    )
                }

                FamilyKingdomTab.VIDEO_CALL -> {
                    AiTutorCallScreen(
                        state = tutorCallState,
                        activePlayerName = parents.daughterName,
                        voiceState = voiceState,
                        engineMode = engineMode,
                        speechRate = speechRate,
                        handsFreeMode = handsFreeMode,
                        onSelectTutor = { tutor -> viewModel.selectHumanTutor(tutor) },
                        onSelectTopic = { tag, starter -> viewModel.setTutorConversationTopic(context, tag, starter) },
                        onSynthesizeNanoBanana = { tag, gesture ->
                            viewModel.synthesizeTutorSceneWithNanoBanana(context, tag, gesture)
                        },
                        onToggleKoreanSubtitles = { viewModel.toggleTutorKoreanTranslations() },
                        onToggleKoreanVoiceMode = { viewModel.toggleKoreanVoiceInputMode() },
                        onSetSpeechRate = { rate -> viewModel.setSpeechRateMultiplier(rate) },
                        onSetHandsFree = { enabled -> viewModel.setHandsFreeMode(enabled) },
                        onStartListening = { viewModel.startTutorLiveListening() },
                        onStopVoice = { viewModel.stopVoiceIo() },
                        onSendTypedMessage = { text -> viewModel.sendUserMessageToTutor(text) },
                        onReplayTutorText = { text -> viewModel.speakWithActiveTutor(text) }
                    )
                }

                FamilyKingdomTab.FAMILY_HUB -> {
                    HubAndShopScreen(
                        progress = progress,
                        kingdomStats = kingdomStats,
                        parents = parents,
                        vocabGradeTier = vocabGradeTier,
                        masteredWordsCount = masteredWordIds.size,
                        harvestedApiCount = harvestedApiWords.size,
                        clearedRpgChaptersCount = clearedRpgChapters.size,
                        unopenedChests = unopenedChests,
                        openedTreasures = openedTreasures,
                        giftRequests = giftRequests,
                        dadMissions = dadMissions,
                        scorecards = scorecards,
                        schedules = schedules,
                        studyLogs = studyLogs,
                        activeStudyElapsedSec = activeStudySec,
                        kakaoSession = kakaoSession,
                        guestInviteSession = guestInviteSession,
                        cloudflareWorkerUrl = cloudflareWorkerUrl,
                        cloudflareStatusKo = cloudflareStatusKo,
                        engineMode = engineMode,
                        llmSlots = llmSlots,
                        slotTestResults = slotTestResults,
                        onOpenMysteryChest = { viewModel.openMysteryTreasureChest() },
                        onSaveParentProfiles = { updated -> viewModel.saveParentProfiles(updated) },
                        onCreateGiftRequest = { target, title, reason ->
                            viewModel.createGiftRequest(target, title, reason)
                        },
                        onApproveGiftWithPin = { giftId, pin ->
                            viewModel.approveGiftRequestWithPin(giftId, pin)
                        },
                        onShareGiftToKakao = { gift ->
                            viewModel.shareGiftRequestToKakao(context, gift)
                        },
                        onAddDadMission = { title, desc, won ->
                            viewModel.addDadAllowanceMission(title, desc, won)
                        },
                        onMarkMissionDone = { id -> viewModel.markMissionDoneByDaughter(id) },
                        onApproveMissionWithPin = { id, pin -> viewModel.approveMissionWithPin(id, pin) },
                        onAddScorecard = { exam, subject, score, target ->
                            viewModel.addScorecardEntry(exam, subject, score, target)
                        },
                        onAddSchedule = { day, time, title, detail ->
                            viewModel.addStudySchedule(day, time, title, detail)
                        },
                        onToggleScheduleDone = { id -> viewModel.toggleStudyScheduleCompleted(id) },
                        onFlushStudyLog = { viewModel.flushActiveStudyTimeLog(showConfirmation = true) },
                        onShareFamilyReportToKakao = { viewModel.shareFamilyReportToKakao(context) },
                        onLaunchKakaoLogin = { viewModel.launchKakaoLogin(context) },
                        onRefreshKakaoFriends = { viewModel.refreshKakaoFriends() },
                        onLogoutKakao = { viewModel.logoutKakao() },
                        onGenerateAndShareGuestInvite = { viewModel.generateAndShareGuestInvite(context) },
                        onRegenerateInviteCode = { viewModel.regenerateGuestInviteCode() },
                        onAcceptGuestInviteCode = { code -> viewModel.acceptGuestInviteCode(code) },
                        onRegisterCloudflareLevel = { url -> viewModel.saveAndRegisterCloudflareLevel(url) },
                        onBuyCollectible = { item -> viewModel.buyCollectible(item) },
                        onRefillHearts = { viewModel.refillHearts() },
                        onSelectEngineMode = { mode -> viewModel.setAiEngineMode(mode) },
                        onSaveLlmSlot = { slot -> viewModel.saveLlmSlot(slot) },
                        onTestLlmSlot = { slot -> viewModel.testLlmSlot(slot) }
                    )
                }
            }
        }
    }

        // Full-Screen White Pygmy Squirrel 10-Job Hero Picker Overlay
        if (rpgState.phase == RpgGamePhase.HERO_PICKER) {
            SquirrelHeroPickerOverlay(
                selectedHeroId = selectedSquirrelHeroId,
                playerNickname = parents.daughterName,
                onSelectHero = { hero -> viewModel.selectSquirrelHero(hero) },
                onClose = { viewModel.closeRpgOverlay() }
            )
        }

        // Full-Screen White Pygmy Squirrel RPG Story + Turn-Based Battle + Sealed Mystery Chest Overlay
        if (rpgState.phase != RpgGamePhase.IDLE && rpgState.phase != RpgGamePhase.HERO_PICKER) {
            SquirrelStoryAndBattleOverlay(
                state = rpgState,
                hero = activeSquirrelHero,
                playerNickname = parents.daughterName,
                voiceState = voiceState,
                onClose = { viewModel.closeRpgOverlay() },
                onChooseStoryOption = { choice -> viewModel.chooseRpgStoryOption(choice) },
                onStartChapterBattle = { viewModel.startChapterBattle() },
                onChooseBattleAction = { action -> viewModel.chooseRpgBattleAction(action) },
                onSubmitWordAnswer = { ans -> viewModel.submitRpgWordAnswer(ans) },
                onSubmitPhraseAnswer = { ans -> viewModel.submitRpgPhraseAnswer(ans) },
                onStartVoicePhraseAttack = { viewModel.startRpgVoicePhraseAttack() },
                onStopVoice = { viewModel.stopVoiceIo() },
                onListenEnglish = { text -> viewModel.listenRpgEnglish(text) },
                onOpenMysteryChest = { viewModel.openMysteryTreasureChest() },
                onNextChapter = { viewModel.advanceOrReplayRpgChapter() },
                onRetryChapter = { viewModel.retryCurrentRpgChapter() }
            )
        }

        // Full-Screen Congratulations Confetti Celebration when a 10-Word Daily Goal Streak milestone is hit
        val activeStreakEvent = streakMilestoneEvent
        if (activeStreakEvent != null) {
            StreakMilestoneConfettiOverlay(
                event = activeStreakEvent,
                hero = activeSquirrelHero,
                onDismiss = { viewModel.dismissStreakMilestoneEvent() }
            )
        }
    }
}
