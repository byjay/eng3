# 수능 영단어 Cloudflare R2 분산 누적 백엔드 작업지시서 (`suneung_vocab_backend_spec.md`)

## 1. 아키텍처 개요
- **무제한 500단어 R2 샤딩**: 단일 대용량 JSON 대신 `shards/shard-0001.json`, `shard-0002.json` ... 형태로 500단어 단위 분할 저장해 Cloudflare Workers 메모리·읽기 비용을 최소화합니다.
- **하루 5회 비정형 Cron + 랜덤 Jitter**: `wrangler.toml`에 5개 비정형 Cron(`17 0`, `43 4`, `11 9`, `29 14`, `52 19` UTC)을 등록하고 회당 10단어 × 5회 = **일 50단어**를 랜덤 지연(260~820ms)과 함께 수집해 무료 API Rate Limit(HTTP 429)을 회피합니다.
- **무료 API 3종 교차 수집**:
  1. **Free Dictionary API**: IPA 발음기호, 품사, 원어민 실전 예문, 1차 동의어/반의어.
  2. **Datamuse API**: `rel_syn`(동의어), `rel_ant`(반의어), `md=dfp`(코퍼스 빈도/정의), 주제별 대기열 확장.
  3. **MyMemory Translation API**: 영어 정의·실전 예문의 자연스러운 한국어 뜻/해석 번역.
- **Union-Find 동의어·반의어 자동 클러스터링**: 수집된 동의어 연결을 결정론적 Union-Find로 병합해 `SYN-0001`, `SYN-0002` 클러스터를 만들고, 반의어 클러스터 간 `antonymGroupIds`를 자동 대립 연결합니다.
- **자동 대기열 보충 (`queue.json < 50`)**: 남은 큐가 50개 미만으로 떨어지면 직전 수집 단어의 미등록 동의어·반의어를 우선 투입하고, 부족분은 Datamuse 수능 주제 쿼리로 자동 보충합니다.

## 2. R2 스토리지 키 구조
- `meta.json` — 누적 단어 수, 현재 샤드 번호, 총 동의어 그룹 수, 마지막 Cron 실행 시각.
- `shards/shard-0001.json` ~ `shard-NNNN.json` — 샤드당 최대 500단어 배열.
- `groups/synonym-groups.json` — Union-Find `SYN-xxxx` 클러스터 및 `antonymGroupIds`.
- `index/word-set.json` — 전체 등록 단어 중복 차단 인덱스.
- `queue.json` — 대기 단어 큐(50개 미만 시 자동 보충).

## 3. HTTP API 명세
- `GET /api/vocab/meta` — 전체 누적 현황 조회.
- `GET /api/vocab/latest?limit=50` — 최신 수집 단어 50개 조회(Android 앱 우선 동기화 엔드포인트).
- `GET /api/vocab/shard/:id` — 특정 500단어 샤드 조회.
- `GET /api/vocab/groups` — `SYN-xxxx` 동의어·반의어 클러스터 조회.
- `POST /api/vocab/bootstrap` — 초기 수동 배치 실행(선택적 `ADMIN_TOKEN` Bearer 인증).
- `POST /api/levels` — 앱 학습 레벨 동기화(기존 호환).

## 4. 배포 절차
```bash
cd cloudflare-r2-vocab-worker
npx wrangler login
npx wrangler r2 bucket create suneung-vocab-r2
# 선택: 관리자 수동 트리거 보호 토큰 등록
npx wrangler secret put ADMIN_TOKEN
npx wrangler deploy
```
배포 후 발급된 Worker URL(`https://suneung-vocab-r2-worker.<subdomain>.workers.dev`)을 **AI Studio Secrets 패널의 `CLOUDFLARE_WORKER_URL`**에 등록하면 Android 앱이 자동으로 R2 최신 샤드를 동기화합니다. (미등록 시 앱 내 3종 무료 API 엔진으로 자동 폴백됩니다.)
