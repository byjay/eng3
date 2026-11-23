package com.example.engine

import com.example.data.HumanTutorRepository
import com.example.model.AiHumanTutor
import com.example.model.LoopRepeatMode
import com.example.model.VocabStudyTrack

sealed interface VoiceSettingCommand {
    data class SetSpeechRate(val rate: Float, val replyKo: String) : VoiceSettingCommand
    data class SetSubtitlesVisible(val visible: Boolean, val replyKo: String) : VoiceSettingCommand
    data class SetHandsFree(val enabled: Boolean, val replyKo: String) : VoiceSettingCommand
    data class SetLoopMode(val mode: LoopRepeatMode, val replyKo: String) : VoiceSettingCommand
    data class SwitchTutor(val tutor: AiHumanTutor, val replyKo: String) : VoiceSettingCommand
    data class SwitchGrade(val grade: Int, val replyKo: String) : VoiceSettingCommand
    data class SwitchVocabTrack(val track: VocabStudyTrack, val replyKo: String) : VoiceSettingCommand
}

object VoiceCommandParser {

    fun parse(rawInput: String): VoiceSettingCommand? {
        val text = rawInput.trim().lowercase()
        if (text.isBlank()) return null

        // 1. Speech speed commands
        if (text.contains("천천히") || text.contains("느리게") || text.contains("속도 줄") ||
            text.contains("속도 낮") || text.contains("slower") || text.contains("slow down")
        ) {
            return VoiceSettingCommand.SetSpeechRate(
                0.78f,
                "알겠어! 지금부터 0.8배 속도로 천천히 또박또박 말해줄게!"
            )
        }
        if (text.contains("빠르게") || text.contains("빨리 말") || text.contains("속도 올") ||
            text.contains("속도 높") || text.contains("faster") || text.contains("speed up")
        ) {
            return VoiceSettingCommand.SetSpeechRate(
                1.15f,
                "좋아! 원어민 실전 속도(1.15배)로 빠르게 대화해 보자!"
            )
        }
        if (text.contains("보통 속도") || text.contains("기본 속도") || text.contains("normal speed")) {
            return VoiceSettingCommand.SetSpeechRate(
                1.0f,
                "오케이! 말하기 속도를 기본 1.0배로 맞췄어."
            )
        }

        // 2. Subtitle commands
        if ((text.contains("자막") || text.contains("해석") || text.contains("subtitle")) &&
            (text.contains("꺼") || text.contains("숨겨") || text.contains("없애") || text.contains("off") || text.contains("hide"))
        ) {
            return VoiceSettingCommand.SetSubtitlesVisible(
                false,
                "한국어 자막을 껐어! 이제 영어 귀 트이기 실전 모드야!"
            )
        }
        if ((text.contains("자막") || text.contains("해석") || text.contains("subtitle")) &&
            (text.contains("켜") || text.contains("보여") || text.contains("on") || text.contains("show"))
        ) {
            return VoiceSettingCommand.SetSubtitlesVisible(
                true,
                "라이브 자막을 다시 켰어! 화면을 보면서 편하게 따라와!"
            )
        }

        // 3. Hands-free call commands
        if ((text.contains("연속") || text.contains("핸즈프리") || text.contains("자동 통화") || text.contains("hands free")) &&
            (text.contains("꺼") || text.contains("중지") || text.contains("off"))
        ) {
            return VoiceSettingCommand.SetHandsFree(
                false,
                "연속 통화 모드를 껐어. 말하고 싶을 때 마이크 버튼을 눌러줘!"
            )
        }
        if ((text.contains("연속") || text.contains("핸즈프리") || text.contains("자동 통화") || text.contains("hands free")) &&
            (text.contains("켜") || text.contains("시작") || text.contains("on"))
        ) {
            return VoiceSettingCommand.SetHandsFree(
                true,
                "연속 회화학습 모드를 켰어! 내가 말 끝나면 바로 네 차례야!"
            )
        }

        // 4. Loop repeat listening commands
        if (text.contains("무한 반복") || text.contains("계속 반복") || text.contains("loop forever")) {
            return VoiceSettingCommand.SetLoopMode(
                LoopRepeatMode.INFINITE,
                "귀에 쏙 박히게 무한 반복 듣기를 켰어! 중지할 때 말해줘."
            )
        }
        if (text.contains("5번 반복") || text.contains("다섯 번 반복")) {
            return VoiceSettingCommand.SetLoopMode(
                LoopRepeatMode.FIVE,
                "5회 집중 반복 듣기로 설정했어!"
            )
        }
        if (text.contains("3번 반복") || text.contains("세 번 반복") || text.contains("반복 듣기 켜") || text.contains("반복 켜")) {
            return VoiceSettingCommand.SetLoopMode(
                LoopRepeatMode.THREE,
                "3회 반복 듣기를 켰어! 귀에 익을 때까지 같이 들어보자."
            )
        }
        if (text.contains("반복 꺼") || text.contains("반복 중지") || text.contains("1회만")) {
            return VoiceSettingCommand.SetLoopMode(
                LoopRepeatMode.ONCE,
                "반복 듣기를 1회 재생으로 변경했어."
            )
        }

        // 5. Switch AI Teacher by Voice
        if (text.contains("바꿔") || text.contains("불러") || text.contains("선생님") || text.contains("switch to")) {
            val matchedTutor = HumanTutorRepository.tutors.firstOrNull { t ->
                val koFirst = t.koreanNickname.substringBefore(" ").lowercase()
                text.contains(t.name.lowercase()) || text.contains(koFirst)
            }
            if (matchedTutor != null) {
                return VoiceSettingCommand.SwitchTutor(
                    matchedTutor,
                    "${matchedTutor.koreanNickname}으로 회화학습을 바로 연결할게!"
                )
            }
        }

        // 6. Switch High School Grade or Vocab Mode by Voice
        if (text.contains("고1") || text.contains("고 1") || text.contains("1학년")) {
            return VoiceSettingCommand.SwitchGrade(1, "고1 공통영어 교과 단어 트랙으로 전환했어!")
        }
        if (text.contains("고2") || text.contains("고 2") || text.contains("2학년")) {
            return VoiceSettingCommand.SwitchGrade(2, "고2 영어Ⅰ·Ⅱ 교과 단어 트랙으로 전환했어!")
        }
        if (text.contains("고3") || text.contains("고 3") || text.contains("3학년")) {
            return VoiceSettingCommand.SwitchGrade(3, "고3 수능 심화 단어 트랙으로 전환했어!")
        }
        if (text.contains("랜덤 단어") || text.contains("랜덤 학습") || text.contains("랜덤으로")) {
            return VoiceSettingCommand.SwitchVocabTrack(
                VocabStudyTrack.RANDOM_MIX,
                "ABC 순서를 완전히 뺀 완전 랜덤 학습 모드로 바꿨어!"
            )
        }
        if (text.contains("빈출") || text.contains("자주 나오는")) {
            return VoiceSettingCommand.SwitchVocabTrack(
                VocabStudyTrack.CSAT_FREQUENCY,
                "수능 최다빈출순 단어 모드로 전환했어!"
            )
        }
        if (text.contains("복습 모드") || text.contains("틀린 단어")) {
            return VoiceSettingCommand.SwitchVocabTrack(
                VocabStudyTrack.SMART_REVIEW,
                "약점·망각곡선 집중 복습 모드로 바꿨어!"
            )
        }

        return null
    }
}
