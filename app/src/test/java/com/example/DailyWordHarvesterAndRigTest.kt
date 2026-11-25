package com.example

import com.example.data.DailyWordHarvesterService
import com.example.data.SynonymUnionFind
import com.example.model.VocabTopic
import com.example.model.VocabWord
import com.example.ui.components.HumanRigGestureCatalog
import com.example.ui.components.RigEmotionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWordHarvesterAndRigTest {

    @Test
    fun unionFind_mergesTransitiveSynonyms_andLinksAntonymGroups() {
        val uf = SynonymUnionFind()
        uf.union("crucial", "vital")
        uf.union("vital", "essential")
        uf.add("trivial")
        uf.union("trivial", "minor")

        val (wordToGroup, clusters) = uf.buildClusters(
            antonymPairs = listOf("crucial" to "trivial")
        )

        val crucialGroup = wordToGroup["crucial"]!!
        val vitalGroup = wordToGroup["vital"]!!
        val essentialGroup = wordToGroup["essential"]!!
        val trivialGroup = wordToGroup["trivial"]!!

        assertTrue(crucialGroup.startsWith("SYN-"))
        assertEquals(crucialGroup, vitalGroup)
        assertEquals(crucialGroup, essentialGroup)
        assertNotEquals(crucialGroup, trivialGroup)

        val crucialCluster = clusters.first { it.groupId == crucialGroup }
        val trivialCluster = clusters.first { it.groupId == trivialGroup }
        assertTrue(trivialGroup in crucialCluster.antonymGroupIds)
        assertTrue(crucialGroup in trivialCluster.antonymGroupIds)
    }

    @Test
    fun unionFind_normalizesAndFiltersInvalidWords() {
        assertEquals("resilient", SynonymUnionFind.normalizeWord("  Resilient "))
        assertEquals("", SynonymUnionFind.normalizeWord("cat")) // < 4 chars
        assertEquals("", SynonymUnionFind.normalizeWord("hello-world")) // hyphen
        assertEquals("", SynonymUnionFind.normalizeWord("word123")) // digits
    }

    @Test
    fun parseMyMemoryTranslation_extractsCleanKorean_andRejectsWarnings() {
        val validJson = """
            {"responseData":{"translatedText":"필수적인, 결정적인"},"responseStatus":200}
        """.trimIndent()
        assertEquals("필수적인, 결정적인", DailyWordHarvesterService.parseMyMemoryTranslation(validJson))

        val warningJson = """
            {"responseData":{"translatedText":"MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY"}}
        """.trimIndent()
        assertEquals("", DailyWordHarvesterService.parseMyMemoryTranslation(warningJson))
        assertEquals("", DailyWordHarvesterService.parseMyMemoryTranslation("{broken json"))
    }

    @Test
    fun parseR2LatestJson_parsesValidWords_deduplicatesExisting_andAssignsClusters() {
        val r2Payload = """
            {
              "words": [
                {
                  "id": "r2_crucial",
                  "word": "crucial",
                  "ipa": "/ˈkruː.ʃəl/",
                  "partOfSpeech": "adj.",
                  "meaningKo": "결정적인, 중대한",
                  "grade": 2,
                  "csatFrequency": 5,
                  "topic": "SCHOOL_EXAM",
                  "examples": [
                    {"labelKo": "수능 실전", "english": "Clear timing is crucial.", "korean": "정확한 타이밍이 결정적이다."}
                  ],
                  "synonyms": ["vital", "essential"],
                  "antonyms": ["trivial"]
                },
                {
                  "id": "r2_vital",
                  "word": "vital",
                  "partOfSpeech": "adj.",
                  "meaningKo": "필수적인",
                  "grade": 2,
                  "topic": "SCHOOL_EXAM",
                  "synonyms": ["crucial"],
                  "antonyms": ["minor"]
                },
                {
                  "id": "r2_existing",
                  "word": "already",
                  "meaningKo": "이미"
                }
              ]
            }
        """.trimIndent()

        val parsed = DailyWordHarvesterService.parseR2LatestJson(
            rawJson = r2Payload,
            existingWordLowerSet = setOf("already")
        )
        assertEquals(2, parsed.size)
        assertFalse(parsed.any { it.word == "already" })

        val (clustered, clusters) = DailyWordHarvesterService.applyUnionFindClustering(parsed)
        val crucial = clustered.first { it.word == "crucial" }
        val vital = clustered.first { it.word == "vital" }
        assertTrue(crucial.synonymGroupId.startsWith("SYN-"))
        assertEquals(crucial.synonymGroupId, vital.synonymGroupId)
        assertTrue(clusters.isNotEmpty())
        assertEquals("결정적인, 중대한", crucial.meaningKo)
        assertEquals("Clear timing is crucial.", crucial.exampleEn)

        // Malformed payload fails safely to empty list
        assertTrue(DailyWordHarvesterService.parseR2LatestJson("not-json", emptySet()).isEmpty())
    }

    @Test
    fun replenishQueueIfLow_fillsBelow50_andSkipsRegisteredWords() {
        val currentQueue = listOf("alpha", "bravo", "charlie")
        val existing = setOf("alpha", "existingword")
        val discovered = listOf("existingword", "delta", "echo", "foxtrot", "bravo", "bad!")

        val replenished = DailyWordHarvesterService.replenishQueueIfLow(
            currentQueue = currentQueue,
            existingWords = existing,
            discoveredRelatedWords = discovered,
            minThreshold = 50
        )

        assertFalse("alpha" in replenished)
        assertFalse("existingword" in replenished)
        assertEquals(listOf("bravo", "charlie", "delta", "echo", "foxtrot"), replenished)
    }

    @Test
    fun humanRigGestureCatalog_hasAll33UniqueMotionsGroupedInto4EmotionCategories() {
        val gestures = HumanRigGestureCatalog.gestures
        assertEquals(33, gestures.size)

        val expectedIds = (1..33).map { "g-%02d".format(it) }
        assertEquals(expectedIds, gestures.map { it.id })

        val map = HumanRigGestureCatalog.emotionCategoryMap
        assertEquals(4, map.size)
        assertTrue(map[RigEmotionCategory.JOY]!!.isNotEmpty())
        assertTrue(map[RigEmotionCategory.THINKING]!!.isNotEmpty())
        assertTrue(map[RigEmotionCategory.SURPRISE]!!.isNotEmpty())
        assertTrue(map[RigEmotionCategory.ENCOURAGE]!!.isNotEmpty())
        assertEquals(33, map.values.sumOf { it.size })

        assertEquals(RigEmotionCategory.JOY, HumanRigGestureCatalog.selectBestGestureForEmotion("기쁨").category)
        assertEquals(RigEmotionCategory.THINKING, HumanRigGestureCatalog.selectBestGestureForEmotion("호기심").category)
        assertEquals(RigEmotionCategory.THINKING, HumanRigGestureCatalog.selectBestGestureForEmotion("차분함").category)
        assertEquals(RigEmotionCategory.SURPRISE, HumanRigGestureCatalog.selectBestGestureForEmotion("놀람").category)
        assertEquals(RigEmotionCategory.ENCOURAGE, HumanRigGestureCatalog.selectBestGestureForEmotion("응원").category)
    }

    @Test
    fun humanRigGestureCatalog_buildsMultiSegmentSequenceQueueForShiftingEmotions() {
        val utterance = "Wow, really?! Why did that happen? Great job, you nailed it!"
        val queue = HumanRigGestureCatalog.buildGestureSequenceQueue(
            utterance = utterance,
            defaultEmotionTag = "차분함",
            speechRate = 1.0f
        )

        assertEquals(3, queue.size)
        assertEquals("놀람", queue[0].categoryKo)
        assertEquals("생각함", queue[1].categoryKo)
        assertEquals("응원", queue[2].categoryKo)
        assertTrue(queue.all { it.gestureId.startsWith("g-") && it.durationMs in 900..2600 })
    }
}
