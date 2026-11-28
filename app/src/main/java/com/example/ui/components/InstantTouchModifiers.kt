package com.example.ui.components

import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Ultra-low-latency quiz option touch configuration:
 * - Fires on `ACTION_DOWN` (Android equivalent of web `touchstart`) instead of waiting for `ACTION_UP`.
 * - 50ms (0.05s) GPU-only press animation duration for crisp tactile sensitivity.
 * - 80ms debounce prevents accidental double-fire on bouncy touches while allowing rapid-fire answers.
 */
const val QUIZ_OPTION_ANIM_DURATION_MS: Int = 50
const val QUIZ_TOUCH_DEBOUNCE_MS: Long = 80L
const val QUIZ_PRESSED_SCALE: Float = 0.965f

/**
 * Pure debounce predicate for instant touchstart triggers (unit-testable on JVM).
 */
fun shouldAcceptInstantTouch(
    nowMs: Long,
    lastFiredMs: Long,
    debounceMs: Long = QUIZ_TOUCH_DEBOUNCE_MS
): Boolean {
    if (lastFiredMs <= 0L) return true
    return (nowMs - lastFiredMs) >= debounceMs
}

/**
 * Triggers crisp immediate tactile haptic feedback on touchstart with zero extra permissions.
 */
fun triggerQuizOptionHaptic(
    view: View,
    hapticFeedback: HapticFeedback
) {
    val performed = runCatching {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }.getOrDefault(false)
    if (!performed) {
        runCatching {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

@Composable
fun rememberIsReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * High-sensitivity touchstart (`ACTION_DOWN`) modifier for quiz option buttons:
 * - Dispatches immediately on initial finger contact (`awaitFirstDown`) to eliminate ~120ms tap-up delay.
 * - Fires immediate haptic feedback on touchstart.
 * - Runs a 0.05s (50ms) GPU `graphicsLayer` scale animation (automatically skipped when reduced motion is enabled).
 * - Preserves full TalkBack & Compose UI test compatibility via `Role.Button` semantics `onClick`.
 */
fun Modifier.instantQuizTouch(
    enabled: Boolean = true,
    hapticEnabled: Boolean = true,
    debounceMs: Long = QUIZ_TOUCH_DEBOUNCE_MS,
    pressedScale: Float = QUIZ_PRESSED_SCALE,
    onTouchStart: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberIsReducedMotionEnabled()

    val currentOnTouchStart by rememberUpdatedState(onTouchStart)
    val currentEnabled by rememberUpdatedState(enabled)
    var isPressed by remember { mutableStateOf(false) }
    var lastFiredAtMs by remember { mutableLongStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = QUIZ_OPTION_ANIM_DURATION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "instantQuizTouchScale"
    )

    fun fireTrigger() {
        if (!currentEnabled) return
        val now = SystemClock.uptimeMillis()
        if (!shouldAcceptInstantTouch(nowMs = now, lastFiredMs = lastFiredAtMs, debounceMs = debounceMs)) {
            return
        }
        lastFiredAtMs = now
        if (hapticEnabled) {
            triggerQuizOptionHaptic(view = view, hapticFeedback = haptic)
        }
        currentOnTouchStart()
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (!enabled) {
                disabled()
            }
            onClick {
                if (!currentEnabled) return@onClick false
                fireTrigger()
                true
            }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                down.consume()
                fireTrigger()
                try {
                    waitForUpOrCancellation()
                } finally {
                    isPressed = false
                }
            }
        }
}
