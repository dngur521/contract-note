"""촬영용 가상 계약 파일을 만든다. 실행: python3 samples/demo/build_demo.py (Edge 필요)"""
import subprocess
from pathlib import Path

HERE = Path(__file__).parent
EDGE = "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
CLIENT, ME = "별빛로스터스 (가상)", "정하나"

QUOTE = f"""<!doctype html><html lang="ko"><head><meta charset="utf-8"><style>
@page{{size:A4;margin:22mm}}body{{font-family:"Apple SD Gothic Neo",sans-serif;color:#222;font-size:11pt;line-height:1.7}}
h1{{font-size:22pt;letter-spacing:.3em;text-align:center;margin:0 0 6px}}.sub{{text-align:center;color:#777;margin-bottom:28px}}
table{{width:100%;border-collapse:collapse;margin:14px 0}}th,td{{border:1px solid #bbb;padding:8px 10px;text-align:left}}th{{background:#f2f2f2;width:28%}}
td.num{{text-align:right}}.foot{{margin-top:28px;color:#555}}
</style></head><body>
<h1>견 적 서</h1><div class="sub">가상 자료 · 시연용</div>
<p>작성일: 2026-09-21<br>의뢰인: {CLIENT} (담당 윤담당)<br>프로젝트: 원두 브랜드 로고·패키지 디자인</p>
<table><tr><th>품목</th><th>내용</th><th>금액</th></tr>
<tr><td>로고 디자인</td><td>로고 1종, 응용형 2종</td><td class="num">1,500,000원</td></tr>
<tr><td>패키지 디자인</td><td>원두 봉투 3종</td><td class="num">2,000,000원</td></tr>
<tr><th>합계</th><td></td><td class="num"><b>3,500,000원</b></td></tr></table>
<table>
<tr><th>계약금</th><td>1,050,000원 (계약 후 3일 내)</td></tr>
<tr><th>중도금</th><td>1,050,000원 (시안 확정 시)</td></tr>
<tr><th>잔금</th><td>1,400,000원 (납품 후 14일 내)</td></tr>
<tr><th>납품 예정일</th><td>2026-10-30</td></tr>
<tr><th>수정</th><td>수정 3회 포함, 초과 시 회당 100,000원</td></tr>
<tr><th>저작권</th><td>완성 디자인의 저작권은 잔금 지급 후 의뢰인에게 넘어갑니다.</td></tr>
<tr><th>세금</th><td>3.3% 원천징수 후 지급</td></tr></table>
<p class="foot">프리랜서 디자이너 {ME} (가상 인물)</p>
</body></html>"""

MAIL = f"""보낸사람: 윤담당 <yoon@byeolbit.example>
날짜: 2026-09-24 (목)
제목: 견적 관련 확인

견적서 잘 받았습니다. 내일(9월 25일)을 계약일로 하고 진행하면 좋겠습니다.
"시안 확정 시"는 날짜가 모호해서, 중도금은 10월 16일에 드리는 걸로 정하면 어떨까요?

보낸사람: {ME} <hana@example.com>
날짜: 2026-09-24 (목)
제목: RE: 견적 관련 확인

네, 계약일은 9월 25일로 하고 중도금은 10월 16일로 하겠습니다.
나머지 조건은 견적서대로 진행하겠습니다.
"""

MSGS = [
    ("them", "오전 10:12", "대표님이 메뉴판 디자인도 같이 맡기고 싶어 하세요. 가능할까요?"),
    ("me", "오전 10:20", "가능합니다. 메뉴판은 50만원이고 납품은 2주 뒤로 미뤄야 해요."),
    ("them", "오전 10:31", "납품을 11월 6일까지로 하면 안 될까요?"),
    ("me", "오전 10:35", "그 일정은 어렵습니다. 11월 13일이면 됩니다."),
    ("them", "오전 10:42", "그럼 11월 13일로 해요. 50만원은 잔금에 얹어 드릴게요."),
    ("them", "오전 10:43", "대신 수정은 3번 말고 2번까지만 해도 될까요? 잔금은 납품 후 7일 내에 드릴게요."),
    ("me", "오전 10:50", "네 좋습니다. 수정 2번, 잔금은 납품 후 7일 내로 할게요."),
]
bubbles = "".join(
    f'<div class="row me"><span class="t">{t}</span><div class="b">{m}</div></div>' if who == "me"
    else f'<div class="n">별빛로스터스 윤담당</div><div class="row"><div class="b">{m}</div><span class="t">{t}</span></div>'
    for who, t, m in MSGS)
KAKAO = f"""<!doctype html><html lang="ko"><head><meta charset="utf-8"><style>
*{{box-sizing:border-box}}body{{margin:0;width:420px;background:#b2c7da;font-family:"Apple SD Gothic Neo",sans-serif;font-size:14px;color:#111}}
.top{{background:#a9bdce;padding:12px 16px;font-weight:600}}.wrap{{padding:14px 12px 20px}}
.date{{text-align:center;margin:6px auto 14px;background:rgba(0,0,0,.18);color:#fff;border-radius:12px;padding:3px 12px;width:max-content;font-size:12px}}
.row{{display:flex;margin:8px 0;align-items:flex-end}}.me{{justify-content:flex-end}}
.b{{max-width:270px;padding:8px 11px;border-radius:10px;background:#fff;line-height:1.45}}.me .b{{background:#fee500}}
.t{{font-size:10.5px;color:#475;margin:0 5px}}.n{{font-size:12px;margin:0 0 3px 2px;color:#333}}
</style></head><body><div class="top">별빛로스터스 윤담당 (가상 대화)</div><div class="wrap">
<div class="date">2026년 10월 14일 수요일</div>{bubbles}</div></body></html>"""

def edge(*args):
    subprocess.run([EDGE, "--headless", "--disable-gpu", "--hide-scrollbars", *args], check=True, capture_output=True)

(HERE / "source").mkdir(exist_ok=True)
quote_html, kakao_html = HERE / "source/quote.html", HERE / "source/kakao.html"
quote_html.write_text(QUOTE, encoding="utf-8")
kakao_html.write_text(KAKAO, encoding="utf-8")
(HERE / "별빛로스터스_메일.txt").write_text(MAIL, encoding="utf-8")
edge("--no-pdf-header-footer", f"--print-to-pdf={(HERE / '별빛로스터스_견적서.pdf').resolve()}", quote_html.resolve().as_uri())
edge("--window-size=420,610", "--force-device-scale-factor=2", f"--screenshot={(HERE / '별빛로스터스_카카오톡.png').resolve()}", kakao_html.resolve().as_uri())


# ---------- 두 번째 시연 계약: 온유스튜디오 (바뀌었다 되돌아간 계약) ----------
ONYU, EDITOR = "온유스튜디오 (가상)", "이도윤"
ONYU_QUOTE = QUOTE
for old, new in [
    ("2026-09-21", "2026-09-07"), (f"{CLIENT} (담당 윤담당)", f"{ONYU} (담당 한담당)"),
    ("원두 브랜드 로고·패키지 디자인", "브랜드 소개 영상 5편 제작"),
    ('<tr><td>로고 디자인</td><td>로고 1종, 응용형 2종</td><td class="num">1,500,000원</td></tr>\n', ""),
    ('<tr><td>패키지 디자인</td><td>원두 봉투 3종</td><td class="num">2,000,000원</td></tr>',
     '<tr><td>영상 제작</td><td>브랜드 소개 영상 5편</td><td class="num">6,000,000원</td></tr>'),
    ("<b>3,500,000원</b>", "<b>6,000,000원</b>"),
    ("1,050,000원 (계약 후 3일 내)", "2,400,000원 (계약 당일, 계약일 2026-09-07)"),
    ("<tr><th>중도금</th><td>1,050,000원 (시안 확정 시)</td></tr>\n", ""),
    ("1,400,000원 (납품 후 14일 내)", "3,600,000원 (납품 후 10일 내)"),
    ("2026-10-30", "2026-10-16"), ("수정 3회 포함, 초과 시 회당 100,000원", "수정 2회 포함"),
    ("완성 디자인의 저작권은 잔금 지급 후 의뢰인에게 넘어갑니다.", "완성 영상의 저작권은 잔금 지급 후 의뢰인에게 넘어갑니다."),
    ("<tr><th>세금</th><td>3.3% 원천징수 후 지급</td></tr>", ""),
    (f"프리랜서 디자이너 {ME}", f"프리랜서 영상 제작자 {EDITOR}"),
]:
    assert old in ONYU_QUOTE, old
    ONYU_QUOTE = ONYU_QUOTE.replace(old, new)

ONYU_MAIL = f"""보낸사람: 한담당 <han@onyu.example>
날짜: 2026-09-21 (월)
제목: 납품 일정 변경 요청

촬영 장소 섭외가 늦어져 납품을 10월 30일로 미뤄 주실 수 있을까요?

보낸사람: {EDITOR} <doyun@example.com>
날짜: 2026-09-21 (월)
제목: RE: 납품 일정 변경 요청

네, 납품은 10월 30일로 하겠습니다.

보낸사람: 오담당 <oh@onyu.example>
날짜: 2026-10-12 (월)
제목: 담당자 변경 안내

한담당님 후임 오담당입니다. 혹시 납품을 11월 6일로 더 미룰 수 있을지 내부에서 논의 중입니다. 가능 여부를 알려 주세요.
"""

ONYU_DAYS = [
    ("2026년 10월 5일 월요일", [
        ("한담당", "오전 11:02", "인터뷰 영상 1편을 더 만들 수 있을까요? 120만원 드릴게요."),
        ("한담당", "오전 11:03", "추가금은 잔금에 얹어 드리겠습니다."),
        ("me", "오전 11:15", "네 가능합니다. 잔금에 더해 주세요."),
    ]),
    ("2026년 10월 20일 화요일", [
        ("오담당", "오후 2:21", "회사 사정으로 인터뷰 영상은 취소하기로 했어요. 120만원도 빼 주세요."),
        ("me", "오후 2:40", "네 취소할게요. 금액은 원래대로 600만원입니다."),
    ]),
    ("2026년 10월 26일 월요일", [
        ("오담당", "오전 9:30", "납품은 예정대로 10월 30일로 해 주세요."),
        ("오담당", "오전 9:31", "잔금 중 100만원은 11월 2일에 먼저 보내 드릴게요. 나머지는 납품 후 10일 내에 드릴게요."),
        ("me", "오전 9:45", "네 좋습니다."),
    ]),
]
onyu_body = ""
for day, msgs in ONYU_DAYS:
    onyu_body += f'<div class="date">{day}</div>'
    for who, t, m in msgs:
        onyu_body += (f'<div class="row me"><span class="t">{t}</span><div class="b">{m}</div></div>' if who == "me"
                      else f'<div class="n">온유스튜디오 {who}</div><div class="row"><div class="b">{m}</div><span class="t">{t}</span></div>')
ONYU_KAKAO = KAKAO[:KAKAO.index("</style>")] + '</style></head><body><div class="top">온유스튜디오 담당자 (가상 대화)</div><div class="wrap">' + onyu_body + "</div></body></html>"

onyu_quote_html, onyu_kakao_html = HERE / "source/onyu_quote.html", HERE / "source/onyu_kakao.html"
onyu_quote_html.write_text(ONYU_QUOTE, encoding="utf-8")
onyu_kakao_html.write_text(ONYU_KAKAO, encoding="utf-8")
(HERE / "온유스튜디오_메일.txt").write_text(ONYU_MAIL, encoding="utf-8")
edge("--no-pdf-header-footer", f"--print-to-pdf={(HERE / '온유스튜디오_견적서.pdf').resolve()}", onyu_quote_html.resolve().as_uri())
edge("--window-size=420,800", "--force-device-scale-factor=2", f"--screenshot={(HERE / '온유스튜디오_카카오톡.png').resolve()}", onyu_kakao_html.resolve().as_uri())
