# 계약노트 코드

K-AWARDS 2026 출품 프로토타입. 대회 정보와 단계별 기록은 `~/Library/CloudStorage/Nextcloud-nextcloud.doomfan.win-admin/대학/공모전/2026 K-AWARDS DIGITAL EXCELLENCE AWARDS/`의 `CLAUDE.md`, `개발기록.md`에 있다. **의미 있는 변경은 끝날 때마다 `개발기록.md`와 메모리에 기록한다.**

## 명령

```bash
docker compose up -d                                   # DB (포트 5433)
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run   # 8080
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test              # 단위 테스트
cd frontend && npm run dev                             # 5173
cd frontend && npx tsc -b                              # 타입 검사
python3 eval/build_cases.py                            # 시험 자료 전체 생성 (Edge 필요). 묶음 하나만: dev|holdout|complex|final
python3 eval/run_eval.py dev                           # 실제 서버로 채점. AI를 호출하므로 무료 사용량을 쓴다
python3 eval/gen_complex.py                            # 복잡한 시나리오 50건 생성 (s_tune 30, s_final 20). 파이프로 자르지 말 것
python3 eval/run_eval.py s_tune --resume               # 끝난 건을 건너뛰고 이어서 채점
python3 eval/robustness.py                             # 잘못된 입력 시험
python3 eval/multi_contract.py                         # 여러 고객 섞임 시험
python samples/demo/play_demo.py prepare|run|ask      # 시연 진행. Playwright와 --remote-debugging-port=9333 Edge 필요. prepare는 저장된 계약을 모두 지움
```

Claude로 켜기: `./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.gemini.api-key= --app.claude.model=claude-sonnet-5-5"`. 시연 영상은 이 설정으로 찍었고, 기본 설정은 Gemini다.

## 지켜야 할 것

- **날짜와 금액은 AI가 아니라 `PaymentCalculator`가 계산한다.** AI에는 규칙(`PaymentRule`)만 뽑게 한다.
- AI가 낸 근거는 `EvidenceChecker`로 원문과 대조한다.
- 응답 형식을 바꾸면 `ExtractionResult`와 `GeminiSchemas`를 함께 고친다. 지시문은 `AiPrompts`에 있다.
- DB 변경은 `db/migration`에 새 파일을 추가한다. 기존 마이그레이션은 고치지 않는다.
- AI 호출은 트랜잭션 밖에서 한다.
- `.env`는 읽거나 출력하지 않고 저장소에 올리지 않는다. 시험과 시연에는 가상 자료만 쓴다.
- `eval/results_final_first_run.json`과 `results_holdout_first_run.json`은 처음 본 시험의 기록이므로 덮어쓰지 않는다. 새 수치를 대표로 내려면 처음 보는 시험 자료를 새로 만든다.
- `eval/results_s_tune_first_run.json`도 덮어쓰지 않는다. `s_final`은 수정을 마친 뒤 한 번만 돌리고, 끝나면 바로 `results_s_final_first_run.json`으로 복사한다.
- 시험을 이어 하기 전에 서버가 켜져 있는지 확인한다(위 명령 참고, Docker Desktop 필요). 채점 도구 실행 위치는 이 폴더다.
- GitHub 저장소 `dngur521/contract-note`(공개). 비밀 값과 개인정보를 올리지 않는다. `eval/forms`는 재배포 허용을 확인하지 못해 저장소에서 뺐고 로컬에만 있다. 커밋은 요청이 있을 때만 하고, 푸시와 저장소 작업은 Claude 권한 설정에서 막힐 수 있으니 거부되면 사용자에게 `!` 명령을 안내한다.
- Gemini 무료 한도 응답은 이 모델 기준 하루 500회다(막히는 시점은 일정하지 않았다). 채점 한 건에 약 3회 쓴다. 촬영 직전에는 시험을 돌리지 않는다.
- 기본 모델이 한도(429)에 걸리면 `app.gemini.fallback-model`(`gemini-3.5-flash`, 하루 20회·분당 5회로 표기됨)로 넘어간다. 채점 수치는 한 모델로 재야 하므로 시험할 때는 서버를 `-Dspring-boot.run.arguments=--app.gemini.fallback-model=`로 켠다.
- zsh에서 URL에 `$변수:` 를 쓰면 수정자로 해석된다. `${변수}:` 로 쓴다.
