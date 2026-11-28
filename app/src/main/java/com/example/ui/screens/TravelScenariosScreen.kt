package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.TravelCurriculumRepository
import com.example.engine.VoiceEngineState
import com.example.model.AiHumanTutor
import com.example.model.ScenarioProgress
import com.example.model.TravelScenario
import androidx.compose.material.icons.outlined.RecordVoiceOver
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.GoogleStreetViewStage
import com.example.ui.components.PronunciationReportCard
import com.example.ui.components.StitchIconBadge
import com.example.ui.components.StitchIconPillTab
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.VoiceWaveformOrb3D
import com.example.ui.components.travelZoneStitchIcon
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.TrophyGoldShadow
import com.example.viewmodel.TravelPracticeState

@Composable
fun TravelScenariosScreen(
    state: TravelPracticeState,
    activePlayerName: String,
    selectedTutor: AiHumanTutor,
    scenarioProgress: List<ScenarioProgress>,
    voiceState: VoiceEngineState,
    speechRate: Float,
    onSelectZone: (Int) -> Unit,
    onSelectScenario: (TravelScenario) -> Unit,
    onSelectSentence: (Int) -> Unit,
    onToggleKorean: () -> Unit,
    onSetSpeechRate: (Float) -> Unit,
    onListenSentence: () -> Unit,
    onStartSentenceSpeech: () -> Unit,
    onEvaluateTypedSentence: (String) -> Unit,
    onSelectTwist: (Int) -> Unit,
    onPlayTwistPrompt: () -> Unit,
    onStartTwistSpeech: () -> Unit,
    onSubmitTypedTwist: (String) -> Unit,
    onTriggerProactiveSpeak: () -> Unit,
    onReplayProactiveLine: () -> Unit,
    onStopVoice: () -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = 30 Sentences, 1 = Dynamic Twists
    var typedInput by remember(state.activeScenario.id, state.activeSentenceIndex, activeSubTab) {
        mutableStateOf("")
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (activeSubTab == 0) onStartSentenceSpeech() else onStartTwistSpeech()
        }
    }

    fun requestOrStartMic() {
        if (voiceState.isListening) {
            onStopVoice()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (activeSubTab == 0) onStartSentenceSpeech() else onStartTwistSpeech()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val scenariosInZone = remember(state.selectedZoneId) {
        TravelCurriculumRepository.scenariosForZone(state.selectedZoneId)
    }
    val progressByScenario = remember(scenarioProgress) {
        scenarioProgress.associateBy { it.scenarioId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Google Stitch Iconified 10 Adventure Travel Zones
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(TravelCurriculumRepository.zones, key = { it.id }) { zone ->
                    val selected = zone.id == state.selectedZoneId
                    val accent = Color(zone.accentHex)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectZone(zone.id) }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                            .testTag("travel_zone_${zone.id}")
                    ) {
                        StitchIconBadge(
                            icon = travelZoneStitchIcon(zone.id),
                            contentDescription = zone.titleKo,
                            accent = accent,
                            filled = selected,
                            size = 50.dp,
                            iconSize = 23.dp,
                            cornerBadgeText = "${zone.id}"
                        )
                        Text(
                            text = zone.titleKo.take(4),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                            ),
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 1-B. Real Google Maps 360° Street View + SVG+CSS Lip-Sync Proactive AI Companion
        item {
            GoogleStreetViewStage(
                zoneId = state.selectedZoneId,
                scenarioTitleKo = state.activeScenario.titleKo,
                activePlayerName = activePlayerName,
                tutor = selectedTutor,
                isListening = voiceState.isListening,
                isSpeaking = voiceState.isSpeaking,
                isThinking = state.proactiveLoading || state.twistLoading,
                rmsDb = voiceState.rmsDb,
                speechRate = speechRate,
                speechBeatIndex = voiceState.speechBeatIndex,
                proactiveLineEn = state.proactiveLineEn,
                proactiveLineKo = state.proactiveLineKo,
                proactiveThoughtKo = state.proactiveThoughtKo,
                proactiveMood = state.proactiveMood,
                proactiveEngineBadge = state.proactiveEngineBadge,
                showKorean = state.showKoreanSubtitles,
                onTriggerProactiveSpeak = onTriggerProactiveSpeak,
                onReplayProactiveLine = onReplayProactiveLine,
                onMicClick = { requestOrStartMic() }
            )
        }

        // 2. 10 Scenarios inside the selected Zone (1..100 total)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scenariosInZone, key = { it.id }) { scenario ->
                    val selected = scenario.id == state.activeScenario.id
                    val prog = progressByScenario[scenario.id]
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) SkyQuestBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (selected) 2.dp else 1.2.dp,
                            if (selected) SkyQuestBlue else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .width(195.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectScenario(scenario) }
                            .testTag("travel_scenario_${scenario.number}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${scenario.number} · +${scenario.coinReward}코인",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TrophyGoldShadow
                                )
                                if (prog?.twistCleared == true) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "클리어", tint = AdventureEmerald, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = scenario.titleKo,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Text(
                                text = "30문장 · 완료 ${prog?.completedSentencesCount ?: 0}/30",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Google Stitch Icon Mode Dock: 30문장 말하기 vs 돌발 대응
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StitchIconPillTab(
                        icon = Icons.Outlined.RecordVoiceOver,
                        label = "30문장",
                        selected = activeSubTab == 0,
                        accent = SkyQuestBlue,
                        tag = "travel_tab_sentences",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubTab = 0 }
                    )
                    StitchIconPillTab(
                        icon = Icons.Outlined.Bolt,
                        label = "돌발 3종",
                        selected = activeSubTab == 1,
                        accent = CoralOrange,
                        tag = "travel_tab_twists",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubTab = 1 }
                    )
                }
            }
        }

        if (activeSubTab == 0) {
            // === TAB A: 30 SENTENCES LISTEN & SPEAK STUDIO ===
            val sentence = state.activeScenario.sentences[state.activeSentenceIndex]

            item {
                // Numbered 1..30 Selector Strip
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(state.activeScenario.sentences) { idx, item ->
                        val isActive = idx == state.activeSentenceIndex
                        val isDone = idx in state.completedIndices
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isActive -> SkyQuestBlue
                                isDone -> AdventureEmerald.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(1.dp, if (isActive) SkyQuestBlue else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .size(width = 44.dp, height = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSentence(idx) }
                                .testTag("travel_sentence_num_${item.index}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${item.index}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when {
                                        isActive -> Color.White
                                        isDone -> AdventureEmeraldShadow
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.3f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SkyQuestBlue.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "#${state.activeScenario.number} ${state.activeScenario.titleKo} · ${sentence.phaseKo} (${sentence.index}/30)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SkyQuestBlueShadow,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleKorean,
                                modifier = Modifier.testTag("travel_toggle_korean")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = "한국어 해석 토글",
                                    tint = if (state.showKoreanSubtitles) SkyQuestBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = sentence.english,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.showKoreanSubtitles) {
                            Text(
                                text = sentence.korean,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${sentence.ipaHint}\n${sentence.speakingTipKo}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AdventureEmeraldShadow
                        )

                        // Gliglish-style Speech Speed Pills (0.6x / 0.8x / 1.0x / 1.2x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("속도:", style = MaterialTheme.typography.labelMedium)
                            listOf(0.6f, 0.8f, 1.0f, 1.2f).forEach { speed ->
                                val active = kotlin.math.abs(speechRate - speed) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) SkyQuestBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSetSpeechRate(speed) }
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Tactile3DButton(
                                text = "원어민 듣기",
                                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                containerColor = SkyQuestBlue,
                                shadowColor = SkyQuestBlueShadow,
                                height = 50.dp,
                                modifier = Modifier.weight(1f),
                                tag = "travel_listen_sentence",
                                onClick = onListenSentence
                            )
                            Spacer(Modifier.width(12.dp))
                            VoiceWaveformOrb3D(
                                isListening = voiceState.isListening,
                                isSpeaking = voiceState.isSpeaking,
                                rmsDb = voiceState.rmsDb,
                                onClick = { requestOrStartMic() },
                                tag = "travel_sentence_mic_orb"
                            )
                        }

                        if (voiceState.partialTranscript.isNotBlank()) {
                            Text(
                                text = "실시간 인식: \"${voiceState.partialTranscript}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyQuestBlueShadow
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = typedInput,
                                onValueChange = { typedInput = it },
                                placeholder = { Text("문장 직접 입력 발음 체크") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (typedInput.isNotBlank()) {
                                        onEvaluateTypedSentence(typedInput)
                                        typedInput = ""
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "평가", tint = AdventureEmerald)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Tactile3DButton(
                                text = "이전 문장",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                shadowColor = MaterialTheme.colorScheme.outline,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                height = 44.dp,
                                enabled = state.activeSentenceIndex > 0,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectSentence(state.activeSentenceIndex - 1) }
                            )
                            Spacer(Modifier.width(10.dp))
                            Tactile3DButton(
                                text = "다음 문장 (${(state.activeSentenceIndex + 2).coerceAtMost(30)}/30)",
                                containerColor = AdventureEmerald,
                                shadowColor = AdventureEmeraldShadow,
                                height = 44.dp,
                                enabled = state.activeSentenceIndex < 29,
                                modifier = Modifier.weight(1f),
                                tag = "travel_next_sentence",
                                onClick = { onSelectSentence(state.activeSentenceIndex + 1) }
                            )
                        }
                    }
                }
            }
        } else {
            // === TAB B: DYNAMIC TRAVEL TWIST (변수 대응 시뮬레이터) ===
            val twist = state.activeScenario.twists[state.activeTwistIndex]
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.activeScenario.twists.forEachIndexed { idx, _ ->
                        val active = idx == state.activeTwistIndex
                        Tactile3DButton(
                            text = "변수 ${idx + 1}",
                            containerColor = if (active) RoyalPurple else MaterialTheme.colorScheme.surface,
                            shadowColor = if (active) RoyalPurpleShadow else MaterialTheme.colorScheme.outline,
                            contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            height = 42.dp,
                            modifier = Modifier.weight(1f),
                            tag = "travel_twist_btn_$idx",
                            onClick = { onSelectTwist(idx) }
                        )
                    }
                }
            }

            item {
                Elevated3DCard(accentShadow = CoralOrange.copy(alpha = 0.4f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = CoralOrange)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = twist.titleKo,
                                style = MaterialTheme.typography.titleLarge,
                                color = CoralOrangeShadow
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SpartaRed.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "현지 직원 돌발 발언:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SpartaRed
                                )
                                Text(
                                    text = "\"${twist.staffPromptEn}\"",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = twist.staffPromptKo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AdventureEmerald.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "대응 전략: ${twist.strategyKo}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AdventureEmeraldShadow
                                )
                                twist.keyPhrases.forEachIndexed { pIdx, phrase ->
                                    Text(
                                        text = "추천 대응 ${pIdx + 1}: \"$phrase\"",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Tactile3DButton(
                                text = "돌발 상황 듣기",
                                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                containerColor = CoralOrange,
                                shadowColor = CoralOrangeShadow,
                                height = 50.dp,
                                modifier = Modifier.weight(1f),
                                onClick = onPlayTwistPrompt
                            )
                            Spacer(Modifier.width(12.dp))
                            VoiceWaveformOrb3D(
                                isListening = voiceState.isListening,
                                isSpeaking = voiceState.isSpeaking,
                                rmsDb = voiceState.rmsDb,
                                onClick = { requestOrStartMic() },
                                tag = "travel_twist_mic_orb"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = typedInput,
                                onValueChange = { typedInput = it },
                                placeholder = { Text("영어로 돌발 대응 입력 또는 마이크 발화") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("travel_twist_input")
                            )
                            IconButton(
                                onClick = {
                                    if (typedInput.isNotBlank()) {
                                        onSubmitTypedTwist(typedInput)
                                        typedInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("travel_twist_send")
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "대응 전송", tint = CoralOrange)
                            }
                        }

                        if (state.twistLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI 선생님이 실시간 반응을 생성하고 있습니다...", style = MaterialTheme.typography.labelMedium)
                            }
                        } else if (state.twistReplyText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SkyQuestBlue.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "실시간 AI 반응 (${state.twistEngineBadge})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SkyQuestBlueShadow
                                    )
                                    Text(state.twistReplyText, style = MaterialTheme.typography.titleMedium)
                                    Text(state.twistReplyKo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("코칭 팁: ${state.twistCoachTipKo}", style = MaterialTheme.typography.labelMedium, color = AdventureEmeraldShadow)
                                }
                            }
                        }
                    }
                }
            }
        }

        val report = state.latestPronunciation
        if (report != null) {
            item {
                PronunciationReportCard(
                    report = report,
                    onCompareNativeClick = {
                        if (activeSubTab == 0) onListenSentence() else onPlayTwistPrompt()
                    }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
