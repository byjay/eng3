package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MediaReviewStatus
import com.example.engine.RewardTransaction
import com.example.engine.VocabAnalyticsTracker
import com.example.engine.VocabMediaResolver
import com.example.engine.VocabMnemonicEngine
import com.example.model.VocabWord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIris
import com.example.ui.theme.TrophyGold

/**
 * Interactive P7 Media & Mnemonic Audit + Section 13 Learning Analytics Dashboard Sheet.
 * - Tab 0 (미디어·연상 검수): Shows word-matched WordSceneIllustration preview, morpheme status,
 *   license metadata, and manual 승인/보류 review buttons persisted to DataStore.
 * - Tab 1 (학습 분석 지표): Displays live VocabAnalyticsTracker funnel metrics and recent reward ledger.
 */
@Composable
fun MediaAuditAndAnalyticsSheetContent(
    allWords: List<VocabWord>,
    reviewOverrides: Map<String, MediaReviewStatus>,
    rewardLedger: List<RewardTransaction>,
    onSetMediaReviewStatus: (wordId: String, status: MediaReviewStatus) -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = P7 Audit, 1 = Section 13 Analytics
    var showPendingOnly by remember { mutableStateOf(true) }

    val auditSummary = remember(allWords, reviewOverrides) {
        VocabMediaResolver.auditStats(allWords, reviewOverrides)
    }
    val analyticsSummary = remember(activeTab, rewardLedger.size) {
        VocabAnalyticsTracker.summary()
    }
    val filteredWords = remember(allWords, reviewOverrides, showPendingOnly) {
        val list = if (showPendingOnly) {
            allWords.filter { w ->
                val override = reviewOverrides[w.id]
                val asset = VocabMediaResolver.resolve(w, override)
                asset.reviewStatus != MediaReviewStatus.APPROVED
            }
        } else {
            allWords
        }
        list.take(24)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("media_audit_analytics_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "미디어 검수 · 학습 분석",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "P7 어원·일러스트 검수 및 Section 13 지표",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) {
                Text("닫기")
            }
        }

        // 2-Tab Switcher
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StitchIconPillTab(
                    icon = Icons.Rounded.Verified,
                    label = "P7 검수 (${auditSummary.needsHumanReviewCount})",
                    selected = activeTab == 0,
                    accent = AdventureEmerald,
                    tag = "audit_tab_media_review",
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = 0 }
                )
                StitchIconPillTab(
                    icon = Icons.Rounded.Analytics,
                    label = "학습 지표",
                    selected = activeTab == 1,
                    accent = StudioCobalt,
                    tag = "audit_tab_analytics",
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = 1 }
                )
            }
        }

        if (activeTab == 0) {
            // Summary Strip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AdventureEmerald.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, AdventureEmerald.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "총 ${auditSummary.totalWords}단어 · 검증 어근 ${auditSummary.verifiedMorphemeCount}개 · 수동 승인 ${auditSummary.manuallyApprovedCount}개",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "라이선스: ${auditSummary.approvedLicenseLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdventureEmerald
                    )
                }
            }

            // Filter Toggle: 검수 대기만 vs 전체 보기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showPendingOnly) CoralOrange else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showPendingOnly = true }
                        .testTag("audit_filter_pending")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "검수 대기 (${auditSummary.needsHumanReviewCount})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (showPendingOnly) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!showPendingOnly) StudioCobalt else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showPendingOnly = false }
                        .testTag("audit_filter_all")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "전체 단어 보기",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (!showPendingOnly) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (filteredWords.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AdventureEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "모든 단어의 이미지·어원 검수가 승인 완료되었습니다!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AdventureEmerald,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                filteredWords.forEach { word ->
                    val override = reviewOverrides[word.id]
                    val asset = VocabMediaResolver.resolve(word, override)
                    val guide = VocabMnemonicEngine.buildStructuredGuide(word)
                    val statusColor = when (asset.reviewStatus) {
                        MediaReviewStatus.APPROVED -> AdventureEmerald
                        MediaReviewStatus.PENDING -> CoralOrange
                        MediaReviewStatus.REJECTED -> RoseCrimson
                    }
                    val statusText = when (asset.reviewStatus) {
                        MediaReviewStatus.APPROVED -> "승인"
                        MediaReviewStatus.PENDING -> "대기"
                        MediaReviewStatus.REJECTED -> "보류"
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_word_row_${word.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                WordSceneIllustration(word = word, modifier = Modifier.matchParentSize())
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${word.word} · ${word.meaningKo}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = statusColor.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${guide.visualMotif.labelKo} · ${guide.sceneTitleKo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioIris,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (guide.hasVerifiedMorpheme) "어근: ${guide.morphemeBreakdown}" else "장면 연상 템플릿 · ${asset.licenseName}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AdventureEmerald.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSetMediaReviewStatus(word.id, MediaReviewStatus.APPROVED) }
                                        .testTag("audit_approve_${word.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "승인", tint = AdventureEmerald, modifier = Modifier.size(13.dp))
                                        Text("승인", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AdventureEmerald)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RoseCrimson.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSetMediaReviewStatus(word.id, MediaReviewStatus.REJECTED) }
                                        .testTag("audit_reject_${word.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "보류", tint = RoseCrimson, modifier = Modifier.size(13.dp))
                                        Text("보류", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = RoseCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tab 1: Section 13 Analytics Dashboard
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsStatCard("카드 조회", "${analyticsSummary.cardsViewed}회", StudioCobalt, Modifier.weight(1f))
                    AnalyticsStatCard("연상 조회", "${analyticsSummary.mnemonicsViewed}회", StudioIris, Modifier.weight(1f))
                    AnalyticsStatCard("회상 정답률", "${analyticsSummary.recallAccuracyPercent}%", AdventureEmerald, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsStatCard("발음 재생", "${analyticsSummary.pronunciationPlayed}회", CoralOrange, Modifier.weight(1f))
                    AnalyticsStatCard("철자 통과", "${analyticsSummary.typingPassed}회", TrophyGold, Modifier.weight(1f))
                    AnalyticsStatCard("중복 차단", "${analyticsSummary.duplicateRewardsBlocked}건", RoseCrimson, Modifier.weight(1f))
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "최근 보상 원장 트랜잭션 (${rewardLedger.size}건)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    if (rewardLedger.isEmpty()) {
                        Text(
                            text = "아직 기록된 보상 트랜잭션이 없습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        rewardLedger.take(8).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.reason.ifBlank { tx.eventType.name },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "+${tx.xpDelta}XP · +${tx.coinDelta}C",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = AdventureEmerald
                                )
                            }
                        }
                    }
                    Text(
                        text = "※ 앱 내 학습 보상(XP/코인)은 현금 가치가 없으며 idempotencyKey로 중복 지급이 차단됩니다.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AnalyticsStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accent,
                maxLines = 1
            )
        }
    }
}
