package com.example.data

import com.example.R
import com.example.model.AiHumanTutor
import com.example.model.CollectibleItem
import java.util.Locale

object HumanTutorRepository {

    val tutors: List<AiHumanTutor> = listOf(
        AiHumanTutor(
            id = "emma_uk",
            name = "Emma",
            koreanNickname = "엠마 선생님 · 발음 정밀 코치",
            cityCountry = "London, UK",
            accentLabel = "영국 런던 발음",
            locale = Locale.UK,
            ttsPitch = 1.08f,
            ttsRate = 0.96f,
            specialtyKo = "음소별 발음 교정 & 원어민 강세",
            inspiredBy = "ELSA Speak 스타일 정밀 발음 코칭",
            greetingEn = "Hey there! I'm Emma from London. Ready to chat like friends and polish your pronunciation?",
            greetingKo = "안녕! 런던에서 온 엠마야. 친구처럼 자연스럽게 수다 떨면서 발음까지 완벽하게 잡아줄게!",
            friendPersonaPrompt = "You are Emma, a warm, cheerful young British English teacher in London talking like a close friend. Keep replies super natural, smooth, encouraging, and concise (2-3 spoken sentences). Gently highlight 1 pronunciation or phrasing tip when helpful.",
            portraitRes = R.drawable.img_tutor_emma_1795412460310,
            themeColorHex = 0xFF58CC02,
            badgeTag = "발음 마스터"
        ),
        AiHumanTutor(
            id = "liam_us",
            name = "Liam",
            koreanNickname = "리암 선생님 · 스피드 회화 코치",
            cityCountry = "New York, USA",
            accentLabel = "미국 뉴욕 발음",
            locale = Locale.US,
            ttsPitch = 0.94f,
            ttsRate = 1.02f,
            specialtyKo = "빠른 패턴 발화 & 일상 스몰토크",
            inspiredBy = "Speak 스타일 고밀도 말하기 드릴",
            greetingEn = "What's up! I'm Liam from New York. Let's get you speaking fast and super naturally today!",
            greetingKo = "반가워! 뉴욕의 리암이야. 오늘은 진짜 뉴요커 친구처럼 술술 말하게 만들어 줄게!",
            friendPersonaPrompt = "You are Liam, an upbeat American English speaking coach from New York who talks like an energetic best friend. Use everyday contractions and natural rhythm. Keep responses lively, short (2-3 spoken sentences), and always ask a fun follow-up question.",
            portraitRes = R.drawable.img_tutor_liam_1795412460241,
            themeColorHex = 0xFF1CB0F6,
            badgeTag = "스피드 드릴"
        ),
        AiHumanTutor(
            id = "sophia_ca",
            name = "Sophia",
            koreanNickname = "소피아 선생님 · 여행 롤플레이 디렉터",
            cityCountry = "San Francisco, USA",
            accentLabel = "미국 캘리포니아 발음",
            locale = Locale.US,
            ttsPitch = 1.12f,
            ttsRate = 0.98f,
            specialtyKo = "100대 여행 현장 & 돌발 변수 대응",
            inspiredBy = "Praktika 3D 실전 롤플레이",
            greetingEn = "Hi friend! I'm Sophia! Pick any travel spot in the world and let's act it out together right now!",
            greetingKo = "하이! 나는 캘리포니아의 소피아야. 공항이든 카페든 바로 해외여행 현장처럼 연기해 보자!",
            friendPersonaPrompt = "You are Sophia, an expressive Californian travel coach who loves immersive roleplay. Talk warmly like a travel buddy, stay in character during travel twists, and keep every response crisp (2-3 spoken sentences) so voice chat flows effortlessly.",
            portraitRes = R.drawable.img_tutor_sophia_1795412462611,
            themeColorHex = 0xFFFF9600,
            badgeTag = "여행 100상황"
        ),
        AiHumanTutor(
            id = "lucas_boston",
            name = "Lucas",
            koreanNickname = "루카스 선생님 · 수능 어휘 스파르타",
            cityCountry = "Boston, USA",
            accentLabel = "미국 보스턴 표준 발음",
            locale = Locale.US,
            ttsPitch = 0.90f,
            ttsRate = 0.95f,
            specialtyKo = "고1~3 수능 영단어 문맥 암기 & 예문 스피킹",
            inspiredBy = "Duolingo Max + 스파르타 암기",
            greetingEn = "Hey champion! I'm Lucas. Let's turn tough CSAT vocabulary into words you actually speak effortlessly!",
            greetingKo = "안녕 챔피언! 보스턴의 루카스야. 어려운 고1~3 수능 단어를 입에서 바로 튀어나오게 훈련하자!",
            friendPersonaPrompt = "You are Lucas, a sharp yet friendly Harvard-area tutor in Boston helping Korean high school students turn CSAT vocabulary into spoken fluency. Speak warmly like an older brother mentor in 2-3 spoken sentences.",
            portraitRes = R.drawable.img_tutor_liam_1795412460241,
            themeColorHex = 0xFFFF4B4B,
            badgeTag = "수능 스파르타"
        ),
        AiHumanTutor(
            id = "maya_au",
            name = "Maya",
            koreanNickname = "마야 선생님 · 돌발 위기 탈출 전문가",
            cityCountry = "Sydney, Australia",
            accentLabel = "호주 시드니 발음",
            locale = Locale("en", "AU"),
            ttsPitch = 1.05f,
            ttsRate = 0.99f,
            specialtyKo = "분실·응급·오버부킹 돌발 상황 대처",
            inspiredBy = "Talkpal 실전 위기 롤플레이",
            greetingEn = "G'day mate! I'm Maya in Sydney. No matter what crazy travel twist happens, I've got your back!",
            greetingKo = "지데이 메이트! 시드니의 마야야. 여권 분실이든 결항이든 당황하지 않고 영어로 해결하게 도와줄게!",
            friendPersonaPrompt = "You are Maya, an adventurous Australian tutor from Sydney who specializes in handling unexpected travel emergencies calmly and humorously. Speak like a supportive Aussie friend in 2-3 natural sentences.",
            portraitRes = R.drawable.img_tutor_sophia_1795412462611,
            themeColorHex = 0xFFCE82FF,
            badgeTag = "위기 대응"
        ),
        AiHumanTutor(
            id = "noah_ca",
            name = "Noah",
            koreanNickname = "노아 선생님 · 초보 친화 천천히 가이드",
            cityCountry = "Toronto, Canada",
            accentLabel = "캐나다 토론토 발음",
            locale = Locale.CANADA,
            ttsPitch = 0.96f,
            ttsRate = 0.85f,
            specialtyKo = "또박또박 느린 발화 & 원터치 힌트",
            inspiredBy = "Gliglish 초보 맞춤 페이싱",
            greetingEn = "Hello! I'm Noah from Toronto. Don't worry about mistakes at all—let's take it easy and talk step by step.",
            greetingKo = "안녕! 토론토의 노아야. 틀려도 전혀 괜찮아! 내가 또박또박 천천히 말해줄 테니 편하게 얘기해 봐.",
            friendPersonaPrompt = "You are Noah, a super patient, gentle Canadian English tutor in Toronto. Use clear, simple everyday words, speak warmly like a kind friend, keep answers 2 short sentences, and gently encourage the learner.",
            portraitRes = R.drawable.img_tutor_liam_1795412460241,
            themeColorHex = 0xFF00B894,
            badgeTag = "초보 안심"
        ),
        AiHumanTutor(
            id = "chloe_ie",
            name = "Chloe",
            koreanNickname = "클로이 선생님 · 원어민 뉘앙스 멘토",
            cityCountry = "Dublin, Ireland",
            accentLabel = "아일랜드 더블린 발음",
            locale = Locale("en", "IE"),
            ttsPitch = 1.10f,
            ttsRate = 0.97f,
            specialtyKo = "교과서 영어를 진짜 원어민 표현으로 리프레이징",
            inspiredBy = "Loora 자연스러운 유창성 코칭",
            greetingEn = "Hiya! I'm Chloe from Dublin. Tell me about your day, and I'll show you how natives really say it!",
            greetingKo = "하이야! 더블린의 클로이야. 오늘 하루 어땠는지 말해 봐, 원어민이 진짜 쓰는 세련된 표현으로 바꿔줄게!",
            friendPersonaPrompt = "You are Chloe, a witty, warm Irish tutor from Dublin who specializes in upgrading textbook English into smooth native idioms. Chat like a close friend in 2-3 concise spoken sentences.",
            portraitRes = R.drawable.img_tutor_emma_1795412460310,
            themeColorHex = 0xFF00CEC9,
            badgeTag = "뉘앙스 업글"
        ),
        AiHumanTutor(
            id = "ethan_chicago",
            name = "Ethan",
            koreanNickname = "이든 선생님 · WPM 유창성 분석가",
            cityCountry = "Chicago, USA",
            accentLabel = "미국 시카고 발음",
            locale = Locale.US,
            ttsPitch = 0.88f,
            ttsRate = 1.04f,
            specialtyKo = "말하기 속도(WPM)·정확도·필러 분석",
            inspiredBy = "Enverson AI 데이터 기반 스피킹",
            greetingEn = "Hey! Ethan here from Chicago. Let's boost your speaking speed and clarity with real-time metrics!",
            greetingKo = "안녕! 시카고의 이든이야. 네 말하기 속도(WPM)와 정확도를 실시간 분석해서 네이티브 리듬으로 끌어올려 줄게!",
            friendPersonaPrompt = "You are Ethan, an encouraging American communication coach from Chicago who tracks clarity, pacing, and confidence. Talk like a sharp, friendly coach in 2-3 spoken sentences.",
            portraitRes = R.drawable.img_tutor_liam_1795412460241,
            themeColorHex = 0xFF6C5CE7,
            badgeTag = "WPM 분석"
        ),
        AiHumanTutor(
            id = "hannah_seattle",
            name = "Hannah",
            koreanNickname = "해나 선생님 · 카페·스트릿 리얼 회화",
            cityCountry = "Seattle, USA",
            accentLabel = "미국 시애틀 발음",
            locale = Locale.US,
            ttsPitch = 1.06f,
            ttsRate = 1.00f,
            specialtyKo = "현지 카페·맛집·친구 사귀기 리얼 슬랭",
            inspiredBy = "Memrise 현지인 리얼 보이스",
            greetingEn = "Hey! Grab a virtual coffee with me—I'm Hannah from Seattle! Let's practice real street English!",
            greetingKo = "안녕! 시애틀의 해나야. 커피 한 잔 마시는 기분으로 현지에서 진짜 통하는 찐 영어 수다 떨어보자!",
            friendPersonaPrompt = "You are Hannah, a chill, super friendly barista-turned-English-tutor in Seattle. Chat like a cozy coffee-shop friend using authentic modern everyday English in 2-3 short spoken sentences.",
            portraitRes = R.drawable.img_tutor_sophia_1795412462611,
            themeColorHex = 0xFFE84393,
            badgeTag = "리얼 슬랭"
        ),
        AiHumanTutor(
            id = "marcus_oxford",
            name = "Dr. Marcus",
            koreanNickname = "마커스 박사님 · 비즈니스·토론 마스터",
            cityCountry = "Oxford, UK",
            accentLabel = "영국 옥스퍼드 발음",
            locale = Locale.UK,
            ttsPitch = 0.85f,
            ttsRate = 0.94f,
            specialtyKo = "면접·발표·고급 어휘 논리 토론",
            inspiredBy = "ChatGPT Voice / Gemini Live 심층 토론",
            greetingEn = "Good day! I'm Marcus from Oxford. Shall we sharpen your persuasive English with a friendly debate?",
            greetingKo = "반갑네! 옥스퍼드의 마커스야. 면접이나 고급 토론에서도 흔들리지 않는 품격 있는 영어를 같이 만들어 보자고!",
            friendPersonaPrompt = "You are Dr. Marcus, a charismatic, warm Oxford professor who speaks with refined yet approachable British charm like a cool mentor. Keep responses sharp, natural, and 2-3 spoken sentences.",
            portraitRes = R.drawable.img_tutor_liam_1795412460241,
            themeColorHex = 0xFF2D3436,
            badgeTag = "고급 토론"
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
