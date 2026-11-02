"""브라우저에 사람이 치는 것처럼 입력한다.

두벌식 키보드처럼 자모를 하나씩 조합해 보여 준다. 예: ㅂ → 벼 → 별 → 별ㅂ → 별비 → 별빛
글자마다 간격을 다르게 주고, 띄어쓰기와 문장부호 뒤에는 조금 더 쉰다.
"""
import random
import time

CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
JONG = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
DOUBLE_JUNG = {"ㅘ": "ㅗㅏ", "ㅙ": "ㅗㅐ", "ㅚ": "ㅗㅣ", "ㅝ": "ㅜㅓ", "ㅞ": "ㅜㅔ", "ㅟ": "ㅜㅣ", "ㅢ": "ㅡㅣ"}
DOUBLE_JONG = {"ㄳ": "ㄱㅅ", "ㄵ": "ㄴㅈ", "ㄶ": "ㄴㅎ", "ㄺ": "ㄹㄱ", "ㄻ": "ㄹㅁ", "ㄼ": "ㄹㅂ", "ㄽ": "ㄹㅅ",
               "ㄾ": "ㄹㅌ", "ㄿ": "ㄹㅍ", "ㅀ": "ㄹㅎ", "ㅄ": "ㅂㅅ"}
JOIN_JUNG = {v: k for k, v in DOUBLE_JUNG.items()}
JOIN_JONG = {v: k for k, v in DOUBLE_JONG.items()}


def is_syllable(char):
    return "가" <= char <= "힣"


def keystrokes(text):
    """글자를 누르는 순서의 자모로 나눈다."""
    keys = []
    for char in text:
        if not is_syllable(char):
            keys.append(char)
            continue
        code = ord(char) - ord("가")
        cho, jung, jong = code // 588, code % 588 // 28, code % 28
        keys.append(CHO[cho])
        keys.extend(DOUBLE_JUNG.get(JUNG[jung], JUNG[jung]))
        if jong:
            keys.extend(DOUBLE_JONG.get(JONG[jong], JONG[jong]))
    return keys


def show(cho, jung, jong):
    if not jung:
        return cho
    final = JONG.index(jong) if jong else 0
    return chr(ord("가") + CHO.index(cho) * 588 + JUNG.index(jung) * 28 + final)


def composing_states(text):
    """키를 하나 누를 때마다 입력칸에 보일 글자를 차례로 돌려준다."""
    done, cho, jung, jong = "", "", "", ""
    states = []
    for key in keystrokes(text):
        if key in CHO:
            if not cho:
                cho = key
            elif jung and not jong and key in JONG:
                jong = key
            elif jong + key in JOIN_JONG:
                jong = JOIN_JONG[jong + key]
            else:
                done += show(cho, jung, jong)
                cho, jung, jong = key, "", ""
        elif key in JUNG:
            if jong:
                keep, moved = DOUBLE_JONG.get(jong, " " + jong)
                done += show(cho, jung, keep.strip())
                cho, jung, jong = moved, key, ""
            elif jung:
                jung = JOIN_JUNG[jung + key]
            else:
                jung = key
        else:
            if cho:
                done += show(cho, jung, jong)
                cho, jung, jong = "", "", ""
            done += key
        states.append(done + (show(cho, jung, jong) if cho else ""))
    return states


def delay_after(char, rng):
    delay = rng.uniform(0.05, 0.14)
    if char == " ":
        delay += rng.uniform(0.05, 0.22)
    elif char in ",.?!":
        delay += rng.uniform(0.15, 0.40)
    if rng.random() < 0.04:  # 가끔 생각하듯 멈춘다
        delay += rng.uniform(0.25, 0.70)
    return delay


SET_VALUE = """([selector, value]) => {
  const input = document.querySelector(selector)
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, value)
  input.dispatchEvent(new Event('input', { bubbles: true }))
}"""


def human_type(page, selector, text, rng=None):
    """화면에 맞춘 조합 입력. 입력칸 값을 키 하나마다 바꾸고 React에도 알린다."""
    rng = rng or random.Random()
    page.click(selector)
    time.sleep(rng.uniform(0.25, 0.6))
    for state in composing_states(text):
        page.evaluate(SET_VALUE, [selector, state])
        time.sleep(delay_after(state[-1], rng))
