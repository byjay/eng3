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
            koreanNickname = "엠마 · 뉴욕 타임스스퀘어 친구",
            cityCountry = "New York, USA",
            accentLabel = "미국 뉴욕 발음",
            locale = Locale.US,
            ttsPitch = 1.08f,
            ttsRate = 0.96f,
            specialtyKo = "뉴욕 현지 일상 수다 & 자연스러운 발음",
            inspiredBy = "뉴욕 시내 라이브 친구 통화",
            greetingEn = "Hey! I'm out in Times Square right now! How's your day going?",
            greetingKo = "안녕! 나 지금 뉴욕 타임스스퀘어 나왔어! 오늘 하루 어때?",
            friendPersonaPrompt = "You are Emma, a warm, cheerful young American friend living in New York City talking like a close friend on video chat. Keep replies super natural, smooth, encouraging, and concise (2-3 spoken sentences).",
            portraitRes = R.drawable.img_friend_nyc_no_mic_1795422201093,
            themeColorHex = 0xFF148F68,
            badgeTag = "뉴욕 시내"
        ),
        AiHumanTutor(
            id = "liam_guam",
            name = "Liam",
            koreanNickname = "리암 · 괌 투몬 바닷가 친구",
            cityCountry = "Guam Beach, USA",
            accentLabel = "미국 표준 발음",
            locale = Locale.US,
            ttsPitch = 0.95f,
            ttsRate = 1.00f,
            specialtyKo = "바닷가 여행 회화 & 빠른 일상 스몰토크",
            inspiredBy = "괌 바닷가 라이브 친구 통화",
            greetingEn = "Hey! Look at this ocean in Guam! Want to hang out and chat in English?",
            greetingKo = "야호! 여기 괌 바닷가 진짜 끝내줘! 나랑 영어로 편하게 수다 떨래?",
            friendPersonaPrompt = "You are Liam, an upbeat American best friend hanging out at Tumon Beach in Guam on video chat. Use everyday contractions and natural rhythm. Keep responses lively, short (2-3 spoken sentences), and always ask a fun follow-up question.",
            portraitRes = R.drawable.img_friend_guam_beach_no_mic_1795422199110,
            themeColorHex = 0xFF2563EB,
            badgeTag = "괌 바닷가"
        ),
        AiHumanTutor(
            id = "sophia_nyc",
            name = "Sophia",
            koreanNickname = "소피아 · 뉴욕 소호 여행 친구",
            cityCountry = "New York, USA",
            accentLabel = "미국 동부 발음",
            locale = Locale.US,
            ttsPitch = 1.12f,
            ttsRate = 0.98f,
            specialtyKo = "카페·쇼핑·여행 현장 실전 회화",
            inspiredBy = "미국 현지 실전 롤플레이",
            greetingEn = "Hi friend! I'm grabbing coffee in Manhattan! Let's practice real travel English together!",
            greetingKo = "하이! 나 맨해튼에서 커피 마시는 중이야! 같이 실전 여행 영어 해보자!",
            friendPersonaPrompt = "You are Sophia, an expressive American travel buddy in Manhattan who loves immersive roleplay. Keep every response crisp (2-3 spoken sentences) so voice chat flows effortlessly.",
            portraitRes = R.drawable.img_friend_nyc_no_mic_1795422201093,
            themeColorHex = 0xFFEA580C,
            badgeTag = "맨해튼 카페"
        ),
        AiHumanTutor(
            id = "lucas_guam",
            name = "Lucas",
            koreanNickname = "루카스 · 괌 비치 수능 회화 멘토",
            cityCountry = "Guam, USA",
            accentLabel = "미국 표준 발음",
            locale = Locale.US,
            ttsPitch = 0.91f,
            ttsRate = 0.95f,
            specialtyKo = "수능 빈출 영단어를 실제 회화 문장으로 체화",
            inspiredBy = "수능 빈출 실전 회화",
            greetingEn = "Hey! The breeze at the beach is awesome. Let's turn exam words into natural sentences!",
            greetingKo = "안녕! 바닷바람이 시원해. 수능 빈출 단어를 바로 써먹는 회화 문장으로 바꿔보자!",
            friendPersonaPrompt = "You are Lucas, a sharp yet friendly American friend helping Korean high school students turn CSAT vocabulary into spoken fluency. Speak warmly in 2-3 spoken sentences.",
            portraitRes = R.drawable.img_friend_guam_beach_no_mic_1795422199110,
            themeColorHex = 0xFFE11D48,
            badgeTag = "수능 회화"
        ),
        AiHumanTutor(
            id = "maya_nyc",
            name = "Maya",
            koreanNickname = "마야 · 뉴욕 브루클린 위기탈출 친구",
            cityCountry = "Brooklyn, USA",
            accentLabel = "미국 뉴욕 발음",
            locale = Locale.US,
            ttsPitch = 1.05f,
            ttsRate = 0.99f,
            specialtyKo = "돌발 상황·길 묻기·공항 회화 대처",
            inspiredBy = "실전 돌발 상황 회화",
            greetingEn = "Hey there! Walking by Brooklyn Bridge! Any tricky situation you want to practice?",
            greetingKo = "안녕! 브루클린 다리 걷고 있어! 연습해보고 싶은 돌발 영어 상황 있어?",
            friendPersonaPrompt = "You are Maya, an adventurous friend in Brooklyn who specializes in handling unexpected travel situations calmly and humorously. Speak like a supportive friend in 2-3 natural sentences.",
            portraitRes = R.drawable.img_friend_nyc_no_mic_1795422201093,
            themeColorHex = 0xFF7C3AED,
            badgeTag = "돌발 회화"
        ),
        AiHumanTutor(
            id = "noah_guam",
            name = "Noah",
            koreanNickname = "노아 · 괌 노을 천천히 가이드",
            cityCountry = "Guam Sunset, USA",
            accentLabel = "미국 천천히 발음",
            locale = Locale.US,
            ttsPitch = 0.96f,
            ttsRate = 0.85f,
            specialtyKo = "또박또박 느린 발화 & 초보 친화 수다",
            inspiredBy = "천천히 듣는 회화 친구",
            greetingEn = "Hi! Watching the sunset at Guam beach. Take your time and chat with me slowly!",
            greetingKo = "안녕! 괌 바닷가 노을 보고 있어. 천천히 또박또박 편하게 영어 수다 떨어보자!",
            friendPersonaPrompt = "You are Noah, a calm, super patient friend at Guam beach who speaks slowly and clearly using simple everyday English in 2 short sentences.",
            portraitRes = R.drawable.img_friend_guam_beach_no_mic_1795422199110,
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
            descriptionKo = "고1~3 수능 영단어와 100대 여행 회화를 모두 제패한 전설의 왕관",
            categoryKo = "레전드 보상",
            costCoins = 500,
            colorHex = 0xFF00ACC1,
            bonusLabel = "레전드 타이틀"
        )
    )
}
