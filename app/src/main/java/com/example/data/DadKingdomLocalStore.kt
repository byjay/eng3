package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.engine.DailyMissionEngine
import com.example.engine.LearningMission
import com.example.engine.LedgerGrantResult
import com.example.engine.MediaReviewStatus
import com.example.engine.MemoryPalaceEntry
import com.example.engine.MemoryPalaceRoom
import com.example.engine.MissionCadence
import com.example.engine.MissionKind
import com.example.engine.MissionStatus
import com.example.engine.ParentPinCrypto
import com.example.engine.PinVerificationResult
import com.example.engine.RecallGrade
import com.example.engine.RewardEventType
import com.example.engine.RewardLedgerEngine
import com.example.engine.RewardTransaction
import com.example.engine.Sm2ReviewScheduler
import com.example.engine.VocabAnalyticsTracker
import com.example.engine.WordLearningProgress
import com.example.engine.WordLearningStatus
import com.example.model.DadAllowanceMission
import com.example.model.GiftRequestItem
import com.example.model.GuestInviteSession
import com.example.model.KakaoFriend
import com.example.model.KakaoSessionState
import com.example.model.KingdomProgressStats
import com.example.model.MysteryChestReward
import com.example.model.ParentProfiles
import com.example.model.ScorecardEntry
import com.example.model.StudyScheduleItem
import com.example.model.StudyTimeLogEntry
import com.example.model.UserProgress
import com.example.model.WrongNoteRecord
import com.example.ui.components.DailyStudyMetric
import com.example.ui.components.TodayWordResultEntry
import com.example.ui.components.WeeklyStudyReportData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.dadKingdomDataStore by preferencesDataStore(name = "dad_daughter_kingdom_store")

data class LearningAchievementResult(
    val unlockedBoss: Boolean,
    val leveledUp: Boolean,
    val streakMilestoneHit: Int? = null,
    val todayWordsCompleted: Int = 0
)

class DadKingdomLocalStore(private val context: Context) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }

    private val schemaVersionKey = intPreferencesKey("datastore_schema_version")
    private val daughterNameKey = stringPreferencesKey("daughter_name")
    private val dadNameKey = stringPreferencesKey("dad_name")
    private val momNameKey = stringPreferencesKey("mom_name")
    private val parentPinFailCountKey = intPreferencesKey("parent_pin_fail_count")
    private val parentPinLockUntilMsKey = longPreferencesKey("parent_pin_lock_until_ms")
    private val parentPinKey = stringPreferencesKey("parent_pin") // Legacy plaintext key (auto-cleared on save)
    private val parentPinSaltKey = stringPreferencesKey("parent_pin_salt")
    private val parentPinHashKey = stringPreferencesKey("parent_pin_hash")
    private val selectedGradeKey = intPreferencesKey("selected_grade")
    private val selectedTeacherKey = stringPreferencesKey("selected_teacher")
    private val dailyGoalKey = intPreferencesKey("daily_goal")
    private val streakKey = intPreferencesKey("streak_count")
    private val totalXpKey = intPreferencesKey("total_xp")
    private val coinsKey = intPreferencesKey("coins")
    private val heartsKey = intPreferencesKey("hearts")
    private val todayWordsKey = intPreferencesKey("today_words_completed")
    private val lastStudyDateKey = stringPreferencesKey("last_study_date")
    private val unlockedItemsKey = stringPreferencesKey("unlocked_items_json")
    private val masteredWordsKey = stringPreferencesKey("mastered_words_json")
    private val weakWordsKey = stringPreferencesKey("weak_words_json")
    private val wrongNoteRecordsKey = stringPreferencesKey("wrong_note_records_json")
    private val clearedScenariosKey = stringPreferencesKey("cleared_scenarios_json")
    private val dadMissionsKey = stringPreferencesKey("dad_missions_json")
    private val giftRequestsKey = stringPreferencesKey("gift_requests_json")
    private val scorecardKey = stringPreferencesKey("scorecard_json")
    private val scheduleKey = stringPreferencesKey("schedule_json")

    // Family Kingdom RPG 7-mission & Level-up counters
    private val totalAllowanceWonKey = intPreferencesKey("total_allowance_won")
    private val castleHpKey = intPreferencesKey("castle_hp_percent")
    private val missionClearCountKey = intPreferencesKey("mission_clear_count")
    private val difficultyTierKey = intPreferencesKey("difficulty_tier")
    private val bossPendingKey = booleanPreferencesKey("boss_pending")
    private val bossVictoriesKey = intPreferencesKey("boss_victories")
    private val previewCountKey = intPreferencesKey("preview_count")
    private val reviewCountKey = intPreferencesKey("review_count")
    private val perfectScoreCountKey = intPreferencesKey("perfect_score_count")
    private val princessLevelKey = intPreferencesKey("princess_level")
    private val giftTicketsKey = intPreferencesKey("gift_tickets")

    // Kakao Session
    private val kakaoUserIdKey = stringPreferencesKey("kakao_user_id")
    private val kakaoNicknameKey = stringPreferencesKey("kakao_nickname")
    private val kakaoProfileUrlKey = stringPreferencesKey("kakao_profile_url")
    private val kakaoTokenKey = stringPreferencesKey("kakao_access_token")
    private val kakaoFriendsKey = stringPreferencesKey("kakao_friends_json")
    private val shareStreakWithFriendsKey = booleanPreferencesKey("share_streak_with_friends")

    // Daily Free Word API Accumulated DB (50+ fresh words/day) & 1~5 Tier + Cloudflare
    private val harvestedWordsKey = stringPreferencesKey("harvested_api_words_json")
    private val lastHarvestDateKey = stringPreferencesKey("last_harvest_date")
    private val vocabGradeTierKey = intPreferencesKey("vocab_grade_tier_1_to_5")
    private val levelTestDoneKey = booleanPreferencesKey("level_test_done")
    private val cloudflareWorkerUrlKey = stringPreferencesKey("cloudflare_worker_url")
    private val studyTimeLogsKey = stringPreferencesKey("study_time_logs_json")
    private val guestInviteCodeKey = stringPreferencesKey("guest_invite_code")
    private val guestHostKey = stringPreferencesKey("guest_host_nickname")
    private val guestModeKey = booleanPreferencesKey("guest_mode_active")
    private val myGeneratedInviteCodeKey = stringPreferencesKey("my_generated_invite_code")

    // White Pygmy Squirrel RPG Hero + Story Chapters + Mystery Chests (no spoilers!)
    private val selectedSquirrelHeroKey = stringPreferencesKey("selected_squirrel_hero_id")
    private val clearedRpgChaptersKey = stringPreferencesKey("cleared_rpg_chapters_json")
    private val unopenedChestsKey = intPreferencesKey("unopened_mystery_chests")
    private val openedTreasuresKey = stringPreferencesKey("opened_mystery_treasures_json")

    // Weekly Study Report (Per-Day Metrics + Today's Studied Words Scorecard)
    private val dailyWordMetricsKey = stringPreferencesKey("daily_word_metrics_json")
    private val todayWordResultsKey = stringPreferencesKey("today_word_results_json")

    // v2 Master Spec Additive Keys (Zero destructive migration; unified with totalXpKey & coinsKey)
    private val wordSm2ProgressKey = stringPreferencesKey("word_sm2_progress_json")
    private val rewardLedgerKey = stringPreferencesKey("reward_ledger_json")
    private val rewardEventIdsKey = stringPreferencesKey("reward_event_ids_json")
    private val vocabMissionsKey = stringPreferencesKey("vocab_missions_json")
    private val vocabMissionsDateKey = stringPreferencesKey("vocab_missions_date")
    private val memoryPalaceKey = stringPreferencesKey("memory_palace_json")
    private val mediaReviewOverridesKey = stringPreferencesKey("media_review_overrides_json")

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val weeklyStudyReportFlow: Flow<WeeklyStudyReportData> = context.dadKingdomDataStore.data.map { prefs ->
        val today = todayIso()
        val storedDate = prefs[lastStudyDateKey] ?: today
        val todayWordsCompleted = if (storedDate == today) (prefs[todayWordsKey] ?: 0) else 0
        val streakCount = prefs[streakKey] ?: 0
        val dailyGoal = (prefs[dailyGoalKey] ?: 10).coerceIn(5, 30)

        val metricsMap = parseDailyWordMetrics(prefs[dailyWordMetricsKey])
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val koWeekdays = arrayOf("일", "월", "화", "수", "목", "금", "토")

        val rolling7Days = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dateStr = dayFmt.format(cal.time)
            val labelKo = koWeekdays[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
            val stored = metricsMap[dateStr]
            val known = if (daysAgo == 0) maxOf(stored?.first ?: 0, todayWordsCompleted) else (stored?.first ?: 0)
            val review = stored?.second ?: 0
            DailyStudyMetric(
                dateIso = dateStr,
                dayLabelKo = labelKo,
                isToday = daysAgo == 0,
                knownCount = known,
                reviewCount = review
            )
        }

        val todayWords = parseTodayWordResults(prefs[todayWordResultsKey]).filter { it.dateIso == today }

        WeeklyStudyReportData(
            dailyMetrics = rolling7Days,
            todayWordResults = todayWords,
            cumulativeStreakCount = streakCount,
            todayWordsCompleted = todayWordsCompleted,
            dailyGoalTarget = dailyGoal
        )
    }

    val guestInviteSessionFlow: Flow<GuestInviteSession> = context.dadKingdomDataStore.data.map { prefs ->
        val isGuest = prefs[guestModeKey] ?: false
        val code = prefs[guestInviteCodeKey] ?: ""
        val host = prefs[guestHostKey] ?: "원삐"
        val myCode = prefs[myGeneratedInviteCodeKey] ?: ""
        val kakaoLinked = !(prefs[kakaoTokenKey].isNullOrBlank())
        GuestInviteSession(
            isGuestMode = isGuest,
            inviteCode = code,
            hostNickname = host,
            guestNickname = if (isGuest) "손님($code)" else "",
            myGeneratedCode = myCode,
            kakaoLinked = kakaoLinked
        )
    }

    val studyLogsFlow: Flow<List<StudyTimeLogEntry>> = context.dadKingdomDataStore.data.map { prefs ->
        parseStudyLogs(prefs[studyTimeLogsKey])
    }

    val selectedSquirrelHeroIdFlow: Flow<String> = context.dadKingdomDataStore.data.map { prefs ->
        prefs[selectedSquirrelHeroKey] ?: "hero_luna"
    }

    val clearedRpgChaptersFlow: Flow<Set<String>> = context.dadKingdomDataStore.data.map { prefs ->
        parseStringList(prefs[clearedRpgChaptersKey]).toSet()
    }

    val unopenedChestsFlow: Flow<Int> = context.dadKingdomDataStore.data.map { prefs ->
        (prefs[unopenedChestsKey] ?: 0).coerceAtLeast(0)
    }

    val openedTreasuresFlow: Flow<List<MysteryChestReward>> = context.dadKingdomDataStore.data.map { prefs ->
        parseOpenedTreasures(prefs[openedTreasuresKey])
    }

    val harvestedWordsFlow: Flow<List<com.example.model.VocabWord>> = context.dadKingdomDataStore.data.map { prefs ->
        parseHarvestedWords(prefs[harvestedWordsKey])
    }

    val lastHarvestDateFlow: Flow<String> = context.dadKingdomDataStore.data.map { prefs ->
        prefs[lastHarvestDateKey] ?: ""
    }

    val vocabGradeTierFlow: Flow<Int> = context.dadKingdomDataStore.data.map { prefs ->
        (prefs[vocabGradeTierKey] ?: 3).coerceIn(1, 5)
    }

    val levelTestDoneFlow: Flow<Boolean> = context.dadKingdomDataStore.data.map { prefs ->
        prefs[levelTestDoneKey] ?: false
    }

    val cloudflareWorkerUrlFlow: Flow<String> = context.dadKingdomDataStore.data.map { prefs ->
        prefs[cloudflareWorkerUrlKey] ?: ""
    }

    private fun sanitizePlayerName(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return when {
            trimmed.isBlank() ||
                trimmed.contains("공주") ||
                trimmed.contains("우리 딸") ||
                trimmed.contains("우리딸") ||
                trimmed.contains("빅뚱") -> "원삐"
            trimmed.contains("스몰뚱") -> "투삐"
            else -> trimmed
        }
    }

    val parentProfilesFlow: Flow<ParentProfiles> = context.dadKingdomDataStore.data.map { prefs ->
        val hasHashedPin = !prefs[parentPinSaltKey].isNullOrBlank() && !prefs[parentPinHashKey].isNullOrBlank()
        ParentProfiles(
            dadName = prefs[dadNameKey] ?: "아빠 국왕",
            momName = prefs[momNameKey] ?: "엄마 왕비",
            daughterName = sanitizePlayerName(prefs[daughterNameKey]),
            parentPin = "",
            isPinConfigured = hasHashedPin
        )
    }

    val kingdomStatsFlow: Flow<KingdomProgressStats> = context.dadKingdomDataStore.data.map { prefs ->
        val previews = prefs[previewCountKey] ?: 0
        val reviews = prefs[reviewCountKey] ?: 0
        val perfects = prefs[perfectScoreCountKey] ?: 0
        val computedLevel = 1 + ((previews + reviews * 2 + perfects * 3) / 6)
        KingdomProgressStats(
            castleHpPercent = (prefs[castleHpKey] ?: 40).coerceIn(0, 100),
            missionClearCount = prefs[missionClearCountKey] ?: 0,
            difficultyTier = (prefs[difficultyTierKey] ?: 1).coerceAtLeast(1),
            bossPending = prefs[bossPendingKey] ?: false,
            bossVictories = prefs[bossVictoriesKey] ?: 0,
            previewCount = previews,
            reviewCount = reviews,
            perfectScoreCount = perfects,
            princessLevel = maxOf(prefs[princessLevelKey] ?: 1, computedLevel),
            giftTickets = (prefs[giftTicketsKey] ?: 1).coerceAtLeast(0),
            totalAllowanceWon = (prefs[totalAllowanceWonKey] ?: 0).coerceAtLeast(0)
        )
    }

    val userProgressFlow: Flow<UserProgress> = context.dadKingdomDataStore.data.map { prefs ->
        val items = parseStringList(prefs[unlockedItemsKey]).ifEmpty { listOf("badge_starter") }
        val today = todayIso()
        val storedDate = prefs[lastStudyDateKey] ?: today
        val effectiveTodayWords = if (storedDate == today) (prefs[todayWordsKey] ?: 0) else 0
        UserProgress(
            userId = "local_player",
            displayName = sanitizePlayerName(prefs[daughterNameKey]),
            selectedGrade = prefs[selectedGradeKey] ?: 1,
            selectedTeacherId = prefs[selectedTeacherKey] ?: "emma_uk",
            dailyGoal = prefs[dailyGoalKey] ?: 10,
            streakCount = prefs[streakKey] ?: 0,
            totalXp = prefs[totalXpKey] ?: 150,
            coins = prefs[coinsKey] ?: 250,
            hearts = prefs[heartsKey] ?: 5,
            todayWordsCompleted = effectiveTodayWords,
            lastStudyDate = storedDate,
            unlockedItems = items
        )
    }

    val masteredWordIdsFlow: Flow<Set<String>> = context.dadKingdomDataStore.data.map { prefs ->
        parseStringList(prefs[masteredWordsKey]).toSet()
    }

    val weakWordIdsFlow: Flow<Set<String>> = context.dadKingdomDataStore.data.map { prefs ->
        parseStringList(prefs[weakWordsKey]).toSet()
    }

    val wrongNoteRecordsFlow: Flow<Map<String, WrongNoteRecord>> = context.dadKingdomDataStore.data.map { prefs ->
        val stored = parseWrongNoteRecords(prefs[wrongNoteRecordsKey]).toMutableMap()
        val weak = parseStringList(prefs[weakWordsKey]).toSet()
        val mastered = parseStringList(prefs[masteredWordsKey]).toSet()
        val sm2Map = parseWordSm2Progress(prefs[wordSm2ProgressKey])
        val fallbackDate = prefs[lastStudyDateKey]?.takeIf { it.isNotBlank() } ?: todayIso()

        // Ensure all active weak words have a WrongNoteRecord with study date
        weak.forEach { wordId ->
            val sm2 = sm2Map[wordId]
            val sm2Ms = sm2?.lastReviewedAtMs ?: 0L
            val dateIso = if (sm2Ms > 0L) formatDateIso(sm2Ms) else fallbackDate
            val existing = stored[wordId]
            if (existing == null) {
                stored[wordId] = WrongNoteRecord(
                    wordId = wordId,
                    savedDateIso = dateIso,
                    lastStudiedDateIso = dateIso,
                    lastStudiedAtMs = sm2Ms,
                    isMastered = false
                )
            } else if (existing.isMastered) {
                stored[wordId] = existing.copy(isMastered = false)
            }
        }

        // Backfill any historical SM-2 words that were ever missed
        sm2Map.forEach { (wordId, sm2) ->
            if (sm2.wrongCount > 0 || sm2.lapseCount > 0) {
                val dateIso = if (sm2.lastReviewedAtMs > 0L) formatDateIso(sm2.lastReviewedAtMs) else fallbackDate
                val derivedMastered = wordId !in weak &&
                    (wordId in mastered || sm2.learningStatus == WordLearningStatus.MASTERED)
                val existing = stored[wordId]
                if (existing == null) {
                    stored[wordId] = WrongNoteRecord(
                        wordId = wordId,
                        savedDateIso = dateIso,
                        lastStudiedDateIso = dateIso,
                        lastStudiedAtMs = sm2.lastReviewedAtMs,
                        isMastered = derivedMastered
                    )
                } else {
                    val bestMs = maxOf(existing.lastStudiedAtMs, sm2.lastReviewedAtMs)
                    val bestDate = if (sm2.lastReviewedAtMs > existing.lastStudiedAtMs) dateIso else existing.lastStudiedDateIso
                    stored[wordId] = existing.copy(
                        lastStudiedDateIso = bestDate.ifBlank { fallbackDate },
                        lastStudiedAtMs = bestMs,
                        isMastered = if (wordId in weak) false else (existing.isMastered || derivedMastered)
                    )
                }
            }
        }

        // Keep mastered flag synchronized with current mastered/weak sets
        stored.entries.forEach { (wordId, record) ->
            val syncedMastered = when {
                wordId in weak -> false
                wordId in mastered -> true
                else -> record.isMastered
            }
            if (syncedMastered != record.isMastered) {
                stored[wordId] = record.copy(isMastered = syncedMastered)
            }
        }

        stored
    }

    val clearedScenarioIdsFlow: Flow<Set<String>> = context.dadKingdomDataStore.data.map { prefs ->
        parseStringList(prefs[clearedScenariosKey]).toSet()
    }

    val dadMissionsFlow: Flow<List<DadAllowanceMission>> = context.dadKingdomDataStore.data.map { prefs ->
        val raw = prefs[dadMissionsKey]
        if (raw.isNullOrBlank()) defaultDadMissions() else parseMissions(raw)
    }

    val giftRequestsFlow: Flow<List<GiftRequestItem>> = context.dadKingdomDataStore.data.map { prefs ->
        val raw = prefs[giftRequestsKey]
        if (raw.isNullOrBlank()) defaultGiftRequests() else parseGiftRequests(raw)
    }

    val scorecardFlow: Flow<List<ScorecardEntry>> = context.dadKingdomDataStore.data.map { prefs ->
        val raw = prefs[scorecardKey]
        if (raw.isNullOrBlank()) defaultScorecards() else parseScorecards(raw)
    }

    val scheduleFlow: Flow<List<StudyScheduleItem>> = context.dadKingdomDataStore.data.map { prefs ->
        val raw = prefs[scheduleKey]
        if (raw.isNullOrBlank()) defaultSchedules() else parseSchedules(raw)
    }

    val kakaoSessionFlow: Flow<KakaoSessionState> = context.dadKingdomDataStore.data.map { prefs ->
        val token = prefs[kakaoTokenKey] ?: ""
        val kakaoUserId = prefs[kakaoUserIdKey] ?: ""
        val nick = prefs[kakaoNicknameKey] ?: ""
        val profileUrl = prefs[kakaoProfileUrlKey] ?: ""
        val friends = parseKakaoFriends(prefs[kakaoFriendsKey])
        val configured = KakaoAuthAndFriendsService.isKakaoKeyConfigured()
        KakaoSessionState(
            isLoggedIn = token.isNotBlank() && nick.isNotBlank(),
            isConfigured = configured,
            kakaoUserId = kakaoUserId,
            nickname = nick,
            profileImageUrl = profileUrl,
            accessToken = token,
            friends = friends,
            statusMessage = when {
                token.isNotBlank() -> "카카오톡 완벽 연동 완료 (${friends.size}명 친구 동기화)"
                configured -> "카카오톡 실로그인 준비 완료 (테스트 기간 로그인 생략 가능)"
                else -> "테스트 모드: 로그인 없이 즉시 이용 중 · 카톡 선물/성적 공유 바로 가능!"
            }
        )
    }

    val shareStreakWithFriendsFlow: Flow<Boolean> = context.dadKingdomDataStore.data.map { prefs ->
        // Minor privacy-by-default: require explicit opt-in before sharing streak with friends
        prefs[shareStreakWithFriendsKey] ?: false
    }

    suspend fun setShareStreakWithFriends(enabled: Boolean) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[schemaVersionKey] = CURRENT_SCHEMA_VERSION
            prefs[shareStreakWithFriendsKey] = enabled
        }
    }

    suspend fun saveParentProfiles(profiles: ParentProfiles) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[schemaVersionKey] = CURRENT_SCHEMA_VERSION
            prefs[dadNameKey] = profiles.dadName.trim().ifBlank { "아빠 국왕" }.take(24)
            prefs[momNameKey] = profiles.momName.trim().ifBlank { "엄마 왕비" }.take(24)
            prefs[daughterNameKey] = sanitizePlayerName(profiles.daughterName).take(24)
            val candidatePin = profiles.parentPin.filter { it.isDigit() }.take(6)
            if (ParentPinCrypto.isValidPinFormat(candidatePin)) {
                val (salt, hash) = ParentPinCrypto.createSaltedHash(candidatePin)
                prefs[parentPinSaltKey] = salt
                prefs[parentPinHashKey] = hash
                prefs[parentPinFailCountKey] = 0
                prefs[parentPinLockUntilMsKey] = 0L
                prefs.remove(parentPinKey)
            }
        }
    }

    suspend fun verifyParentPinWithLockout(enteredPin: String): PinVerificationResult {
        val prefs = context.dadKingdomDataStore.data.first()
        val salt = prefs[parentPinSaltKey].orEmpty()
        val hash = prefs[parentPinHashKey].orEmpty()
        val failCount = prefs[parentPinFailCountKey] ?: 0
        val lockUntilMs = prefs[parentPinLockUntilMsKey] ?: 0L

        val update = ParentPinCrypto.evaluatePinAttempt(
            candidatePin = enteredPin,
            storedSaltHex = salt,
            storedHashHex = hash,
            currentFailCount = failCount,
            lockUntilMs = lockUntilMs
        )
        if (update.result != PinVerificationResult.NotConfigured) {
            context.dadKingdomDataStore.edit { editable ->
                editable[schemaVersionKey] = CURRENT_SCHEMA_VERSION
                editable[parentPinFailCountKey] = update.nextFailCount
                editable[parentPinLockUntilMsKey] = update.nextLockUntilMs
            }
        }
        return update.result
    }

    suspend fun verifyParentPin(enteredPin: String): Boolean {
        return verifyParentPinWithLockout(enteredPin) == PinVerificationResult.Verified
    }

    suspend fun clearAllLocalData() {
        context.dadKingdomDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun updateActivePlayerName(playerName: String) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[daughterNameKey] = sanitizePlayerName(playerName).take(24)
            if (playerName == "원삐" || playerName == "투삐") {
                prefs[guestModeKey] = false
            }
        }
    }

    suspend fun saveGeneratedInviteCode(code: String) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[myGeneratedInviteCodeKey] = code
        }
    }

    suspend fun activateGuestByInviteCode(code: String, hostNickname: String) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[guestModeKey] = true
            prefs[guestInviteCodeKey] = code
            prefs[guestHostKey] = hostNickname.ifBlank { "원삐" }
            prefs[daughterNameKey] = "손님($code)"
        }
    }

    suspend fun updateSelectedGrade(grade: Int) {
        context.dadKingdomDataStore.edit { it[selectedGradeKey] = grade.coerceIn(0, 3) }
    }

    suspend fun updateSelectedTeacher(teacherId: String) {
        context.dadKingdomDataStore.edit { it[selectedTeacherKey] = teacherId }
    }

    /**
     * Records a completed mission and handles:
     * 1) Every 7th mission -> difficultyTier++ & unlocks Boss Battle!
     * 2) Preview/Review/Perfect accumulation -> Level Up + +1 Gift Request Ticket!
     */
    suspend fun recordLearningAchievement(
        xpDelta: Int,
        coinsDelta: Int,
        wordsDelta: Int = 0,
        castleHpDelta: Int = 2,
        missionCompleted: Boolean = false,
        previewDelta: Int = 0,
        reviewDelta: Int = 0,
        perfectDelta: Int = 0,
        newHearts: Int? = null
    ): LearningAchievementResult {
        var unlockedBoss = false
        var leveledUp = false
        var streakMilestoneHit: Int? = null
        var updatedTodayWords = 0
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            prefs[totalXpKey] = ((prefs[totalXpKey] ?: 150) + xpDelta).coerceAtLeast(0)
            prefs[coinsKey] = ((prefs[coinsKey] ?: 250) + coinsDelta).coerceAtLeast(0)

            val prevDate = prefs[lastStudyDateKey] ?: today
            val oldTodayWords = if (prevDate == today) (prefs[todayWordsKey] ?: 0) else 0
            val newTodayWords = (oldTodayWords + wordsDelta).coerceAtLeast(0)
            updatedTodayWords = newTodayWords
            prefs[todayWordsKey] = newTodayWords

            if (wordsDelta > 0) {
                val milestonesCrossed = (newTodayWords / 10) - (oldTodayWords / 10)
                if (milestonesCrossed > 0) {
                    val nextStreak = (prefs[streakKey] ?: 0) + milestonesCrossed
                    prefs[streakKey] = nextStreak
                    streakMilestoneHit = nextStreak
                }
            }

            prefs[castleHpKey] = ((prefs[castleHpKey] ?: 40) + castleHpDelta).coerceIn(0, 100)
            prefs[lastStudyDateKey] = today
            if (newHearts != null) {
                prefs[heartsKey] = newHearts.coerceIn(1, 5)
            }

            if (missionCompleted) {
                val nextCount = (prefs[missionClearCountKey] ?: 0) + 1
                prefs[missionClearCountKey] = nextCount
                if (nextCount % 7 == 0) {
                    val nextTier = (prefs[difficultyTierKey] ?: 1) + 1
                    prefs[difficultyTierKey] = nextTier
                    prefs[bossPendingKey] = true
                    unlockedBoss = true
                }
            }

            val oldPreviews = prefs[previewCountKey] ?: 0
            val oldReviews = prefs[reviewCountKey] ?: 0
            val oldPerfects = prefs[perfectScoreCountKey] ?: 0
            val oldLevel = maxOf(prefs[princessLevelKey] ?: 1, 1 + ((oldPreviews + oldReviews * 2 + oldPerfects * 3) / 6))

            val newPreviews = oldPreviews + previewDelta
            val newReviews = oldReviews + reviewDelta
            val newPerfects = oldPerfects + perfectDelta
            prefs[previewCountKey] = newPreviews
            prefs[reviewCountKey] = newReviews
            prefs[perfectScoreCountKey] = newPerfects

            val newLevel = 1 + ((newPreviews + newReviews * 2 + newPerfects * 3) / 6)
            if (newLevel > oldLevel) {
                prefs[princessLevelKey] = newLevel
                prefs[giftTicketsKey] = (prefs[giftTicketsKey] ?: 1) + (newLevel - oldLevel)
                leveledUp = true
            }
        }
        return LearningAchievementResult(
            unlockedBoss = unlockedBoss,
            leveledUp = leveledUp,
            streakMilestoneHit = streakMilestoneHit,
            todayWordsCompleted = updatedTodayWords
        )
    }

    suspend fun clearBossBattle() {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[bossPendingKey] = false
            prefs[bossVictoriesKey] = (prefs[bossVictoriesKey] ?: 0) + 1
            prefs[castleHpKey] = ((prefs[castleHpKey] ?: 40) + 20).coerceAtMost(100)
            prefs[coinsKey] = (prefs[coinsKey] ?: 250) + 150
            prefs[totalXpKey] = (prefs[totalXpKey] ?: 150) + 200
            prefs[giftTicketsKey] = (prefs[giftTicketsKey] ?: 1) + 1
        }
    }

    suspend fun markWordMastered(wordId: String) {
        val nowMs = System.currentTimeMillis()
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            val mastered = parseStringList(prefs[masteredWordsKey]).toMutableSet()
            mastered.add(wordId)
            prefs[masteredWordsKey] = serializeStringList(mastered.toList())

            val weak = parseStringList(prefs[weakWordsKey]).toMutableSet()
            val wasWeak = weak.remove(wordId)
            prefs[weakWordsKey] = serializeStringList(weak.toList())

            val records = parseWrongNoteRecords(prefs[wrongNoteRecordsKey]).toMutableMap()
            val existing = records[wordId]
            if (existing != null || wasWeak) {
                records[wordId] = WrongNoteRecord(
                    wordId = wordId,
                    savedDateIso = existing?.savedDateIso?.ifBlank { today } ?: today,
                    lastStudiedDateIso = today,
                    lastStudiedAtMs = nowMs,
                    isMastered = true
                )
                prefs[wrongNoteRecordsKey] = serializeWrongNoteRecords(records)
            }
        }
    }

    suspend fun markWordWeak(wordId: String) {
        val nowMs = System.currentTimeMillis()
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            val weak = parseStringList(prefs[weakWordsKey]).toMutableSet()
            weak.add(wordId)
            prefs[weakWordsKey] = serializeStringList(weak.toList())

            val mastered = parseStringList(prefs[masteredWordsKey]).toMutableSet()
            if (mastered.remove(wordId)) {
                prefs[masteredWordsKey] = serializeStringList(mastered.toList())
            }

            val records = parseWrongNoteRecords(prefs[wrongNoteRecordsKey]).toMutableMap()
            val existing = records[wordId]
            records[wordId] = WrongNoteRecord(
                wordId = wordId,
                savedDateIso = existing?.savedDateIso?.ifBlank { today } ?: today,
                lastStudiedDateIso = today,
                lastStudiedAtMs = nowMs,
                isMastered = false
            )
            prefs[wrongNoteRecordsKey] = serializeWrongNoteRecords(records)
        }
    }

    suspend fun recordDailyWordOutcome(
        wordId: String,
        word: String,
        meaningKo: String,
        known: Boolean
    ) {
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            val metricsMap = parseDailyWordMetrics(prefs[dailyWordMetricsKey]).toMutableMap()
            val prev = metricsMap[today] ?: (0 to 0)
            metricsMap[today] = if (known) {
                (prev.first + 1) to prev.second
            } else {
                prev.first to (prev.second + 1)
            }
            // Keep latest 14 days sorted
            val trimmedMetrics = metricsMap.entries.sortedByDescending { it.key }.take(14)
                .associate { it.key to it.value }
            prefs[dailyWordMetricsKey] = serializeDailyWordMetrics(trimmedMetrics)

            val existingResults = parseTodayWordResults(prefs[todayWordResultsKey])
                .filter { it.dateIso == today && it.wordId != wordId }
            val updatedEntry = TodayWordResultEntry(
                wordId = wordId,
                word = word,
                meaningKo = meaningKo,
                known = known,
                dateIso = today,
                timestampMs = System.currentTimeMillis()
            )
            prefs[todayWordResultsKey] = serializeTodayWordResults((listOf(updatedEntry) + existingResults).take(80))
        }
    }

    private fun serializeDailyWordMetrics(map: Map<String, Pair<Int, Int>>): String {
        val obj = JSONObject()
        map.forEach { (date, counts) ->
            obj.put(
                date,
                JSONObject().apply {
                    put("known", counts.first)
                    put("review", counts.second)
                }
            )
        }
        return obj.toString()
    }

    private fun parseDailyWordMetrics(json: String?): Map<String, Pair<Int, Int>> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Pair<Int, Int>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val date = keys.next()
                val item = obj.optJSONObject(date) ?: continue
                result[date] = item.optInt("known", 0).coerceAtLeast(0) to item.optInt("review", 0).coerceAtLeast(0)
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun serializeTodayWordResults(list: List<TodayWordResultEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("wordId", e.wordId)
                    put("word", e.word)
                    put("meaningKo", e.meaningKo)
                    put("known", e.known)
                    put("dateIso", e.dateIso)
                    put("timestampMs", e.timestampMs)
                }
            )
        }
        return arr.toString()
    }

    private fun parseTodayWordResults(json: String?): List<TodayWordResultEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                TodayWordResultEntry(
                    wordId = o.optString("wordId"),
                    word = o.optString("word"),
                    meaningKo = o.optString("meaningKo"),
                    known = o.optBoolean("known", true),
                    dateIso = o.optString("dateIso"),
                    timestampMs = o.optLong("timestampMs", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun markScenarioCleared(scenarioId: String) {
        context.dadKingdomDataStore.edit { prefs ->
            val list = parseStringList(prefs[clearedScenariosKey]).toMutableSet()
            list.add(scenarioId)
            prefs[clearedScenariosKey] = serializeStringList(list.toList())
        }
    }

    suspend fun buyCollectible(itemId: String, costCoins: Int): Result<Unit> {
        val prefs = context.dadKingdomDataStore.data.first()
        val curCoins = prefs[coinsKey] ?: 250
        val currentItems = parseStringList(prefs[unlockedItemsKey]).ifEmpty { listOf("badge_starter") }
        if (itemId in currentItems) return Result.success(Unit)
        if (curCoins < costCoins) return Result.failure(IllegalStateException("코인이 부족해요! 왕국 미션을 깨고 코인을 모아보세요."))
        context.dadKingdomDataStore.edit { p ->
            p[coinsKey] = (curCoins - costCoins).coerceAtLeast(0)
            p[unlockedItemsKey] = serializeStringList(currentItems + itemId)
            p[castleHpKey] = ((p[castleHpKey] ?: 40) + 5).coerceAtMost(100)
        }
        return Result.success(Unit)
    }

    suspend fun refillHearts(costCoins: Int = 50): Result<Unit> {
        val prefs = context.dadKingdomDataStore.data.first()
        val curCoins = prefs[coinsKey] ?: 250
        if (curCoins < costCoins) return Result.failure(IllegalStateException("하트 충전에 ${costCoins} 코인이 필요합니다."))
        context.dadKingdomDataStore.edit { p ->
            p[coinsKey] = (curCoins - costCoins).coerceAtLeast(0)
            p[heartsKey] = 5
        }
        return Result.success(Unit)
    }

    suspend fun saveDadMissions(missions: List<DadAllowanceMission>, allowanceDeltaWon: Int = 0, castleBonus: Int = 0) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[dadMissionsKey] = serializeMissions(missions)
            if (allowanceDeltaWon != 0) {
                prefs[totalAllowanceWonKey] = ((prefs[totalAllowanceWonKey] ?: 0) + allowanceDeltaWon).coerceAtLeast(0)
            }
            if (castleBonus != 0) {
                prefs[castleHpKey] = ((prefs[castleHpKey] ?: 40) + castleBonus).coerceIn(0, 100)
            }
        }
    }

    suspend fun saveGiftRequests(items: List<GiftRequestItem>, consumeTicket: Boolean = false) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[giftRequestsKey] = serializeGiftRequests(items)
            if (consumeTicket) {
                val cur = prefs[giftTicketsKey] ?: 1
                prefs[giftTicketsKey] = (cur - 1).coerceAtLeast(0)
            }
        }
    }

    suspend fun saveScorecards(entries: List<ScorecardEntry>) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[scorecardKey] = serializeScorecards(entries)
        }
    }

    suspend fun saveSchedules(items: List<StudyScheduleItem>) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[scheduleKey] = serializeSchedules(items)
        }
    }

    suspend fun saveKakaoSession(
        nickname: String,
        profileImageUrl: String,
        accessToken: String,
        friends: List<KakaoFriend>,
        kakaoUserId: String = ""
    ) {
        context.dadKingdomDataStore.edit { prefs ->
            if (kakaoUserId.isNotBlank()) {
                prefs[kakaoUserIdKey] = KakaoAuthAndFriendsService.normalizeKakaoId(kakaoUserId)
            }
            prefs[kakaoNicknameKey] = nickname
            prefs[kakaoProfileUrlKey] = KakaoAuthAndFriendsService.sanitizeHttpsUrl(profileImageUrl)
            prefs[kakaoTokenKey] = accessToken
            prefs[kakaoFriendsKey] = serializeKakaoFriends(friends)
        }
    }

    suspend fun clearKakaoSession() {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[kakaoUserIdKey] = ""
            prefs[kakaoNicknameKey] = ""
            prefs[kakaoProfileUrlKey] = ""
            prefs[kakaoTokenKey] = ""
            prefs[kakaoFriendsKey] = "[]"
        }
    }

    private fun defaultDadMissions(): List<DadAllowanceMission> = listOf(
        DadAllowanceMission(
            id = "mission_castle_gate",
            titleKo = "성문 사수: 수능 빈출 영단어 + 예문 4개 말하기",
            descriptionKo = "ABC 순서 없는 빈출 단어를 뚫고 원어민처럼 소리 내어 읽으면 치킨 성벽이 세워진다!",
            rewardWon = 3000,
            castleHpBonus = 15,
            categoryKo = "아빠 국왕 칙령"
        ),
        DadAllowanceMission(
            id = "mission_royal_voice",
            titleKo = "마법 통신탑 해방: 원어민 회화학습 5턴 수다",
            descriptionKo = "원어민 회화학습 화면에서 선생님과 영어 질문·수다 5번 성공하기!",
            rewardWon = 2000,
            castleHpBonus = 12,
            categoryKo = "엄마 왕비 퀘스트"
        ),
        DadAllowanceMission(
            id = "mission_exam_dragon",
            titleKo = "시험 마왕 격파: 영어 시험 90점 돌파!",
            descriptionKo = "점수표에 90점 이상 기록하고 국왕님(아빠)께 PIN 승인 받으면 특별 용돈 약속 기록!",
            rewardWon = 10000,
            castleHpBonus = 25,
            categoryKo = "보스전 용돈"
        )
    )

    private fun defaultGiftRequests(): List<GiftRequestItem> = listOf(
        GiftRequestItem(
            id = "gift_1",
            targetParent = "아빠",
            giftTitleKo = "주말 저녁 치킨 + 탕후루 세트",
            reasonKo = "예습·복습 누적 레벨 2 달성 & 성문 수복 기념!",
            requiredLevel = 2,
            isApproved = false,
            createdDateLabel = todayIso()
        ),
        GiftRequestItem(
            id = "gift_2",
            targetParent = "엄마",
            giftTitleKo = "예쁜 필기구 세트 & 카페 데이트",
            reasonKo = "수능 빈출 단어 오답 복습 완벽 클리어!",
            requiredLevel = 1,
            isApproved = false,
            createdDateLabel = todayIso()
        )
    )

    private fun defaultScorecards(): List<ScorecardEntry> = listOf(
        ScorecardEntry("sc_1", "1학기 중간고사 영어", "고등 영어", 86, 92, "2026-04-28"),
        ScorecardEntry("sc_2", "6월 전국연합 모의고사", "영어 독해/듣기", 91, 95, "2026-06-04"),
        ScorecardEntry("sc_3", "수능 빈출 단어 스피킹 테스트", "단어·발음", 94, 96, todayIso())
    )

    private fun defaultSchedules(): List<StudyScheduleItem> = listOf(
        StudyScheduleItem("sch_1", "월~금", "오후 8:00", "성벽 방어: 수능 빈출·랜덤 영단어 + 예문 4개", "ABC 순서 없이 읽고·듣고·말하기 15분"),
        StudyScheduleItem("sch_2", "화·목", "오후 8:30", "1~3문장 패턴 교체 & 반복 듣기 도장", "자기소개 및 마트/카페 실전 문장 3회 반복"),
        StudyScheduleItem("sch_3", "토요일", "오전 10:30", "원어민 선생님과 LIVE 회화학습", "한국어로 궁금한 문장 물어보고 바로 발음 교정"),
        StudyScheduleItem("sch_4", "일요일", "오후 7:00", "아빠 국왕·엄마 왕비 주간 정산식", "성적표 확인 · 용돈 승인 · 선물 요청서 제출")
    )

    private fun parseStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun serializeStringList(list: List<String>): String {
        val arr = JSONArray()
        list.distinct().forEach { arr.put(it) }
        return arr.toString()
    }

    private fun serializeMissions(list: List<DadAllowanceMission>): String {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("titleKo", m.titleKo)
                    put("descriptionKo", m.descriptionKo)
                    put("rewardWon", m.rewardWon)
                    put("castleHpBonus", m.castleHpBonus)
                    put("categoryKo", m.categoryKo)
                    put("isCompletedByDaughter", m.isCompletedByDaughter)
                    put("isApprovedByDad", m.isApprovedByDad)
                }
            )
        }
        return arr.toString()
    }

    private fun parseMissions(json: String): List<DadAllowanceMission> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            DadAllowanceMission(
                id = o.optString("id"),
                titleKo = o.optString("titleKo"),
                descriptionKo = o.optString("descriptionKo"),
                rewardWon = o.optInt("rewardWon", 2000),
                castleHpBonus = o.optInt("castleHpBonus", 10),
                categoryKo = o.optString("categoryKo", "아빠 국왕 칙령"),
                isCompletedByDaughter = o.optBoolean("isCompletedByDaughter", false),
                isApprovedByDad = o.optBoolean("isApprovedByDad", false)
            )
        }
    }.getOrDefault(defaultDadMissions())

    private fun serializeGiftRequests(list: List<GiftRequestItem>): String {
        val arr = JSONArray()
        list.forEach { g ->
            arr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("targetParent", g.targetParent)
                    put("giftTitleKo", g.giftTitleKo)
                    put("reasonKo", g.reasonKo)
                    put("requiredLevel", g.requiredLevel)
                    put("isApproved", g.isApproved)
                    put("createdDateLabel", g.createdDateLabel)
                }
            )
        }
        return arr.toString()
    }

    private fun parseGiftRequests(json: String): List<GiftRequestItem> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            GiftRequestItem(
                id = o.optString("id"),
                targetParent = o.optString("targetParent", "아빠"),
                giftTitleKo = o.optString("giftTitleKo"),
                reasonKo = o.optString("reasonKo"),
                requiredLevel = o.optInt("requiredLevel", 1),
                isApproved = o.optBoolean("isApproved", false),
                createdDateLabel = o.optString("createdDateLabel", todayIso())
            )
        }
    }.getOrDefault(defaultGiftRequests())

    private fun serializeScorecards(list: List<ScorecardEntry>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("examTitleKo", s.examTitleKo)
                    put("subjectKo", s.subjectKo)
                    put("score", s.score)
                    put("targetScore", s.targetScore)
                    put("dateLabel", s.dateLabel)
                }
            )
        }
        return arr.toString()
    }

    private fun parseScorecards(json: String): List<ScorecardEntry> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            ScorecardEntry(
                id = o.optString("id"),
                examTitleKo = o.optString("examTitleKo"),
                subjectKo = o.optString("subjectKo", "영어"),
                score = o.optInt("score", 85),
                targetScore = o.optInt("targetScore", 95),
                dateLabel = o.optString("dateLabel", todayIso())
            )
        }
    }.getOrDefault(defaultScorecards())

    private fun serializeSchedules(list: List<StudyScheduleItem>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("dayLabelKo", s.dayLabelKo)
                    put("timeLabel", s.timeLabel)
                    put("titleKo", s.titleKo)
                    put("detailKo", s.detailKo)
                    put("isCompleted", s.isCompleted)
                }
            )
        }
        return arr.toString()
    }

    private fun parseSchedules(json: String): List<StudyScheduleItem> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            StudyScheduleItem(
                id = o.optString("id"),
                dayLabelKo = o.optString("dayLabelKo"),
                timeLabel = o.optString("timeLabel"),
                titleKo = o.optString("titleKo"),
                detailKo = o.optString("detailKo"),
                isCompleted = o.optBoolean("isCompleted", false)
            )
        }
    }.getOrDefault(defaultSchedules())

    private fun serializeKakaoFriends(list: List<KakaoFriend>): String {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(
                JSONObject().apply {
                    put("uuid", f.uuid)
                    put("kakaoId", f.kakaoId)
                    put("nickname", f.nickname)
                    put("thumbnailUrl", f.thumbnailUrl)
                    put("favorite", f.favorite)
                }
            )
        }
        return arr.toString()
    }

    suspend fun setVocabGradeTier(tier: Int, fromLevelTest: Boolean = false) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[vocabGradeTierKey] = tier.coerceIn(1, 5)
            if (fromLevelTest) {
                prefs[levelTestDoneKey] = true
            }
        }
    }

    suspend fun saveCloudflareWorkerUrl(url: String) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[cloudflareWorkerUrlKey] = url.trim()
        }
    }

    suspend fun appendHarvestedWords(newWords: List<com.example.model.VocabWord>): Int {
        var totalAfter = 0
        context.dadKingdomDataStore.edit { prefs ->
            val existing = parseHarvestedWords(prefs[harvestedWordsKey])
            val merged = (existing + newWords).distinctBy { it.word.lowercase() }.takeLast(2500)
            totalAfter = merged.size
            prefs[harvestedWordsKey] = serializeHarvestedWords(merged)
            prefs[lastHarvestDateKey] = todayIso()
        }
        return totalAfter
    }

    private fun serializeHarvestedWords(list: List<com.example.model.VocabWord>): String {
        val arr = JSONArray()
        list.forEach { w ->
            val exArr = JSONArray()
            w.examples.forEach { ex ->
                exArr.put(
                    JSONObject().apply {
                        put("labelKo", ex.labelKo)
                        put("english", ex.english)
                        put("korean", ex.korean)
                        put("ipaHint", ex.ipaHint)
                    }
                )
            }
            val distArr = JSONArray()
            w.distractors.forEach { distArr.put(it) }
            val synArr = JSONArray()
            w.synonyms.forEach { synArr.put(it) }
            val antArr = JSONArray()
            w.antonyms.forEach { antArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("id", w.id)
                    put("grade", w.grade)
                    put("unit", w.textbookUnitKo)
                    put("freq", w.csatFrequency)
                    put("diff", w.csatDifficulty)
                    put("topic", w.topic.name)
                    put("word", w.word)
                    put("ipa", w.ipa)
                    put("pos", w.partOfSpeech)
                    put("meaningKo", w.meaningKo)
                    put("tipKo", w.pronunciationTipKo)
                    put("examples", exArr)
                    put("distractors", distArr)
                    put("synGroupId", w.synonymGroupId)
                    put("synonyms", synArr)
                    put("antonyms", antArr)
                }
            )
        }
        return arr.toString()
    }

    private fun parseHarvestedWords(json: String?): List<com.example.model.VocabWord> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                val exArr = o.optJSONArray("examples")
                val examples = if (exArr != null && exArr.length() > 0) {
                    (0 until exArr.length()).map { j ->
                        val e = exArr.getJSONObject(j)
                        com.example.model.VocabExample(
                            labelKo = e.optString("labelKo", "실전 예문"),
                            english = e.optString("english"),
                            korean = e.optString("korean"),
                            ipaHint = e.optString("ipaHint")
                        )
                    }
                } else {
                    emptyList()
                }
                val distArr = o.optJSONArray("distractors")
                val distractors = if (distArr != null) {
                    (0 until distArr.length()).map { j -> distArr.optString(j) }
                } else {
                    emptyList()
                }
                val synArr = o.optJSONArray("synonyms")
                val synonyms = if (synArr != null) {
                    (0 until synArr.length()).mapNotNull { j -> synArr.optString(j).takeIf(String::isNotBlank) }
                } else {
                    emptyList()
                }
                val antArr = o.optJSONArray("antonyms")
                val antonyms = if (antArr != null) {
                    (0 until antArr.length()).mapNotNull { j -> antArr.optString(j).takeIf(String::isNotBlank) }
                } else {
                    emptyList()
                }
                val topicEnum = com.example.model.VocabTopic.entries.firstOrNull {
                    it.name == o.optString("topic")
                } ?: com.example.model.VocabTopic.SCHOOL_EXAM
                com.example.model.VocabWord(
                    id = o.optString("id"),
                    grade = o.optInt("grade", 1),
                    textbookUnitKo = o.optString("unit", "무료API 일일수집"),
                    csatFrequency = o.optInt("freq", 4),
                    csatDifficulty = o.optInt("diff", 2),
                    topic = topicEnum,
                    word = o.optString("word"),
                    ipa = o.optString("ipa"),
                    partOfSpeech = o.optString("pos", "n."),
                    meaningKo = o.optString("meaningKo"),
                    exampleEn = examples.firstOrNull()?.english ?: "",
                    exampleKo = examples.firstOrNull()?.korean ?: "",
                    examples = examples,
                    pronunciationTipKo = o.optString("tipKo"),
                    distractors = distractors,
                    synonymGroupId = o.optString("synGroupId"),
                    synonyms = synonyms,
                    antonyms = antonyms
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun appendStudyTimeLog(entry: StudyTimeLogEntry) {
        if (entry.durationSec < 1) return
        context.dadKingdomDataStore.edit { prefs ->
            val existing = parseStudyLogs(prefs[studyTimeLogsKey])
            val merged = (listOf(entry) + existing).take(200)
            prefs[studyTimeLogsKey] = serializeStudyLogs(merged)
        }
    }

    private fun serializeStudyLogs(list: List<StudyTimeLogEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("dateIso", e.dateIso)
                    put("startTimeLabel", e.startTimeLabel)
                    put("endTimeLabel", e.endTimeLabel)
                    put("playerName", e.playerName)
                    put("activityTypeKo", e.activityTypeKo)
                    put("methodKo", e.methodKo)
                    put("durationSec", e.durationSec)
                    put("wordsStudied", e.wordsStudied)
                    put("knownCount", e.knownCount)
                    put("unknownCount", e.unknownCount)
                }
            )
        }
        return arr.toString()
    }

    private fun parseStudyLogs(json: String?): List<StudyTimeLogEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                StudyTimeLogEntry(
                    id = o.optString("id"),
                    dateIso = o.optString("dateIso"),
                    startTimeLabel = o.optString("startTimeLabel"),
                    endTimeLabel = o.optString("endTimeLabel"),
                    playerName = sanitizePlayerName(o.optString("playerName")),
                    activityTypeKo = o.optString("activityTypeKo", "단어 학습"),
                    methodKo = o.optString("methodKo", "장면연상법"),
                    durationSec = o.optInt("durationSec", 0).coerceAtLeast(0),
                    wordsStudied = o.optInt("wordsStudied", 0).coerceAtLeast(0),
                    knownCount = o.optInt("knownCount", 0).coerceAtLeast(0),
                    unknownCount = o.optInt("unknownCount", 0).coerceAtLeast(0)
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun saveSelectedSquirrelHero(heroId: String) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[selectedSquirrelHeroKey] = heroId
        }
    }

    suspend fun recordRpgChapterVictory(
        chapterId: String,
        xpDelta: Int,
        coinsDelta: Int,
        castleHpBonus: Int,
        wordsCount: Int
    ): Int? {
        var streakMilestoneHit: Int? = null
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            val cleared = parseStringList(prefs[clearedRpgChaptersKey]).toMutableSet()
            cleared.add(chapterId)
            prefs[clearedRpgChaptersKey] = serializeStringList(cleared.toList())
            prefs[totalXpKey] = (prefs[totalXpKey] ?: 0) + xpDelta
            prefs[coinsKey] = (prefs[coinsKey] ?: 250) + coinsDelta
            prefs[castleHpKey] = ((prefs[castleHpKey] ?: 40) + castleHpBonus).coerceAtMost(100)
            val prevDate = prefs[lastStudyDateKey] ?: today
            val oldTodayWords = if (prevDate == today) (prefs[todayWordsKey] ?: 0) else 0
            val newTodayWords = (oldTodayWords + wordsCount).coerceAtLeast(0)
            prefs[todayWordsKey] = newTodayWords
            prefs[lastStudyDateKey] = today
            if (wordsCount > 0) {
                val crossed = (newTodayWords / 10) - (oldTodayWords / 10)
                if (crossed > 0) {
                    val nextStreak = (prefs[streakKey] ?: 0) + crossed
                    prefs[streakKey] = nextStreak
                    streakMilestoneHit = nextStreak
                }
            }
            prefs[unopenedChestsKey] = (prefs[unopenedChestsKey] ?: 0) + 1
        }
        return streakMilestoneHit
    }

    suspend fun grantBonusCoins(coinsDelta: Int) {
        if (coinsDelta <= 0) return
        context.dadKingdomDataStore.edit { prefs ->
            prefs[coinsKey] = (prefs[coinsKey] ?: 250) + coinsDelta
        }
    }

    suspend fun consumeMysteryChestAndSaveReward(reward: MysteryChestReward) {
        context.dadKingdomDataStore.edit { prefs ->
            val current = (prefs[unopenedChestsKey] ?: 0)
            if (current > 0) {
                prefs[unopenedChestsKey] = current - 1
            }
            val existing = parseOpenedTreasures(prefs[openedTreasuresKey])
            val updated = (listOf(reward) + existing).take(60)
            prefs[openedTreasuresKey] = serializeOpenedTreasures(updated)
        }
    }

    private fun serializeOpenedTreasures(list: List<MysteryChestReward>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("titleKo", item.titleKo)
                    put("rarityKo", item.rarityKo)
                    put("categoryKo", item.categoryKo)
                    put("openedDateLabel", item.openedDateLabel)
                }
            )
        }
        return arr.toString()
    }

    private fun parseOpenedTreasures(json: String?): List<MysteryChestReward> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                MysteryChestReward(
                    id = o.optString("id"),
                    titleKo = o.optString("titleKo"),
                    rarityKo = o.optString("rarityKo", "희귀"),
                    categoryKo = o.optString("categoryKo", "비밀 보물"),
                    openedDateLabel = o.optString("openedDateLabel", "")
                )
            }
        }.getOrDefault(emptyList())
    }

    // ==================== v2 MASTER SPEC: SM-2, IDEMPOTENT REWARD LEDGER, MISSIONS & MEMORY PALACE ====================

    val wordSm2ProgressFlow: Flow<Map<String, WordLearningProgress>> = context.dadKingdomDataStore.data.map { prefs ->
        parseWordSm2Progress(prefs[wordSm2ProgressKey])
    }

    val rewardLedgerFlow: Flow<List<RewardTransaction>> = context.dadKingdomDataStore.data.map { prefs ->
        parseRewardLedger(prefs[rewardLedgerKey])
    }

    val dailyMissionsFlow: Flow<List<LearningMission>> = context.dadKingdomDataStore.data.map { prefs ->
        val today = todayIso()
        val goal = (prefs[dailyGoalKey] ?: 10).coerceIn(5, 30)
        val existing = parseLearningMissions(prefs[vocabMissionsKey])
        DailyMissionEngine.rolloverMissions(existing, today, goal)
    }

    val memoryPalaceFlow: Flow<Map<String, MemoryPalaceEntry>> = context.dadKingdomDataStore.data.map { prefs ->
        parseMemoryPalace(prefs[memoryPalaceKey])
    }

    val mediaReviewOverridesFlow: Flow<Map<String, MediaReviewStatus>> = context.dadKingdomDataStore.data.map { prefs ->
        parseMediaReviewOverrides(prefs[mediaReviewOverridesKey])
    }

    suspend fun setDailyWordGoal(goal: Int) {
        val safeGoal = goal.coerceIn(5, 30)
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            prefs[dailyGoalKey] = safeGoal
            val existing = parseLearningMissions(prefs[vocabMissionsKey])
            val rolled = DailyMissionEngine.rolloverMissions(existing, today, safeGoal)
            prefs[vocabMissionsDateKey] = today
            prefs[vocabMissionsKey] = serializeLearningMissions(rolled)
        }
    }

    suspend fun toggleSuspendWord(wordId: String): WordLearningProgress {
        var result = WordLearningProgress(wordId = wordId)
        context.dadKingdomDataStore.edit { prefs ->
            val map = parseWordSm2Progress(prefs[wordSm2ProgressKey]).toMutableMap()
            val updated = Sm2ReviewScheduler.toggleSuspended(map[wordId], wordId)
            map[wordId] = updated
            result = updated
            prefs[wordSm2ProgressKey] = serializeWordSm2Progress(map)
        }
        return result
    }

    suspend fun setMediaReviewStatus(wordId: String, status: MediaReviewStatus) {
        context.dadKingdomDataStore.edit { prefs ->
            val map = parseMediaReviewOverrides(prefs[mediaReviewOverridesKey]).toMutableMap()
            map[wordId] = status
            prefs[mediaReviewOverridesKey] = serializeMediaReviewOverrides(map)
        }
    }

    suspend fun recordWordSm2Recall(
        wordId: String,
        grade: RecallGrade,
        nowMs: Long = System.currentTimeMillis()
    ): WordLearningProgress {
        var resultProgress = WordLearningProgress(wordId = wordId)
        context.dadKingdomDataStore.edit { prefs ->
            val map = parseWordSm2Progress(prefs[wordSm2ProgressKey]).toMutableMap()
            val updated = Sm2ReviewScheduler.review(map[wordId], wordId, grade, nowMs)
            map[wordId] = updated
            resultProgress = updated
            prefs[wordSm2ProgressKey] = serializeWordSm2Progress(map)

            // Keep mastered/weak sets and wrong-note history synchronized with SM-2 status
            val mastered = parseStringList(prefs[masteredWordsKey]).toMutableSet()
            val weak = parseStringList(prefs[weakWordsKey]).toMutableSet()
            val records = parseWrongNoteRecords(prefs[wrongNoteRecordsKey]).toMutableMap()
            val dateIso = formatDateIso(nowMs)
            val existingRecord = records[wordId]
            when (grade) {
                RecallGrade.AGAIN -> {
                    weak.add(wordId)
                    mastered.remove(wordId)
                    records[wordId] = WrongNoteRecord(
                        wordId = wordId,
                        savedDateIso = existingRecord?.savedDateIso?.ifBlank { dateIso } ?: dateIso,
                        lastStudiedDateIso = dateIso,
                        lastStudiedAtMs = nowMs,
                        isMastered = false
                    )
                }
                RecallGrade.HARD -> {
                    weak.add(wordId)
                    records[wordId] = WrongNoteRecord(
                        wordId = wordId,
                        savedDateIso = existingRecord?.savedDateIso?.ifBlank { dateIso } ?: dateIso,
                        lastStudiedDateIso = dateIso,
                        lastStudiedAtMs = nowMs,
                        isMastered = false
                    )
                }
                RecallGrade.GOOD, RecallGrade.EASY -> {
                    val wasWeak = weak.remove(wordId)
                    if (updated.learningStatus == WordLearningStatus.MASTERED || grade == RecallGrade.EASY) {
                        mastered.add(wordId)
                    }
                    if (existingRecord != null || wasWeak) {
                        records[wordId] = WrongNoteRecord(
                            wordId = wordId,
                            savedDateIso = existingRecord?.savedDateIso?.ifBlank { dateIso } ?: dateIso,
                            lastStudiedDateIso = dateIso,
                            lastStudiedAtMs = nowMs,
                            isMastered = true
                        )
                    }
                }
            }
            prefs[masteredWordsKey] = serializeStringList(mastered.toList())
            prefs[weakWordsKey] = serializeStringList(weak.toList())
            prefs[wrongNoteRecordsKey] = serializeWrongNoteRecords(records)
        }
        return resultProgress
    }

    /**
     * Grants XP & Coins into the SINGLE unified wallet (`totalXpKey`, `coinsKey`) with strict
     * `eventId` idempotency so duplicate taps, retries, or crash-recoveries never double-pay.
     */
    suspend fun grantIdempotentReward(
        eventId: String,
        eventType: RewardEventType,
        xpDelta: Int,
        coinDelta: Int,
        reason: String,
        wordId: String = "",
        missionId: String = "",
        sessionId: String = ""
    ): LedgerGrantResult {
        var grantResult = LedgerGrantResult(
            granted = false,
            duplicateBlocked = true,
            newTotalXp = 0,
            newCoinBalance = 0
        )
        context.dadKingdomDataStore.edit { prefs ->
            val seenIds = parseStringList(prefs[rewardEventIdsKey]).toMutableSet()
            val curXp = prefs[totalXpKey] ?: 150
            val curCoins = prefs[coinsKey] ?: 250
            val tx = RewardTransaction(
                transactionId = "tx_${System.currentTimeMillis()}_${eventId.hashCode().toUInt()}",
                eventId = eventId,
                eventType = eventType,
                xpDelta = xpDelta,
                coinDelta = coinDelta,
                reason = reason,
                wordId = wordId,
                missionId = missionId,
                sessionId = sessionId
            )
            val applied = RewardLedgerEngine.applyTransaction(seenIds, curXp, curCoins, tx)
            grantResult = applied
            if (applied.granted) {
                prefs[totalXpKey] = applied.newTotalXp
                prefs[coinsKey] = applied.newCoinBalance
                seenIds.add(eventId)
                // Keep latest 600 event IDs to bound storage while blocking same-day/session duplicates
                val trimmedIds = seenIds.toList().takeLast(600)
                prefs[rewardEventIdsKey] = serializeStringList(trimmedIds)
                val ledger = (listOf(tx) + parseRewardLedger(prefs[rewardLedgerKey])).take(120)
                prefs[rewardLedgerKey] = serializeRewardLedger(ledger)
                VocabAnalyticsTracker.log("reward_granted", wordId = wordId, meta = "${eventType.name}:+${xpDelta}xp/+${coinDelta}c")
            } else {
                VocabAnalyticsTracker.log("duplicate_reward_blocked", wordId = wordId, meta = eventId)
            }
        }
        return grantResult
    }

    suspend fun advanceVocabMission(kind: MissionKind, delta: Int = 1) {
        if (delta <= 0) return
        val today = todayIso()
        context.dadKingdomDataStore.edit { prefs ->
            val goal = (prefs[dailyGoalKey] ?: 10).coerceIn(5, 30)
            val base = DailyMissionEngine.rolloverMissions(
                existing = parseLearningMissions(prefs[vocabMissionsKey]),
                todayDateKey = today,
                dailyWordTarget = goal
            )
            val updated = DailyMissionEngine.recordProgress(base, kind, delta)
            prefs[vocabMissionsDateKey] = today
            prefs[vocabMissionsKey] = serializeLearningMissions(updated)
        }
    }

    suspend fun claimVocabMission(missionId: String): Pair<LearningMission?, LedgerGrantResult?> {
        val today = todayIso()
        var claimedMission: LearningMission? = null
        context.dadKingdomDataStore.edit { prefs ->
            val goal = (prefs[dailyGoalKey] ?: 10).coerceIn(5, 30)
            val base = DailyMissionEngine.rolloverMissions(
                existing = parseLearningMissions(prefs[vocabMissionsKey]),
                todayDateKey = today,
                dailyWordTarget = goal
            )
            val (updated, claimed) = DailyMissionEngine.claimMission(base, missionId)
            claimedMission = claimed
            if (claimed != null) {
                prefs[vocabMissionsDateKey] = today
                prefs[vocabMissionsKey] = serializeLearningMissions(updated)
            }
        }
        val mission = claimedMission ?: return null to null
        val rewardType = when (mission.cadence) {
            MissionCadence.WEEKLY -> RewardEventType.WEEKLY_MISSION_CLAIMED
            else -> RewardEventType.DAILY_MISSION_CLAIMED
        }
        val reward = grantIdempotentReward(
            eventId = "mission_claim_${mission.missionId}",
            eventType = rewardType,
            xpDelta = mission.rewardXp,
            coinDelta = mission.rewardCoins,
            reason = "미션 완료 보상: ${mission.titleKo}",
            missionId = mission.missionId
        )
        VocabAnalyticsTracker.log("mission_claimed", meta = mission.missionId)
        return mission to reward
    }

    suspend fun saveMemoryPalaceEntry(wordId: String, room: MemoryPalaceRoom, personalMemoKo: String) {
        context.dadKingdomDataStore.edit { prefs ->
            val map = parseMemoryPalace(prefs[memoryPalaceKey]).toMutableMap()
            map[wordId] = MemoryPalaceEntry(
                wordId = wordId,
                room = room,
                personalMemoKo = personalMemoKo.trim().take(120),
                updatedAtMs = System.currentTimeMillis()
            )
            prefs[memoryPalaceKey] = serializeMemoryPalace(map)
        }
    }

    private fun serializeWordSm2Progress(map: Map<String, WordLearningProgress>): String {
        val arr = JSONArray()
        map.values.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("wordId", p.wordId)
                    put("status", p.learningStatus.name)
                    put("lastGrade", p.lastRecallGrade?.name ?: "")
                    put("rep", p.repetition)
                    put("interval", p.intervalDays)
                    put("ease", p.easeFactor)
                    put("dueAt", p.dueAtMs)
                    put("lastRev", p.lastReviewedAtMs)
                    put("lapse", p.lapseCount)
                    put("consec", p.consecutiveCorrect)
                    put("correct", p.correctCount)
                    put("wrong", p.wrongCount)
                    put("mastery", p.masteryScore)
                }
            )
        }
        return arr.toString()
    }

    private fun parseWordSm2Progress(json: String?): Map<String, WordLearningProgress> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val arr = JSONArray(json)
            val result = LinkedHashMap<String, WordLearningProgress>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("wordId")
                if (id.isBlank()) continue
                val status = runCatching { WordLearningStatus.valueOf(o.optString("status", "NEW")) }
                    .getOrDefault(WordLearningStatus.NEW)
                val gradeRaw = o.optString("lastGrade", "")
                val grade = gradeRaw.takeIf { it.isNotBlank() }?.let {
                    runCatching { RecallGrade.valueOf(it) }.getOrNull()
                }
                result[id] = WordLearningProgress(
                    wordId = id,
                    learningStatus = status,
                    lastRecallGrade = grade,
                    repetition = o.optInt("rep", 0),
                    intervalDays = o.optInt("interval", 0),
                    easeFactor = o.optDouble("ease", 2.5),
                    dueAtMs = o.optLong("dueAt", 0L),
                    lastReviewedAtMs = o.optLong("lastRev", 0L),
                    lapseCount = o.optInt("lapse", 0),
                    consecutiveCorrect = o.optInt("consec", 0),
                    correctCount = o.optInt("correct", 0),
                    wrongCount = o.optInt("wrong", 0),
                    masteryScore = o.optInt("mastery", 0)
                )
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun serializeRewardLedger(list: List<RewardTransaction>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("txId", t.transactionId)
                    put("eventId", t.eventId)
                    put("type", t.eventType.name)
                    put("xp", t.xpDelta)
                    put("coins", t.coinDelta)
                    put("reason", t.reason)
                    put("wordId", t.wordId)
                    put("missionId", t.missionId)
                    put("sessionId", t.sessionId)
                    put("createdAt", t.createdAtMs)
                }
            )
        }
        return arr.toString()
    }

    private fun parseRewardLedger(json: String?): List<RewardTransaction> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RewardTransaction(
                    transactionId = o.optString("txId"),
                    eventId = o.optString("eventId"),
                    eventType = runCatching { RewardEventType.valueOf(o.optString("type")) }
                        .getOrDefault(RewardEventType.NEW_WORD_LEARNED),
                    xpDelta = o.optInt("xp", 0),
                    coinDelta = o.optInt("coins", 0),
                    reason = o.optString("reason", ""),
                    wordId = o.optString("wordId", ""),
                    missionId = o.optString("missionId", ""),
                    sessionId = o.optString("sessionId", ""),
                    createdAtMs = o.optLong("createdAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeLearningMissions(list: List<LearningMission>): String {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.missionId)
                    put("kind", m.kind.name)
                    put("title", m.titleKo)
                    put("desc", m.descriptionKo)
                    put("target", m.targetCount)
                    put("current", m.currentCount)
                    put("xp", m.rewardXp)
                    put("coins", m.rewardCoins)
                    put("status", m.status.name)
                    put("dateKey", m.dateKey)
                    put("claimedAt", m.claimedAtMs)
                    put("cadence", m.cadence.name)
                }
            )
        }
        return arr.toString()
    }

    private fun parseLearningMissions(json: String?): List<LearningMission> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val kind = runCatching { MissionKind.valueOf(o.optString("kind")) }
                    .getOrDefault(MissionKind.DAILY_NEW_WORDS)
                val defaultCadence = if (kind.name.startsWith("WEEKLY_")) MissionCadence.WEEKLY else MissionCadence.DAILY
                LearningMission(
                    missionId = o.optString("id"),
                    kind = kind,
                    titleKo = o.optString("title"),
                    descriptionKo = o.optString("desc"),
                    targetCount = o.optInt("target", 5),
                    currentCount = o.optInt("current", 0),
                    rewardXp = o.optInt("xp", 15),
                    rewardCoins = o.optInt("coins", 5),
                    status = runCatching { MissionStatus.valueOf(o.optString("status")) }
                        .getOrDefault(MissionStatus.AVAILABLE),
                    dateKey = o.optString("dateKey", ""),
                    claimedAtMs = o.optLong("claimedAt", 0L),
                    cadence = runCatching { MissionCadence.valueOf(o.optString("cadence")) }
                        .getOrDefault(defaultCadence)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeMemoryPalace(map: Map<String, MemoryPalaceEntry>): String {
        val arr = JSONArray()
        map.values.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("wordId", e.wordId)
                    put("room", e.room.name)
                    put("memo", e.personalMemoKo)
                    put("updatedAt", e.updatedAtMs)
                }
            )
        }
        return arr.toString()
    }

    private fun parseMemoryPalace(json: String?): Map<String, MemoryPalaceEntry> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val arr = JSONArray(json)
            val result = LinkedHashMap<String, MemoryPalaceEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("wordId")
                if (id.isBlank()) continue
                val room = runCatching { MemoryPalaceRoom.valueOf(o.optString("room")) }
                    .getOrDefault(MemoryPalaceRoom.HOME_ROOM)
                result[id] = MemoryPalaceEntry(
                    wordId = id,
                    room = room,
                    personalMemoKo = o.optString("memo", ""),
                    updatedAtMs = o.optLong("updatedAt", 0L)
                )
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun serializeMediaReviewOverrides(map: Map<String, MediaReviewStatus>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v.name) }
        return obj.toString()
    }

    private fun parseMediaReviewOverrides(json: String?): Map<String, MediaReviewStatus> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val result = LinkedHashMap<String, MediaReviewStatus>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val status = runCatching { MediaReviewStatus.valueOf(obj.optString(key)) }.getOrNull()
                if (status != null) result[key] = status
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun parseKakaoFriends(json: String?): List<KakaoFriend> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                val uuid = o.optString("uuid")
                val rawKakaoId = o.optString("kakaoId").ifBlank { uuid }
                KakaoFriend(
                    uuid = uuid,
                    nickname = o.optString("nickname"),
                    thumbnailUrl = KakaoAuthAndFriendsService.sanitizeHttpsUrl(o.optString("thumbnailUrl")),
                    favorite = o.optBoolean("favorite", false),
                    kakaoId = KakaoAuthAndFriendsService.normalizeKakaoId(rawKakaoId)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun formatDateIso(epochMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))
    }

    private fun serializeWrongNoteRecords(map: Map<String, WrongNoteRecord>): String {
        val arr = JSONArray()
        map.values.forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("wordId", rec.wordId)
                    put("savedDateIso", rec.savedDateIso)
                    put("lastStudiedDateIso", rec.lastStudiedDateIso)
                    put("lastStudiedAtMs", rec.lastStudiedAtMs)
                    put("isMastered", rec.isMastered)
                }
            )
        }
        return arr.toString()
    }

    private fun parseWrongNoteRecords(json: String?): Map<String, WrongNoteRecord> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val arr = JSONArray(json)
            val result = LinkedHashMap<String, WrongNoteRecord>()
            for (idx in 0 until arr.length()) {
                val o = arr.getJSONObject(idx)
                val wordId = o.optString("wordId").trim()
                if (wordId.isNotBlank()) {
                    val savedDate = o.optString("savedDateIso").ifBlank { todayIso() }
                    val lastDate = o.optString("lastStudiedDateIso").ifBlank { savedDate }
                    result[wordId] = WrongNoteRecord(
                        wordId = wordId,
                        savedDateIso = savedDate,
                        lastStudiedDateIso = lastDate,
                        lastStudiedAtMs = o.optLong("lastStudiedAtMs", 0L),
                        isMastered = o.optBoolean("isMastered", false)
                    )
                }
            }
            result
        }.getOrDefault(emptyMap())
    }
}
