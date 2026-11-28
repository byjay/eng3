package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.engine.LeagueStanding
import java.io.File
import java.io.FileOutputStream

data class AchievementShareSnapshot(
    val learnerName: String,
    val streakCount: Int,
    val todayWords: Int,
    val dailyGoal: Int,
    val masteredWords: Int,
    val accuracyPercent: Int,
    val gradeLabel: String,
    val leagueStanding: LeagueStanding,
    val inviteCode: String
)

/**
 * Generates a studio-quality 1080x1350 viral PNG achievement card natively via Android Canvas
 * and shares it via FileProvider + Android Share Sheet (KakaoTalk, Instagram, Messages, etc.).
 * Zero third-party dependencies and zero storage permissions required.
 */
object ViralShareCardGenerator {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1350

    fun renderAchievementBitmap(snapshot: AchievementShareSnapshot): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep royal-cobalt to midnight background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#1E3A8A"),
                    Color.parseColor("#312E81")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // Decorative atmospheric glow circles
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            alpha = 36
        }
        canvas.drawCircle(920f, 190f, 260f, glowPaint)
        glowPaint.color = Color.parseColor("#F59E0B")
        glowPaint.alpha = 30
        canvas.drawCircle(160f, 1140f, 280f, glowPaint)

        // 2. Top Brand Pill + League Badge
        val leagueAccent = (0xFF000000L or (snapshot.leagueStanding.currentTier.accentHex and 0x00FFFFFFL)).toInt()
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(56, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(72f, 76f, 520f, 148f), 36f, 36f, pillPaint)

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("DAILY ENGLISH CHALLENGE", 104f, 124f, pillTextPaint)

        val leaguePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = leagueAccent
        }
        canvas.drawRoundRect(RectF(690f, 76f, 1008f, 148f), 36f, 36f, leaguePillPaint)
        val leagueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(snapshot.leagueStanding.currentTier.titleKo, 849f, 124f, leagueTextPaint)

        // 3. Learner Title + Hero Streak Headline
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 68f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("${snapshot.learnerName}님의 오늘 영어 기록", 76f, 258f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("3초 스피드 콤보 · AI 원어민 회화 · 수능/여행 마스터", 76f, 318f, subPaint)

        // 4. Center Hero Streak Card
        val heroCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 255, 255, 255)
        }
        val heroBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(95, 255, 255, 255)
        }
        val heroRect = RectF(72f, 376f, 1008f, 710f)
        canvas.drawRoundRect(heroRect, 44f, 44f, heroCardPaint)
        canvas.drawRoundRect(heroRect, 44f, 44f, heroBorderPaint)

        val streakLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE68A")
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("연속 학습 스트릭", 124f, 452f, streakLabelPaint)

        val streakBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 148f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("${snapshot.streakCount}일", 120f, 608f, streakBigPaint)

        // Grade Circle on right side of hero card
        val gradeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
        }
        canvas.drawCircle(835f, 540f, 112f, gradeCirclePaint)
        val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 84f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(snapshot.gradeLabel, 835f, 566f, gradePaint)
        val gradeSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ECFDF5")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("오늘 등급", 835f, 612f, gradeSubPaint)

        // 5. 3 KPI Stat Tiles
        val tileY1 = 752f
        val tileY2 = 986f
        val tileW = 294f
        val gap = 27f
        val stats = listOf(
            Triple("오늘 완료", "${snapshot.todayWords}/${snapshot.dailyGoal}", "목표 단어"),
            Triple("정답률", "${snapshot.accuracyPercent}%", "오늘 정확도"),
            Triple("완벽 암기", "${snapshot.masteredWords}개", "누적 마스터")
        )
        stats.forEachIndexed { idx, (label, value, sub) ->
            val left = 72f + idx * (tileW + gap)
            val rect = RectF(left, tileY1, left + tileW, tileY2)
            canvas.drawRoundRect(rect, 34f, 34f, heroCardPaint)
            canvas.drawRoundRect(rect, 34f, 34f, heroBorderPaint)

            val cx = left + tileW / 2f
            val kpiLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#93C5FD")
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val kpiValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 58f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val kpiSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 26f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, cx, tileY1 + 64f, kpiLabel)
            canvas.drawText(value, cx, tileY1 + 142f, kpiValue)
            canvas.drawText(sub, cx, tileY1 + 196f, kpiSub)
        }

        // 6. Bottom Viral Invite Footer Banner
        val footerRect = RectF(72f, 1044f, 1008f, 1264f)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                72f, 1044f, 1008f, 1264f,
                Color.parseColor("#F59E0B"),
                Color.parseColor("#EA580C"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(footerRect, 40f, 40f, footerPaint)

        val ctaTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("함께 3분 영어 스트릭 도전할래요?", 118f, 1126f, ctaTitle)

        val codeLabel = snapshot.inviteCode.takeIf { it.isNotBlank() }?.let { "초대 코드: $it  ·  " } ?: ""
        val ctaSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7")
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("${codeLabel}Google Play에서 무료 시작", 118f, 1196f, ctaSub)

        return bitmap
    }

    fun shareAchievementCard(context: Context, snapshot: AchievementShareSnapshot): Boolean {
        val shareCaption = buildString {
            append("${snapshot.learnerName}님의 영어 학습 기록! ")
            append("연속 ${snapshot.streakCount}일 스트릭 · ${snapshot.leagueStanding.currentTier.titleKo} · 오늘 ${snapshot.todayWords}단어 완료(${snapshot.gradeLabel})\n")
            if (snapshot.inviteCode.isNotBlank()) {
                append("초대 코드: ${snapshot.inviteCode}\n")
            }
            append("Google Play: https://play.google.com/store/apps/details?id=${context.packageName}")
        }

        val imageUri: Uri? = runCatching {
            val bitmap = renderAchievementBitmap(snapshot)
            val cacheDir = File(context.cacheDir, "share_cards").apply { mkdirs() }
            val outFile = File(cacheDir, "daily_english_card.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 96, fos)
            }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }.getOrNull()

        return runCatching {
            val intent = if (imageUri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, shareCaption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareCaption)
                }
            }
            val chooser = Intent.createChooser(intent, "학습 성적표 카드 공유하기").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    fun openGooglePlayStoreListing(context: Context): Boolean {
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(marketIntent)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(webIntent)
                true
            }.getOrDefault(false)
        }
    }

    fun shareConstructiveFeedback(context: Context, feedback: String): Boolean {
        val clean = feedback.trim()
        if (clean.isEmpty()) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "[앱 개선 의견] ${context.packageName}")
                putExtra(Intent.EXTRA_TEXT, clean)
            }
            val chooser = Intent.createChooser(intent, "개선 의견 보내기").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
