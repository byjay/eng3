package com.example.data

import com.example.model.VocabExample
import com.example.model.VocabStudyTrack
import com.example.model.VocabTopic
import com.example.model.VocabWord

object VocabRepository {

    // High School Grade 1, 2, 3 CSAT & Textbook Essential Vocabulary
    // IMPORTANT: Never presented in alphabetical order!
    // Every word carries real CSAT frequency (3..5), CSAT difficulty (1..3), textbook unit,
    // situational topic, and 4 rich contextual sentences for Read / Listen / Speak mastery.
    private data class RawWord(
        val word: String,
        val ipa: String,
        val pos: String,
        val meaningKo: String,
        val csatFreq: Int, // 5 = 최다빈출, 4 = 고빈출, 3 = 필수
        val csatDiff: Int, // 1 = 기초, 2 = 핵심, 3 = 1등급 킬러
        val topic: VocabTopic,
        val unitKo: String,
        val coreEn: String,
        val coreKo: String,
        val tipKo: String
    )

    private val rawGrade1: List<RawWord> = listOf(
        RawWord("consistent", "/kənˈsɪstənt/", "adj.", "일관된, 꾸준한", 5, 1, VocabTopic.SCHOOL_EXAM, "공통영어1 · 습관과 성장", "Consistent effort beats occasional bursts of energy.", "꾸준한 노력이 가끔 쏟아붓는 에너지를 이긴다.", "두 번째 음절 'sɪs'를 또렷하게 치고 넘어가세요."),
        RawWord("available", "/əˈveɪləbəl/", "adj.", "이용 가능한, 시간이 있는", 5, 1, VocabTopic.TRAVEL_DAILY, "공통영어1 · 일상 소통", "Are there any window seats available on the morning train?", "아침 기차에 이용 가능한 창가 좌석이 있나요?", "v 발음 시 윗니와 아랫입술 진동을 꼭 느껴보세요."),
        RawWord("affect", "/əˈfekt/", "v.", "영향을 미치다", 5, 1, VocabTopic.SCIENCE_AI, "공통영어1 · 자연과 환경", "Weather changes can strongly affect our flight schedule.", "날씨 변화는 우리의 비행 일정에 큰 영향을 미칠 수 있다.", "f 발음은 윗니로 아랫입술을 가볍게 스치며 바람을 내세요."),
        RawWord("consequence", "/ˈkɑːnsəkwəns/", "n.", "결과, 중요성", 5, 2, VocabTopic.LOGIC_DEBATE, "공통영어1 · 논리와 선택", "Every small daily habit creates a powerful consequence.", "매일의 작은 습관이 강력한 결과를 만들어낸다.", "qu는 입술을 모으는 /kw/ 소리('퀀스')로 발음하세요."),
        RawWord("adapt", "/əˈdæpt/", "v.", "적응하다, 개조하다", 5, 1, VocabTopic.TRAVEL_DAILY, "공통영어1 · 문화와 여행", "Travelers quickly adapt to local customs and new foods.", "여행자들은 현지 관습과 새로운 음식에 빠르게 적응한다.", "adopt(채택하다)와 구별해 'dæpt'의 입을 옆으로 벌리세요."),
        RawWord("approach", "/əˈproʊtʃ/", "v./n.", "접근하다, 접근법", 5, 1, VocabTopic.LOGIC_DEBATE, "공통영어1 · 문제 해결", "A creative approach can solve even the hardest puzzle.", "창의적인 접근법은 가장 어려운 퍼즐도 풀 수 있다.", "oʊ 이중모음을 '오우'로 둥글게 이어 발음하세요."),
        RawWord("benefit", "/ˈbenəfɪt/", "n./v.", "이익, 혜택, 도움이 되다", 5, 1, VocabTopic.SOCIETY_ECONOMY, "공통영어1 · 경제와 건강", "Regular morning walks bring a huge benefit to your health.", "규칙적인 아침 산책은 건강에 큰 혜택을 가져다준다.", "f를 'ㅍ'이 아니라 윗니+아랫입술 마찰음으로 내세요."),
        RawWord("aware", "/əˈwer/", "adj.", "인식하고 있는, 알고 있는", 5, 1, VocabTopic.EMOTION_MIND, "공통영어1 · 자기 인식", "Be aware of the boarding time printed on your ticket.", "티켓에 인쇄된 탑승 시간을 꼭 인지하고 계세요.", "w 발음은 입술을 작게 오므렸다가 '웨어'로 열어주세요."),
        RawWord("accurate", "/ˈækjərət/", "adj.", "정확한", 5, 2, VocabTopic.SCIENCE_AI, "공통영어1 · 정보와 데이터", "Please check the timetable to get accurate departure info.", "정확한 출발 정보를 얻으려면 시간표를 확인해 주세요.", "첫 음절 'æ'를 턱을 내려 넓게 발음하면 아주 정확해요."),
        RawWord("attitude", "/ˈætɪtuːd/", "n.", "태도, 마음가짐", 5, 1, VocabTopic.EMOTION_MIND, "공통영어1 · 마음가짐", "A positive attitude turns every mistake into a lesson.", "긍정적인 태도는 모든 실수를 배움으로 바꾼다.", "미국식 발음에서 중간 t는 가볍게 탄설음(/ɾ/)으로 굴러갑니다."),
        RawWord("accomplish", "/əˈkɑːmplɪʃ/", "v.", "성취하다, 완수하다", 4, 1, VocabTopic.SCHOOL_EXAM, "공통영어1 · 목표 달성", "Daily practice helps you accomplish big goals step by step.", "매일의 연습은 큰 목표를 한 걸음씩 성취하게 돕는다.", "마지막 sh는 입술을 동그랗게 내밀며 부드럽게 마무리하세요."),
        RawWord("alternative", "/ɔːlˈtɜːrnətɪv/", "n./adj.", "대안, 대체의", 4, 2, VocabTopic.LOGIC_DEBATE, "공통영어1 · 창의적 사고", "Let us find an alternative route to avoid heavy traffic.", "심한 교통 체증을 피할 대안 경로를 찾아봅시다.", "강세 'tɜːr'에서 혀를 뒤로 말며 길게 빼주세요."),
        RawWord("confirm", "/kənˈfɜːrm/", "v.", "확인하다, 확정하다", 4, 1, VocabTopic.TRAVEL_DAILY, "공통영어1 · 실전 예약", "Could you confirm my hotel reservation for two nights?", "2박 호텔 예약을 확인해 주실 수 있나요?", "f 마찰음 + r 굴림을 합친 '펌' 소리에 집중하세요."),
        RawWord("concentrate", "/ˈkɑːnsəntreɪt/", "v.", "집중하다", 4, 1, VocabTopic.SCHOOL_EXAM, "공통영어1 · 공부 전략", "Turn off distractions and concentrate on the speaker's rhythm.", "방해 요소를 끄고 화자의 리듬에 집중하세요.", "첫 음절 '콘'에 주강세, 끝 '트레이트'는 가볍게!"),
        RawWord("challenge", "/ˈtʃælɪndʒ/", "n./v.", "도전, 이의를 제기하다", 5, 1, VocabTopic.EMOTION_MIND, "공통영어1 · 성장 마인드", "Accept today's challenge and beat your personal record.", "오늘의 도전을 받아들이고 개인 기록을 깨보세요.", "ch와 끝 ge의 울림 차이를 또렷하게 구분해 보세요."),
        RawWord("appropriate", "/əˈproʊpriət/", "adj.", "적절한", 5, 2, VocabTopic.SOCIETY_ECONOMY, "공통영어2 · 매너와 사회", "Choose appropriate clothes for the rainy autumn weather.", "비 오는 가을 날씨에 적절한 옷을 선택하세요.", "형용사일 때 끝을 '잇'에 가깝게 가볍게 떨어뜨리세요."),
        RawWord("artificial", "/ˌɑːrtɪˈfɪʃəl/", "adj.", "인공의, 인위적인", 4, 2, VocabTopic.SCIENCE_AI, "공통영어2 · 인공지능 시대", "Artificial intelligence helps learners practice speaking daily.", "인공지능은 학습자들이 매일 말하기를 연습하도록 돕는다.", "'fɪ'에 주강세를 두고 f 마찰음을 또렷이 살리세요."),
        RawWord("analyze", "/ˈænəlaɪz/", "v.", "분석하다", 5, 2, VocabTopic.SCIENCE_AI, "공통영어2 · 과학 탐구", "Scientists analyze climate data to predict future storms.", "과학자들은 미래의 폭풍을 예측하기 위해 기후 데이터를 분석한다.", "끝의 z는 성대를 울리며 '즈'로 길게 마무리하세요."),
        RawWord("circumstance", "/ˈsɜːrkəmstæns/", "n.", "상황, 환경", 4, 2, VocabTopic.TRAVEL_DAILY, "공통영어2 · 위기 대처", "Stay calm under any unexpected travel circumstance.", "어떤 예상치 못한 여행 상황에서도 침착함을 유지하세요.", "첫 음절 'sɜːr'에서 r을 확실히 말아 올리세요."),
        RawWord("combine", "/kəmˈbaɪn/", "v.", "결합하다", 4, 1, VocabTopic.SCHOOL_EXAM, "공통영어2 · 융합 학습", "Combine listening and speaking to double your memory speed.", "듣기와 말하기를 결합하면 암기 속도가 두 배가 된다.", "동사 강세는 두 번째 음절 '바인'에 위치합니다."),
        RawWord("assume", "/əˈsuːm/", "v.", "추정하다, 떠맡다", 5, 2, VocabTopic.LOGIC_DEBATE, "공통영어2 · 비판적 읽기", "Do not assume the answer without reading the full sentence.", "전체 문장을 읽지 않고 답을 추정하지 마세요.", "미국식은 '어숨', 영국식은 '어슘'으로 길게 발음합니다."),
        RawWord("capable", "/ˈkeɪpəbəl/", "adj.", "유능한, ~할 수 있는", 4, 1, VocabTopic.EMOTION_MIND, "공통영어2 · 자신감", "You are completely capable of mastering thirty words today.", "당신은 오늘 30단어를 완벽히 마스터할 능력이 충분하다.", "첫 음절 '케이'를 길고 강하게 올려 발음하세요."),
        RawWord("commit", "/kəˈmɪt/", "v.", "헌신하다, 저지르다", 4, 2, VocabTopic.SCHOOL_EXAM, "공통영어2 · 약속과 책임", "Commit ten focused minutes every morning to shadowing.", "매일 아침 집중된 10분을 섀도잉에 헌신하세요.", "'커밋'에서 두 번째 음절 '밋'을 짧고 강하게!"),
        RawWord("compare", "/kəmˈper/", "v.", "비교하다", 4, 1, VocabTopic.LOGIC_DEBATE, "공통영어2 · 비교와 대조", "Compare your recorded voice directly with the native tutor.", "녹음된 자신의 목소리를 원어민 튜터와 직접 비교해 보세요.", "'컴페어'에서 p의 파열음과 끝 r을 자연스럽게 이어주세요."),
        RawWord("abandon", "/əˈbændən/", "v.", "버리다, 포기하다", 4, 2, VocabTopic.EMOTION_MIND, "공통영어1 · 용기와 끈기", "Never abandon your dream even when the path gets steep.", "길이 가팔라져도 결코 너의 꿈을 포기하지 마라.", "강세는 두 번째 음절 'bæn'에 두고 '어밴던'으로 또렷하게!"),
        RawWord("absorb", "/əbˈzɔːrb/", "v.", "흡수하다, 몰두시키다", 4, 2, VocabTopic.SCIENCE_AI, "공통영어1 · 생명과 에너지", "Plants absorb sunlight and turn it into vital energy.", "식물은 햇빛을 흡수해 필수 에너지로 바꾼다.", "s가 /z/로 울리는 '업조브' 소리 + 끝 r을 살짝 말아주세요."),
        RawWord("adequate", "/ˈædɪkwət/", "adj.", "적절한, 충분한", 4, 2, VocabTopic.SCHOOL_EXAM, "공통영어2 · 건강 관리", "Make sure you get adequate sleep before the exam.", "시험 전에는 충분한 수면을 취하도록 하세요.", "첫 음절에 강세를 두고 뒤는 가볍게 '애디퀏'으로 흘리세요."),
        RawWord("ambition", "/æmˈbɪʃən/", "n.", "야망, 포부", 3, 1, VocabTopic.EMOTION_MIND, "공통영어1 · 진로와 꿈", "Her ambition is to speak English fluently around the world.", "그녀의 포부는 전 세계에서 영어를 유창하게 말하는 것이다.", "중간 'bɪ'에 강세를 주고 '션'은 가볍게 붙여 발음하세요."),
        RawWord("ancient", "/ˈeɪnʃənt/", "adj.", "고대의, 옛날의", 3, 1, VocabTopic.TRAVEL_DAILY, "공통영어1 · 세계 유산", "We walked through an ancient castle overlooking the sea.", "우리는 바다가 내려다보이는 고대 성을 걸어 지나갔다.", "'에인션트'에서 앞의 '에인'을 길게 끌어주세요."),
        RawWord("anticipate", "/ænˈtɪsəpeɪt/", "v.", "기대하다, 예상하다", 4, 2, VocabTopic.TRAVEL_DAILY, "공통영어2 · 미래 예측", "We anticipate a warm welcome when we arrive in London.", "우리는 런던에 도착했을 때 따뜻한 환영을 기대한다.", "두 번째 음절 'tɪ'에 강세를 주어 리듬감을 살리세요."),
        RawWord("apparent", "/əˈpærənt/", "adj.", "분명한, 겉보기의", 4, 2, VocabTopic.LOGIC_DEBATE, "공통영어2 · 추론 독해", "It was apparent that everyone enjoyed the live show.", "모두가 라이브 공연을 즐겼다는 것이 분명했다.", "'pær'에서 r 발음을 혀가 닿지 않게 부드럽게 굴리세요."),
        RawWord("aspect", "/ˈæspekt/", "n.", "측면, 양상", 5, 2, VocabTopic.LOGIC_DEBATE, "공통영어2 · 다각적 시각", "Speaking daily improves every aspect of your English.", "매일 말하기는 영어의 모든 측면을 향상시킨다.", "첫 음절 'æs'에 강세를 두고 끝 ct를 깔끔하게 닫아주세요."),
        RawWord("authority", "/əˈθɔːrəti/", "n.", "권위, 당국", 4, 2, VocabTopic.SOCIETY_ECONOMY, "공통영어2 · 시민과 사회", "Local authorities opened a new bridge for commuters.", "지역 당국은 통근자들을 위해 새 다리를 개통했다.", "th(/θ/)는 혀끝을 치아 사이에 살짝 물고 바람을 빼세요."),
        RawWord("category", "/ˈkætəɡɔːri/", "n.", "범주, 카테고리", 3, 1, VocabTopic.SCHOOL_EXAM, "공통영어1 · 정보 분류", "Sort the new vocabulary into each study category.", "새 영단어를 각 학습 카테고리로 분류하세요.", "'캐터고리'에서 첫 음절 '캐'에 강한 액센트를 주세요."),
        RawWord("collapse", "/kəˈlæps/", "v./n.", "붕괴되다, 무너지다", 4, 2, VocabTopic.SOCIETY_ECONOMY, "공통영어2 · 안전과 재난", "The old wooden bridge might collapse during heavy rain.", "오래된 나무 다리는 폭우 중에 붕괴될 수 있다.", "l 발음은 혀끝을 윗니 뒤 잇몸에 꼭 붙였다 떼세요.")
    )

    private val rawGrade2: List<RawWord> = listOf(
        RawWord("crucial", "/ˈkruːʃəl/", "adj.", "중대한, 결정적인", 5, 2, VocabTopic.LOGIC_DEBATE, "영어Ⅰ · 핵심 주장", "Clear pronunciation is crucial in urgent travel moments.", "긴급한 여행 순간에는 명확한 발음이 결정적이다.", "'크루셜'에서 '크루'를 입술을 모아 길게 발음하세요."),
        RawWord("enhance", "/ɪnˈhæns/", "v.", "향상시키다, 높이다", 5, 2, VocabTopic.SCIENCE_AI, "영어Ⅰ · 기술과 삶", "Real-time feedback will dramatically enhance your confidence.", "실시간 피드백은 당신의 자신감을 극적으로 높여줄 것이다.", "h 숨소리와 넓은 'æ' 모음을 살려 '인핸스'로!"),
        RawWord("emphasize", "/ˈemfəsaɪz/", "v.", "강조하다", 5, 2, VocabTopic.SCHOOL_EXAM, "영어Ⅰ · 논설문 독해", "Native speakers emphasize content words to create rhythm.", "원어민들은 리듬을 만들기 위해 내용어를 강조한다.", "ph는 /f/ 소리! '엠퍼사이즈'로 강약을 살리세요."),
        RawWord("distinguish", "/dɪˈstɪŋɡwɪʃ/", "v.", "구별하다, 식별하다", 5, 2, VocabTopic.LOGIC_DEBATE, "영어Ⅰ · 비교 분석", "Train your ears to distinguish subtle vowel lengths.", "미묘한 모음 길이를 구별하도록 귀를 훈련하세요.", "'스팅귀쉬'에서 '팅'에 강세를 실어주세요."),
        RawWord("demonstrate", "/ˈdemənstreɪt/", "v.", "입증하다, 시연하다", 5, 2, VocabTopic.SCIENCE_AI, "영어Ⅰ · 과학 실험", "The tutor will demonstrate the exact tongue position.", "튜터가 정확한 혀의 위치를 시연해 줄 것입니다.", "첫 음절 '뎀'에 강세를 주고 str 자음군을 매끄럽게!"),
        RawWord("encounter", "/ɪnˈkaʊntər/", "v./n.", "마주치다, 뜻밖의 만남", 5, 2, VocabTopic.TRAVEL_DAILY, "영어Ⅰ · 글로벌 문화", "You may encounter sudden delays at busy international airports.", "붐비는 국제공항에서는 갑작스러운 지연을 마주칠 수 있다.", "'카운'을 크게 올렸다가 '터'를 부드럽게 내려주세요."),
        RawWord("eliminate", "/ɪˈlɪməneɪt/", "v.", "제거하다, 없애다", 5, 2, VocabTopic.SCHOOL_EXAM, "영어Ⅱ · 문제 해결", "Repeating missed words helps eliminate stubborn mistakes.", "틀린 단어를 반복하면 고질적인 실수를 제거할 수 있다.", "'림'에 강세를 주고 L 발음을 깨끗하게 내세요."),
        RawWord("emerge", "/ɪˈmɜːrdʒ/", "v.", "나타나다, 부상하다", 5, 2, VocabTopic.SOCIETY_ECONOMY, "영어Ⅱ · 사회 변화", "New opportunities emerge whenever you step outside your comfort zone.", "안전지대를 벗어날 때마다 새로운 기회가 나타난다.", "'머지'에서 r을 말아 올리고 dʒ로 부드럽게 끝내세요."),
        RawWord("compensate", "/ˈkɑːmpənseɪt/", "v.", "보상하다, 보완하다", 4, 2, VocabTopic.SOCIETY_ECONOMY, "영어Ⅱ · 경제와 보상", "The airline offered a voucher to compensate for the delay.", "항공사는 지연을 보상하기 위해 바우처를 제공했다.", "첫 음절 '콤'에 주강세를 두고 리듬감 있게 발음하세요."),
        RawWord("acknowledge", "/əkˈnɑːlɪdʒ/", "v.", "인정하다, 감사를 표하다", 5, 2, VocabTopic.EMOTION_MIND, "영어Ⅰ · 공감과 리더십", "Great leaders acknowledge their team's hard work openly.", "훌륭한 리더는 팀의 노고를 공개적으로 인정한다.", "c와 k가 겹쳐 /k/ 하나로 발음되고 '날리지'에 강세가 옵니다."),
        RawWord("attribute", "/əˈtrɪbjuːt/", "v./n.", "~의 탓으로 돌리다, 속성", 5, 3, VocabTopic.LOGIC_DEBATE, "영어Ⅱ · 원인과 결과", "She attributes her fluent accent to daily voice shadowing.", "그녀는 자신의 유창한 억양을 매일의 섀도잉 덕분으로 돌린다.", "동사는 '트립'에 강세, 명사는 첫 음절 '애'에 강세를 둡니다."),
        RawWord("authentic", "/ɔːˈθentɪk/", "adj.", "진짜의, 진정성 있는", 4, 2, VocabTopic.TRAVEL_DAILY, "영어Ⅰ · 세계 음식", "This street market serves authentic local dishes at fair prices.", "이 재래시장은 합리적인 가격에 진짜 현지 요리를 제공한다.", "th(/θ/) 발음에 혀끝을 살짝 내밀며 '어쎈틱'으로!"),
        RawWord("collaborate", "/kəˈlæbəreɪt/", "v.", "협력하다, 협업하다", 4, 2, VocabTopic.SCHOOL_EXAM, "영어Ⅰ · 협업 프로젝트", "Students collaborate across borders using digital tools.", "학생들은 디지털 도구를 사용해 국경을 넘어 협력한다.", "'래'에 강세를 두고 L 발음과 R 발음을 분리해 주세요."),
        RawWord("confront", "/kənˈfrʌnt/", "v.", "직면하다, 맞서다", 4, 2, VocabTopic.EMOTION_MIND, "영어Ⅱ · 위기 극복", "Travelers must calmly confront sudden flight cancellations.", "여행자들은 갑작스러운 항공편 취소에 침착하게 맞서야 한다.", "f 뒤의 o는 '어'(/ʌ/) 소리로 짧게 발음합니다."),
        RawWord("cultivate", "/ˈkʌltəveɪt/", "v.", "기르다, 경작하다", 4, 2, VocabTopic.SCHOOL_EXAM, "영어Ⅰ · 습관 형성", "Cultivate a daily habit of speaking English out loud.", "매일 큰 소리로 영어를 말하는 습관을 길러라.", "'컬'의 L을 혀끝으로 정확히 찍고 v 진동으로 마무리!"),
        RawWord("accumulate", "/əˈkjuːmjəleɪt/", "v.", "축적하다, 모으다", 4, 2, VocabTopic.SOCIETY_ECONOMY, "영어Ⅰ · 경제와 성장", "Small coins accumulate quickly when you clear daily quests.", "일일 퀘스트를 깨면 작은 코인들이 빠르게 축적된다.", "'큐'에 강세를 주고 길게 늘여 '어큐뮬레이트'로!"),
        RawWord("advocate", "/ˈædvəkeɪt/", "v./n.", "옹호하다, 지지자", 4, 3, VocabTopic.LOGIC_DEBATE, "영어Ⅱ · 토론과 주장", "Many educators advocate active speaking over silent reading.", "많은 교육자들은 눈으로만 읽기보다 적극적 말하기를 옹호한다.", "v 진동을 확실히 넣고 동사는 끝을 '-케이트'로 발음하세요."),
        RawWord("allocate", "/ˈæləkeɪt/", "v.", "할당하다, 배분하다", 4, 3, VocabTopic.SOCIETY_ECONOMY, "영어Ⅱ · 자원 배분", "Allocate twenty minutes each day to travel roleplay.", "매일 20분을 여행 롤플레이에 할당하세요.", "첫 음절 '앨'에 강세를 두고 L 소리를 또렷이 살리세요."),
        RawWord("ambiguous", "/æmˈbɪɡjuəs/", "adj.", "모호한, 애매한", 5, 3, VocabTopic.LOGIC_DEBATE, "영어Ⅱ · 의미 추론", "Avoid ambiguous expressions when asking for directions.", "길을 물을 때는 모호한 표현을 피하세요.", "'비규어스'에서 '비'에 강세를 두고 부드럽게 연결하세요."),
        RawWord("arbitrary", "/ˈɑːrbətreri/", "adj.", "임의적인, 제멋대로인", 4, 3, VocabTopic.LOGIC_DEBATE, "영어Ⅱ · 공정과 기준", "The grading rubric is transparent and never arbitrary.", "채점 기준은 투명하며 결코 임의적이지 않다.", "r 소리가 세 번 나오니 혀끝이 입천장에 닿지 않게 주의!"),
        RawWord("capacity", "/kəˈpæsəti/", "n.", "수용력, 능력", 5, 2, VocabTopic.SCIENCE_AI, "영어Ⅰ · 뇌과학 학습", "The human brain has an amazing capacity for new languages.", "인간의 뇌는 새로운 언어에 대한 놀라운 수용력을 지녔다.", "두 번째 음절 '패'에 강세를 주고 입을 넓게 여세요."),
        RawWord("coherent", "/koʊˈhɪrənt/", "adj.", "일관성 있는, 조리 있는", 4, 3, VocabTopic.SCHOOL_EXAM, "영어Ⅱ · 글쓰기 논리", "Organize your ideas into a clear and coherent speech.", "당신의 아이디어를 명확하고 조리 있는 발표로 정리하세요.", "'코히어런트'에서 '히'에 강세를 실어주세요."),
        RawWord("compelling", "/kəmˈpelɪŋ/", "adj.", "설득력 있는, 강렬한", 4, 3, VocabTopic.LOGIC_DEBATE, "영어Ⅱ · 설득과 스토리", "She gave a compelling presentation that won the audience.", "그녀는 청중을 사로잡은 설득력 있는 발표를 했다.", "'펠'에 강세를 주고 끝 ng는 비음으로 부드럽게 울리세요."),
        RawWord("comprehend", "/ˌkɑːmprɪˈhend/", "v.", "이해하다, 파악하다", 4, 2, VocabTopic.SCHOOL_EXAM, "영어Ⅰ · 심층 독해", "Listening twice helps you comprehend subtle native jokes.", "두 번 들으면 미묘한 원어민 농담까지 이해하는 데 도움이 된다.", "끝 음절 '헨드'에 주강세를 두고 h 숨소리를 살리세요."),
        RawWord("conceive", "/kənˈsiːv/", "v.", "상상하다, 구상하다", 4, 3, VocabTopic.SCIENCE_AI, "영어Ⅱ · 발명과 상상", "It is hard to conceive modern travel without smartphones.", "스마트폰 없는 현대 여행을 상상하기는 어렵다.", "'시브'의 모음을 길게 빼고 끝 v를 진동시켜 주세요."),
        RawWord("confine", "/kənˈfaɪn/", "v.", "제한하다, 가두다", 4, 3, VocabTopic.EMOTION_MIND, "영어Ⅱ · 사고의 확장", "Do not confine your English practice to textbooks alone.", "영어 연습을 교과서에만 제한하지 마세요.", "f 마찰음 + '아인' 이중모음을 힘 있게 올려주세요."),
        RawWord("consensus", "/kənˈsensəs/", "n.", "합의, 의견 일치", 4, 3, VocabTopic.SOCIETY_ECONOMY, "영어Ⅱ · 민주적 토론", "The committee reached a consensus after an hour of debate.", "위원회는 한 시간의 토론 끝에 합의에 도달했다.", "'센'에 강세를 두고 양쪽 s를 깨끗한 마찰음으로!"),
        RawWord("contemplate", "/ˈkɑːntəmpleɪt/", "v.", "심사숙고하다, 응시하다", 3, 3, VocabTopic.EMOTION_MIND, "영어Ⅱ · 철학과 성찰", "Take a quiet moment to contemplate your future career.", "조용한 시간을 갖고 미래의 진로를 심사숙고해 보세요.", "첫 음절 '콘'에 강세를 주고 p-l 연결을 부드럽게!"),
        RawWord("contradict", "/ˌkɑːntrəˈdɪkt/", "v.", "모순되다, 반박하다", 5, 3, VocabTopic.LOGIC_DEBATE, "영어Ⅱ · 논리 오류", "His actions seem to contradict what he promised earlier.", "그의 행동은 앞서 약속했던 것과 모순되는 것처럼 보인다.", "'딕트'에 주강세를 두고 끝 ct를 깔끔하게 끊어주세요."),
        RawWord("controversy", "/ˈkɑːntrəvɜːrsi/", "n.", "논란, 논쟁", 4, 2, VocabTopic.SOCIETY_ECONOMY, "영어Ⅰ · 미디어 시사", "The new policy sparked lively controversy online.", "새 정책은 온라인에서 활발한 논란을 불러일으켰다.", "미국식은 첫 음절 '콘', 영국식은 '트로'에 강세를 둡니다."),
        RawWord("deliberate", "/dɪˈlɪbərət/", "adj./v.", "의도적인, 신중한, 숙고하다", 5, 3, VocabTopic.SCHOOL_EXAM, "영어Ⅱ · 의도적 연습", "Deliberate practice transforms beginners into fluent speakers.", "의도적인 집중 연습은 초보자를 유창한 화자로 탈바꿈시킨다.", "'립'에 강세를 두고 r을 부드럽게 굴려주세요."),
        RawWord("deprive", "/dɪˈpraɪv/", "v.", "빼앗다, 박탈하다", 4, 3, VocabTopic.EMOTION_MIND, "영어Ⅱ · 수면과 건강", "Poor sleep can deprive you of focus during morning exams.", "수면 부족은 아침 시험 중 집중력을 빼앗을 수 있다.", "p와 r을 한 번에 '프라이브'로 뭉쳐 발음하세요."),
        RawWord("derive", "/dɪˈraɪv/", "v.", "이끌어내다, 유래하다", 5, 2, VocabTopic.SCIENCE_AI, "영어Ⅰ · 언어의 기원", "Many English words derive from ancient Latin roots.", "많은 영어 단어는 고대 라틴어 어근에서 유래한다.", "r 소리와 끝 v 진동이 핵심인 단어입니다."),
        RawWord("diminish", "/dɪˈmɪnɪʃ/", "v.", "줄어들다, 감소시키다", 4, 2, VocabTopic.EMOTION_MIND, "영어Ⅱ · 심리 극복", "Stage fright will diminish as you speak more frequently.", "더 자주 말할수록 무대 공포증은 줄어들 것이다.", "'민'에 강세를 두고 끝 sh를 길게 빼세요."),
        RawWord("elaborate", "/ɪˈlæbərət/", "adj./v.", "정교한, 상세히 설명하다", 4, 3, VocabTopic.TRAVEL_DAILY, "영어Ⅱ · 프레젠테이션", "Could you elaborate on your travel plan for next week?", "다음 주 여행 계획에 대해 좀 더 상세히 설명해 주시겠어요?", "동사로 쓸 때는 끝을 '-레이트'로 또렷이 발음합니다.")
    )

    private val rawGrade3: List<RawWord> = listOf(
        RawWord("inevitable", "/ɪnˈevətəbəl/", "adj.", "피할 수 없는, 필연적인", 5, 3, VocabTopic.LOGIC_DEBATE, "수능특강 · 필연과 인과", "Making early mistakes is an inevitable step toward fluency.", "초반의 실수는 유창함으로 가는 피할 수 없는 단계이다.", "'에브'에 강세를 두고 v 마찰음을 확실히 살리세요."),
        RawWord("resilient", "/rɪˈzɪljənt/", "adj.", "회복력이 강한, 탄력 있는", 5, 3, VocabTopic.EMOTION_MIND, "수능특강 · 심리 회복력", "Resilient learners treat every tough drill as a fun game.", "회복력이 강한 학습자는 모든 힘든 훈련을 재밌는 게임으로 여긴다.", "'질'에 강세, z 진동과 L 혀끝 위치를 동시에 체크하세요."),
        RawWord("ubiquitous", "/juːˈbɪkwətəs/", "adj.", "어디에나 있는, 보편적인", 5, 3, VocabTopic.SCIENCE_AI, "수능완성 · 디지털 사회", "Contactless payments have become ubiquitous in major cities.", "비접촉 결제는 주요 도시 어디에나 보편화되었다.", "'빅'에 강세를 주고 qu를 /kw/로 입술을 모아 발음하세요."),
        RawWord("pragmatic", "/præɡˈmætɪk/", "adj.", "실용적인, 현실적인", 5, 3, VocabTopic.SOCIETY_ECONOMY, "수능특강 · 정책과 실용", "We need a pragmatic solution to reach the station on time.", "제시간에 역에 도착하려면 실용적인 해결책이 필요하다.", "'매'에 강세를 주고 입을 옆으로 넓혀 발음하세요."),
        RawWord("empirical", "/ɪmˈpɪrɪkəl/", "adj.", "경험적인, 실증적인", 5, 3, VocabTopic.SCIENCE_AI, "수능특강 · 과학 철학", "Empirical evidence proves that active recall beats passive reading.", "실증적 증거는 능동적 회상이 수동적 읽기보다 뛰어남을 증명한다.", "'피어'에 강세를 두고 r을 부드럽게 굴리세요."),
        RawWord("imperative", "/ɪmˈperətɪv/", "adj.", "필수적인, 긴급한", 5, 3, VocabTopic.TRAVEL_DAILY, "수능완성 · 국제 규범", "It is imperative to double-check your passport expiration date.", "여권 만료일을 다시 확인하는 것은 필수적이다.", "'페어'에 강세를 두고 끝 v 진동을 살려주세요."),
        RawWord("mitigate", "/ˈmɪtəɡeɪt/", "v.", "완화하다, 경감시키다", 5, 3, VocabTopic.SOCIETY_ECONOMY, "수능특강 · 기후와 위험", "Travel insurance helps mitigate unexpected medical expenses.", "여행자 보험은 예상치 못한 의료비 부담을 완화해 준다.", "첫 음절 '밋'에 강세를 주고 끝 '-게이트'를 또렷이!"),
        RawWord("paradox", "/ˈpærədɑːks/", "n.", "역설, 모순", 5, 3, VocabTopic.LOGIC_DEBATE, "수능특강 · 역설 독해", "Here is a paradox: slowing down actually makes you fluent faster.", "여기 역설이 있다. 속도를 늦추는 것이 실제로 더 빨리 유창해지게 한다.", "첫 음절 '패어'에 강세, 끝 x는 /ks/로 또렷하게!"),
        RawWord("fluctuate", "/ˈflʌktʃueɪt/", "v.", "변동하다, 오르내리다", 5, 3, VocabTopic.SOCIETY_ECONOMY, "수능완성 · 시장 경제", "Exchange rates fluctuate daily depending on global markets.", "환율은 글로벌 시장에 따라 매일 변동한다.", "f와 l을 깔끔히 붙이고 '추에이트'로 연결하세요."),
        RawWord("viable", "/ˈvaɪəbəl/", "adj.", "실행 가능한, 생존 가능한", 5, 3, VocabTopic.LOGIC_DEBATE, "수능특강 · 대안 평가", "Taking the express train is the most viable option right now.", "지금은 급행열차를 타는 것이 가장 실행 가능한 선택이다.", "첫 v 진동을 강하게 넣고 '바이-어-블' 3음절로 또렷이!"),
        RawWord("alleviate", "/əˈliːvieɪt/", "v.", "완화하다, 덜어주다", 4, 3, VocabTopic.EMOTION_MIND, "수능특강 · 스트레스 관리", "Deep breathing can alleviate anxiety before an oral interview.", "심호흡은 구술 면접 전의 불안감을 완화할 수 있다.", "'리'를 길게 빼고 v 진동 후 '에이트'로 연결하세요."),
        RawWord("ambiguity", "/ˌæmbɪˈɡjuːəti/", "n.", "모호함, 다의성", 4, 3, VocabTopic.LOGIC_DEBATE, "수능완성 · 언어와 의미", "Clear phrasing removes ambiguity in cross-cultural communication.", "명확한 표현은 이문화 소통에서 모호함을 없앤다.", "'규'에 주강세를 두고 리듬감 있게 이어 발음하세요."),
        RawWord("analogous", "/əˈnæləɡəs/", "adj.", "유사한, 비슷한", 4, 3, VocabTopic.SCIENCE_AI, "수능특강 · 과학적 비유", "Learning pronunciation is analogous to training musical pitch.", "발음을 배우는 것은 음악의 음정을 훈련하는 것과 유사하다.", "'낼'에 강세를 두고 L을 정확히 잇몸에 대세요."),
        RawWord("articulate", "/ɑːrˈtɪkjəleɪt/", "v./adj.", "분명히 표현하다, 또렷한", 4, 3, VocabTopic.SCHOOL_EXAM, "수능특강 · 의사소통", "Practice daily until you can articulate complex ideas smoothly.", "복잡한 생각도 매끄럽게 표현할 수 있을 때까지 매일 연습하라.", "'틱'에 강세를 두고 동사 끝은 '-레이트'로 마무리!"),
        RawWord("ascertain", "/ˌæsərˈteɪn/", "v.", "확인하다, 규명하다", 3, 3, VocabTopic.TRAVEL_DAILY, "수능완성 · 사실 확인", "Please ascertain the gate number before heading to security.", "보안 검색대로 가기 전에 탑승구 번호를 꼭 확인하세요.", "끝 음절 '테인'에 주강세를 실어 또렷하게!"),
        RawWord("bolster", "/ˈboʊlstər/", "v.", "강화하다, 북돋우다", 4, 3, VocabTopic.EMOTION_MIND, "수능특강 · 동기 부여", "Winning daily streaks will bolster your motivation to study.", "매일 스트릭을 이어가면 학습 동기가 한층 강화된다.", "'보울'에서 둥근 oʊ 소리와 L을 함께 살리세요."),
        RawWord("catalyst", "/ˈkætəlɪst/", "n.", "촉매, 기폭제", 4, 3, VocabTopic.SCIENCE_AI, "수능특강 · 변화의 계기", "A short solo trip became the catalyst for her love of English.", "짧은 혼자 여행이 그녀의 영어 사랑에 기폭제가 되었다.", "첫 음절 '캐'에 강세를 두고 뒤는 가볍게 흘리세요."),
        RawWord("cognizant", "/ˈkɑːɡnɪzənt/", "adj.", "인식하고 있는, 잘 아는", 3, 3, VocabTopic.TRAVEL_DAILY, "수능완성 · 글로벌 에티켓", "Global travelers should be cognizant of local etiquette rules.", "글로벌 여행객은 현지 에티켓 규칙을 잘 인식하고 있어야 한다.", "g를 부드럽게 발음하고 z 진동을 살려주세요."),
        RawWord("corroborate", "/kəˈrɑːbəreɪt/", "v.", "확증하다, 뒷받침하다", 4, 3, VocabTopic.SCIENCE_AI, "수능완성 · 연구 입증", "Recent studies corroborate the power of spaced repetition.", "최근 연구들은 간격 반복 학습의 효과를 뒷받침한다.", "'랍'에 강세를 두고 r 소리를 부드럽게 굴리세요."),
        RawWord("credibility", "/ˌkredəˈbɪləti/", "n.", "신뢰성, 신빙성", 5, 3, VocabTopic.SOCIETY_ECONOMY, "수능특강 · 언론과 신뢰", "Accurate pronunciation builds instant credibility in meetings.", "정확한 발음은 회의에서 즉각적인 신뢰성을 쌓아준다.", "'빌'에 주강세를 두고 뒤의 t를 부드럽게 넘기세요."),
        RawWord("detrimental", "/ˌdetrɪˈmentəl/", "adj.", "해로운, 불리한", 5, 3, VocabTopic.SCHOOL_EXAM, "수능특강 · 건강과 습관", "Skipping review sessions is detrimental to long-term retention.", "복습 세션을 건너뛰는 것은 장기 기억에 해롭다.", "'멘'에 주강세를 두고 리듬을 타보세요."),
        RawWord("discrepancy", "/dɪˈskrepənsi/", "n.", "불일치, 차이", 4, 3, VocabTopic.TRAVEL_DAILY, "수능완성 · 데이터 오류", "I noticed a discrepancy between the menu price and my receipt.", "메뉴 가격과 영수증 사이에 차이가 있는 것을 발견했어요.", "'크렙'에 강세를 주고 scr 자음군을 한 번에 발음하세요."),
        RawWord("disseminate", "/dɪˈseməneɪt/", "v.", "퍼뜨리다, 보급하다", 4, 3, VocabTopic.SCIENCE_AI, "수능특강 · 지식 공유", "Open platforms disseminate knowledge to learners worldwide.", "열린 플랫폼은 전 세계 학습자들에게 지식을 보급한다.", "'셈'에 강세를 두고 깔끔한 s 발음을 유지하세요."),
        RawWord("ephemeral", "/ɪˈfemərəl/", "adj.", "덧없는, 일시적인", 3, 3, VocabTopic.EMOTION_MIND, "수능완성 · 예술과 시간", "Viral trends are often ephemeral, while true skills last forever.", "유행은 종종 일시적이지만 진짜 실력은 영원히 남는다.", "ph를 /f/로 발음해 '이페머럴'로 매끄럽게!"),
        RawWord("equivocal", "/ɪˈkwɪvəkəl/", "adj.", "애매모호한, 불분명한", 3, 3, VocabTopic.LOGIC_DEBATE, "수능완성 · 논리 추론", "The agent gave an equivocal answer about our refund timeline.", "직원은 환불 일정에 대해 애매모호한 답변을 내놓았다.", "'퀴브'에 강세를 두고 v 진동을 놓치지 마세요."),
        RawWord("exacerbate", "/ɪɡˈzæsərbeɪt/", "v.", "악화시키다", 4, 3, VocabTopic.SOCIETY_ECONOMY, "수능특강 · 환경 위기", "Panicking in an emergency will only exacerbate the situation.", "비상 상황에서 당황하는 것은 상황을 악화시킬 뿐이다.", "x가 /gz/로 울려 '이그재서베이트'가 됩니다."),
        RawWord("expedite", "/ˈekspədaɪt/", "v.", "신속히 처리하다, 촉진하다", 3, 3, VocabTopic.TRAVEL_DAILY, "수능완성 · 행정 절차", "Could you expedite my luggage claim before my connecting flight?", "환승 항공편 전에 제 수하물 청구를 신속히 처리해 주실 수 있나요?", "첫 음절 '엑스'에 강세, 끝은 '다이트'로 올려주세요."),
        RawWord("formidable", "/ˈfɔːrmɪdəbəl/", "adj.", "어마어마한, 만만치 않은", 4, 3, VocabTopic.SCHOOL_EXAM, "수능특강 · 도전과 성취", "Mastering three thousand sentences is a formidable achievement.", "3천 문장을 마스터하는 것은 어마어마한 성취이다.", "첫 음절 '포'에 강세를 두고 f+r을 길게 뽑으세요."),
        RawWord("imminent", "/ˈɪmənənt/", "adj.", "임박한, 촉박한", 4, 3, VocabTopic.TRAVEL_DAILY, "수능특강 · 긴급 상황", "The boarding announcement signaled that departure was imminent.", "탑승 안내는 출발이 임박했음을 알렸다.", "첫 음절 '임'을 짧고 강하게 치고 가볍게 떨어뜨리세요."),
        RawWord("indigenous", "/ɪnˈdɪdʒənəs/", "adj.", "토착의, 고유의", 4, 3, VocabTopic.SOCIETY_ECONOMY, "수능특강 · 세계 문화", "We visited a museum celebrating indigenous art and music.", "우리는 토착 예술과 음악을 기념하는 박물관을 방문했다.", "'디지'에 강세를 두고 '인디저너스'로 부드럽게!"),
        RawWord("intricate", "/ˈɪntrɪkət/", "adj.", "복잡한, 정교한", 4, 3, VocabTopic.SCIENCE_AI, "수능완성 · 건축과 디자인", "The cathedral ceiling was covered in intricate golden carvings.", "대성당 천장은 정교한 황금 조각으로 덮여 있었다.", "첫 음절 '인'에 강세를 두고 끝은 가볍게 '-킷'!"),
        RawWord("lucrative", "/ˈluːkrətɪv/", "adj.", "수익성이 좋은", 3, 3, VocabTopic.SOCIETY_ECONOMY, "수능완성 · 미래 직업", "Bilingual communication skills open doors to lucrative careers.", "이중언어 소통 능력은 수익성 좋은 커리어의 문을 열어준다.", "'루'를 입술을 모아 길게 빼고 L을 정확히 찍으세요."),
        RawWord("nuance", "/ˈnuːɑːns/", "n.", "미묘한 차이, 뉘앙스", 4, 3, VocabTopic.EMOTION_MIND, "수능특강 · 언어 감각", "Voice coaching helps you capture every emotional nuance.", "음성 코칭은 모든 감정적 뉘앙스를 포착하게 돕는다.", "'누안스'에서 앞의 '누'를 길게 올려주세요."),
        RawWord("perpetual", "/pərˈpetʃuəl/", "adj.", "영구적인, 끊임없는", 4, 3, VocabTopic.EMOTION_MIND, "수능완성 · 성장과 호기심", "Curiosity is the fuel of perpetual personal growth.", "호기심은 끊임없는 개인 성장의 연료이다.", "'페추얼'에서 '페'에 강세를 두고 부드럽게 연결하세요."),
        RawWord("scrutinize", "/ˈskruːtənaɪz/", "v.", "면밀히 조사하다", 4, 3, VocabTopic.LOGIC_DEBATE, "수능특강 · 비판적 검증", "Customs officers sometimes scrutinize unfamiliar travel documents.", "세관 직원들은 때때로 생소한 여행 서류를 면밀히 조사한다.", "'스크루'에 강세를 두고 끝 z를 울려 마무리하세요.")
    )

    private fun buildRichExamples(r: RawWord): List<VocabExample> {
        return com.example.engine.KoreanTranslationRefinerEngine.buildNaturalVocabExamples(
            word = r.word,
            ipa = r.ipa,
            pos = r.pos,
            meaningKo = r.meaningKo,
            coreEn = r.coreEn,
            coreKo = r.coreKo
        )
    }

    private fun buildGradeWords(grade: Int, rawList: List<RawWord>): List<VocabWord> {
        val allMeanings = rawList.map { it.meaningKo }
        return rawList.mapIndexed { idx, r ->
            val distractors = allMeanings
                .filter { it != r.meaningKo }
                .shuffled()
                .take(3)
            VocabWord(
                id = "g${grade}_${r.word.lowercase()}",
                grade = grade,
                textbookUnitKo = r.unitKo,
                csatFrequency = r.csatFreq,
                csatDifficulty = r.csatDiff,
                topic = r.topic,
                word = r.word,
                ipa = r.ipa,
                partOfSpeech = r.pos,
                meaningKo = r.meaningKo,
                exampleEn = r.coreEn,
                exampleKo = r.coreKo,
                examples = buildRichExamples(r),
                pronunciationTipKo = r.tipKo,
                distractors = distractors
            )
        }
    }

    val grade1Words: List<VocabWord> by lazy { buildGradeWords(1, rawGrade1) }
    val grade2Words: List<VocabWord> by lazy { buildGradeWords(2, rawGrade2) }
    val grade3Words: List<VocabWord> by lazy { buildGradeWords(3, rawGrade3) }
    val allWords: List<VocabWord> by lazy { grade1Words + grade2Words + grade3Words }

    fun wordsForGrade(grade: Int): List<VocabWord> = when (grade) {
        0 -> allWords // 전학년 통합
        1 -> grade1Words
        2 -> grade2Words
        else -> grade3Words
    }

    /**
     * Builds a 100% NON-ALPHABETICAL deck according to evidence-based study tracks:
     * - CSAT_FREQUENCY: High frequency (5 -> 4 -> 3) with intra-tier shuffle
     * - CSAT_DIFFICULTY: Difficulty tier (1 -> 2 -> 3) with intra-tier shuffle
     * - RANDOM_MIX: Pure interleaving shuffle
     * - SMART_REVIEW: Weak/missed words first, then unmastered words shuffled
     * - TOPIC_SITUATION: Situational cluster filtered & shuffled
     */
    fun buildSmartDeck(
        grade: Int,
        track: VocabStudyTrack,
        topic: VocabTopic? = null,
        weakWordIds: Set<String> = emptySet(),
        masteredWordIds: Set<String> = emptySet(),
        difficultyBoostTier: Int = 1,
        count: Int = 30
    ): List<VocabWord> {
        val pool = wordsForGrade(grade)
        val ordered: List<VocabWord> = when (track) {
            VocabStudyTrack.CSAT_FREQUENCY -> {
                pool.groupBy { it.csatFrequency }
                    .toSortedMap(compareByDescending { it })
                    .values
                    .flatMap { it.shuffled() }
            }

            VocabStudyTrack.CSAT_DIFFICULTY -> {
                val hardFirst = difficultyBoostTier >= 3
                pool.groupBy { it.csatDifficulty }
                    .toSortedMap(if (hardFirst) compareByDescending { it } else compareBy { it })
                    .values
                    .flatMap { it.shuffled() }
            }

            VocabStudyTrack.RANDOM_MIX -> {
                pool.shuffled()
            }

            VocabStudyTrack.SMART_REVIEW -> {
                val weak = pool.filter { it.id in weakWordIds }.shuffled()
                val unmastered = pool.filter { it.id !in weakWordIds && it.id !in masteredWordIds }.shuffled()
                val mastered = pool.filter { it.id in masteredWordIds && it.id !in weakWordIds }.shuffled()
                weak + unmastered + mastered
            }

            VocabStudyTrack.TOPIC_SITUATION -> {
                val chosenTopic = topic ?: VocabTopic.SCHOOL_EXAM
                val matched = allWords.filter { it.topic == chosenTopic }.shuffled()
                val filler = pool.filter { it.topic != chosenTopic }.shuffled()
                matched + filler
            }
        }
        return ordered.take(count.coerceAtMost(ordered.size))
    }

    fun dailySpartaDeck(grade: Int, count: Int = 30): List<VocabWord> {
        return buildSmartDeck(
            grade = grade,
            track = VocabStudyTrack.CSAT_FREQUENCY,
            count = count
        )
    }
}
