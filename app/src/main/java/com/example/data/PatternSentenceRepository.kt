package com.example.data

import com.example.model.PatternDrillItem
import com.example.model.PatternSlot

object PatternSentenceRepository {

    val drills: List<PatternDrillItem> = listOf(
        PatternDrillItem(
            id = "pat_intro_1",
            stageTagKo = "자기소개 · 1문장",
            sentenceCount = 1,
            titleKo = "자신감 넘치는 영어 자기소개 시작",
            templateEn = "Hi, I'm {name}, and I love {hobby} in my free time.",
            templateKo = "안녕, 나는 {name}이고, 여가 시간에 {hobby} 하는 걸 좋아해.",
            coachingTipKo = "I'm과 love에 강세를 주고, 'in my free time'을 한 덩어리(Chunk)로 부드럽게 이어 말하세요!",
            slots = listOf(
                PatternSlot(
                    "name", "이름/호칭",
                    listOf("Minji" to "민지", "Jiwon" to "지원", "Sujin" to "수진", "a high school student" to "고등학생")
                ),
                PatternSlot(
                    "hobby", "취미 바꾸기",
                    listOf(
                        "listening to music" to "음악 듣기",
                        "watching English dramas" to "영어 드라마 보기",
                        "traveling with my family" to "가족과 여행하기",
                        "baking sweet desserts" to "달콤한 디저트 굽기"
                    )
                )
            )
        ),
        PatternDrillItem(
            id = "pat_mart_1",
            stageTagKo = "1문장 집중 · 마트 계산",
            sentenceCount = 1,
            titleKo = "마트에서 이거 빼고 현금으로 결제하기",
            templateEn = "Could you take {item} off? I'll pay {method}.",
            templateKo = "{item} 빼주실 수 있나요? {method} 결제할게요.",
            coachingTipKo = "'take ~ off'는 계산대에서 물건을 뺄 때 원어민이 가장 많이 쓰는 실전 표현이에요!",
            slots = listOf(
                PatternSlot(
                    "item", "뺄 물건",
                    listOf(
                        "this one" to "이거",
                        "the milk" to "우유",
                        "these apples" to "이 사과들",
                        "the extra bag" to "추가 봉투"
                    )
                ),
                PatternSlot(
                    "method", "결제 수단",
                    listOf(
                        "in cash" to "현금으로",
                        "by credit card" to "신용카드로",
                        "with Apple Pay" to "애플페이로",
                        "half cash and half card" to "현금 반 카드 반으로"
                    )
                )
            )
        ),
        PatternDrillItem(
            id = "pat_cafe_1",
            stageTagKo = "1문장 집중 · 카페 주문",
            sentenceCount = 1,
            titleKo = "원어민처럼 카페에서 옵션 바꿔 주문하기",
            templateEn = "Can I get {drink} with {option}, please?",
            templateKo = "{option} 넣은 {drink} 한 잔 주시겠어요?",
            coachingTipKo = "'Can I get ~'은 'I want ~'보다 훨씬 자연스럽고 공손한 원어민 단골 패턴입니다.",
            slots = listOf(
                PatternSlot(
                    "drink", "음료 선택",
                    listOf(
                        "an iced latte" to "아이스 라떼",
                        "a vanilla milkshake" to "바닐라 밀크셰이크",
                        "a hot Americano" to "따뜻한 아메리카노",
                        "a strawberry smoothie" to "딸기 스무디"
                    )
                ),
                PatternSlot(
                    "option", "옵션 바꾸기",
                    listOf(
                        "less ice" to "얼음 적게",
                        "oat milk" to "귀리우유로",
                        "no whipped cream" to "휘핑크림 빼고",
                        "an extra shot" to "샷 추가해서"
                    )
                )
            )
        ),
        PatternDrillItem(
            id = "pat_intro_2",
            stageTagKo = "자기소개 · 2문장 연결",
            sentenceCount = 2,
            titleKo = "내 꿈과 오늘의 다짐 2문장으로 말하기",
            templateEn = "My biggest dream is to become {dream}. That's why I practice {habit} every single day.",
            templateKo = "내 가장 큰 꿈은 {dream} 되는 거야. 그래서 매일 {habit} 연습하고 있어.",
            coachingTipKo = "1문장 끝을 살짝 내리고 'That's why~'로 자연스럽게 이유를 연결하는 리듬을 익히세요!",
            slots = listOf(
                PatternSlot(
                    "dream", "꿈/목표",
                    listOf(
                        "a global designer" to "글로벌 디자이너가",
                        "a fluent English speaker" to "유창한 영어 화자가",
                        "a creative doctor" to "멋진 의사가",
                        "a world traveler" to "세계 여행가가"
                    )
                ),
                PatternSlot(
                    "habit", "매일 습관",
                    listOf(
                        "speaking English out loud" to "큰 소리로 영어 말하기를",
                        "thirty new vocabulary words" to "새 영단어 30개를",
                        "shadowing native speakers" to "원어민 섀도잉을",
                        "writing short English diaries" to "짧은 영어 일기 쓰기를"
                    )
                )
            )
        ),
        PatternDrillItem(
            id = "pat_travel_2",
            stageTagKo = "2문장 연결 · 해외 위기대처",
            sentenceCount = 2,
            titleKo = "정중하게 도움 요청하고 해결하기",
            templateEn = "Excuse me, I think there's a small problem with {issue}. Could you help me {action}?",
            templateKo = "실례합니다, {issue}에 작은 문제가 있는 것 같아요. {action} 도와주실 수 있나요?",
            coachingTipKo = "'a small problem'처럼 부드럽게 말하면 현지 직원이 훨씬 친절하게 도와줍니다!",
            slots = listOf(
                PatternSlot(
                    "issue", "문제 상황",
                    listOf(
                        "my receipt" to "제 영수증",
                        "my hotel room key" to "제 호텔 방 키",
                        "my flight seat" to "제 비행기 좌석",
                        "this food order" to "이 음식 주문"
                    )
                ),
                PatternSlot(
                    "action", "요청 행동",
                    listOf(
                        "double-check the price" to "가격을 다시 확인하도록",
                        "change it right away" to "바로 바꿔주시도록",
                        "find the right gate" to "맞는 탑승구를 찾도록",
                        "get a quick refund" to "빠르게 환불받도록"
                    )
                )
            )
        ),
        PatternDrillItem(
            id = "pat_story_3",
            stageTagKo = "3문장 실전 · 풀 자기소개 & 스토리",
            sentenceCount = 3,
            titleKo = "가족과 나의 목표를 3문장으로 유창하게 발표하기",
            templateEn = "Hello everyone, I'm excited to share my story today. I'm currently focusing on {focus} with my awesome family's support. Soon, I'll confidently {goal} anywhere in the world!",
            templateKo = "안녕하세요 여러분, 오늘 제 이야기를 나누게 되어 신나요. 저는 지금 멋진 가족의 응원 속에서 {focus}에 집중하고 있어요. 곧 전 세계 어디서나 자신 있게 {goal} 거예요!",
            coachingTipKo = "3문장은 속도보다 호흡! 각 문장 끝에서 0.5초 쉬고, excited / awesome / confidently 세 단어에 감정을 실어보세요.",
            slots = listOf(
                PatternSlot(
                    "focus", "집중 분야",
                    listOf(
                        "mastering high school English" to "고등학교 영어 마스터",
                        "speaking naturally like a native" to "원어민처럼 자연스럽게 말하기",
                        "preparing for my dream university" to "꿈의 대학 진학 준비",
                        "saving our family English kingdom" to "우리 가족 영어 왕국 구하기"
                    )
                ),
                PatternSlot(
                    "goal", "이룰 목표",
                    listOf(
                        "chat with friends in English" to "친구들과 영어로 수다 떨",
                        "travel around Europe solo" to "유럽을 자유롭게 여행할",
                        "give an amazing presentation" to "멋진 영어 발표를 해낼",
                        "ace every English exam" to "모든 영어 시험을 만점 받을"
                    )
                )
            )
        )
    )

    fun renderEnglish(item: PatternDrillItem, selectedIndices: Map<String, Int>): String {
        var result = item.templateEn
        item.slots.forEach { slot ->
            val idx = (selectedIndices[slot.slotKey] ?: 0).coerceIn(0, slot.options.lastIndex)
            result = result.replace("{${slot.slotKey}}", slot.options[idx].first)
        }
        return result
    }

    fun renderKorean(item: PatternDrillItem, selectedIndices: Map<String, Int>): String {
        var result = item.templateKo
        item.slots.forEach { slot ->
            val idx = (selectedIndices[slot.slotKey] ?: 0).coerceIn(0, slot.options.lastIndex)
            result = result.replace("{${slot.slotKey}}", slot.options[idx].second)
        }
        return result
    }
}
