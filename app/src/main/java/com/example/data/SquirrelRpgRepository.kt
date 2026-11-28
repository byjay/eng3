package com.example.data

import com.example.R
import com.example.model.EmeraldStoryChapter
import com.example.model.RpgMonster
import com.example.model.RpgStoryChoice
import com.example.model.SquirrelJobHero
import com.example.model.SquirrelSkillKind

object SquirrelRpgRepository {

    val heroes: List<SquirrelJobHero> = listOf(
        SquirrelJobHero(
            id = "hero_luna",
            nameKo = "루나",
            jobTitleKo = "에메랄드 성기사",
            badgeKo = "도토리 방패",
            passiveKo = "오답 시 1회 방어막 자동 수호",
            skillNameKo = "성채 수호 실드",
            skillDescKo = "방어막 생성 + 강력한 방패 강타",
            skillKind = SquirrelSkillKind.SHIELD_GUARD,
            skillMpCost = 50,
            maxHp = 140,
            baseAtk = 28,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_a_1795421787418,
            columnIndex = 0,
            accentHex = 0xFF4F6EF7,
            shadowHex = 0xFF3952CC
        ),
        SquirrelJobHero(
            id = "hero_stella",
            nameKo = "스텔라",
            jobTitleKo = "별빛 대마법사",
            badgeKo = "별빛 모자",
            passiveKo = "단어 공격 데미지 +25%",
            skillNameKo = "메테오 단어 폭격",
            skillDescKo = "단어 정답 시 2배 별빛 폭발",
            skillKind = SquirrelSkillKind.WORD_METEOR,
            skillMpCost = 50,
            maxHp = 115,
            baseAtk = 34,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_a_1795421787418,
            columnIndex = 1,
            accentHex = 0xFF2563EB,
            shadowHex = 0xFF1D4ED8
        ),
        SquirrelJobHero(
            id = "hero_robin",
            nameKo = "로빈",
            jobTitleKo = "바람 숲 궁수",
            badgeKo = "나뭇잎 망토",
            passiveKo = "2연속 정답부터 크리티컬 확정",
            skillNameKo = "바람 연속 화살",
            skillDescKo = "치명타 확정 2연속 회화 사격",
            skillKind = SquirrelSkillKind.COMBO_ARROW,
            skillMpCost = 50,
            maxHp = 120,
            baseAtk = 31,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_a_1795421787418,
            columnIndex = 2,
            accentHex = 0xFF0EA5E9,
            shadowHex = 0xFF0284C7
        ),
        SquirrelJobHero(
            id = "hero_mia",
            nameKo = "미아",
            jobTitleKo = "태양 성직자",
            badgeKo = "치유 지팡이",
            passiveKo = "정답마다 HP +12 자동 회복",
            skillNameKo = "축복의 태양 광선",
            skillDescKo = "HP +45 회복 & 성스러운 타격",
            skillKind = SquirrelSkillKind.SUN_HEAL,
            skillMpCost = 50,
            maxHp = 130,
            baseAtk = 27,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_a_1795421787418,
            columnIndex = 3,
            accentHex = 0xFFFF7A59,
            shadowHex = 0xFFD95B3D
        ),
        SquirrelJobHero(
            id = "hero_leo",
            nameKo = "레오",
            jobTitleKo = "드래곤 나이트",
            badgeKo = "미니 용갑옷",
            passiveKo = "보스 몬스터 추가 피해 +30%",
            skillNameKo = "드래곤 창 돌파",
            skillDescKo = "보스 방어 관통 초대형 타격",
            skillKind = SquirrelSkillKind.DRAGON_BREAKER,
            skillMpCost = 50,
            maxHp = 135,
            baseAtk = 33,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_a_1795421787418,
            columnIndex = 4,
            accentHex = 0xFFE11D48,
            shadowHex = 0xFFBE123C
        ),
        SquirrelJobHero(
            id = "hero_coco",
            nameKo = "코코",
            jobTitleKo = "구름 연금술사",
            badgeKo = "황금 플라스크",
            passiveKo = "승리 시 왕국 코인 +35% 보너스",
            skillNameKo = "황금 물약 폭발",
            skillDescKo = "공격력 +25% 버프 & 황금 타격",
            skillKind = SquirrelSkillKind.GOLDEN_BREW,
            skillMpCost = 50,
            maxHp = 120,
            baseAtk = 29,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_b_1795421786829,
            columnIndex = 0,
            accentHex = 0xFFEA580C,
            shadowHex = 0xFFC2410C
        ),
        SquirrelJobHero(
            id = "hero_harmony",
            nameKo = "하모니",
            jobTitleKo = "음유시인 바드",
            badgeKo = "작은 황금 하프",
            passiveKo = "회화·말하기 공격 데미지 +30%",
            skillNameKo = "에메랄드 합창",
            skillDescKo = "회화 필살기 + HP +20 회복",
            skillKind = SquirrelSkillKind.MELODY_BOOST,
            skillMpCost = 50,
            maxHp = 122,
            baseAtk = 29,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_b_1795421786829,
            columnIndex = 1,
            accentHex = 0xFF0D9488,
            shadowHex = 0xFF0F766E
        ),
        SquirrelJobHero(
            id = "hero_noah",
            nameKo = "노아",
            jobTitleKo = "시간술사 크로노",
            badgeKo = "별빛 모래시계",
            passiveKo = "단어 연상 힌트 항상 무료 공개",
            skillNameKo = "시간 정지",
            skillDescKo = "적 1턴 기절 + 정확한 어휘 일격",
            skillKind = SquirrelSkillKind.TIME_REVEAL,
            skillMpCost = 50,
            maxHp = 118,
            baseAtk = 30,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_b_1795421786829,
            columnIndex = 2,
            accentHex = 0xFF7C3AED,
            shadowHex = 0xFF5B21B6
        ),
        SquirrelJobHero(
            id = "hero_yuki",
            nameKo = "유키",
            jobTitleKo = "달빛 닌자",
            badgeKo = "벚꽃 두건",
            passiveKo = "5초 내 빠른 정답 시 2연타",
            skillNameKo = "그림자 벚꽃 베기",
            skillDescKo = "초고속 2연타 치명타 공격",
            skillKind = SquirrelSkillKind.SHADOW_DOUBLE,
            skillMpCost = 50,
            maxHp = 115,
            baseAtk = 35,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_b_1795421786829,
            columnIndex = 3,
            accentHex = 0xFF4F46E5,
            shadowHex = 0xFF3730A3
        ),
        SquirrelJobHero(
            id = "hero_mochi",
            nameKo = "모찌",
            jobTitleKo = "황금 왕실 요리사",
            badgeKo = "황금 요리모",
            passiveKo = "위기 시 1회 HP 40% 자동 부활",
            skillNameKo = "특제 도토리 만찬",
            skillDescKo = "HP +50 회복 + 공격력 +20% 버프",
            skillKind = SquirrelSkillKind.ROYAL_FEAST,
            skillMpCost = 50,
            maxHp = 135,
            baseAtk = 27,
            sheetRes = R.drawable.img_squirrel_heroes_sheet_b_1795421786829,
            columnIndex = 4,
            accentHex = 0xFFD97706,
            shadowHex = 0xFFB45309
        )
    )

    fun findHeroById(id: String): SquirrelJobHero =
        heroes.firstOrNull { it.id == id } ?: heroes.first()

    val chapters: List<EmeraldStoryChapter> = listOf(
        EmeraldStoryChapter(
            number = 1,
            id = "ch_1_whisper_meadow",
            titleKo = "Ch.1 속삭임의 초원",
            locationKo = "에메랄드 입구 숲",
            storyIntroKo = "왕국의 영단어 결정이 흩어졌어! 반짝이는 초원 다리 앞에서 장난꾸러기 단어 슬라임이 길을 막고 있어.",
            squirrelLineKo = "찍찍! 하얀 피그미 다람쥐 출동! 첫 모험을 어떻게 시작할까?",
            choices = listOf(
                RpgStoryChoice(
                    id = "c1_a",
                    labelKo = "빛나는 도토리 샘물 마시기",
                    tagKo = "HP+20 · MP+25",
                    outcomeKo = "에메랄드 샘물의 기운이 가득 찼어! 스킬 게이지와 체력이 충전된 채로 전투 시작!",
                    hpBonus = 20,
                    mpBonus = 25
                ),
                RpgStoryChoice(
                    id = "c1_b",
                    labelKo = "수풀 속 숨은 코인 주머니 탐색",
                    tagKo = "+60 코인 · 공격+15%",
                    outcomeKo = "반짝이는 왕국 코인 60개와 날카로운 바람 버프를 발견했어!",
                    bonusCoins = 60,
                    atkBuffPercent = 15
                )
            ),
            monster = RpgMonster(
                id = "m_slime",
                nameKo = "잉크 단어 슬라임",
                titleKo = "초원의 장난꾸러기",
                maxHp = 95,
                atk = 14,
                accentHex = 0xFF4F6EF7,
                intentLabelKo = "잉크 방울 튀기기 (14)",
                battleQuoteKo = "몰랑몰랑~ 단어 뜻 맞히면 비켜줄게!"
            ),
            rewardCoins = 120,
            rewardXp = 60,
            castleHpBonus = 8,
            accentHex = 0xFF4F6EF7
        ),
        EmeraldStoryChapter(
            number = 2,
            id = "ch_2_crystal_bridge",
            titleKo = "Ch.2 수정 호수 다리",
            locationKo = "에메랄드 수정 강",
            storyIntroKo = "수정 다리 문지기가 영어 암호를 요구하고 있어! 건너편 별빛 탑으로 가려면 다리를 통과해야 해.",
            squirrelLineKo = "물결이 반짝여! 문지기에게 어떤 방식으로 다가갈까?",
            choices = listOf(
                RpgStoryChoice(
                    id = "c2_a",
                    labelKo = "정중하게 영어로 길 물어보기",
                    tagKo = "방어막 + MP+30",
                    outcomeKo = "예의 바른 영어 인사에 수정 호수가 수호 방어막을 선물해 줬어!",
                    mpBonus = 30,
                    startShield = true
                ),
                RpgStoryChoice(
                    id = "c2_b",
                    labelKo = "빠르게 다리 난간 위로 점프 돌파",
                    tagKo = "공격+25% · +80 코인",
                    outcomeKo = "다람쥐 특유의 민첩함으로 선제 공격 기회를 잡았어!",
                    bonusCoins = 80,
                    atkBuffPercent = 25
                )
            ),
            monster = RpgMonster(
                id = "m_goblin",
                nameKo = "암호 문지기 고블린",
                titleKo = "수정 다리 파수꾼",
                maxHp = 125,
                atk = 18,
                accentHex = 0xFF2563EB,
                intentLabelKo = "문장 몽둥이 (18)",
                battleQuoteKo = "실전 회화 한 마디 못 하면 못 지나간다!"
            ),
            rewardCoins = 160,
            rewardXp = 85,
            castleHpBonus = 10,
            accentHex = 0xFF2563EB
        ),
        EmeraldStoryChapter(
            number = 3,
            id = "ch_3_starlight_library",
            titleKo = "Ch.3 별빛 도서관 탑",
            locationKo = "고대 어휘 서고",
            storyIntroKo = "수능 핵심 단어 책들이 공중에 떠다니는 별빛 도서관! 오래된 종이 골렘이 봉인된 책을 지키고 있어.",
            squirrelLineKo = "오래된 마법 책에서 반짝이는 글자가 떠올라! 먼저 뭘 살펴볼까?",
            choices = listOf(
                RpgStoryChoice(
                    id = "c3_a",
                    labelKo = "연상 기억의 수정구 들여다보기",
                    tagKo = "MP+40 · 방어막",
                    outcomeKo = "단어 연상 이미지가 머릿속에 선명해졌어! MP와 수호막이 가득 찼어!",
                    mpBonus = 40,
                    startShield = true
                ),
                RpgStoryChoice(
                    id = "c3_b",
                    labelKo = "황금 어휘 두루마리 펼치기",
                    tagKo = "공격+30% · +100 코인",
                    outcomeKo = "수능 빈출 주문이 깃들어 모든 공격력이 30% 상승했어!",
                    bonusCoins = 100,
                    atkBuffPercent = 30
                )
            ),
            monster = RpgMonster(
                id = "m_golem",
                nameKo = "고대 사전 골렘",
                titleKo = "별빛 서고 수호자",
                maxHp = 155,
                atk = 22,
                accentHex = 0xFF7C3AED,
                intentLabelKo = "두꺼운 사전 내려찍기 (22)",
                battleQuoteKo = "우르릉... 진짜 실력으로 단어의 뜻을 증명하라!"
            ),
            rewardCoins = 210,
            rewardXp = 110,
            castleHpBonus = 12,
            accentHex = 0xFF7C3AED
        ),
        EmeraldStoryChapter(
            number = 4,
            id = "ch_4_cloud_market",
            titleKo = "Ch.4 구름 시장의 비밀",
            locationKo = "하늘 상단 광장",
            storyIntroKo = "구름 위 마법 시장에서 괴도 까마귀가 왕국 초대장을 훔쳐 달아났어! 상인들과 영어로 대화해 단서를 찾아야 해.",
            squirrelLineKo = "시장에 맛있는 냄새랑 반짝이는 보물이 가득해! 어떻게 추적할까?",
            choices = listOf(
                RpgStoryChoice(
                    id = "c4_a",
                    labelKo = "카페 상인에게 영어로 행방 묻기",
                    tagKo = "+120 코인 · MP+35",
                    outcomeKo = "상인이 고마워하며 코인과 마력 차를 건네줬어!",
                    bonusCoins = 120,
                    mpBonus = 35
                ),
                RpgStoryChoice(
                    id = "c4_b",
                    labelKo = "지붕 위로 뛰어올라 기습하기",
                    tagKo = "공격+30% · HP+25",
                    outcomeKo = "구름 바람을 타고 완벽한 기습 위치를 선점했어!",
                    hpBonus = 25,
                    atkBuffPercent = 30
                )
            ),
            monster = RpgMonster(
                id = "m_phantom",
                nameKo = "구름 괴도 팬텀",
                titleKo = "하늘 시장 트릭스터",
                maxHp = 180,
                atk = 25,
                accentHex = 0xFFEA580C,
                intentLabelKo = "연막 속임수 공격 (25)",
                battleQuoteKo = "후후, 자연스러운 영어 회화로 날 막을 수 있을까?"
            ),
            rewardCoins = 260,
            rewardXp = 135,
            castleHpBonus = 14,
            accentHex = 0xFFEA580C
        ),
        EmeraldStoryChapter(
            number = 5,
            id = "ch_5_flame_gate",
            titleKo = "Ch.5 화염의 시험 성문",
            locationKo = "성채 외곽 용의 관문",
            storyIntroKo = "에메랄드 성채 바로 앞! 붉은 시험 드래곤이 불꽃을 뿜으며 마지막 관문을 지키고 있어.",
            squirrelLineKo = "뜨거운 불꽃이지만 무섭지 않아! 다람쥐 용사의 각오를 보여주자!",
            choices = listOf(
                RpgStoryChoice(
                    id = "c5_a",
                    labelKo = "에메랄드 방패 진형 세우기",
                    tagKo = "HP+35 · 방어막",
                    outcomeKo = "불꽃을 막아내는 찬란한 에메랄드 결계가 둘러졌어!",
                    hpBonus = 35,
                    startShield = true
                ),
                RpgStoryChoice(
                    id = "c5_b",
                    labelKo = "용의 약점 문장 먼저 외치기",
                    tagKo = "공격+35% · MP+30",
                    outcomeKo = "드래곤이 움찔했어! 공격력과 필살기 게이지가 폭발적으로 올랐어!",
                    mpBonus = 30,
                    atkBuffPercent = 35
                )
            ),
            monster = RpgMonster(
                id = "m_dragon",
                nameKo = "화염 모의고사 드래곤",
                titleKo = "중간 보스 · 관문 수호룡",
                maxHp = 220,
                atk = 28,
                accentHex = 0xFFE11D48,
                intentLabelKo = "화염 빈칸 브레스 (28)",
                battleQuoteKo = "크아아! 여기까지 온 꼬마 다람쥐, 실력을 보여라!",
                isBoss = true
            ),
            rewardCoins = 340,
            rewardXp = 180,
            castleHpBonus = 18,
            accentHex = 0xFFE11D48
        ),
        EmeraldStoryChapter(
            number = 6,
            id = "ch_6_emerald_throne",
            titleKo = "Ch.6 에메랄드 성채 결전",
            locationKo = "왕국 정상 크리스탈 홀",
            storyIntroKo = "드디어 성채 꼭대기! 왕국의 영어 결정을 봉인한 암흑 시험 마왕이 기다리고 있어. 지금껏 모은 어휘와 회화로 왕국을 되찾자!",
            squirrelLineKo = "지금까지 쌓아온 어휘와 회화 실력이 빛날 순간이야! 최종 결전 시작!",
            choices = listOf(
                RpgStoryChoice(
                    id = "c6_a",
                    labelKo = "10종 다람쥐 기사단의 응원 받기",
                    tagKo = "HP+45 · MP+50 · 방어막",
                    outcomeKo = "모든 피그미 다람쥐 동료들의 빛이 하나로 모였어! 첫 턴부터 필살기 발동 가능!",
                    hpBonus = 45,
                    mpBonus = 50,
                    startShield = true
                ),
                RpgStoryChoice(
                    id = "c6_b",
                    labelKo = "전설의 에메랄드 검 뽑아 들기",
                    tagKo = "공격+45% · +200 코인",
                    outcomeKo = "전설의 검이 빛나며 모든 단어·회화 공격이 초강력해졌어!",
                    bonusCoins = 200,
                    atkBuffPercent = 45
                )
            ),
            monster = RpgMonster(
                id = "m_dark_lord",
                nameKo = "암흑 시험 마왕",
                titleKo = "최종 보스 · 망각의 군주",
                maxHp = 280,
                atk = 32,
                accentHex = 0xFF9333EA,
                intentLabelKo = "망각의 폭풍 일격 (32)",
                battleQuoteKo = "하얀 피그미 다람쥐여... 너의 어휘와 회화로 날 이길 수 있겠느냐!",
                isBoss = true
            ),
            rewardCoins = 500,
            rewardXp = 260,
            castleHpBonus = 25,
            accentHex = 0xFF9333EA
        )
    )
}
