package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.VocabMediaResolver
import com.example.engine.VocabMnemonicEngine
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.ui.theme.GlacierCyan
import com.example.ui.theme.GlacierCyanSoft
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltSoft
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCoralSoft
import com.example.ui.theme.StudioIris
import com.example.ui.theme.StudioIrisSoft

data class ChunkBuildStep(
    val stepNum: Int,
    val badgeKo: String,
    val englishChunk: String,
    val koreanHint: String,
    val accent: Color,
    val containerBg: Color
)

/**
 * Bright, sunlit Visual Association & Chunk-Building Learning Module combining:
 * 1) 장면·발음·검증 어근 연상 (Meaning-matched visual scene + sound aid + verified morphemes)
 * 2) 3단 덩어리 블록 결합 (3-step progressive Lego blocks with tap-to-speak)
 */
@Composable
fun VisualMnemonicAndChunkCard(
    word: VocabWord,
    onSpeakChunk: (String) -> Unit,
    onStartChunkSpeakingDrill: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by remember(word.id) { mutableIntStateOf(0) } // 0 = 장면·어근 연상, 1 = 3단 블록 결합
    val structured = remember(word.id) { VocabMnemonicEngine.buildStructuredGuide(word) }
    val chunkSteps = remember(word.id) { buildSiwonschoolChunks(word) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_mnemonic_chunk_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2-Tab Switch: 장면·어근 연상 | 3단 블록 결합
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MethodPillTab(
                    title = "장면·어근 연상",
                    icon = Icons.Outlined.Lightbulb,
                    selected = activeTab == 0,
                    accent = StudioIris,
                    tag = "mnemonic_tab_hippocampus",
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = 0 }
                )
                MethodPillTab(
                    title = "3단 블록 결합",
                    icon = Icons.Outlined.Extension,
                    selected = activeTab == 1,
                    accent = StudioCobalt,
                    tag = "mnemonic_tab_siwon_blocks",
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = 1 }
                )
            }

            if (activeTab == 0) {
                // === TAB 1: 장면·발음·검증 어근 연상 (Crisp Visual Equation Header — no duplicate generic photo!) ===
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161D38),
                    border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WordSceneIllustration(
                            word = word,
                            styleIndex = 1,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = StudioIris
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = structured.visualMotif.labelKo,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = word.topic.labelKo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "${word.word} = ${word.meaningKo}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = structured.sceneTitleKo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Visual Scene + Recall Question Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioCoralSoft,
                    border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "장면 & 발음 연상",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = StudioCoral
                        )
                        Text(
                            text = structured.sceneDescriptionKo,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = structured.soundAssociationKo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Verified-Only Morpheme Breakdown + Active Recall Prompt
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, StudioIris.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (structured.hasVerifiedMorpheme) StudioIrisSoft else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (structured.hasVerifiedMorpheme) "검증 어근" else "어근 미등록",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (structured.hasVerifiedMorpheme) StudioIris else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = structured.morphemeBreakdown,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (structured.hasVerifiedMorpheme) StudioIris else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "회상 질문: ${structured.memoryQuestion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // === TAB 2: 시원스쿨 3단 덩어리 블록 결합 (Tap any block to hear!) ===
                Text(
                    text = "블록을 터치해 소리 내며 1→2→3단계로 문장을 확장해 보세요",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                chunkSteps.forEach { step ->
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = step.containerBg,
                        border = BorderStroke(1.2.dp, step.accent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .clickable { onSpeakChunk(step.englishChunk) }
                            .testTag("siwon_chunk_step_${step.stepNum}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = step.accent
                            ) {
                                Text(
                                    text = "${step.stepNum}단",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = step.badgeKo,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = step.accent
                                )
                                Text(
                                    text = step.englishChunk,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = step.koreanHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "${step.stepNum}단계 듣기",
                                        tint = step.accent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (onStartChunkSpeakingDrill != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioCobalt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onStartChunkSpeakingDrill)
                            .testTag("siwon_chunk_speak_drill_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MicNone,
                                contentDescription = "3단 문장 말하기",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "3단계 완성 문장 따라 말하기",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodPillTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent else Color.Transparent,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@DrawableRes
private fun mnemonicArtworkForTopic(topic: VocabTopic): Int = when (topic) {
    VocabTopic.TRAVEL_DAILY -> R.drawable.img_mnemonic_world_travel_1795492661880
    VocabTopic.EMOTION_MIND -> R.drawable.img_mnemonic_emotion_scene_1795492662342
    VocabTopic.SCIENCE_AI, VocabTopic.LOGIC_DEBATE -> R.drawable.img_mnemonic_action_sprint_1795492662453
    VocabTopic.SCHOOL_EXAM, VocabTopic.SOCIETY_ECONOMY -> R.drawable.img_mnemonic_memory_stage_1795492662379
}

private fun buildSiwonschoolChunks(word: VocabWord): List<ChunkBuildStep> {
    val fullEn = word.examples.firstOrNull()?.english?.trim()?.ifBlank { word.exampleEn } ?: word.exampleEn
    val fullKo = word.examples.firstOrNull()?.korean?.trim()?.ifBlank { word.exampleKo } ?: word.exampleKo

    val tokens = fullEn.split(Regex("\\s+")).filter { it.isNotBlank() }
    val targetIdx = tokens.indexOfFirst { it.lowercase().contains(word.word.lowercase().take(4)) }
    val coreBlockEn = when {
        tokens.size <= 4 -> fullEn
        targetIdx >= 0 -> {
            val start = (targetIdx - 1).coerceAtLeast(0)
            val end = (targetIdx + 2).coerceAtMost(tokens.lastIndex)
            tokens.subList(start, end + 1).joinToString(" ").trimEnd('.', ',', ';')
        }
        else -> tokens.take(4).joinToString(" ").trimEnd('.', ',', ';')
    }

    val coreBlockKo = when {
        word.partOfSpeech.contains("v") -> "동사 덩어리 · '${word.meaningKo}' 동작 붙이기"
        word.partOfSpeech.contains("adj") -> "상태 덩어리 · '${word.meaningKo}' 꾸며주기"
        else -> "핵심 뼈대 · '${word.meaningKo}' 중심으로 말하기"
    }

    return listOf(
        ChunkBuildStep(
            stepNum = 1,
            badgeKo = "핵심 단어 블록",
            englishChunk = word.word,
            koreanHint = "${word.meaningKo} (${word.partOfSpeech})",
            accent = StudioCobalt,
            containerBg = StudioCobaltSoft
        ),
        ChunkBuildStep(
            stepNum = 2,
            badgeKo = "뼈대 덩어리 결합",
            englishChunk = coreBlockEn,
            koreanHint = coreBlockKo,
            accent = GlacierCyan,
            containerBg = GlacierCyanSoft
        ),
        ChunkBuildStep(
            stepNum = 3,
            badgeKo = "실전 문장 완성",
            englishChunk = fullEn,
            koreanHint = fullKo,
            accent = StudioCoral,
            containerBg = StudioCoralSoft
        )
    )
}
