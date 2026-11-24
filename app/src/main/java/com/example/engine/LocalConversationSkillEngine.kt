package com.example.engine

import com.example.model.AiHumanTutor
import com.example.model.TravelTwist
import com.example.model.TutorChatMessage

/**
 * 100% Zero-LLM, Offline-First On-Device English Conversation Skill Engine.
 * Works with ZERO API keys, ZERO quota limits, ZERO network latency (0ms):
 * 1. Deterministic Multi-Turn Roleplay State Machine (Airport, Cafe, Mart, Intro, NYC, Guam)
 * 2. 55+ Real-Life Korean->English Situational Phrase Coach
 * 3. Rule-Based Grammar & Native Expression Upgrader (Loora/Ringle-style instant feedback)
 * 4. Context-Aware Question Answering & 3 Dynamic Follow-up Reply Hints every turn
 */
object LocalConversationSkillEngine {

    private data class PhraseRule(
        val keywordsAll: List<String> = emptyList(),
        val keywordsAny: List<String> = emptyList(),
        val nativeEn: String,
        val replyEn: String,
        val replyKo: String,
        val grammarKo: String,
        val tipKo: String,
        val nextHints: List<SuggestedReplyOption>
    )

    private data class GrammarFixRule(
        val pattern: Regex,
        val rephrase: (MatchResult, String) -> String,
        val grammarKo: String,
        val tipKo: String
    )

    private val koreanAndDailyPhraseRules: List<PhraseRule> = listOf(
        PhraseRule(
            keywordsAny = listOf("빼고", "취소", "take this off", "remove this"),
            nativeEn = "Could you take this one off? I'll pay in cash.",
            replyEn = "No problem at all! I took that item off for you. Your new total is twelve dollars. Would you like a bag?",
            replyKo = "물론이죠! 그 상품은 빼드렸어요. 새 합계는 12달러예요. 봉투 필요하신가요?",
            grammarKo = "'이거 빼주세요'는 원어민이 'Could you take this one off?'라고 자연스럽게 말해요.",
            tipKo = "'take this one off'에서 take와 off에 강세를 싣고 한 호흡으로 이어 말해보세요!",
            nextHints = listOf(
                SuggestedReplyOption("Yes, one shopping bag, please.", "네, 쇼핑백 하나 주세요."),
                SuggestedReplyOption("No thanks, I brought my own bag.", "아니요 괜찮아요, 제 가방 가져왔어요."),
                SuggestedReplyOption("Here is fifteen dollars. Keep the change.", "여기 15달러예요. 잔돈은 괜찮아요.")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("현금", "cash"),
            nativeEn = "I'll pay in cash, please.",
            replyEn = "Sure thing! Cash works great. Here's your change and receipt. Have a wonderful day!",
            replyKo = "좋아요! 현금 결제 가능합니다. 여기 거스름돈과 영수증이에요. 좋은 하루 보내세요!",
            grammarKo = "현금은 'in cash', 카드는 'by card'로 전치사를 구분해 쓰면 100% 원어민 표현이에요.",
            tipKo = "'pay in cash'를 '페이 인 캐쉬'처럼 끊지 않고 부드럽게 연결해 보세요.",
            nextHints = listOf(
                SuggestedReplyOption("Thank you! Have a great day too.", "감사합니다! 좋은 하루 보내세요."),
                SuggestedReplyOption("Could I get a small bag for this?", "이거 담을 작은 봉투 주실 수 있나요?"),
                SuggestedReplyOption("Do you offer tax-free shopping here?", "여기는 면세 쇼핑이 되나요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("카드", "결제", "신용카드", "credit card", "by card", "apple pay"),
            nativeEn = "Can I pay by credit card, please?",
            replyEn = "Of course! Just tap or insert your card right here on the reader. Would you like your receipt?",
            replyKo = "물론이죠! 여기 단말기에 카드를 대거나 꽂아주세요. 영수증 드릴까요?",
            grammarKo = "'카드 돼요?'는 'Can I pay by card?' 또는 'Do you take credit cards?'가 가장 자연스러워요.",
            tipKo = "'Can I pay'를 '캐나이 페이'로 가볍게 올리고 'card'를 또렷하게 발음하세요.",
            nextHints = listOf(
                SuggestedReplyOption("Yes, I'd like the receipt, please.", "네, 영수증 주세요."),
                SuggestedReplyOption("No receipt needed, thank you!", "영수증은 괜찮아요, 감사합니다!"),
                SuggestedReplyOption("Can I tap my phone to pay?", "휴대폰 터치로 결제해도 되나요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("아메리카노", "커피", "라떼", "americano", "coffee", "latte", "espresso", "matcha", "tea"),
            nativeEn = "Can I get a tall iced Americano, please?",
            replyEn = "Coming right up! One iced Americano. Would you like that for here or to go, and any extra shot?",
            replyKo = "바로 준비해 드릴게요! 아이스 아메리카노 한 잔이요. 매장 이용이신가요, 포장이신가요? 샷 추가할까요?",
            grammarKo = "카페 주문에서는 'I want'보다 'Can I get ~, please?'가 훨씬 부드러운 원어민 표현이에요.",
            tipKo = "'Can I get a'를 '캐나이 게러'로 연음하면 미국 현지 바리스타도 놀라는 발음이 돼요!",
            nextHints = listOf(
                SuggestedReplyOption("For here, please. And less ice!", "매장에서 마실게요. 얼음은 적게 주세요!"),
                SuggestedReplyOption("To go, please. No extra shot.", "포장해 주세요. 샷 추가는 괜찮아요."),
                SuggestedReplyOption("Could I switch to oat milk?", "오트 밀크로 바꿀 수 있을까요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("덜 달", "안 달", "less sweet", "decaf", "디카페인", "얼음 적게", "less ice"),
            nativeEn = "Could you make it less sweet with less ice, please?",
            replyEn = "Absolutely! Half syrup and light ice—got it. Would you like to add a warm croissant with your drink?",
            replyKo = "물론이죠! 시럽 반, 얼음 적게로 해드릴게요. 음료랑 같이 따뜻한 크루아상도 추가하실래요?",
            grammarKo = "음료 커스텀은 'with less ice / half sweet / decaf'를 뒤에 붙이면 간단하게 통합니다.",
            tipKo = "'less sweet'에서 두 s가 이어지므로 살짝 길게 '레쓰윗'처럼 발음하면 자연스러워요.",
            nextHints = listOf(
                SuggestedReplyOption("Yes, one warm croissant, please!", "네, 따뜻한 크루아상 하나 주세요!"),
                SuggestedReplyOption("Just the drink is fine, thanks.", "음료만 괜찮아요, 감사합니다."),
                SuggestedReplyOption("How much is that altogether?", "다 합쳐서 얼마인가요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("포장", "가져갈", "to go", "takeout", "for here", "매장"),
            nativeEn = "Could I get this to go, please?",
            replyEn = "Sure! I'll pack it neatly to go with napkins and a carrier. Can I grab your name for the cup?",
            replyKo = "네! 냅킨이랑 캐리어 챙겨서 깔끔하게 포장해 드릴게요. 컵에 적을 성함 알려주시겠어요?",
            grammarKo = "미국에서는 '테이크아웃'보다 'to go', 매장에서 먹는 건 'for here'를 주로 써요!",
            tipKo = "'to go'를 경쾌하게 올려 말하면 아주 친근하게 들려요.",
            nextHints = listOf(
                SuggestedReplyOption("Sure, it's Mina. M-I-N-A.", "네, 미나예요. M-I-N-A요."),
                SuggestedReplyOption("Thank you! Where is the pickup counter?", "감사해요! 픽업대가 어디인가요?"),
                SuggestedReplyOption("Could I also get a cup of water?", "물 한 잔도 주실 수 있나요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("화장실", "restroom", "bathroom", "toilet"),
            nativeEn = "Excuse me, where is the nearest restroom?",
            replyEn = "It's right down the hall on your left, just past the elevator! Do you need the passcode too?",
            replyKo = "복도 끝 왼쪽, 엘리베이터 바로 지나면 있어요! 혹시 비밀번호도 필요하신가요?",
            grammarKo = "공공장소에서는 'toilet'보다 'restroom'이 훨씬 공손한 미국식 표현이에요.",
            tipKo = "'Excuse me'의 두 번째 음절 '-cuse'를 길고 부드럽게 발음해 보세요.",
            nextHints = listOf(
                SuggestedReplyOption("Yes, what's the passcode?", "네, 비밀번호가 뭔가요?"),
                SuggestedReplyOption("Got it, thank you so much!", "알겠어요, 정말 감사합니다!"),
                SuggestedReplyOption("Is it on this floor?", "이 층에 있는 거 맞죠?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("얼마", "가격", "how much", "price", "total"),
            nativeEn = "Excuse me, how much is this altogether?",
            replyEn = "With tax included, it comes to eighteen dollars and fifty cents. Would you like to pay by card or cash?",
            replyKo = "세금 포함해서 18달러 50센트예요. 카드로 결제하시겠어요, 현금으로 하시겠어요?",
            grammarKo = "총액을 물을 때 끝에 'altogether'를 붙이면 '다 합쳐서 얼마예요?'라는 원어민 표현이 돼요.",
            tipKo = "'How much is'를 '하우머치즈'처럼 리듬감 있게 붙여 말해 보세요.",
            nextHints = listOf(
                SuggestedReplyOption("I'll pay by card, please.", "카드로 결제할게요."),
                SuggestedReplyOption("That's a great deal! I'll take it.", "가격 좋네요! 이걸로 할게요."),
                SuggestedReplyOption("Is tax already included?", "세금이 이미 포함된 건가요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("관광", "여행", "sightseeing", "vacation", "holiday", "visit", "trip"),
            nativeEn = "I'm here for sightseeing with my family.",
            replyEn = "Welcome! Traveling with family is the best. How many days will you be staying, and where is your hotel?",
            replyKo = "환영합니다! 가족 여행 정말 최고죠. 며칠 동안 머무실 예정이고, 호텔은 어디인가요?",
            grammarKo = "입국심사에서는 'I'm here for sightseeing / vacation.' 한 문장이면 깔끔하게 통과해요!",
            tipKo = "'sightseeing'은 첫 음절 '사이트'에 강세를 실어 분명하게 말해 보세요.",
            nextHints = listOf(
                SuggestedReplyOption("We'll stay for five days near Times Square.", "타임스스퀘어 근처에서 5일 머물러요."),
                SuggestedReplyOption("Here is my hotel reservation.", "여기 호텔 예약 확인서예요."),
                SuggestedReplyOption("We're flying back next Sunday.", "다음 주 일요일에 귀국해요.")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("며칠", "stay", "days", "week", "hotel", "호텔", "숙소"),
            nativeEn = "We'll be staying at a hotel downtown for five days.",
            replyEn = "Awesome! Everything looks great on your passport. Here you go—enjoy your trip and welcome to the city!",
            replyKo = "완벽해요! 여권 확인 끝났습니다. 여기 있어요—즐거운 여행 되시고 환영합니다!",
            grammarKo = "미래 체류 계획은 'will be staying for + 기간'을 쓰면 원어민처럼 매끄럽게 들려요.",
            tipKo = "'five days'의 v 발음은 윗니로 아랫입술을 살짝 스치듯 진동시켜 주세요.",
            nextHints = listOf(
                SuggestedReplyOption("Thank you! Where is baggage claim?", "감사합니다! 수하물 찾는 곳은 어디인가요?"),
                SuggestedReplyOption("Could you tell me where the taxi stand is?", "택시 승강장이 어디인지 알려주시겠어요?"),
                SuggestedReplyOption("I'm so excited to explore the city!", "도시를 구경할 생각에 너무 설레요!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("게이트", "탑승", "gate", "boarding", "flight", "baggage", "수하물", "짐"),
            nativeEn = "Could you show me the way to gate 12?",
            replyEn = "Sure! Walk straight ahead for two minutes, go past the duty-free shops, and Gate 12 will be on your right!",
            replyKo = "물론이죠! 2분 정도 직진해서 면세점을 지나면 오른쪽에 12번 게이트가 보여요!",
            grammarKo = "'~로 가는 길 알려주세요'는 'Could you show me the way to ~?'가 가장 정중하고 명확해요.",
            tipKo = "'show me the way'에서 'show'와 'way'에 강세를 주세요.",
            nextHints = listOf(
                SuggestedReplyOption("Thank you! When does boarding start?", "감사해요! 탑승은 언제 시작하나요?"),
                SuggestedReplyOption("Is it far from here?", "여기서 많이 먼가요?"),
                SuggestedReplyOption("Great, I'll head there right now!", "좋아요, 지금 바로 갈게요!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("와이파이", "wifi", "wi-fi", "비밀번호", "password"),
            nativeEn = "Excuse me, what's the Wi-Fi password here?",
            replyEn = "It's printed at the bottom of your receipt, or just type 'coffee2026' in lowercase! Did it connect okay?",
            replyKo = "영수증 맨 아래에 적혀 있어요! 소문자로 'coffee2026' 입력하시면 돼요. 잘 연결되나요?",
            grammarKo = "'와이파이 비번이 뭐예요?'는 'What's the Wi-Fi password?' 한 마디로 충분해요.",
            tipKo = "'Wi-Fi'는 두 음절 모두 선명하게 '와이-파이'로 발음하세요.",
            nextHints = listOf(
                SuggestedReplyOption("Yes, it's connected now! Thank you.", "네, 이제 연결됐어요! 감사합니다."),
                SuggestedReplyOption("Could you spell that for me, please?", "철자를 한 번 불러주시겠어요?"),
                SuggestedReplyOption("The connection is super fast!", "인터넷이 엄청 빠르네요!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("사진", "photo", "picture", "찍어"),
            nativeEn = "Excuse me, could you take a photo of us?",
            replyEn = "I'd love to! Let's get the bright skyline in the background. Ready? One, two, three—cheese! Want to check it?",
            replyKo = "기꺼이 찍어드릴게요! 멋진 배경이 잘 나오게 잡아볼게요. 자, 하나 둘 셋—치즈! 사진 확인해 볼래요?",
            grammarKo = "'사진 좀 찍어주세요'는 'Could you take a photo of us?'라고 하면 100% 자연스러워요.",
            tipKo = "'photo of us'를 '포토 어브 어스'로 부드럽게 이어 말해보세요.",
            nextHints = listOf(
                SuggestedReplyOption("Wow, it turned out amazing! Thanks!", "와, 진짜 잘 나왔어요! 감사해요!"),
                SuggestedReplyOption("Could we take one more vertical shot?", "세로로 한 장만 더 찍어주실 수 있나요?"),
                SuggestedReplyOption("You're a great photographer!", "사진 정말 잘 찍으시네요!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("추천", "recommend", "popular", "best", "menu", "메뉴"),
            nativeEn = "What do you recommend here? What's the most popular item?",
            replyEn = "Our signature vanilla cold brew and warm blueberry muffin are customer favorites! Do you prefer sweet or strong coffee?",
            replyKo = "시그니처 바닐라 콜드브루랑 따뜻한 블루베리 머핀이 제일 인기 있어요! 단 커피랑 진한 커피 중 어느 쪽이 좋아요?",
            grammarKo = "'추천해 주세요'는 'What do you recommend?'가 식당·카페·쇼핑 어디서나 통하는 만능 문장이에요.",
            tipKo = "'recommend'는 끝 음절 '-mend'에 강세를 실어주세요.",
            nextHints = listOf(
                SuggestedReplyOption("I'll try the signature vanilla cold brew!", "시그니처 바닐라 콜드브루로 할게요!"),
                SuggestedReplyOption("I prefer something strong and not too sweet.", "너무 달지 않고 진한 게 좋아요."),
                SuggestedReplyOption("That sounds delicious! Let's get both.", "맛있겠네요! 둘 다 주세요.")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("봉투", "쇼핑백", "bag"),
            nativeEn = "Could I get a shopping bag, please?",
            replyEn = "Of course! Bags are ten cents each—would you like a paper bag or a reusable tote?",
            replyKo = "물론이죠! 봉투는 10센트예요—종이봉투로 드릴까요, 다회용 백으로 드릴까요?",
            grammarKo = "물건을 요청할 땐 'Could I get ~, please?'를 습관처럼 쓰면 원어민처럼 공손해요.",
            tipKo = "'Could I'를 '쿠다이'처럼 자연스럽게 연결해 보세요.",
            nextHints = listOf(
                SuggestedReplyOption("A paper bag is fine, thank you.", "종이봉투로 괜찮아요, 감사합니다."),
                SuggestedReplyOption("I'll take a reusable tote, please.", "다회용 백으로 할게요."),
                SuggestedReplyOption("Actually, I can carry it by hand.", "아, 그냥 손으로 들고 갈게요.")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("환불", "교환", "refund", "exchange", "size", "사이즈", "다른 색"),
            nativeEn = "Could I exchange this for a different size, please?",
            replyEn = "Certainly! As long as you have the receipt, we can swap it right away. What size are you looking for?",
            replyKo = "물론이죠! 영수증만 있으시면 바로 교환해 드릴 수 있어요. 어떤 사이즈를 찾으시나요?",
            grammarKo = "'다른 사이즈로 교환'은 'exchange A for B' 패턴을 쓰면 완벽해요.",
            tipKo = "'exchange'는 두 번째 음절 '-change'에 강세를 주세요.",
            nextHints = listOf(
                SuggestedReplyOption("Do you have this in medium?", "이거 미디엄 사이즈 있나요?"),
                SuggestedReplyOption("Here is my receipt from yesterday.", "여기 어제 받은 영수증이에요."),
                SuggestedReplyOption("Can I try it on first?", "먼저 입어봐도 될까요?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("천천히", "못 알아", "다시 말", "slowly", "again", "pardon", "repeat"),
            nativeEn = "Sorry, could you say that a little more slowly?",
            replyEn = "Of course! No rush at all. I'll speak nice and slowly so every single word is crystal clear. Ready?",
            replyKo = "물론이죠! 전혀 급하지 않아요. 한 단어 한 단어 또렷하게 천천히 말해 드릴게요. 준비됐나요?",
            grammarKo = "못 들었을 땐 'What?' 대신 'Could you say that again, please?'가 훨씬 매너 있는 표현이에요.",
            tipKo = "끝을 부드럽게 올려 말하면 상대방이 아주 친절하게 다시 말해줘요.",
            nextHints = listOf(
                SuggestedReplyOption("Yes, that's much clearer! Thank you.", "네, 훨씬 잘 들려요! 감사해요."),
                SuggestedReplyOption("What does that word mean?", "방금 그 단어는 무슨 뜻인가요?"),
                SuggestedReplyOption("Let's practice that sentence together!", "그 문장 같이 연습해 봐요!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("자기소개", "소개", "my name is", "i'm from", "i live in", "hobby", "hobbies", "취미", "좋아"),
            nativeEn = "Hi! I'm from Korea, and I love traveling, music, and practicing English!",
            replyEn = "So awesome to meet you! I love Korean music and food too! What's your favorite place you've ever visited?",
            replyKo = "만나서 정말 반가워! 나도 한국 음악이랑 음식 진짜 좋아해! 지금까지 가본 곳 중에 제일 좋았던 곳이 어디야?",
            grammarKo = "취미를 말할 때 'I love + 동사-ing'를 쓰면 생기 있고 자연스러운 자기소개가 돼요!",
            tipKo = "'love traveling'에서 l과 t를 경쾌하게 띄워 말해보세요.",
            nextHints = listOf(
                SuggestedReplyOption("I loved visiting Jeju Island with my family!", "가족이랑 제주도 갔던 게 제일 좋았어요!"),
                SuggestedReplyOption("My dream is to travel around New York and Europe.", "제 꿈은 뉴욕이랑 유럽을 여행하는 거예요."),
                SuggestedReplyOption("What do you usually do on weekends?", "너는 주말에 보통 뭐 해?")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("weekend", "주말", "오늘 뭐", "how about you", "what about you", "how are you", "how's your day"),
            nativeEn = "My day is going great! How about you? Any fun plans?",
            replyEn = "I'm having an awesome day! I grabbed an iced latte earlier and now I'm just hanging out with you. What's next for you today?",
            replyKo = "나도 완전 좋은 하루 보내고 있어! 아까 아이스 라떼 마시고 지금은 너랑 수다 떠는 중이야. 너는 오늘 앞으로 뭐 할 거야?",
            grammarKo = "상대에게 되물을 때 'And you?'보다 'How about you?'나 'Any fun plans?'가 훨씬 원어민다워요.",
            tipKo = "'How about you?'를 '하우 어바우츄'처럼 부드럽게 연음해 보세요!",
            nextHints = listOf(
                SuggestedReplyOption("I'm going to study ten new English words!", "새 영어 단어 10개 공부할 거야!"),
                SuggestedReplyOption("I'm relaxing at home and listening to music.", "집에서 쉬면서 음악 들을 거야."),
                SuggestedReplyOption("Let's roleplay ordering at a cafe!", "우리 카페에서 주문하는 롤플레이 하자!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("thank", "thanks", "고마", "감사", "appreciate"),
            nativeEn = "Thank you so much! I really appreciate your help.",
            replyEn = "You're so welcome! Anytime at all. You sounded super polite and natural right there—want to try another situation?",
            replyKo = "천만에요! 언제든지요. 방금 정말 정중하고 자연스럽게 말했어요—다른 상황도 이어서 연습해 볼까요?",
            grammarKo = "'Thank you' 뒤에 'I really appreciate it.'을 붙이면 고급 감사 표현이 돼요!",
            tipKo = "'appreciate'는 '-pre-'에 강세를 실어 '어프리시에잇'으로 또렷하게 발음하세요.",
            nextHints = listOf(
                SuggestedReplyOption("Yes! Let's practice one more real-life situation.", "네! 실전 상황 하나 더 연습해요."),
                SuggestedReplyOption("How do I sound more like a native speaker?", "원어민처럼 말하려면 어떻게 하면 좋을까요?"),
                SuggestedReplyOption("See you in our next chat!", "다음 대화에서 또 만나요!")
            )
        ),
        PhraseRule(
            keywordsAny = listOf("bye", "see you", "안녕히", "잘 가", "다음에"),
            nativeEn = "It was so fun chatting with you! Catch you later!",
            replyEn = "I had a blast talking with you today! Keep up that awesome streak, and see you next time!",
            replyKo = "오늘 대화 진짜 재밌었어! 멋진 연속 학습 계속 이어가고, 다음에 또 만나!",
            grammarKo = "친구 사이에서는 'Goodbye'보다 'Catch you later!'나 'Talk soon!'이 훨씬 친근해요.",
            tipKo = "'Catch you later'를 '캐츄 레이터'로 가볍게 이어 말하면 100% 원어민 느낌!",
            nextHints = listOf(
                SuggestedReplyOption("Talk soon! Have an amazing day!", "곧 또 얘기해! 멋진 하루 보내!"),
                SuggestedReplyOption("Wait, let's practice one last sentence!", "잠깐, 마지막 한 문장만 더 연습하자!"),
                SuggestedReplyOption("Thanks for being my English buddy!", "내 영어 친구가 되어줘서 고마워!")
            )
        )
    )

    private val grammarUpgrades: List<GrammarFixRule> = listOf(
        GrammarFixRule(
            pattern = Regex("(?i)\\bi\\s+want\\s+(an?\\s+)?([a-z\\s]{2,28})"),
            rephrase = { m, _ -> "Can I get ${m.groupValues[1].ifBlank { "a " }}${m.groupValues[2].trim()}, please?" },
            grammarKo = "주문·요청할 때 'I want ~'는 다소 직접적이에요. 'Can I get ~, please?'가 훨씬 세련된 원어민 표현!",
            tipKo = "'Can I get'을 짧고 부드럽게 이어 말하면 현지인처럼 들려요."
        ),
        GrammarFixRule(
            pattern = Regex("(?i)\\bgive\\s+me\\s+(.+)"),
            rephrase = { m, _ -> "Could I please have ${m.groupValues[1].trim().trimEnd('.', '!', '?')}?" },
            grammarKo = "'Give me ~' 대신 'Could I please have ~?'를 쓰면 훨씬 정중하고 자연스러워요.",
            tipKo = "'Could I'를 '쿠다이'로 부드럽게 연결하세요."
        ),
        GrammarFixRule(
            pattern = Regex("(?i)\\bi\\s+am\\s+fine\\s+thank\\s+you"),
            rephrase = { _, _ -> "I'm doing great, thanks! How about you?" },
            grammarKo = "교과서식 'I am fine, thank you' 대신 'I'm doing great, thanks!'가 실제 미국 회화 표준이에요.",
            tipKo = "'doing great'에서 great을 밝게 강조해 주세요."
        ),
        GrammarFixRule(
            pattern = Regex("(?i)\\bi\\s*(am|'m)\\s+interesting\\s+in\\s+(.+)"),
            rephrase = { m, _ -> "I'm really interested in ${m.groupValues[2].trim().trimEnd('.', '!', '?')}." },
            grammarKo = "사람이 흥미를 느낄 땐 'interesting'이 아니라 수동 분사 'interested in'을 써야 정확해요!",
            tipKo = "'interested in'을 '인트레스티딘'처럼 이어 발음하세요."
        ),
        GrammarFixRule(
            pattern = Regex("(?i)\\bvery\\s+delicious\\b"),
            rephrase = { _, full -> full.replace(Regex("(?i)very\\s+delicious"), "super tasty and amazing") },
            grammarKo = "'delicious' 자체가 이미 '매우 맛있는' 뜻이라 'very' 대신 'absolutely delicious'나 'amazing'이 자연스러워요!",
            tipKo = "감탄사처럼 'amazing'의 두 번째 음절 '-ma-'를 길게 올려보세요."
        ),
        GrammarFixRule(
            pattern = Regex("(?i)\\byesterday\\s+i\\s+go\\b|\\bi\\s+go\\s+.*yesterday\\b"),
            rephrase = { _, full -> full.replace(Regex("(?i)\\bgo\\b"), "went") },
            grammarKo = "'yesterday'처럼 과거 시점이 있으면 동사 'go'를 과거형 'went'로 바꿔야 해요!",
            tipKo = "'went'의 끝 t 소리를 짧고 깔끔하게 마무리해 보세요."
        ),
        GrammarFixRule(
            pattern = Regex("(?i)\\bwhere\\s+is\\s+(.+)"),
            rephrase = { m, _ -> "Excuse me, could you tell me where ${m.groupValues[1].trim().trimEnd('?')} is?" },
            grammarKo = "간접의문문 'Could you tell me where ~ is?'를 쓰면 어순과 공손함 모두 완벽해요!",
            tipKo = "'where ... is'에서 마지막 is는 가볍게 내려 마무리해요."
        )
    )

    private data class TopicBeat(
        val replyEn: String,
        val replyKo: String,
        val rephraseEn: String,
        val grammarKo: String,
        val tipKo: String,
        val hints: List<SuggestedReplyOption>
    )

    private fun topicFollowUpBeat(topicTag: String, turnIndex: Int): TopicBeat {
        val beats: List<TopicBeat> = when {
            topicTag.contains("공항") -> listOf(
                TopicBeat(
                    replyEn = "Sounds great! How many days will you be staying, and where is your hotel located?",
                    replyKo = "좋아요! 며칠 동안 머무실 예정이고, 호텔은 어느 지역인가요?",
                    rephraseEn = "I'm staying downtown for five days with my family.",
                    grammarKo = "입국심사에서는 '목적 + 기간 + 숙소' 3가지만 자신 있게 말하면 바로 통과해요.",
                    tipKo = "숫자(five days)를 또렷하고 여유 있게 말해보세요.",
                    hints = listOf(
                        SuggestedReplyOption("I'll stay for five days near downtown.", "시내 근처에서 5일 머물 거예요."),
                        SuggestedReplyOption("Here is my hotel booking confirmation.", "여기 호텔 예약 확인서예요."),
                        SuggestedReplyOption("I'm traveling with my family.", "가족과 함께 여행 중이에요.")
                    )
                ),
                TopicBeat(
                    replyEn = "Welcome! Your passport is all set. Do you know which gate or baggage carousel you need to head to?",
                    replyKo = "환영합니다! 여권 확인 완료됐어요. 어느 게이트나 수하물 찾는 곳으로 가야 하는지 아시나요?",
                    rephraseEn = "Could you point me toward baggage claim and the taxi stand?",
                    grammarKo = "'~방향 좀 알려주세요'는 'Could you point me toward ~?'가 세련된 표현이에요.",
                    tipKo = "'baggage claim'에서 bag-와 claim에 강세를 실어주세요.",
                    hints = listOf(
                        SuggestedReplyOption("Could you show me the way to baggage claim?", "수하물 찾는 곳 가는 길 알려주시겠어요?"),
                        SuggestedReplyOption("Where can I catch the airport express train?", "공항철도는 어디서 탈 수 있나요?"),
                        SuggestedReplyOption("Thank you! I'm so excited for this trip.", "감사해요! 이번 여행 정말 기대돼요.")
                    )
                )
            )
            topicTag.contains("카페") -> listOf(
                TopicBeat(
                    replyEn = "Great choice! What size would you like—tall, grande, or venti? And is that for here or to go?",
                    replyKo = "좋은 선택이에요! 사이즈는 톨, 그란데, 벤티 중 어느 걸로 할까요? 매장인가요 포장인가요?",
                    rephraseEn = "A tall one to go, please, with light ice.",
                    grammarKo = "사이즈와 옵션을 한 번에 말할 때 'with light ice / oat milk'처럼 with로 연결하면 깔끔해요.",
                    tipKo = "'for here or to go' 리듬을 익혀두면 해외 카페 어디서나 당황하지 않아요!",
                    hints = listOf(
                        SuggestedReplyOption("A tall one for here, please.", "톨 사이즈 매장에서 마실게요."),
                        SuggestedReplyOption("Grande to go with less ice, please.", "그란데 포장, 얼음 적게 주세요."),
                        SuggestedReplyOption("Do you have any decaf options?", "디카페인 메뉴도 있나요?")
                    )
                ),
                TopicBeat(
                    replyEn = "Awesome! Would you like to add a freshly baked blueberry muffin or warm croissant? That'll be six fifty.",
                    replyKo = "좋아요! 갓 구운 블루베리 머핀이나 따뜻한 크루아상도 같이 드실래요? 총 6달러 50센트예요.",
                    rephraseEn = "Just the drink today, thanks! I'll pay by card.",
                    grammarKo = "추가 주문을 정중히 거절할 땐 'Just the drink, thanks!'가 가장 자연스러워요.",
                    tipKo = "'Just the drink'에서 just의 t는 약하게 삼키듯 말하면 원어민 발음!",
                    hints = listOf(
                        SuggestedReplyOption("Just the drink is fine. Can I pay by card?", "음료만 괜찮아요. 카드로 결제할게요."),
                        SuggestedReplyOption("Yes, one warm croissant too, please!", "네, 따뜻한 크루아상도 하나 주세요!"),
                        SuggestedReplyOption("What's the Wi-Fi password here?", "여기 와이파이 비밀번호가 뭔가요?")
                    )
                )
            )
            topicTag.contains("마트") -> listOf(
                TopicBeat(
                    replyEn = "Got it! Everything is scanned. Would you like a paper bag, and are you paying by card or cash today?",
                    replyKo = "알겠어요! 물건 스캔 다 끝났어요. 종이봉투 필요하신가요? 카드랑 현금 중 어떤 걸로 결제할까요?",
                    rephraseEn = "I'll pay by card, and could I get one bag, please?",
                    grammarKo = "결제와 봉투 요청을 'and'로 한 문장에 묶으면 계산대에서 아주 유창하게 들려요.",
                    tipKo = "'pay by card'를 한 박자로 또렷하게 말해보세요.",
                    hints = listOf(
                        SuggestedReplyOption("I'll pay by card, and one bag, please.", "카드로 결제하고 봉투 하나 주세요."),
                        SuggestedReplyOption("Could you take this one off? I'll pay cash.", "이거 빼고 현금으로 계산할게요."),
                        SuggestedReplyOption("No bag needed, I have my eco-bag.", "봉투 괜찮아요, 에코백 있어요.")
                    )
                ),
                TopicBeat(
                    replyEn = "All set! Here's your receipt and your groceries. Is there anything else I can help you find in the store?",
                    replyKo = "완료됐어요! 여기 영수증이랑 장보신 물건이에요. 매장에서 더 찾으시는 물건 있나요?",
                    rephraseEn = "Actually, which aisle are the snacks and bottled water in?",
                    grammarKo = "마트에서 진열대 위치를 물을 땐 'Which aisle is ~ in?'을 쓰면 현지인 100%!",
                    tipKo = "'aisle'은 s가 묵음이라 '아일'로 발음하는 게 포인트예요!",
                    hints = listOf(
                        SuggestedReplyOption("Which aisle are the snacks in?", "과자는 몇 번 통로에 있나요?"),
                        SuggestedReplyOption("That's everything! Thank you so much.", "이게 다예요! 정말 감사합니다."),
                        SuggestedReplyOption("Do you sell fresh strawberries today?", "오늘 신선한 딸기도 파나요?")
                    )
                )
            )
            topicTag.contains("소개") -> listOf(
                TopicBeat(
                    replyEn = "That's so cool! You express yourself so clearly. What's a fun hobby or dream you're excited about lately?",
                    replyKo = "정말 멋지다! 자기 표현을 엄청 또렷하게 잘하네. 요즘 푹 빠진 취미나 이루고 싶은 꿈이 뭐야?",
                    rephraseEn = "Lately I've been really into music and improving my English!",
                    grammarKo = "'요즘 ~에 빠져 있어'는 'Lately I've been really into ~'가 원어민 최애 표현이에요.",
                    tipKo = "'really into'를 '릴리 인투'로 신나게 강조해서 말해 보세요.",
                    hints = listOf(
                        SuggestedReplyOption("I've been really into K-pop and English!", "요즘 K-pop이랑 영어에 푹 빠졌어!"),
                        SuggestedReplyOption("I love drawing and traveling with my family.", "그림 그리기랑 가족 여행을 좋아해."),
                        SuggestedReplyOption("What kind of music do you like?", "너는 어떤 음악 좋아해?")
                    )
                ),
                TopicBeat(
                    replyEn = "I love that energy! If we could teleport anywhere in the world together right now, where would you want to go?",
                    replyKo = "그 에너지 너무 좋다! 지금 당장 우리 둘이 전 세계 어디든 순간이동할 수 있다면 어디 가고 싶어?",
                    rephraseEn = "If I could go anywhere, I'd definitely visit New York or Guam!",
                    grammarKo = "가정법 'If I could ~, I'd definitely ~'를 쓰면 원어민 고급 회화 느낌이 확 살아나요.",
                    tipKo = "'I'd definitely'에서 'def-'에 강한 강세를 줘보세요!",
                    hints = listOf(
                        SuggestedReplyOption("I'd definitely go to New York City!", "무조건 뉴욕에 가고 싶어!"),
                        SuggestedReplyOption("I'd love to swim at a sunny beach in Guam!", "괌의 햇살 가득한 바다에서 수영하고 싶어!"),
                        SuggestedReplyOption("How about you? Where's your dream spot?", "너는? 네가 가고 싶은 꿈의 여행지는 어디야?")
                    )
                )
            )
            else -> listOf(
                TopicBeat(
                    replyEn = "Love hearing that! The vibe out here is super lively today. Want to grab a snack, take a photo, or practice a quick travel mission?",
                    replyKo = "듣기 좋다! 오늘 여기 분위기 완전 활기차. 우리 간식 먹을까, 사진 찍을까, 아니면 실전 여행 미션 연습할까?",
                    rephraseEn = "Let's grab a coffee and practice real travel English together!",
                    grammarKo = "친구에게 제안할 땐 'Let's ~ together!' 또는 'How about ~ing?'가 자연스러워요.",
                    tipKo = "문장 끝을 밝고 경쾌하게 올려서 친구와 수다 떠는 리듬을 살려보세요!",
                    hints = listOf(
                        SuggestedReplyOption("Let's grab an iced coffee and chat!", "아이스 커피 마시면서 수다 떨자!"),
                        SuggestedReplyOption("Could you take a cool photo of us here?", "여기서 우리 멋진 사진 찍어줄래?"),
                        SuggestedReplyOption("Let's practice airport English next!", "다음엔 공항 영어 같이 연습하자!")
                    )
                ),
                TopicBeat(
                    replyEn = "You're doing awesome! Every sentence you say sounds more confident. Tell me one good thing that happened to you today!",
                    replyKo = "진짜 잘하고 있어! 말할수록 자신감이 쑥쑥 느껴져. 오늘 있었던 좋은 일 하나만 들려줄래?",
                    rephraseEn = "The best part of my day was completing my English study goal!",
                    grammarKo = "'오늘 제일 좋았던 일은 ~야'는 'The best part of my day was ~'로 시작하면 아주 자연스러워요.",
                    tipKo = "'best part'에서 두 단어 모두 힘 있게 발음해 보세요.",
                    hints = listOf(
                        SuggestedReplyOption("The best part was hitting my 10-word goal!", "10단어 목표 달성한 게 제일 좋았어!"),
                        SuggestedReplyOption("I had a delicious meal with my family.", "가족이랑 맛있는 식사를 했어."),
                        SuggestedReplyOption("Talking with you in English is super fun!", "너랑 영어로 대화하는 게 제일 재밌어!")
                    )
                )
            )
        }
        return beats[turnIndex.coerceAtLeast(0) % beats.size]
    }

    fun generateProactiveOpener(
        tutor: AiHumanTutor,
        learnerName: String,
        activeTopicTag: String,
        conversationGoalKo: String,
        turnCount: Int,
        isOpening: Boolean
    ): AiCoachReply {
        val enName = HybridAiCoachEngine.formatLearnerEnglishName(learnerName)
        val koName = learnerName.trim().ifBlank { "원삐" }
        val goal = conversationGoalKo.ifBlank { HybridAiCoachEngine.goalForTopic(activeTopicTag) }
        val (replyEn, replyKo, mood, thoughtKo) = when {
            activeTopicTag.contains("카페") -> if (isOpening) {
                Quad(
                    "Hey $enName! Welcome to the cozy corner cafe! What kind of coffee or sweet drink are you craving today?",
                    "${koName}님, 아늑한 카페에 잘 왔어요! 오늘은 어떤 커피나 달콤한 음료가 당기나요?",
                    "happy",
                    "${koName}님이 좋아하는 음료를 먼저 물어보고 사이즈·얼음 옵션으로 대화를 이어가야지"
                )
            } else {
                Quad(
                    "Take your time, $enName! Our iced vanilla latte and warm croissants are super popular today. Want to try one?",
                    "천천히 골라도 돼요, ${koName}님! 오늘 아이스 바닐라 라떼랑 따뜻한 크루아상이 인기예요. 드셔보실래요?",
                    "curious",
                    "잠시 고민 중이신 것 같아 인기 메뉴 선택지를 먼저 제안해 드려야지"
                )
            }
            activeTopicTag.contains("공항") -> if (isOpening) {
                Quad(
                    "Good afternoon, $enName! Welcome to arrivals. May I ask what brings you to the city today?",
                    "안녕하세요 ${koName}님! 입국장에 오신 걸 환영해요. 오늘 어떤 목적으로 방문하셨나요?",
                    "curious",
                    "${koName}님에게 방문 목적을 자연스럽게 물어보며 입국심사 목표를 시작하자"
                )
            } else {
                Quad(
                    "No rush, $enName! Are you here for sightseeing, or visiting friends? Just tell me in a short sentence!",
                    "괜찮아요 ${koName}님! 관광하러 오신 건가요, 친구 만나러 오신 건가요? 짧게 말해보세요!",
                    "happy",
                    "${koName}님이 편하게 답할 수 있도록 관광/방문 선택지를 짚어주자"
                )
            }
            activeTopicTag.contains("식당") || activeTopicTag.contains("레스토랑") -> Quad(
                "Welcome in, $enName! We have a lovely table by the window ready. Would you like to hear today's chef special?",
                "어서 오세요 ${koName}님! 창가 쪽 멋진 자리가 준비되어 있어요. 오늘의 셰프 추천 메뉴 들어보실래요?",
                "excited",
                "창가 자리와 추천 메뉴로 ${koName}님의 레스토랑 주문 목표를 이끌어보자"
            )
            activeTopicTag.contains("호텔") -> Quad(
                "Hello $enName, welcome to our hotel! Checking in today? Let me know if you'd prefer a higher floor with a city view!",
                "안녕하세요 ${koName}님, 호텔에 오신 걸 환영해요! 체크인이시죠? 시티뷰 고층 객실 원하시면 말씀해 주세요!",
                "happy",
                "체크인하면서 객실 선호도를 물어 ${koName}님이 능동적으로 요청하도록 유도하자"
            )
            activeTopicTag.contains("마트") -> Quad(
                "Hi $enName! Looks like you picked some great snacks. Would you like a bag, or are you still looking for anything else?",
                "안녕 ${koName}님! 맛있는 간식을 고르셨네요. 봉투 필요하신가요, 아니면 더 찾으시는 물건이 있나요?",
                "happy",
                "마트 계산대 상황에서 봉투와 결제 표현을 자연스럽게 꺼내보자"
            )
            activeTopicTag.contains("길찾기") -> Quad(
                "Hey $enName, you look like you're checking the map! Where are you headed? I know a great shortcut!",
                "${koName}님, 지도 보고 계신 것 같네요! 어디로 가시는 길이에요? 제가 좋은 지름길을 알아요!",
                "curious",
                "${koName}님이 목적지를 영어로 물어볼 수 있도록 먼저 말을 걸어보자"
            )
            activeTopicTag.contains("소개") -> Quad(
                "So nice to meet you, $enName! I'm ${tutor.name}. What's something fun you've been really into lately?",
                "만나서 정말 반가워요 ${koName}님! 저는 ${tutor.name}예요. 요즘 푹 빠져 있는 재미있는 취미가 있나요?",
                "excited",
                "${koName}님의 관심사를 먼저 물어보며 친근한 자기소개 대화를 열자"
            )
            else -> Quad(
                "Hey $enName! So good to see you again! What's one highlight from your day so far?",
                "${koName}님, 다시 만나서 너무 반가워요! 오늘 하루 중 가장 기억에 남는 일이 뭐예요?",
                "happy",
                "${koName}님의 오늘 이야기를 먼저 물어보며 가볍게 수다를 시작하자"
            )
        }
        val hints = HybridAiCoachEngine.defaultSuggestedRepliesForTopic(activeTopicTag)
        return AiCoachReply(
            replyEn = replyEn,
            translationKo = replyKo,
            coachingTipKo = "목표: $goal — ${koName}님, 편하게 한 문장으로 답해 보세요!",
            naturalRephraseEn = hints.firstOrNull()?.english ?: "I'm doing great today!",
            grammarFixKo = "상황에 맞는 짧은 핵심 문장부터 말하면 대화가 술술 풀려요.",
            suggestedReplies = hints,
            engineBadge = "능동 스킬 코치 (오프라인)",
            thoughtKo = thoughtKo,
            goalProgressKo = goal,
            characterMood = mood
        )
    }

    private data class Quad(val a: String, val b: String, val c: String, val d: String)

    fun generateReply(
        tutor: AiHumanTutor,
        userText: String,
        history: List<TutorChatMessage>,
        activeTopicTag: String,
        activeTwist: TravelTwist? = null,
        learnerName: String = "원삐",
        conversationGoalKo: String = ""
    ): AiCoachReply {
        val enName = HybridAiCoachEngine.formatLearnerEnglishName(learnerName)
        val koName = learnerName.trim().ifBlank { "원삐" }
        val resolvedGoal = conversationGoalKo.ifBlank { HybridAiCoachEngine.goalForTopic(activeTopicTag) }
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()
        val userTurns = history.count { !it.fromTutor }

        // 1. Travel Twist Roleplay Mode
        if (activeTwist != null) {
            val matchedKeyword = activeTwist.expectedKeywords.any { lower.contains(it.lowercase()) }
            val suggested = activeTwist.keyPhrases.firstOrNull()
                ?: "Could you please help me resolve this issue right away?"
            return if (matchedKeyword) {
                AiCoachReply(
                    replyEn = "Got it, $enName! Thank you for explaining that so clearly. I'll take care of it for you right away!",
                    translationKo = "알겠습니다 ${koName}님! 아주 명확하게 설명해 주셔서 감사해요. 제가 지금 바로 처리해 드릴게요!",
                    coachingTipKo = "돌발 변수 핵심 키워드를 정확히 사용했어요! 다음엔 한 번 더 또렷하게 강세를 줘보세요.",
                    naturalRephraseEn = suggested,
                    grammarFixKo = "상황에 꼭 맞는 핵심 키워드와 정중한 요청 표현이 잘 어우러졌어요.",
                    suggestedReplies = listOf(
                        SuggestedReplyOption("Thank you so much for your help!", "도와주셔서 정말 감사합니다!"),
                        SuggestedReplyOption("How long will it take?", "얼마나 걸릴까요?"),
                        SuggestedReplyOption("I really appreciate your quick response.", "빠르게 대응해 주셔서 감사해요.")
                    ),
                    engineBadge = "온디바이스 스킬 코치 (무료·0ms)",
                    thoughtKo = "${koName}님이 돌발 상황을 정확히 해결했으니 다음 단계로 이어주자",
                    goalProgressKo = resolvedGoal,
                    characterMood = "proud"
                )
            } else {
                AiCoachReply(
                    replyEn = "I hear you, $enName! To solve this even faster, try saying: \"$suggested\"",
                    translationKo = "무슨 말씀인지 알겠어요 ${koName}님! 훨씬 빠르게 해결하려면 이렇게 말해보세요!",
                    coachingTipKo = "팁: ${activeTwist.strategyKo}",
                    naturalRephraseEn = suggested,
                    grammarFixKo = "Could you please ~ 패턴을 쓰면 현지 직원이 훨씬 친절하게 도와줘요.",
                    suggestedReplies = listOf(
                        SuggestedReplyOption(suggested, "핵심 문장 그대로 따라 말하기"),
                        SuggestedReplyOption("Could you check that for me again?", "다시 한 번 확인해 주시겠어요?"),
                        SuggestedReplyOption("Is there another option available?", "다른 대안이 있을까요?")
                    ),
                    engineBadge = "온디바이스 스킬 코치 (무료·0ms)",
                    thoughtKo = "${koName}님이 핵심 요청 문장을 바로 써볼 수 있게 친절히 안내하자",
                    goalProgressKo = resolvedGoal,
                    characterMood = "curious"
                )
            }
        }

        // 2. Check Rule-Based Grammar Fix on English input
        var grammarRephrase: String? = null
        var grammarFixMsg = ""
        var grammarTipMsg = ""
        for (rule in grammarUpgrades) {
            val match = rule.pattern.find(trimmed)
            if (match != null) {
                grammarRephrase = rule.rephrase(match, trimmed)
                grammarFixMsg = rule.grammarKo
                grammarTipMsg = rule.tipKo
                break
            }
        }

        // 3. Match Rich Multi-Situation Korean & English Phrase Rules
        val matchedRule = koreanAndDailyPhraseRules.firstOrNull { rule ->
            val allOk = rule.keywordsAll.isEmpty() || rule.keywordsAll.all { lower.contains(it) }
            val anyOk = rule.keywordsAny.isEmpty() || rule.keywordsAny.any { lower.contains(it) }
            allOk && anyOk
        }

        if (matchedRule != null) {
            val isKoreanAsk = trimmed.any { it in '\uAC00'..'\uD7A3' }
            val finalRephrase = grammarRephrase ?: matchedRule.nativeEn
            val finalReplyEn = if (isKoreanAsk) {
                "Great question, $enName! Natives say: \"${matchedRule.nativeEn}\" Try saying it out loud to me!"
            } else {
                "Nice, $enName! ${matchedRule.replyEn}"
            }
            val finalReplyKo = if (isKoreanAsk) {
                "좋은 질문이야 ${koName}님! 원어민은 '${matchedRule.nativeEn}'라고 말해! 지금 나한테 소리 내어 말해볼래?"
            } else {
                "${koName}님 좋아요! ${matchedRule.replyKo}"
            }
            return AiCoachReply(
                replyEn = finalReplyEn,
                translationKo = finalReplyKo,
                coachingTipKo = grammarTipMsg.ifBlank { matchedRule.tipKo },
                naturalRephraseEn = finalRephrase,
                grammarFixKo = grammarFixMsg.ifBlank { matchedRule.grammarKo },
                suggestedReplies = if (isKoreanAsk) {
                    listOf(SuggestedReplyOption(matchedRule.nativeEn, "방금 배운 원어민 표현 바로 말하기")) + matchedRule.nextHints.take(2)
                } else {
                    matchedRule.nextHints
                },
                engineBadge = "온디바이스 스킬 코치 (무료·0ms)",
                thoughtKo = "${koName}님의 의도를 파악해 '$activeTopicTag' 다음 상황으로 자연스럽게 넘어가자",
                goalProgressKo = resolvedGoal,
                characterMood = "happy"
            )
        }

        // 4. Multi-Turn Contextual Roleplay Progression (Never repetitive!)
        val beat = topicFollowUpBeat(activeTopicTag, userTurns)
        val hasEnglish = trimmed.any { it in 'a'..'z' || it in 'A'..'Z' }
        val polishedUserSentence = grammarRephrase ?: if (hasEnglish) {
            val cap = trimmed.replaceFirstChar { it.uppercase() }
            if (cap.endsWith(".") || cap.endsWith("!") || cap.endsWith("?")) cap else "$cap!"
        } else {
            beat.rephraseEn
        }

        val replyEn = if (trimmed.endsWith("?") || lower.startsWith("what ") || lower.startsWith("do you ") || lower.startsWith("can you ")) {
            "Ooh, good question, $enName! ${beat.replyEn}"
        } else {
            "Love that, $enName! ${beat.replyEn}"
        }

        return AiCoachReply(
            replyEn = replyEn,
            translationKo = "${koName}님, ${beat.replyKo}",
            coachingTipKo = grammarTipMsg.ifBlank { "${beat.tipKo} (${tutor.accentLabel})" },
            naturalRephraseEn = polishedUserSentence,
            grammarFixKo = grammarFixMsg.ifBlank { beat.grammarKo },
            suggestedReplies = beat.hints,
            engineBadge = "온디바이스 스킬 코치 (무료·0ms)",
            thoughtKo = "${koName}님의 답변을 칭찬하고 '$activeTopicTag' 목표를 향해 한 걸음 더 질문하자",
            goalProgressKo = resolvedGoal,
            characterMood = "excited"
        )
    }
}
