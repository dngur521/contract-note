"""시연 영상을 찍을 때 브라우저를 대본대로 움직인다.

사용법 (서버와 프론트엔드, --remote-debugging-port=9333 으로 띄운 Edge가 켜져 있어야 함)
  python play_demo.py prepare         저장된 계약을 모두 지우고 빈 첫 화면으로 돌아간다. AI를 호출하지 않는다.
  python play_demo.py run [기록파일]   대본대로 진행하고 화면별 시각을 기록한다. AI를 호출한다.
  python play_demo.py ask "질문" ...   질문하기 화면에서 질문만 해 본다. 질문마다 AI를 한 번 호출한다.
"""
import json
import random
import sys
import time
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

from human import human_type

HERE = Path(__file__).parent
API = "http://localhost:8080/api"
APP = "http://localhost:5173"

# 시연용 커서. 자동 조작은 실제 마우스를 움직이지 않으므로 화면에 그려 준다.
CURSOR = """() => {
  document.getElementById('demo-cursor')?.remove()
  const c = document.createElement('div')
  c.id = 'demo-cursor'
  c.innerHTML = '<svg width="26" height="26" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">' +
    '<path d="M4 2 L4 20 L9 15.5 L12.5 22.5 L15.5 21 L12 14.2 L18.5 14.2 Z" fill="#111" stroke="#fff" stroke-width="1.6" stroke-linejoin="round"/></svg>'
  c.style.cssText = 'position:fixed;left:700px;top:450px;z-index:99999;pointer-events:none;line-height:0;' +
    'transform-origin:4px 2px;filter:drop-shadow(0 1px 2px rgba(0,0,0,.35));transition:left .55s ease,top .55s ease,transform .12s'
  document.body.appendChild(c)
}"""
rng = random.Random()
started = time.time()
timeline = []


def mark(label):
    elapsed = int(time.time() - started)
    line = f"{elapsed // 60:02d}:{elapsed % 60:02d}  {label}"
    timeline.append(line)
    print(line, flush=True)


def pause(low=1.0, high=1.6):
    time.sleep(rng.uniform(low, high))


def api_json(path):
    with urllib.request.urlopen(API + path) as response:
        return json.load(response)


def show(locator):
    """요소가 화면 가운데 오도록 부드럽게 내린다."""
    locator.first.evaluate("e => e.scrollIntoView({behavior:'smooth', block:'center'})")
    time.sleep(0.9)


def top(page):
    page.evaluate("window.scrollTo({top:0, behavior:'smooth'})")
    time.sleep(0.8)


def cursor_to(page, locator):
    box = locator.first.bounding_box()
    page.evaluate("""([x, y]) => { const c = document.getElementById('demo-cursor'); c.style.left = x + 'px'; c.style.top = y + 'px' }""",
                  [box["x"] + box["width"] / 2, box["y"] + box["height"] / 2])
    time.sleep(0.65)


def click(page, locator):
    show(locator)
    cursor_to(page, locator)
    page.evaluate("() => { const c = document.getElementById('demo-cursor'); c.style.transform = 'scale(.82)'; setTimeout(() => c.style.transform = 'none', 140) }")
    time.sleep(0.2)
    locator.first.click()


def tab(page, name):
    click(page, page.get_by_role("button", name=name))


def demo_page(p):
    browser = p.chromium.connect_over_cdp("http://localhost:9333")
    page = next(x for x in browser.contexts[0].pages if x.url.startswith(APP))
    page.bring_to_front()
    return page


def prepare():
    removed = []
    for contract in api_json("/contracts"):
        request = urllib.request.Request(f"{API}/contracts/{contract['id']}", method="DELETE")
        urllib.request.urlopen(request)
        removed.append(contract["clientName"])
    with sync_playwright() as p:
        page = demo_page(p)
        page.set_viewport_size({"width": 1400, "height": 860})
        page.goto(APP)
        page.wait_for_selector(".stats, .empty")
        page.evaluate(CURSOR)
        print("지운 계약:", removed or "없음")
        print("남은 계약:", [c["clientName"] for c in api_json("/contracts")])
        print("샘플 AI 표시:", "샘플" in page.inner_text("body"))


def upload_and_review(page, prefix, highlights):
    tab(page, "기록 올리기")
    pause()
    files = sorted(str(f) for f in HERE.glob(prefix + "_*") if f.suffix in (".pdf", ".png", ".txt"))
    # 실제로 눌러 Finder 창을 띄우고, 사용자가 직접 파일을 고를 때까지 기다린다.
    click(page, page.locator(".dropzone"))
    mark(f"{prefix}: Finder 창 열림, 사용자가 파일 {len(files)}개 선택")
    page.wait_for_function("n => document.querySelectorAll('.file-list li').length === n", arg=len(files), timeout=300000)
    mark(f"{prefix}: 파일 {len(files)}개 올림")
    pause(1.8, 2.4)
    click(page, page.get_by_role("button", name="계약 조건 정리하기"))
    mark(f"{prefix}: AI 정리 시작 (편집 때 줄일 구간)")
    page.wait_for_selector(".panel-head, .error-box", timeout=180000)
    if page.locator(".error-box").count():
        mark(f"{prefix}: 오류 {page.inner_text('.error-box')}")
        raise SystemExit(1)
    top(page)
    mark(f"{prefix}: 정리 끝, 변경 안내 보여 줌")
    pause(3.5, 4.5)
    for selector, seconds, label in highlights:
        show(page.locator(selector))
        mark(f"{prefix}: {label}")
        time.sleep(seconds)
    click(page, page.get_by_role("button", name="확인하고 일정에 등록"))
    page.wait_for_selector(".success-box, .error-box", timeout=30000)
    mark(f"{prefix}: 확인하고 일정에 등록")
    pause(1.5, 2.0)


def ask(page, prefix, question):
    mark(f"{prefix}: 질문 입력 '{question}'")
    click(page, page.locator(".ask-form input"))
    pause(0.3, 0.6)
    human_type(page, ".ask-form input", question, rng)
    pause(0.5, 0.9)
    click(page, page.locator(".ask-form button"))
    page.wait_for_function("() => !document.body.innerText.includes('기록을 찾는 중')", timeout=150000)
    answer = page.locator(".chat > div").last
    print("   답:", answer.locator(".bubble.ai").inner_text().split("\n")[0], flush=True)
    time.sleep(1.2)  # 앱의 자동 스크롤이 끝나기를 기다린다
    box = answer.locator(".bubble.ai").bounding_box()
    print(f"   답 위치: 화면 안 {box['y'] >= 0 and box['y'] < page.evaluate('innerHeight')}, 위쪽 {int(box['y'])}px, 높이 {int(box['height'])}px", flush=True)
    pause(2.5, 3.5)


def run(log_path):
    with sync_playwright() as p:
        page = demo_page(p)
        page.evaluate(CURSOR)
        mark("시작: 일정 첫 화면")
        pause(3, 3.5)

        # 1부: 기본 흐름
        upload_and_review(page, "별빛로스터스", [
            (".form-grid", 4.0, "근거 문장"),
            (".payments", 4.5, "지급 조건과 실수령액"),
        ])
        tab(page, "일정")
        pause()
        top(page)
        mark("별빛로스터스: 일정 화면")
        pause(3, 3.5)
        row = page.locator("ul.schedule li").filter(has_text="별빛로스터스").filter(has_text="계약금")
        click(page, row.locator("input[type=checkbox]"))
        mark("별빛로스터스: 계약금 받음 체크")
        pause(2.5, 3)
        tab(page, "질문하기")
        pause()
        for question in ["별빛로스터스 수정 몇 번까지야?", "별빛로스터스 잔금은 언제 받아?",
                         "별빛로스터스 메뉴판 비용은 얼마야?", "별빛로스터스 위약금은 얼마야?"]:
            ask(page, "별빛로스터스", question)

        # 2부: 바뀌었다 되돌아간 계약
        upload_and_review(page, "온유스튜디오", [
            (".payments", 5.0, "잔금이 둘로 나뉜 지급 조건"),
        ])
        tab(page, "질문하기")
        pause()
        for question in ["온유스튜디오 추가 영상 비용은 지금 얼마야?", "온유스튜디오 납기가 11월 6일이야?",
                         "온유스튜디오 11월에 받을 돈은 얼마야?"]:
            ask(page, "온유스튜디오", question)

        tab(page, "일정")
        pause()
        top(page)
        mark("끝: 일정 화면")
        pause(3, 3.5)
    if log_path:
        Path(log_path).write_text("\n".join(timeline) + "\n", encoding="utf-8")


def ask_once(questions):
    with sync_playwright() as p:
        page = demo_page(p)
        page.evaluate(CURSOR)
        pause(1, 1.5)
        tab(page, "질문하기")
        pause()
        for question in questions:
            ask(page, "리허설", question)


if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in ("prepare", "run", "ask"):
        raise SystemExit(__doc__)
    if sys.argv[1] == "prepare":
        prepare()
    elif sys.argv[1] == "ask":
        ask_once(sys.argv[2:])
    else:
        run(sys.argv[2] if len(sys.argv) > 2 else None)
