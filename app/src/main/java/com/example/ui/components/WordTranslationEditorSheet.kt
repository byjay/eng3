package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.KoreanTranslationRefinerEngine
import com.example.model.VocabWord
import com.example.ui.theme.AdventureEmerald
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioIris
import kotlinx.coroutines.launch

/**
 * Interactive Word & Korean Translation Correction Engine Bottom Sheet.
 *
 * Allows learners to inspect naturalness quality score (0-100), auto-polish awkward
 * Korean translations locally, request Cloudflare Workers AI naturalization, edit
 * meanings/examples directly, and persist overrides across the entire app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordTranslationEditorSheet(
    word: VocabWord,
    hasCustomOverride: Boolean,
    onDismiss: () -> Unit,
    onAiPolish: suspend (word: String, pos: String, meaningKo: String, exampleEn: String, exampleKo: String) -> Pair<String, String>,
    onSave: (meaningKo: String, exampleEn: String, exampleKo: String, pronunciationTipKo: String) -> Unit,
    onResetToDefault: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var meaningKo by remember(word.id) { mutableStateOf(word.meaningKo) }
    var exampleEn by remember(word.id) { mutableStateOf(word.exampleEn) }
    var exampleKo by remember(word.id) { mutableStateOf(word.exampleKo) }
    var tipKo by remember(word.id) { mutableStateOf(word.pronunciationTipKo) }
    var isAiPolishing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val qualityReport = remember(exampleKo) {
        KoreanTranslationRefinerEngine.analyzeQuality(exampleKo)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("word_translation_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "단어 · 한글 번역 수정 엔진",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioCobalt
                    )
                    Text(
                        text = "${word.word}  (${word.partOfSpeech})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_translation_editor_btn")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "닫기")
                }
            }

            // Naturalness Quality Score Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (qualityReport.naturalnessScore >= 85) {
                    AdventureEmerald.copy(alpha = 0.12f)
                } else {
                    StudioCoral.copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    1.dp,
                    if (qualityReport.naturalnessScore >= 85) {
                        AdventureEmerald.copy(alpha = 0.4f)
                    } else {
                        StudioCoral.copy(alpha = 0.45f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
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
                            text = "한글 번역 자연스러움 점수",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${qualityReport.naturalnessScore} / 100",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (qualityReport.naturalnessScore >= 85) AdventureEmerald else StudioCoral
                        )
                    }
                    if (qualityReport.issuesDetected.isEmpty()) {
                        Text(
                            text = "자연스러운 한국어 문장입니다. 원하는 표현으로 자유롭게 다듬을 수 있어요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        qualityReport.issuesDetected.forEach { issue ->
                            Text(
                                text = "• $issue",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioCoral
                            )
                        }
                    }
                }
            }

            // Quick Auto-Polish & AI Naturalize Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        meaningKo = KoreanTranslationRefinerEngine.normalizeMeaningKo(meaningKo)
                        exampleKo = KoreanTranslationRefinerEngine.polishKorean(exampleKo)
                        tipKo = KoreanTranslationRefinerEngine.polishKorean(tipKo)
                        statusMessage = "조사·번역투 자동 교정을 적용했어요."
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_auto_polish_btn")
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("자동 번역 교정", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (!isAiPolishing) {
                            scope.launch {
                                isAiPolishing = true
                                statusMessage = "AI 자연어 의역 생성 중..."
                                val (polishedMeaning, polishedEx) = onAiPolish(
                                    word.word,
                                    word.partOfSpeech,
                                    meaningKo,
                                    exampleEn,
                                    exampleKo
                                )
                                meaningKo = polishedMeaning
                                exampleKo = polishedEx
                                isAiPolishing = false
                                statusMessage = "원어민 자연어 의역을 반영했어요. 확인 후 저장해 주세요."
                            }
                        }
                    },
                    enabled = !isAiPolishing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioIris),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_ai_polish_btn")
                ) {
                    if (isAiPolishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(5.dp))
                    Text("AI 자연어 의역", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StudioCobalt
                )
            }

            // Editable fields
            OutlinedTextField(
                value = meaningKo,
                onValueChange = { meaningKo = it },
                label = { Text("한국어 뜻 (핵심 의미)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_meaning_input")
            )

            OutlinedTextField(
                value = exampleEn,
                onValueChange = { exampleEn = it },
                label = { Text("영어 예문") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_example_en_input")
            )

            OutlinedTextField(
                value = exampleKo,
                onValueChange = { exampleKo = it },
                label = { Text("한글 예문 번역") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_example_ko_input")
            )

            OutlinedTextField(
                value = tipKo,
                onValueChange = { tipKo = it },
                label = { Text("발음 · 암기 팁 (선택)") },
                minLines = 1,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_tip_input")
            )

            Spacer(Modifier.height(4.dp))

            // Save & Reset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasCustomOverride) {
                    OutlinedButton(
                        onClick = {
                            onResetToDefault()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .testTag("editor_reset_btn")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("기본값", fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        onSave(
                            meaningKo.trim(),
                            exampleEn.trim(),
                            exampleKo.trim(),
                            tipKo.trim()
                        )
                        onDismiss()
                    },
                    enabled = meaningKo.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCobalt),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_save_btn")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("저장 (앱 전체 반영)", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
