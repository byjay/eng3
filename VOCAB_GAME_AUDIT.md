# VOCAB_GAME_AUDIT.md — v2 마스터 지시서 구조 감사 및 P0–P8 100% 완료 보고

## 1. 기준선 확인 & 핵심 UI 수정 사항
- **단어 학습 UI**: `SpartaVocabScreen.kt`, `VocabMemorizationDeckView.kt`, `VocabAdventureHubSection.kt`, `VisualMnemonicAndChunkCard.kt`, `WordSceneIllustration.kt`, `MediaAuditAndAnalyticsSheet.kt`
- **원어민 회화 캐릭터 리깅 수정 (`SvgCssLipSyncRigView.kt`)**: 가상 GPU/에뮬레이터에서 `LAYER_TYPE_HARDWARE` 투명 `WebView`가 검은 화면으로 렌더링되던 문제를 제거하고 **100% 네이티브 Jetpack Compose 60fps `Canvas` 캐릭터 리깅**으로 전면 교체(눈 깜빡임, 33종 감정·제스처, 실시간 립싱크, 음성 파동 링).
- **단어별 의미 매칭 일러스트 전면 교체 (`WordSceneIllustration.kt` + `VocabMnemonicEngine.kt`)**: 주제별 4장 스톡 사진 재활용(예: `crucial`에 달리는 여성 그림 중복 표시)을 제거하고 **24종 의미 기반 시맨틱 벡터 장면**(`crucial` → 황금 아치 쐐기돌 `KEYSTONE_PIVOT`, `fluctuate` → 오르내리는 파도 `RISING_WAVES`, `ambiguous` → 안개 낀 갈림길 `FORKED_CROSSROADS` 등) + 4종 아트 팔레트(`에디토리얼 / 미드나잇 / 파스텔 / 블루프린트`)로 전면 교체, 하단 중복 이미지 제거.
- **데이터 & 보상 저장소**: `DadKingdomLocalStore.kt`, `SpeakQuestViewModel.kt`, `VocabAdventureEngine.kt`, `EnGameEngine.kt`.

## 2. 데이터 보존 및 단일 지갑 통합 설계
- **기존 DataStore 키 100% 보존**: `total_xp`, `coins`, `streak_count`, `mastered_words_json`, `weak_words_json`, `harvested_api_words_json`, `cleared_rpg_chapters_json` 등 기존 키 무변경.
- **단일 통합 지갑(Unified Wallet)**: `grantIdempotentReward`가 기존 `total_xp`·`coins`에 원자적 반영, `RewardLedgerEngine.spendCoins` 음수 잔액 차단, `idempotencyKey`(`eventId`)로 중복 지급 원천 차단.
- **추가 전용(Additive) 신규 키**: `word_sm2_progress_json`, `reward_ledger_json`, `reward_event_ids_json`, `vocab_missions_json`, `memory_palace_json`, `media_review_overrides_json`.

## 3. Section 16 & Section 18 (P0–P8) 최종 완료 체크리스트
- [x] **P0. 현황 감사 & 기준선 보호**: 기존 5개 탭·학습 데이터·오답 노트·발음 평가·TTS·RPG 챕터·R2 단어 수집 기능 100% 유지.
- [x] **P1. 이미지 중심 카드 & 3D 플립 + 4모드 + 철자 직접 회상**:
  - `VocabMemorizationDeckView.kt`: `WordSceneIllustration` 기반 `215.dp` 풀블리드 의미 일러스트, 3D Y축 뒤집기, 셔플·뒤집기·아트(4종 팔레트), 학습/회상/듣기/문장(Cloze) 4모드, 인라인 철자 입력(`TypedRecallEvaluator`: 5자 이상 1오타 허용), 단어 숨김/제외(`SUSPENDED`), 300ms 중복 탭 방지, 일일 목표 동적 반영(`5/10/15/20`).
- [x] **P2. 검증 어근(45+ 확장)·장면 연상 정교화 & 과장 표현 제거**:
  - `VocabMnemonicEngine.kt`: 45+ 핵심 수능 단어 검증 어근, 미검증 단어 정직한 `"어근 분석 자료 없음"` 표기, 전 화면 과장 `"해마"` 문구 제거 완료.
- [x] **P3. SM-2 간격 반복 복습 & 4지표 큐 + 단어 숨김(`SUSPENDED`)**:
  - `Sm2ReviewScheduler`: `AGAIN / HARD / GOOD / EASY` 간격·난이도·숙련도 계산, `toggleSuspended`, `ReviewQueueSnapshot`(지금 복습 · 오늘 예정 · 기한 초과 · 취약 단어) 및 오늘 복습 큐 모드.
- [x] **P4. 통합 지갑 & 멱등성 보상 원장 + 7단계 레벨 + 업적 배지 + 비현금 안내**:
  - `RewardConfig`, `LevelConfig`(Lv.1~Lv.7), `RewardLedgerEngine`(중복 지급 차단 + 음수 방지), `AchievementBadgeEngine`(6종 배지), `QuestCompleteCelebrationDialog`, `YourJourneySheetContent` 내 최근 원장 내역 및 비현금 정책 안내.
- [x] **P5. 규칙 기반 일일·주간·테마 탐험 퀘스트 + 날짜 롤오버 + 2D 기억의 궁전**:
  - `DailyMissionEngine.rolloverMissions`: 날짜 변경 시 일일 퀘스트만 초기화하고 주간(`WEEKLY`)·테마 탐험(`EXPLORATION` 3종) 진행도 보존, 사용자 일일 목표(`5/10/15/20`) 동적 동기화, `COMPLETED → CLAIMED` 분리.
- [x] **P6. 8종 단어·기억 미니게임 (`EnGame 2.0`) + 결과 화면 취약 단어 바로 복습 CTA**:
  - `SPEED_BLITZ`, `SENTENCE_BUILDER`, `SYNONYM_LINK`, `VOICE_BOSS_RUSH`, `SPELLING_TILES`, `PICTURE_MATCH`(`WordSceneIllustration` 의미 일러스트 적용), `MEMORY_FLIP`, `CLOZE_CONTEXT` + 결과 화면 `"취약 단어 바로 복습하기"` 버튼.
- [x] **P7. 인터랙티브 이미지·연상 수동 검수 & Section 13 학습 분석 대시보드**:
  - `MediaAuditAndAnalyticsSheet.kt`: 단어별 일러스트·어근·라이선스 확인 및 **승인/보류** 영구 저장(DataStore) + `VocabAnalyticsTracker.summary()` 실시간 퍼널 지표 + 최근 보상 원장 내역.
- [x] **P8. 컴파일 & 단위 테스트 검증**:
  - `compile_applet` 및 `gradle :app:testDebugUnitTest` 전체 통과 확인.
