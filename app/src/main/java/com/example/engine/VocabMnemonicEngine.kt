package com.example.engine

import com.example.model.VocabWord

data class MnemonicGuide(
    val hippocampusStoryKo: String,
    val rootBreakdownKo: String,
    val phoneticHookKo: String,
    val reviewTimingKo: String
)

/**
 * Generates real evidence-based Hippocampus (해마학습법) and Word Association (단어연상법)
 * memory hooks for both curated CSAT words and dynamically harvested daily Free API words.
 */
object VocabMnemonicEngine {

    private val prefixMeanings = listOf(
        "re" to "re-(다시, 뒤로)",
        "con" to "con-(함께, 완전히)",
        "com" to "com-(함께)",
        "pro" to "pro-(앞으로, 찬성)",
        "pre" to "pre-(미리, 앞)",
        "sub" to "sub-(아래로)",
        "inter" to "inter-(사이에, 서로)",
        "trans" to "trans-(가로질러, 바꾸어)",
        "ex" to "ex-(밖으로)",
        "in" to "in-(안으로 / 반대)",
        "im" to "im-(안으로 / 반대)",
        "un" to "un-(아닌, 반대)",
        "dis" to "dis-(떨어져, 부정)",
        "de" to "de-(아래로, 제거)",
        "per" to "per-(끝까지, 통과)",
        "ad" to "ad-(~쪽으로)",
        "al" to "al-(~쪽으로)",
        "ab" to "ab-(떨어져)",
        "over" to "over-(넘어서)",
        "out" to "out-(밖으로, 능가)"
    )

    private val suffixMeanings = listOf(
        "tion" to "-tion(명사: ~함, 상태)",
        "sion" to "-sion(명사: ~함)",
        "ment" to "-ment(명사: 결과·수단)",
        "ity" to "-ity(명사: 성질)",
        "ness" to "-ness(명사: 상태)",
        "able" to "-able(형용사: ~할 수 있는)",
        "ible" to "-ible(형용사: ~할 수 있는)",
        "ive" to "-ive(형용사: ~성향의)",
        "ous" to "-ous(형용사: ~가 가득한)",
        "al" to "-al(형용사: ~의 성질)",
        "ic" to "-ic(형용사: ~적인)",
        "ate" to "-ate(동사: ~하게 만들다)",
        "ize" to "-ize(동사: ~화하다)",
        "fy" to "-fy(동사: ~하게 하다)",
        "ly" to "-ly(부사: ~하게)"
    )

    private val curatedHooks = mapOf(
        "alleviate" to Pair(
            "al(~쪽으로) + levi(가볍게, levitate 떠올리다) + ate(만들다) → 무거운 짐을 가볍게 '완화하다'",
            "발음 '얼리비에이트' → '얼리(빨리) 비워(비에이트)줘서 고통을 완화하다!'로 머릿속에 그림 그리기"
        ),
        "pragmatic" to Pair(
            "pragma(실제 행동·실무) + tic(형용사) → 이론보다 실제 행동을 중시하는 '실용적인'",
            "발음 '프래그매틱' → '프로그램(프래그)을 매틱(자동)으로 돌리는 엄청 실용적인 사람!' 장면 연상"
        ),
        "ubiquitous" to Pair(
            "ubi(어디나) + quit(존재) + ous(가득한) → 어디에나 존재하는",
            "발음 '유비쿼터스' → '유비(삼국지)가 쿼터(사방)에 다 있어서 어디에나 존재하는!' 코믹 장면"
        ),
        "resilient" to Pair(
            "re(다시) + sili(튀어 오르다) + ent → 꺾여도 다시 튀어 오르는 '회복력 있는'",
            "발음 '리질리언트' → '리(다시) 질기게(질리) 튕겨 오르는 고무공 같은 회복력!'"
        ),
        "ambiguous" to Pair(
            "ambi(양쪽) + ig(몰아가다) + ous → 양쪽 길로 헷갈리게 하는 '모호한'",
            "발음 '앰비규어스' → '양쪽(앰비) 규정이 애매해서 모호한 상황!'"
        ),
        "corroborate" to Pair(
            "cor(함께) + robor(단단한 힘, robust) + ate → 증거를 합쳐 단단하게 '확증하다'",
            "발음 '커로버레이트' → '거대 로봇(로버)이 증거를 쾅 찍어 확증하다!'"
        ),
        "discrepancy" to Pair(
            "dis(떨어져) + crep(소리 내다) + ancy → 서로 다른 소리를 내는 '불일치, 차이'",
            "발음 '디스크레펀시' → '디스크가 삐걱(크레) 어긋나서 생긴 불일치!'"
        ),
        "fluctuate" to Pair(
            "fluctu(파도·물결) + ate → 파도처럼 위아래로 '변동하다, 오르내리다'",
            "발음 '플럭추에이트' → '파도가 철썩(플럭) 춤추듯(추에이트) 오르내리는 그래프!'"
        ),
        "inevitable" to Pair(
            "in(아닌) + e(밖으로) + vit(피하다) + able → 밖으로 피할 수 없는 '피할 수 없는, 필연적인'",
            "발음 '이네비터블' → '내비게이션(네비)으로도 못 피해 가는 피할 수 없는 길!'"
        ),
        "sustainable" to Pair(
            "sus(아래서) + tain(붙잡다) + able → 아래서 계속 받쳐줄 수 있는 '지속 가능한'",
            "발음 '서스테이너블' → '계속(서스) 지탱(테인)할 수 있는 초록 지구!'"
        )
    )

    fun buildGuide(word: VocabWord): MnemonicGuide {
        val lower = word.word.lowercase().trim()
        val curated = curatedHooks[lower]

        val rootBreakdown = curated?.first ?: buildDynamicRootAnalysis(lower, word.meaningKo)
        val phoneticHook = curated?.second ?: buildDynamicPhoneticHook(word.word, word.ipa, word.meaningKo)

        val sceneLocation = when (word.topic.name) {
            "SCHOOL_EXAM" -> "교실 칠판 앞에서 시험지 괴물을 물리치는 장면"
            "TRAVEL_DAILY" -> "해외 공항·마트에서 원어민 친구와 웃으며 대화하는 장면"
            "SCIENCE_AI" -> "미래 우주 연구실에서 로봇이 반짝이는 장면"
            "SOCIETY_ECONOMY" -> "뉴스 앵커가 대형 스크린 앞에서 발표하는 장면"
            "EMOTION_MIND" -> "마음속 날씨(햇살·구름)가 휙 바뀌는 드라마 장면"
            else -> "토론 무대에서 마이크를 잡고 멋지게 반박하는 장면"
        }

        val hippocampusStory =
            "해마 장면 각인: [$sceneLocation]을 눈감고 3초 떠올리며 '${word.word}'(${word.meaningKo})를 소리 내어 2번 말해보세요! 감각+감정이 붙으면 해마가 장기기억으로 보냅니다."

        val reviewTiming = when (word.csatDifficulty) {
            3 -> "해마 골든타임 복습: 오늘 10분 뒤 → 내일 아침 → 3일 뒤 → 7일 뒤 (1등급 킬러 집중)"
            2 -> "해마 골든타임 복습: 오늘 자기 전 → 2일 뒤 → 7일 뒤 (수능 핵심 고정)"
            else -> "해마 골든타임 복습: 내일 1회 빠른 회상으로 장기기억 완료"
        }

        return MnemonicGuide(
            hippocampusStoryKo = hippocampusStory,
            rootBreakdownKo = rootBreakdown,
            phoneticHookKo = phoneticHook,
            reviewTimingKo = reviewTiming
        )
    }

    private fun buildDynamicRootAnalysis(lower: String, meaningKo: String): String {
        val matchedPrefix = prefixMeanings.firstOrNull { (p, _) ->
            lower.startsWith(p) && lower.length > p.length + 2
        }
        val matchedSuffix = suffixMeanings.firstOrNull { (s, _) ->
            lower.endsWith(s) && lower.length > s.length + 2
        }
        val stemStart = matchedPrefix?.first?.length ?: 0
        val stemEnd = (lower.length - (matchedSuffix?.first?.length ?: 0)).coerceAtLeast(stemStart + 1)
        val stem = lower.substring(stemStart, stemEnd)

        return buildString {
            append("어근 분해: ")
            if (matchedPrefix != null) append("${matchedPrefix.second} + ")
            append("[$stem]")
            if (matchedSuffix != null) append(" + ${matchedSuffix.second}")
            append(" → 핵심 이미지 '$meaningKo'")
        }
    }

    private fun buildDynamicPhoneticHook(word: String, ipa: String, meaningKo: String): String {
        val syllables = word.chunked(3).joinToString("-")
        return "소리·이미지 연상: '$word' ($syllables, $ipa)의 첫소리 리듬을 타며 '$meaningKo' 동작을 손짓으로 한 번 따라 하면 기억력이 2배 올라갑니다!"
    }
}
