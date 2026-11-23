package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SquirrelRpgRepository
import com.example.engine.VocabMnemonicEngine
import com.example.engine.VoiceEngineState
import com.example.model.RpgBattleAction
import com.example.model.RpgGamePhase
import com.example.model.RpgGameUiState
import com.example.model.RpgStoryChoice
import com.example.model.SquirrelJobHero
import com.example.ui.components.Elevated3DCard
import com.example.ui.components.SquirrelHeroAvatar
import com.example.ui.components.SquirrelHeroCardPortrait
import com.example.ui.components.SquirrelMoodState
import com.example.ui.components.Tactile3DButton
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.AdventureEmeraldShadow
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeShadow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleShadow
import com.example.ui.theme.SkyQuestBlue
import com.example.ui.theme.SkyQuestBlueShadow
import com.example.ui.theme.SpartaRed
import com.example.ui.theme.SpartaRedShadow
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldShadow

/**
 * 10-Hero White Pygmy Squirrel Fantasy Job Picker (Unified Family Look)
 */
@Composable
fun SquirrelHeroPickerOverlay(
    selectedHeroId: String,
    playerNickname: String,
    onSelectHero: (SquirrelJobHero) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "흰색 피그미 다람쥐 · 10종 직업 선택",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "$playerNickname 의 모험 파트너를 고르세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_hero_picker_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(SquirrelRpgRepository.heroes, key = { it.id }) { hero ->
                    val selected = hero.id == selectedHeroId
                    val accent = Color(hero.accentHex)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (selected) 2.5.dp else 1.dp,
                            if (selected) accent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectHero(hero) }
                            .testTag("pick_squirrel_${hero.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(112.dp)
                            ) {
                                SquirrelHeroCardPortrait(
                                    hero = hero,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 14.dp
                                )
                                if (selected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accent,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "장착 중",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .size(18.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${hero.nameKo} · ${hero.jobTitleKo}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SpartaRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "HP ${hero.maxHp}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SpartaRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "ATK ${hero.baseAtk}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = hero.passiveKo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Storybook RPG Story Choices + Turn-Based Vocab/Conversation Battle + Sealed Mystery Chest
 */
@Composable
fun SquirrelStoryAndBattleOverlay(
    state: RpgGameUiState,
    hero: SquirrelJobHero,
    playerNickname: String,
    voiceState: VoiceEngineState,
    onClose: () -> Unit,
    onChooseStoryOption: (RpgStoryChoice) -> Unit,
    onStartChapterBattle: () -> Unit,
    onChooseBattleAction: (RpgBattleAction) -> Unit,
    onSubmitWordAnswer: (String) -> Unit,
    onSubmitPhraseAnswer: (String) -> Unit,
    onStartVoicePhraseAttack: () -> Unit,
    onStopVoice: () -> Unit,
    onListenEnglish: (String) -> Unit,
    onOpenMysteryChest: () -> Unit,
    onNextChapter: () -> Unit,
    onRetryChapter: () -> Unit
) {
    BackHandler(onBack = onClose)
    val chapter = state.activeChapter ?: return

    val monsterScale by animateFloatAsState(
        targetValue = if (state.monsterHitTick % 2 == 1) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "monsterHitScale"
    )
    val chestPulse = 1.0f
    val squirrelMood = when {
        state.phase == RpgGamePhase.VICTORY -> SquirrelMoodState.VICTORY_DANCE
        state.phase == RpgGamePhase.DEFEAT -> SquirrelMoodState.HURT_SHAKE
        voiceState.isListening -> SquirrelMoodState.LISTENING_TILT
        state.phase == RpgGamePhase.BATTLE_WORD_CHALLENGE && state.wordChallenge?.isSkillAmplified == true -> SquirrelMoodState.SKILL_CAST
        state.phase == RpgGamePhase.BATTLE_PHRASE_CHALLENGE && state.phraseChallenge?.isSkillAmplified == true -> SquirrelMoodState.SKILL_CAST
        state.heroShieldActive -> SquirrelMoodState.GUARD_SHIELD
        state.floatingBannerKo.contains("격파") || state.floatingBannerKo.contains("CRITICAL") || state.floatingBannerKo.contains("명중") -> SquirrelMoodState.ATTACK_LUNGE
        state.floatingBannerKo.contains("빗나감") -> SquirrelMoodState.HURT_SHAKE
        state.phase == RpgGamePhase.STORY_OUTCOME -> SquirrelMoodState.HAPPY_CHEER
        else -> SquirrelMoodState.IDLE
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar (Family Look Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("rpg_back_home_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "왕국홈으로")
                    }
                    Column {
                        Text(
                            text = chapter.titleKo,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "${chapter.locationKo} · $playerNickname (${hero.nameKo})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.comboCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CoralOrange
                    ) {
                        Text(
                            text = "${state.comboCount} COMBO",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Stage Banner (Emerald Kingdom Backdrop + White Pygmy Squirrel vs Monster)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(216.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, TrophyGold.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.img_emerald_rpg_stage_1795421788415),
                    contentDescription = "에메랄드 영어 왕국 판타지 스테이지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.56f)
                                )
                            )
                        )
                )

                // Floating Combat Event Popup
                if (state.floatingBannerKo.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        border = BorderStroke(1.2.dp, TrophyGold),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = state.floatingBannerKo,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TrophyGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Left: White Pygmy Squirrel Protagonist
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        SquirrelHeroAvatar(
                            hero = hero,
                            size = 86.dp,
                            mood = squirrelMood,
                            borderWidth = 3.dp,
                            borderColor = if (state.heroShieldActive) SkyQuestBlue else TrophyGold
                        )
                        if (state.heroShieldActive) {
                            Surface(
                                shape = CircleShape,
                                color = SkyQuestBlue,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "방어막",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.68f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${hero.nameKo} HP ${state.heroHp}/${state.heroMaxHp}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            LinearProgressIndicator(
                                progress = { (state.heroHp.toFloat() / state.heroMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(86.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AdventureEmerald,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Right: Monster / Boss (shown during battle)
                if (state.phase != RpgGamePhase.STORY_DIALOGUE && state.phase != RpgGamePhase.STORY_OUTCOME) {
                    val monster = chapter.monster
                    val monsterColor = Color(monster.accentHex)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (state.monsterStunned) RoyalPurple else SpartaRed.copy(alpha = 0.86f)
                        ) {
                            Text(
                                text = if (state.monsterStunned) "기절 (공격 불가)" else monster.intentLabelKo,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .scale(monsterScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(monsterColor, Color(0xFF1A102B))
                                    )
                                )
                                .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (monster.isBoss) Icons.Default.LocalFireDepartment else Icons.Default.Security,
                                contentDescription = monster.nameKo,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.68f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${monster.nameKo} ${state.monsterHp}/${state.monsterMaxHp}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                LinearProgressIndicator(
                                    progress = { (state.monsterHp.toFloat() / state.monsterMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = SpartaRed,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Main Interactive Content Area (Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state.phase) {
                    RpgGamePhase.STORY_DIALOGUE -> {
                        Elevated3DCard(accentShadow = Color(chapter.accentHex).copy(alpha = 0.35f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = chapter.storyIntroKo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TrophyGold.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "${hero.nameKo}: \"${chapter.squirrelLineKo}\"",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                        chapter.choices.forEachIndexed { idx, choice ->
                            Tactile3DButton(
                                text = "${choice.labelKo}  (${choice.tagKo})",
                                icon = if (idx == 0) Icons.Default.AutoAwesome else Icons.Default.Bolt,
                                containerColor = if (idx == 0) AdventureEmerald else SkyQuestBlue,
                                shadowColor = if (idx == 0) AdventureEmeraldShadow else SkyQuestBlueShadow,
                                height = 52.dp,
                                modifier = Modifier.fillMaxWidth(),
                                tag = "rpg_story_choice_${choice.id}",
                                onClick = { onChooseStoryOption(choice) }
                            )
                        }
                    }

                    RpgGamePhase.STORY_OUTCOME -> {
                        Elevated3DCard(accentShadow = AdventureEmeraldShadow.copy(alpha = 0.35f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = state.selectedChoiceOutcomeKo,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${chapter.monster.nameKo}: \"${chapter.monster.battleQuoteKo}\"",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SpartaRed
                                )
                                Tactile3DButton(
                                    text = "턴제 영어 배틀 시작!",
                                    icon = Icons.Default.LocalFireDepartment,
                                    containerColor = SpartaRed,
                                    shadowColor = SpartaRedShadow,
                                    height = 52.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    tag = "rpg_enter_battle_button",
                                    onClick = onStartChapterBattle
                                )
                            }
                        }
                    }

                    RpgGamePhase.BATTLE_ACTION -> {
                        // Hero MP & Buff Strip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "MP ${state.heroMp}/100",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = RoyalPurple
                                    )
                                }
                                Text(
                                    text = buildString {
                                        append("턴 ${state.turnNumber}")
                                        if (state.heroAtkBuffPercent > 0) append(" · 공격+${state.heroAtkBuffPercent}%")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (state.lastBattleLogKo.isNotBlank()) {
                            Text(
                                text = state.lastBattleLogKo,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 2x2 Tactile Turn Action Grid
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Tactile3DButton(
                                text = "단어·문장 공격",
                                icon = Icons.Default.AutoAwesome,
                                containerColor = AdventureEmerald,
                                shadowColor = AdventureEmeraldShadow,
                                height = 56.dp,
                                modifier = Modifier.weight(1f),
                                tag = "rpg_action_word",
                                onClick = { onChooseBattleAction(RpgBattleAction.WORD_STRIKE) }
                            )
                            Tactile3DButton(
                                text = "실전 회화 공격",
                                icon = Icons.Default.RecordVoiceOver,
                                containerColor = SkyQuestBlue,
                                shadowColor = SkyQuestBlueShadow,
                                height = 56.dp,
                                modifier = Modifier.weight(1f),
                                tag = "rpg_action_phrase",
                                onClick = { onChooseBattleAction(RpgBattleAction.PHRASE_CAST) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            val canSkill = state.heroMp >= hero.skillMpCost
                            Tactile3DButton(
                                text = hero.skillNameKo,
                                icon = Icons.Default.Bolt,
                                containerColor = RoyalPurple,
                                shadowColor = RoyalPurpleShadow,
                                height = 54.dp,
                                enabled = canSkill,
                                modifier = Modifier.weight(1f),
                                tag = "rpg_action_skill",
                                onClick = { onChooseBattleAction(RpgBattleAction.CLASS_SKILL) }
                            )
                            Tactile3DButton(
                                text = "방어 · MP충전",
                                icon = Icons.Default.Shield,
                                containerColor = TrophyGold,
                                shadowColor = TrophyGoldShadow,
                                contentColor = Color(0xFF3E2C00),
                                height = 54.dp,
                                modifier = Modifier.weight(1f),
                                tag = "rpg_action_guard",
                                onClick = { onChooseBattleAction(RpgBattleAction.GUARD_HINT) }
                            )
                        }
                    }

                    RpgGamePhase.BATTLE_WORD_CHALLENGE -> {
                        val challenge = state.wordChallenge
                        if (challenge != null) {
                            Elevated3DCard(accentShadow = AdventureEmeraldShadow.copy(alpha = 0.35f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "수능 빈출 단어 + 문장 공격",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AdventureEmerald
                                        )
                                        IconButton(
                                            onClick = { onListenEnglish("${challenge.word.word}. ${challenge.exampleSentenceEn}") },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "발음 듣기",
                                                tint = SkyQuestBlue
                                            )
                                        }
                                    }
                                    Text(
                                        text = challenge.word.word,
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Always show CSAT sentence with the word!
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = challenge.exampleSentenceEn,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                    AnimatedVisibility(visible = challenge.showHint) {
                                        val pack = VocabMnemonicEngine.buildGuide(challenge.word)
                                        Text(
                                            text = "해마 힌트: ${pack.phoneticHookKo}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = RoyalPurple
                                        )
                                    }
                                }
                            }
                            challenge.options.forEachIndexed { idx, opt ->
                                Tactile3DButton(
                                    text = "${idx + 1}. $opt",
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shadowColor = MaterialTheme.colorScheme.outline,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    height = 48.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    tag = "rpg_word_option_$idx",
                                    onClick = { onSubmitWordAnswer(opt) }
                                )
                            }
                        }
                    }

                    RpgGamePhase.BATTLE_PHRASE_CHALLENGE -> {
                        val phrase = state.phraseChallenge
                        if (phrase != null) {
                            Elevated3DCard(accentShadow = SkyQuestBlueShadow.copy(alpha = 0.35f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "실전 영어 회화 공격 (말하면 +35%)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SkyQuestBlue
                                        )
                                        IconButton(
                                            onClick = { onListenEnglish(phrase.targetEnglish) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "원어민 문장 듣기",
                                                tint = SkyQuestBlue
                                            )
                                        }
                                    }
                                    Text(
                                        text = "\"${phrase.promptKorean}\"",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    // Direct Voice Speaking Attack Button
                                    Tactile3DButton(
                                        text = if (voiceState.isListening) "듣는 중... 멈추기" else "영어로 직접 말해서 크리티컬 공격!",
                                        icon = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        containerColor = if (voiceState.isListening) SpartaRed else RoyalPurple,
                                        shadowColor = if (voiceState.isListening) SpartaRedShadow else RoyalPurpleShadow,
                                        height = 46.dp,
                                        modifier = Modifier.fillMaxWidth(),
                                        tag = "rpg_phrase_mic_attack",
                                        onClick = {
                                            if (voiceState.isListening) onStopVoice() else onStartVoicePhraseAttack()
                                        }
                                    )
                                }
                            }
                            phrase.options.forEachIndexed { idx, opt ->
                                Tactile3DButton(
                                    text = opt,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shadowColor = MaterialTheme.colorScheme.outline,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    height = 50.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    tag = "rpg_phrase_option_$idx",
                                    onClick = { onSubmitPhraseAnswer(opt) }
                                )
                            }
                        }
                    }

                    RpgGamePhase.VICTORY -> {
                        Elevated3DCard(accentShadow = TrophyGoldShadow.copy(alpha = 0.5f)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "승리",
                                    tint = TrophyGold,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "${chapter.titleKo} 클리어!",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "+${state.earnedCoins} 코인 · +${state.earnedXp} XP · 성채 +${state.earnedCastleHp}%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = AdventureEmerald
                                )

                                // Sealed Mystery Treasure Chest — ZERO spoilers beforehand!
                                if (state.mysteryChestPendingOpen && state.revealedChestReward == null) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = TrophyGold.copy(alpha = 0.18f),
                                        border = BorderStroke(2.dp, TrophyGold),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(chestPulse)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable(onClick = onOpenMysteryChest)
                                            .testTag("open_mystery_chest_button")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(TrophyGold),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Help,
                                                    contentDescription = "미스터리 보물상자",
                                                    tint = Color(0xFF3E2C00),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Text(
                                                text = "??? 미스터리 보물상자 발견!",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF3E2C00)
                                            )
                                            Text(
                                                text = "터치해서 상자 열기",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TrophyGoldShadow
                                            )
                                        }
                                    }
                                } else if (state.revealedChestReward != null) {
                                    val reward = state.revealedChestReward
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = RoyalPurple.copy(alpha = 0.14f),
                                        border = BorderStroke(1.8.dp, RoyalPurple),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Inventory2,
                                                    contentDescription = null,
                                                    tint = RoyalPurple,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "[${reward.rarityKo}] 획득!",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = RoyalPurple
                                                )
                                            }
                                            Text(
                                                text = reward.titleKo,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Tactile3DButton(
                                        text = "왕국홈",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        shadowColor = MaterialTheme.colorScheme.outline,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        height = 48.dp,
                                        modifier = Modifier.weight(1f),
                                        onClick = onClose
                                    )
                                    Tactile3DButton(
                                        text = if (chapter.number < 6) "다음 챕터" else "다시 모험",
                                        icon = Icons.Default.AutoAwesome,
                                        containerColor = AdventureEmerald,
                                        shadowColor = AdventureEmeraldShadow,
                                        height = 48.dp,
                                        modifier = Modifier.weight(1.3f),
                                        tag = "rpg_next_chapter_button",
                                        onClick = onNextChapter
                                    )
                                }
                            }
                        }
                    }

                    RpgGamePhase.DEFEAT -> {
                        Elevated3DCard(accentShadow = SpartaRedShadow.copy(alpha = 0.4f)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = SpartaRed,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "${hero.nameKo}가 잠시 숨을 고르는 중!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Tactile3DButton(
                                        text = "왕국홈",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        shadowColor = MaterialTheme.colorScheme.outline,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        height = 48.dp,
                                        modifier = Modifier.weight(1f),
                                        onClick = onClose
                                    )
                                    Tactile3DButton(
                                        text = "다시 도전",
                                        icon = Icons.Default.Refresh,
                                        containerColor = AdventureEmerald,
                                        shadowColor = AdventureEmeraldShadow,
                                        height = 48.dp,
                                        modifier = Modifier.weight(1.3f),
                                        tag = "rpg_retry_button",
                                        onClick = onRetryChapter
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
