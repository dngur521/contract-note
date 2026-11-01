"""잘못된 입력에 500 오류나 영어 오류 없이 안내 문구로 답하는지 확인한다. AI를 부르는 항목은 깨진 PDF 하나뿐이다."""
import json, sys, urllib.request, urllib.error, uuid
API = "http://localhost:8080/api"
def call(method, url, data=None, headers=None):
    req = urllib.request.Request(API + url, data=data, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=120) as r: return r.status, r.read()
    except urllib.error.HTTPError as e: return e.code, e.read()
def files(items):
    b = uuid.uuid4().hex; out = b""
    for name, mime, data in items:
        out += f'--{b}\r\nContent-Disposition: form-data; name="files"; filename="{name}"\r\nContent-Type: {mime}\r\n\r\n'.encode() + data + b"\r\n"
    return out + f"--{b}--\r\n".encode(), {"Content-Type": f"multipart/form-data; boundary={b}"}
J = {"Content-Type": "application/json"}
OK = {"clientName": "가", "confirm": False, "withholding": False}
T = [
 ("실행 파일 거부", 400, lambda: call("POST", "/contracts/extract", *files([("x.exe", "application/octet-stream", b"MZ")]))),
 ("webp 거부", 400, lambda: call("POST", "/contracts/extract", *files([("x.webp", "image/webp", b"RIFF")]))),
 ("파일 없음", 400, lambda: call("POST", "/contracts/extract", b"", {"Content-Type": "multipart/form-data; boundary=x"})),
 ("빈 파일", 400, lambda: call("POST", "/contracts/extract", *files([("빈.pdf", "application/pdf", b"")]))),
 ("20MB 초과", 413, lambda: call("POST", "/contracts/extract", *files([("큰.pdf", "application/pdf", b"0" * (21 * 1024 * 1024))]))),
 ("깨진 PDF", (400, 502, 504), lambda: call("POST", "/contracts/extract", *files([("깨짐.pdf", "application/pdf", b"%PDF-broken not a real pdf")]))),
 ("빈 질문", 400, lambda: call("POST", "/ask", json.dumps({"question": " "}).encode(), J)),
 ("질문 JSON 오류", 400, lambda: call("POST", "/ask", b"{not json", J)),
 ("없는 계약 조회", 404, lambda: call("GET", "/contracts/99999999")),
 ("없는 계약 수정", 404, lambda: call("PUT", "/contracts/99999999", json.dumps(OK).encode(), J)),
 ("없는 계약 삭제", 404, lambda: call("DELETE", "/contracts/99999999")),
 ("없는 지급 항목", 404, lambda: call("PATCH", "/payments/99999999", json.dumps({"paid": True}).encode(), J)),
 ("없는 문서", 404, lambda: call("GET", "/documents/99999999/file")),
 ("계약 ID 형식 오류", 400, lambda: call("GET", "/contracts/abc")),
 ("고객 이름 비움", 400, lambda: call("PUT", "/contracts/1", json.dumps({**OK, "clientName": ""}).encode(), J)),
 ("날짜 형식 오류", 400, lambda: call("PUT", "/contracts/1", json.dumps({**OK, "deliveryDate": "내일"}).encode(), J)),
 ("SQL 주입 문구 질문", 200, lambda: call("POST", "/ask", json.dumps({"question": "'; DROP TABLE contract; --", "contractId": 99999999}).encode(), J)),
]
bad = 0
for name, want, f in T:
    s, b = f()
    wants = want if isinstance(want, tuple) else (want,)
    try: msg = json.loads(b).get("message", "") if b else ""
    except Exception: msg = "(JSON 아님)"
    good = s in wants and (s < 400 or (msg and any("가" <= c <= "힣" for c in msg)))
    bad += not good
    print("통과" if good else "실패", name, s, msg[:70])
print(f"\n{len(T)-bad}/{len(T)} 통과")
