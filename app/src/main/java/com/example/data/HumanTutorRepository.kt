package com.example.data

import com.example.R
import com.example.model.AiHumanTutor
import com.example.model.CollectibleItem
import java.util.Locale

object HumanTutorRepository {

    val tutors: List<AiHumanTutor> = listOf(
        AiHumanTutor(
            id = "emma_nyc",
            name = "Emma",
            koreanNickname = "엠마 · 뉴욕 일상 AI 튜터",
            cityCountry = "New York, USA",
            accentLabel = "미국 뉴욕 발음",
            locale = Locale.US,
            ttsPitch = 1.08f,
            ttsRate = 0.96f,
            specialtyKo = "뉴욕 현지 일상 수다 & 자연스러운 발음 연습",
            inspiredBy = "뉴욕 시내 AI 회화 시뮬레이션",
            greetingEn = "Hey! I'm out in Times Square right now! How's your day going?",
            greetingKo = "안녕! 나 지금 뉴욕 타임스스퀘어 나왔어! 오늘 하루 어때?",
            friendPersonaPrompt = "You are Emma, a warm, cheerful young American friend living in New York City talking like a close friend on video chat. Keep replies super natural, smooth, encouraging, and concise (2-3 spoken sentences).",
            portraitRes = R.drawable.img_real_tutor_emma_1795478674413,
            themeColorHex = 0xFF4F6EF7,
            badgeTag = "뉴욕 시내"
        ),
        AiHumanTutor(
            id = "liam_guam",
            name = "Liam",
            koreanNickname = "리암 · 카페 일상 AI 튜터",
            cityCountry = "Manhattan Cafe, USA",
            accentLabel = "미국 표준 발음",
            locale = Locale.US,
            ttsPitch = 0.95f,
            ttsRate = 1.00f,
            specialtyKo = "카페·여행 실전 회화 & 빠른 일상 스몰토크",
            inspiredBy = "현지 카페 AI 회화 시뮬레이션",
            greetingEn = "Hey! I'm grabbing a fresh coffee right now. Want to practice real English with me?",
            greetingKo = "야호! 나 지금 카페에서 커피 한 잔 마시는 중이야. 영어로 편하게 수다 떨래?",
            friendPersonaPrompt = "You are Liam, an upbeat American best friend hanging out at a bright specialty coffee shop on video chat. Use everyday contractions and natural rhythm. Keep responses lively, short (2-3 spoken sentences), and always ask a fun follow-up question.",
            portraitRes = R.drawable.img_real_tutor_liam_clean_1795478701897,
            themeColorHex = 0xFF2563EB,
            badgeTag = "카페 수다"
        ),
        AiHumanTutor(
            id = "sophia_nyc",
            name = "Sophia",
            koreanNickname = "소피아 · 해변 여행 AI 튜터",
            cityCountry = "Gold Coast / Guam",
            accentLabel = "미국 동부 발음",
            locale = Locale.US,
            ttsPitch = 1.12f,
            ttsRate = 0.98f,
            specialtyKo = "해외여행·호텔·쇼핑 현장 실전 회화",
            inspiredBy = "해외 해변 AI 여행 시뮬레이션",
            greetingEn = "Hi friend! Look at this ocean sunset! Let's practice real travel English together!",
            greetingKo = "하이! 여기 바닷가 노을 진짜 예쁘지! 같이 실전 여행 영어 해보자!",
            friendPersonaPrompt = "You are Sophia, an expressive travel buddy walking along a sunny coastal boardwalk who loves immersive roleplay. Keep every response crisp (2-3 spoken sentences) so voice chat flows effortlessly.",
            portraitRes = R.drawable.img_real_tutor_sophia_clean_1795478702156,
            themeColorHex = 0xFFEA580C,
            badgeTag = "해변 여행"
        ),
        AiHumanTutor(
            id = "lucas_guam",
            name = "Lucas",
            koreanNickname = "루카스 · 핵심 어휘 AI 멘토",
            cityCountry = "Boston, USA",
            accentLabel = "미국 표준 발음",
            locale = Locale.US,
            ttsPitch = 0.91f,
            ttsRate = 0.95f,
            specialtyKo = "핵심 고등 영단어를 실제 회화 문장으로 체화",
            inspiredBy = "핵심 어휘 AI 회화 시뮬레이션",
            greetingEn = "Hey! Good to see you! Let's turn high school vocabulary into natural spoken sentences!",
            greetingKo = "안녕! 반가워! 고등 필수 단어를 바로 써먹는 회화 문장으로 바꿔보자!",
            friendPersonaPrompt = "You are Lucas, a sharp yet friendly American AI English tutor helping Korean high school students turn core vocabulary into spoken fluency. Speak warmly in 2-3 spoken sentences.",
            portraitRes = R.drawable.img_real_tutor_liam_clean_1795478701897,
            themeColorHex = 0xFFE11D48,
            badgeTag = "어휘 회화"
        ),
        AiHumanTutor(
            id = "maya_nyc",
            name = "Maya",
            koreanNickname = "마야 · 돌발 상황 AI 튜터",
            cityCountry = "Brooklyn, USA",
            accentLabel = "미국 뉴욕 발음",
            locale = Locale.US,
            ttsPitch = 1.05f,
            ttsRate = 0.99f,
            specialtyKo = "돌발 상황·길 묻기·공항 회화 대처",
            inspiredBy = "돌발 상황 AI 시뮬레이션",
            greetingEn = "Hey there! Walking around New York! Any tricky travel situation you want to practice?",
            greetingKo = "안녕! 뉴욕 거리 걷고 있어! 연습해보고 싶은 돌발 영어 상황 있어?",
            friendPersonaPrompt = "You are Maya, an adventurous AI English tutor in Brooklyn who specializes in handling unexpected travel situations calmly and humorously. Speak like a supportive tutor in 2-3 natural sentences.",
            portraitRes = R.drawable.img_real_tutor_emma_1795478674413,
            themeColorHex = 0xFF7C3AED,
            badgeTag = "돌발 회화"
        ),
        AiHumanTutor(
            id = "noah_guam",
            name = "Noah",
            koreanNickname = "노아 · 천천히 또박 AI 가이드",
            cityCountry = "California, USA",
            accentLabel = "미국 천천히 발음",
            locale = Locale.US,
            ttsPitch = 0.96f,
            ttsRate = 0.85f,
            specialtyKo = "또박또박 느린 발화 & 초보 친화 수다",
            inspiredBy = "천천히 듣는 AI 회화 연습",
            greetingEn = "Hi! Take your time and chat with me slowly. Every single sentence counts!",
            greetingKo = "안녕! 천천히 또박또박 편하게 영어 수다 떨어보자! 한 문장씩 해보면 돼!",
            friendPersonaPrompt = "You are Noah, a calm, super patient friend who speaks slowly and clearly using simple everyday English in 2 short sentences.",
            portraitRes = R.drawable.img_real_tutor_sophia_clean_1795478702156,
            themeColorHex = 0xFF0D9488,
            badgeTag = "천천히 회화"
        )
    )

    fun findById(id: String): AiHumanTutor = tutors.firstOrNull { it.id == id } ?: tutors.first()

    val collectibleShopItems: List<CollectibleItem> = listOf(
        CollectibleItem(
            id = "badge_starter",
            titleKo = "탐험가 윙 배지",
            descriptionKo = "SpeakQuest 3D 모험을 시작한 용감한 도전자에게 주어지는 기본 3D 엠블럼",
            categoryKo = "3D 배지",
            costCoins = 0,
            colorHex = 0xFF58CC02,
            bonusLabel = "기본 지급"
        ),
        CollectibleItem(
            id = "item_golden_mic",
            titleKo = "골든 스튜디오 마이크",
            descriptionKo = "원어민 발음 분석 90점 이상을 노리는 스피킹 장인의 황금 마이크 트로피",
            categoryKo = "수집 아이템",
            costCoins = 120,
            colorHex = 0xFFFFC800,
            bonusLabel = "발음 코치 장식"
        ),
        CollectibleItem(
            id = "item_passport_vip",
            titleKo = "VIP 골드 여권 케이스",
            descriptionKo = "100가지 해외여행 시나리오 정복자의 상징인 3D 럭셔리 패스포트",
            categoryKo = "여행 컬렉션",
            costCoins = 180,
            colorHex = 0xFF1CB0F6,
            bonusLabel = "여행 월드 장식"
        ),
        CollectibleItem(
            id = "item_sparta_shield",
            titleKo = "스파르타 불꽃 방패",
            descriptionKo = "매일 30단어 이상 오답 무한 반복을 버텨낸 강철 의지의 상징",
            categoryKo = "수능 트로피",
            costCoins = 220,
            colorHex = 0xFFFF4B4B,
            bonusLabel = "스파르타 아우라"
        ),
        CollectibleItem(
            id = "item_london_bus",
            titleKo = "미니어처 런던 2층 버스",
            descriptionKo = "엠마 선생님과 런던 발음 정복 기념 3D 수집 피규어",
            categoryKo = "도시 피규어",
            costCoins = 250,
            colorHex = 0xFFE53935,
            bonusLabel = "월드맵 장식"
        ),
        CollectibleItem(
            id = "item_ny_taxi",
            titleKo = "뉴욕 옐로우 캡 3D 모델",
            descriptionKo = "리암 선생님과 뉴욕 스피드 드릴을 돌파한 스페셜 3D 자동차",
            categoryKo = "도시 피규어",
            costCoins = 280,
            colorHex = 0xFFFFB300,
            bonusLabel = "월드맵 장식"
        ),
        CollectibleItem(
            id = "item_aviator_goggles",
            titleKo = "파일럿 고글 헬멧",
            descriptionKo = "국제선 비행과 입국 심사 100% 통과를 기념하는 3D 탐험 장비",
            categoryKo = "탐험 장비",
            costCoins = 320,
            colorHex = 0xFF8E24AA,
            bonusLabel = "프로필 프레임"
        ),
        CollectibleItem(
            id = "item_crystal_crown",
            titleKo = "다이아몬드 리그 왕관",
            descriptionKo = "고1~3 핵심 영단어 105개와 100대 여행 회화를 모두 제패한 전설의 왕관",
            categoryKo = "레전드 보상",
            costCoins = 500,
            colorHex = 0xFF00ACC1,
            bonusLabel = "레전드 타이틀"
        )
    )
}
