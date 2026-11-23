package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    private val daughterNameKey = stringPreferencesKey("daughter_name")
    private val dadNameKey = stringPreferencesKey("dad_name")
    private val momNameKey = stringPreferencesKey("mom_name")
    private val parentPinKey = stringPreferencesKey("parent_pin")
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
    private val kakaoNicknameKey = stringPreferencesKey("kakao_nickname")
    private val kakaoProfileUrlKey = stringPreferencesKey("kakao_profile_url")
    private val kakaoTokenKey = stringPreferencesKey("kakao_access_token")
    private val kakaoFriendsKey = stringPreferencesKey("kakao_friends_json")

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

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val weeklyStudyReportFlow: Flow<WeeklyStudyReportData> = context.dadKingdomDataStore.data.map { prefs ->
        val today = todayIso()
        val storedDate = prefs[lastStudyDateKey] ?: today
        val todayWordsCompleted = if (storedDate == today) (prefs[todayWordsKey] ?: 0) else 0
        val streakCount = prefs[streakKey] ?: 0
        val dailyGoal = (prefs[dailyGoalKey] ?: 10).coerceAtLeast(10)

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
        ParentProfiles(
            dadName = prefs[dadNameKey] ?: "아빠 국왕",
            momName = prefs[momNameKey] ?: "엄마 왕비",
            daughterName = sanitizePlayerName(prefs[daughterNameKey]),
            parentPin = prefs[parentPinKey] ?: "1234"
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
        val nick = prefs[kakaoNicknameKey] ?: ""
        val profileUrl = prefs[kakaoProfileUrlKey] ?: ""
        val friends = parseKakaoFriends(prefs[kakaoFriendsKey])
        val configured = KakaoAuthAndFriendsService.isKakaoKeyConfigured()
        KakaoSessionState(
            isLoggedIn = token.isNotBlank() && nick.isNotBlank(),
            isConfigured = configured,
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

    suspend fun saveParentProfiles(profiles: ParentProfiles) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[dadNameKey] = profiles.dadName.trim().ifBlank { "아빠 국왕" }.take(24)
            prefs[momNameKey] = profiles.momName.trim().ifBlank { "엄마 왕비" }.take(24)
            prefs[daughterNameKey] = sanitizePlayerName(profiles.daughterName).take(24)
            val cleanPin = profiles.parentPin.filter { it.isDigit() }.take(4).ifBlank { "1234" }
            prefs[parentPinKey] = cleanPin
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
        context.dadKingdomDataStore.edit { prefs ->
            val mastered = parseStringList(prefs[masteredWordsKey]).toMutableSet()
            mastered.add(wordId)
            prefs[masteredWordsKey] = serializeStringList(mastered.toList())

            val weak = parseStringList(prefs[weakWordsKey]).toMutableSet()
            weak.remove(wordId)
            prefs[weakWordsKey] = serializeStringList(weak.toList())
        }
    }

    suspend fun markWordWeak(wordId: String) {
        context.dadKingdomDataStore.edit { prefs ->
            val weak = parseStringList(prefs[weakWordsKey]).toMutableSet()
            weak.add(wordId)
            prefs[weakWordsKey] = serializeStringList(weak.toList())
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
        friends: List<KakaoFriend>
    ) {
        context.dadKingdomDataStore.edit { prefs ->
            prefs[kakaoNicknameKey] = nickname
            prefs[kakaoProfileUrlKey] = profileImageUrl
            prefs[kakaoTokenKey] = accessToken
            prefs[kakaoFriendsKey] = serializeKakaoFriends(friends)
        }
    }

    suspend fun clearKakaoSession() {
        context.dadKingdomDataStore.edit { prefs ->
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
            descriptionKo = "점수표에 90점 이상 기록하고 국왕님(아빠)께 PIN 승인 받으면 특별 용돈 지급!",
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
            val merged = (existing + newWords).distinctBy { it.word.lowercase() }.takeLast(600)
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
                    distractors = distractors
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
                    methodKo = o.optString("methodKo", "해마학습법"),
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

    private fun parseKakaoFriends(json: String?): List<KakaoFriend> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                KakaoFriend(
                    uuid = o.optString("uuid"),
                    nickname = o.optString("nickname"),
                    thumbnailUrl = o.optString("thumbnailUrl"),
                    favorite = o.optBoolean("favorite", false)
                )
            }
        }.getOrDefault(emptyList())
    }
}
