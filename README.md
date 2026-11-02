# 계약노트

프리랜서가 계약 기록(견적서, 카카오톡 캡처, 메일)을 올리면 AI가 계약 조건을 정리하고, 지급일을 챙겨 주고, 질문에 원문 근거와 함께 답하는 서비스입니다. K-AWARDS 2026 출품용 프로토타입입니다.

## 실행

프로젝트 맨 위에 `.env` 파일을 만들고 키를 한 줄 적습니다. 이 파일은 저장소에 올라가지 않습니다. 키가 없으면 샘플 AI로 동작합니다.

```
GEMINI_API_KEY=발급받은키
```

Docker Desktop을 켠 뒤 터미널 3개에서 실행합니다. 처음 질문하거나 기록을 올릴 때 임베딩 모델(약 135MB)을 내려받습니다.

```bash
# 1. 데이터베이스
docker compose up -d

# 2. 백엔드 (http://localhost:8080)
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw spring-boot:run

# 3. 프론트엔드 (http://localhost:5173)
cd frontend
npm run dev
```

시연용 가상 자료는 `samples/`에 있습니다.

## 구조

| 위치 | 하는 일 |
|---|---|
| `frontend/` | React 화면: 일정, 계약 확인, 기록 올리기, 질문하기 |
| `backend/.../contract` | 계약 저장·수정·일정 API. 날짜와 금액은 `PaymentCalculator`가 계산 |
| `backend/.../ai` | AI 호출. Gemini 또는 Claude, 키가 없으면 `SampleContractAi` |
| `backend/.../rag` | 문장 나누기, 임베딩, 검색, 질문 답변 |
| PostgreSQL + pgvector | 계약 데이터와 문장 벡터를 함께 저장 |

## 역할 분담

- **AI(Gemini 또는 Claude):** 문서 읽기, 조건 추출, 찾은 근거 안에서 답하기
- **코드:** 지급일 계산, 3.3% 원천징수 계산, 일정 정렬
- **사용자:** AI가 정리한 내용을 확인해야 확정됨

## 질문 답변(RAG) 흐름

1. 기록을 줄 단위로 나눠 벡터로 바꾼 뒤 저장합니다. 임베딩은 공개 모델(paraphrase-multilingual-MiniLM-L12-v2)을 이 컴퓨터에서 직접 돌립니다.
2. 질문도 벡터로 바꿔 가까운 문장을 찾고, 질문의 낱말이 든 문장에 점수를 더합니다(`HybridRanker`). 질문에 고객 이름이 있으면 그 고객의 기록만 봅니다. 기록이 200문장 이하면 전부 읽습니다.
3. 찾은 문장의 앞뒤 문장과 코드가 계산한 계약 카드를 AI에 넘겨 그 안에서만 답하게 하고, 사용한 문장을 화면에 표시합니다.

## 시험

`eval/`에 공개 표준계약서 양식 기반의 가상 시험 자료와 자동 채점기가 있습니다. 명령은 `CLAUDE.md`를 보세요. 처음 본 최종 시험 8건의 결과는 항목 일치 98.3%(58/59)이고, 가상 자료 기준이라 실제 환경의 정확도는 아닙니다.

## 주의

Gemini 무료 등급에 보낸 내용은 Google이 제품 개선에 쓸 수 있고 검토자가 읽을 수 있습니다. 가상 자료만 올립니다. 실제 계약 자료를 다루는 서비스라면 유료 등급을 써야 합니다.

## 시험 자료

`eval/forms/`의 표준계약서 원문은 저장소에 포함하지 않습니다. 시험 자료를 다시 만들려면 공개된 표준계약서 양식을 직접 받아 이 폴더에 넣어야 합니다(`common_standard.txt`, `sw_standard.txt`, `visual_design_standard.txt`, `multimedia_design_standard.txt`).
