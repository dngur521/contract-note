"""여러 고객 기록을 함께 넣고, 계약을 고르지 않은 상태에서 다른 고객과 섞지 않고 답하는지 본다."""
import json, pathlib, time
import importlib.util
spec = importlib.util.spec_from_file_location("r", pathlib.Path(__file__).parent / "run_eval.py")
r = importlib.util.module_from_spec(spec); spec.loader.exec_module(r)
ids = {}
for cid in ["d02", "d05", "d06", "c02", "c04", "c10"]:
    d = r.ROOT / "cases" / cid; t = json.loads((d / "truth.json").read_text(encoding="utf-8"))
    s, c, _ = r.upload([d / f for f in t["files"]]); assert s == 200, c
    ids[cid] = c["id"]; time.sleep(r.GAP)
Q = [("봄내식품 시안 수정 몇 번 무료야?", ["3"], "봄내"), ("하나로교육 영상 총 금액 얼마야?", ["240만"], "하나로"), ("그린테크 수정 몇 번까지야?", ["4"], "그린테크"),
     ("미소베이커리 잔금은 언제 받아?", ["7일", "11월 13일", "11-13"], "미소"), ("세움출판 번역 몇 쪽이야?", ["180"], "세움"), ("빛고을상사 대금 언제까지 받아?", ["10일", "10월 26일", "10-26"], "빛고을"),
     ("없는고객사 잔금 언제야?", None, None), ("저작권이 수요자에게 있는 건 어디야?", ["봄내", "미소"], None), ("지급 기한을 60일로 바꾸려고 한 곳이 어디야?", ["빛고을"], None)]
ok = 0
for q, want, client in Q:
    time.sleep(r.GAP)
    s, a, sec = r.call("POST", "/ask", json.dumps(dict(question=q)).encode(), {"Content-Type": "application/json"})
    good = s == 200 and ((not a["found"]) if want is None else (a["found"] and r.has_keyword(a["answer"], want)))
    mixed = client and s == 200 and any(client not in x["clientName"] for x in a["sources"])
    good = good and not mixed
    ok += bool(good)
    print("맞음" if good else "틀림", f"{sec:.1f}초", q, "->", (a.get("answer") or a.get("message", ""))[:90], "| 출처", sorted({x["clientName"] for x in a.get("sources", [])}))
for i in ids.values(): r.call("DELETE", f"/contracts/{i}")
print(f"\n{ok}/{len(Q)} 맞음")
