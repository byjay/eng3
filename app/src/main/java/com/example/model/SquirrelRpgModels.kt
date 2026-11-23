package com.example.model

import androidx.annotation.DrawableRes

enum class SquirrelSkillKind {
    SHIELD_GUARD,     // 루나: 방어막 + 반격
    WORD_METEOR,      // 스텔라: 단어 공격 초강력 타격
    COMBO_ARROW,      // 로빈: 콤보 확정 치명타
    SUN_HEAL,         // 미아: HP 대량 회복 + 공격
    DRAGON_BREAKER,   // 레오: 보스 특효 강타
    GOLDEN_BREW,      // 코코: 코인 보너스 + 공격력 버프
    MELODY_BOOST,     // 하모니: 회화 공격력 대폭 상승
    TIME_REVEAL,      // 노아: 힌트 무료 공개 + 턴 스턴
    SHADOW_DOUBLE,    // 유키: 빠른 타격 2연타
    ROYAL_FEAST       // 모찌: 부활 버프 + 회복 + 성채 수복
}

data class SquirrelJobHero(
    val id: String,
    val nameKo: String,
    val jobTitleKo: String,
    val badgeKo: String,
    val passiveKo: String,
    val skillNameKo: String,
    val skillDescKo: String,
    val skillKind: SquirrelSkillKind,
    val skillMpCost: Int = 50,
    val maxHp: Int = 120,
    val baseAtk: Int = 28,
    @DrawableRes val sheetRes: Int,
    val columnIndex: Int, // 0..4 inside 5-column portrait sheet
    val accentHex: Long,
    val shadowHex: Long
)

data class RpgStoryChoice(
    val id: String,
    val labelKo: String,
    val tagKo: String,
    val outcomeKo: String,
    val bonusCoins: Int = 0,
    val hpBonus: Int = 0,
    val mpBonus: Int = 0,
    val atkBuffPercent: Int = 0,
    val startShield: Boolean = false
)

data class RpgMonster(
    val id: String,
    val nameKo: String,
    val titleKo: String,
    val maxHp: Int,
    val atk: Int,
    val accentHex: Long,
    val intentLabelKo: String,
    val battleQuoteKo: String,
    val isBoss: Boolean = false
)

data class EmeraldStoryChapter(
    val number: Int, // 1..6
    val id: String,
    val titleKo: String,
    val locationKo: String,
    val storyIntroKo: String,
    val squirrelLineKo: String,
    val choices: List<RpgStoryChoice>,
    val monster: RpgMonster,
    val rewardCoins: Int,
    val rewardXp: Int,
    val castleHpBonus: Int,
    val accentHex: Long
)

enum class RpgBattleAction(val labelKo: String, val subKo: String) {
    WORD_STRIKE("단어 공격", "1~5등급 어휘"),
    PHRASE_CAST("회화 공격", "문장 · 말하기"),
    CLASS_SKILL("직업 스킬", "MP 50 필살기"),
    GUARD_HINT("방어·힌트", "피해↓ 해마힌트")
}

enum class RpgGamePhase {
    IDLE,
    HERO_PICKER,
    STORY_DIALOGUE,
    STORY_OUTCOME,
    BATTLE_ACTION,
    BATTLE_WORD_CHALLENGE,
    BATTLE_PHRASE_CHALLENGE,
    VICTORY,
    DEFEAT
}

data class MysteryChestReward(
    val id: String,
    val titleKo: String,
    val rarityKo: String,
    val categoryKo: String,
    val openedDateLabel: String
)

data class RpgWordChallenge(
    val word: VocabWord,
    val exampleSentenceEn: String,
    val exampleSentenceKo: String,
    val options: List<String>,
    val showHint: Boolean = false,
    val isSkillAmplified: Boolean = false,
    val startedAtMs: Long = System.currentTimeMillis()
)

data class RpgPhraseChallenge(
    val promptKorean: String,
    val targetEnglish: String,
    val options: List<String>,
    val speakingTipKo: String,
    val showHint: Boolean = false,
    val isSkillAmplified: Boolean = false,
    val startedAtMs: Long = System.currentTimeMillis()
)

data class RpgGameUiState(
    val phase: RpgGamePhase = RpgGamePhase.IDLE,
    val activeChapter: EmeraldStoryChapter? = null,
    val selectedChoiceOutcomeKo: String = "",
    val heroHp: Int = 120,
    val heroMaxHp: Int = 120,
    val heroMp: Int = 40,
    val heroShieldActive: Boolean = false,
    val heroGuarding: Boolean = false,
    val heroAtkBuffPercent: Int = 0,
    val heroReviveAvailable: Boolean = false,
    val monsterHp: Int = 100,
    val monsterMaxHp: Int = 100,
    val monsterStunned: Boolean = false,
    val turnNumber: Int = 1,
    val comboCount: Int = 0,
    val wordChallenge: RpgWordChallenge? = null,
    val phraseChallenge: RpgPhraseChallenge? = null,
    val floatingBannerKo: String = "",
    val lastBattleLogKo: String = "",
    val heroHitTick: Int = 0,
    val monsterHitTick: Int = 0,
    val earnedCoins: Int = 0,
    val earnedXp: Int = 0,
    val earnedCastleHp: Int = 0,
    val wordsClearedInBattle: Int = 0,
    val mysteryChestPendingOpen: Boolean = false,
    val revealedChestReward: MysteryChestReward? = null
)
