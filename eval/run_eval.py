"""시험 자료를 실제 서버에 올려 정답표와 비교한다. 만든 계약은 끝나면 지운다.
실행: python3 eval/run_eval.py dev   또는   holdout   결과: eval/results_<이름>.json"""
import json, mimetypes, pathlib, re, subprocess, sys, time, urllib.request, urllib.error, uuid

API = "http://localhost:8080/api"
ROOT = pathlib.Path(__file__).parent
GAP = 4  # 무료 한도를 고려한 호출 간격(초)
MIME = {".pdf": "application/pdf", ".png": "image/png", ".txt": "text/plain"}

def call(method, url, data=None, headers=None, timeout=150):
    req = urllib.request.Request(API + url, data=data, headers=headers or {}, method=method)
    t = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            body = r.read(); return r.status, (json.loads(body) if body else None), time.time() - t
    except urllib.error.HTTPError as e:
        body = e.read()
        try: return e.code, json.loads(body), time.time() - t
        except Exception: return e.code, {"message": body[:200].decode(errors="ignore")}, time.time() - t
    except Exception as e:
        return 0, {"message": str(e)}, time.time() - t

def upload(paths):
    b = uuid.uuid4().hex; out = b""
    for p in paths:
        out += f'--{b}\r\nContent-Disposition: form-data; name="files"; filename="{p.name}"\r\nContent-Type: {MIME[p.suffix]}\r\n\r\n'.encode() + p.read_bytes() + b"\r\n"
    out += f"--{b}--\r\n".encode()
    return call("POST", "/contracts/extract", out, {"Content-Type": f"multipart/form-data; boundary={b}"})

def has_keyword(answer, keywords):
    """"100만"과 "1,000,000"처럼 같은 금액의 다른 표기는 같은 답으로 본다."""
    forms = list(keywords)
    for k in keywords:
        m = re.fullmatch(r"([\d,]+)만원?", k)
        if m: forms.append(f"{int(m[1].replace(',', '')) * 10000:,}")
    plain = answer.replace(",", "")
    return any(k in answer or k.replace(",", "") in plain for k in forms)

def norm(s): return re.sub(r"[\s ·,.\-:()\"'“”‘’]+", "", s or "")

def score(contract, truth, source):
    r = {}
    def check(name, ok, got, want): r[name] = dict(ok=bool(ok), got=got, want=want)
    check("client", truth["client"] in (contract["clientName"] or ""), contract["clientName"], truth["client"])
    check("total", contract["totalAmount"] == truth["total"], contract["totalAmount"], truth["total"])
    check("delivery", contract["deliveryDate"] == truth["delivery"], contract["deliveryDate"], truth["delivery"])
    check("revision", contract["revisionLimit"] == truth["revision"], contract["revisionLimit"], truth["revision"])
    check("withholding", contract["withholding"] == truth["withholding"], contract["withholding"], truth["withholding"])
    if truth.get("contract"): check("contract", contract["contractDate"] == truth["contract"], contract["contractDate"], truth["contract"])
    if truth.get("copyright"): check("copyright", any(k in (contract["copyrightOwner"] or "") for k in truth["copyright"]), contract["copyrightOwner"], truth["copyright"])
    got = [dict(label=p["label"], amount=p["amount"], due=p["dueDate"], rule=p["ruleType"], days=p["offsetDays"]) for p in contract["payments"]]
    want = truth["payments"]; left = list(got); matched = 0
    for w in want:
        hit = next((g for g in left if g["amount"] == w["amount"] and g["due"] == w["due"]), None)
        if hit: left.remove(hit); matched += 1
    check("payments", matched == len(want) and not left, got, want)
    if truth.get("conflicts"): check("conflicts", len(contract["notes"]) >= truth["conflicts"], contract["notes"], truth["conflicts"])
    # 근거 문장이 실제 원문에 있는가(지어낸 근거 찾기)
    src = norm(source); ev = contract["evidences"]; quotes = [e["quote"] for e in ev] + [p["quote"] for p in contract["payments"] if p["quote"]]
    fake = [q for q in quotes if norm(q) not in src]
    return r, dict(quotes=len(quotes), not_in_source=fake)

def main(split):
    path = ROOT / f"results_{split}.json"
    results = []
    if "--resume" in sys.argv and path.exists():
        results = [r for r in json.loads(path.read_text(encoding="utf-8")) if "error" not in r]
        print("이미 끝난", len(results), "건은 건너뜁니다.", flush=True)
    done = {r["id"] for r in results}
    limited = 0
    for d in sorted(x for x in (ROOT / "cases").iterdir() if x.is_dir()):
        t = json.loads((d / "truth.json").read_text(encoding="utf-8"))
        if split != "all" and t["split"] != split and t["id"] != split: continue
        if t["id"] in done: continue
        status, contract, sec = upload([d / f for f in t["files"]])
        item = dict(id=t["id"], desc=t["desc"], seconds=round(sec, 1), status=status)
        if status != 200:
            item["error"] = contract; results.append(item); print(t["id"], "오류", status, contract, flush=True); time.sleep(GAP)
            limited = limited + 1 if status == 429 else 0
            if limited >= 2:
                print("무료 사용량 한도에 걸려 멈춥니다.", flush=True); break
            continue
        limited = 0
        item["fields"], item["evidence"] = score(contract, t["truth"], t["source"])
        item["notes"] = contract["notes"]
        qs = []
        for q, expect in t["questions"]:
            time.sleep(GAP)
            s, a, qsec = call("POST", "/ask", json.dumps(dict(question=q, contractId=contract["id"])).encode(), {"Content-Type": "application/json"})
            if s == 429:
                item["error"] = a; break
            if s != 200: qs.append(dict(q=q, ok=False, answer=a, status=s)); continue
            ok = (not a["found"] or "아직" in a["answer"] or "않았" in a["answer"]) if expect is None else (a["found"] and has_keyword(a["answer"], expect))
            qs.append(dict(q=q, ok=bool(ok), found=a["found"], answer=a["answer"], want=expect, sources=[x["text"][:80] for x in a["sources"]], seconds=round(qsec, 1)))
        item["questions"] = qs
        call("DELETE", f"/contracts/{contract['id']}")
        if "error" in item:
            results.append(item); limited += 1; print(t["id"], "질문 중 한도 오류", flush=True)
            if limited >= 2:
                print("무료 사용량 한도에 걸려 멈춥니다.", flush=True); break
            time.sleep(GAP); continue
        bad = [k for k, v in item["fields"].items() if not v["ok"]]
        badq = [x["q"] for x in qs if not x["ok"]]
        print(t["id"], f"{sec:.0f}초", "틀림:" if bad or badq else "모두 맞음", bad, badq, "근거불일치", len(item["evidence"]["not_in_source"]), flush=True)
        results.append(item); time.sleep(GAP)
        (ROOT / f"results_{split}.json").write_text(json.dumps(results, ensure_ascii=False, indent=1), encoding="utf-8")
    (ROOT / f"results_{split}.json").write_text(json.dumps(results, ensure_ascii=False, indent=1), encoding="utf-8")
    results = [r for r in results if "error" not in r or "fields" not in r]
    fields = [v["ok"] for r in results for v in r.get("fields", {}).values()]
    pay = [r["fields"]["payments"]["ok"] for r in results if "fields" in r]
    qs = [q["ok"] for r in results for q in r.get("questions", [])]
    ev = sum(r["evidence"]["quotes"] for r in results if "evidence" in r); fake = sum(len(r["evidence"]["not_in_source"]) for r in results if "evidence" in r)
    print(f"\n항목 일치 {sum(fields)}/{len(fields)} = {sum(fields)/max(len(fields),1):.1%} | 지급 전체 일치 {sum(pay)}/{len(pay)} | 질문 {sum(qs)}/{len(qs)} | 근거가 원문에 있음 {ev-fake}/{ev} | 오류 {sum(1 for r in results if 'error' in r)}건")

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "dev")
