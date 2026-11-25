package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Subway
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Google Stitch–inspired Material 3 Expressive Icon Design System:
 * - Layered squircle badges with soft diagonal gradients, subtle radial glow,
 *   and crisp luminous specular rim borders.
 * - Tactile spring press physics and 48dp accessible touch targets.
 * - Minimalist icon-first navigation, tabs, action pills, and metric chips.
 */
@Composable
fun StitchIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp,
    filled: Boolean = false,
    cornerBadgeText: String? = null
) {
    val shape = RoundedCornerShape((size.value * 0.34f).dp)
    val bgBrush = if (filled) {
        Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.92f),
                accent
            ),
            start = Offset(0f, 0f),
            end = Offset(140f, 140f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.19f),
                accent.copy(alpha = 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(140f, 140f)
        )
    }
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (filled) 0.45f else 0.85f),
            accent.copy(alpha = if (filled) 0.18f else 0.28f)
        )
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(bgBrush)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (filled) Color.White.copy(alpha = 0.24f) else accent.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(this.size.width * 0.28f, this.size.height * 0.22f),
                            radius = this.size.minDimension * 0.72f
                        )
                    )
                }
                .border(width = 1.2.dp, brush = rimBrush, shape = shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (filled) Color.White else accent,
                modifier = Modifier.size(iconSize)
            )
        }

        if (!cornerBadgeText.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = if (filled) Color(0xFF0F172A) else accent,
                border = BorderStroke(1.5.dp, Color.White),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                Text(
                    text = cornerBadgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Tactile Google Stitch Squircle/Circle Icon Action Button (48dp accessible touch target).
 */
@Composable
fun StitchIconButton(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    badgeCount: Int? = null,
    tag: String = "stitch_icon_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(stiffness = 750f, dampingRatio = 0.65f),
        label = "stitchIconScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        StitchIconBadge(
            icon = icon,
            contentDescription = contentDescription,
            accent = accent,
            size = size,
            iconSize = iconSize,
            filled = filled,
            cornerBadgeText = badgeCount?.takeIf { it > 0 }?.toString()
        )
    }
}

/**
 * Google Stitch Expressive Icon-First Tab Pill:
 * Crisp centered vector icon with ultra-compact 1-word caption and smooth spring selection.
 */
@Composable
fun StitchIconPillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 750f),
        label = "stitchTabScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent else Color.Transparent,
        label = "stitchTabBg"
    )
    val fgColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "stitchTabFg"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fgColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    color = fgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White else accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (selected) accent else Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact Google Stitch Icon + Value Chip (used in HUD, metrics, and compact action strips).
 */
@Composable
fun StitchIconMetricChip(
    icon: ImageVector,
    valueText: String,
    accent: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tag: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val baseMod = modifier
        .clip(shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
        .let { m -> if (tag != null) m.testTag(tag) else m }

    Surface(
        shape = shape,
        color = accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        modifier = baseMod
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Distinct vector icon mapping for the 10 Travel Expedition Worlds.
 */
fun travelZoneStitchIcon(zoneId: Int): ImageVector = when (zoneId) {
    1 -> Icons.Outlined.Flight
    2 -> Icons.Outlined.Luggage
    3 -> Icons.Outlined.Subway
    4 -> Icons.Outlined.Hotel
    5 -> Icons.Outlined.LocalCafe
    6 -> Icons.Outlined.ShoppingBag
    7 -> Icons.Outlined.Museum
    8 -> Icons.Outlined.LocalHospital
    9 -> Icons.Outlined.BeachAccess
    else -> Icons.Outlined.Celebration
}

enum class MaterialButtonStyle {
    FILLED,
    TONAL,
    OUTLINED
}

/**
 * Clean Material Design 3 Action Button with leading icon, 48dp accessible height,
 * crisp 14dp rounded corners, and optional inline count badge.
 */
@Composable
fun MaterialLearningButton(
    text: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MaterialButtonStyle = MaterialButtonStyle.TONAL,
    badgeText: String? = null,
    enabled: Boolean = true,
    tag: String = "m3_learning_button"
) {
    val shape = RoundedCornerShape(14.dp)
    val contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)

    val buttonContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!badgeText.isNullOrBlank()) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (style == MaterialButtonStyle.FILLED) {
                        Color.White.copy(alpha = 0.24f)
                    } else {
                        accent.copy(alpha = 0.18f)
                    }
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (style == MaterialButtonStyle.FILLED) Color.White else accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }

    when (style) {
        MaterialButtonStyle.FILLED -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                ),
                modifier = modifier
                    .heightIn(min = 48.dp)
                    .testTag(tag)
            ) {
                buttonContent()
            }
        }

        MaterialButtonStyle.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accent.copy(alpha = 0.13f),
                    contentColor = accent
                ),
                modifier = modifier
                    .heightIn(min = 48.dp)
                    .testTag(tag)
            ) {
                buttonContent()
            }
        }

        MaterialButtonStyle.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = accent
                ),
                modifier = modifier
                    .heightIn(min = 48.dp)
                    .testTag(tag)
            ) {
                buttonContent()
            }
        }
    }
}

/**
 * Clean Material Design 3 2x2 Learning Mode Action Bar:
 * [듣기] · [말하기] / [오답 노트] · [완벽 암기]
 */
@Composable
fun LearningModeMaterialButtonGrid(
    listenAccent: Color,
    speakAccent: Color,
    wrongAccent: Color,
    masterAccent: Color,
    isSpeakingActive: Boolean,
    isWrongNoteMode: Boolean,
    wrongCount: Int,
    onListenClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onWrongNoteClick: () -> Unit,
    onMasteredClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaterialLearningButton(
                text = "듣기",
                icon = Icons.Rounded.Headphones,
                accent = listenAccent,
                style = MaterialButtonStyle.TONAL,
                tag = "learning_mode_listen_btn",
                modifier = Modifier.weight(1f),
                onClick = onListenClick
            )
            MaterialLearningButton(
                text = if (isSpeakingActive) "말하기 중지" else "말하기",
                icon = if (isSpeakingActive) Icons.Rounded.Stop else Icons.Rounded.Mic,
                accent = speakAccent,
                style = if (isSpeakingActive) MaterialButtonStyle.FILLED else MaterialButtonStyle.TONAL,
                tag = "learning_mode_speak_btn",
                modifier = Modifier.weight(1f),
                onClick = onSpeakClick
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaterialLearningButton(
                text = "오답 노트",
                icon = if (isWrongNoteMode) Icons.Rounded.Bookmarks else Icons.Rounded.BookmarkBorder,
                accent = wrongAccent,
                style = if (isWrongNoteMode) MaterialButtonStyle.FILLED else MaterialButtonStyle.OUTLINED,
                badgeText = if (wrongCount > 0) "$wrongCount" else null,
                tag = "vocab_memo_again_btn",
                modifier = Modifier.weight(1f),
                onClick = onWrongNoteClick
            )
            MaterialLearningButton(
                text = "완벽 암기",
                icon = Icons.Rounded.Verified,
                accent = masterAccent,
                style = MaterialButtonStyle.FILLED,
                tag = "vocab_memo_mastered_btn",
                modifier = Modifier.weight(1f),
                onClick = onMasteredClick
            )
        }
    }
}
