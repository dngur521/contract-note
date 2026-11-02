import { useEffect, useRef, useState } from 'react'
import { api } from './api'
import type { AskResponse, Contract } from './api'

const EXAMPLES = ['수정 몇 번까지야?', '잔금은 언제 받기로 했어?', '저작권은 누구한테 있어?']

type Turn = { question: string; response?: AskResponse; error?: string }

export function AskPage({ contracts }: { contracts: Contract[] }) {
  const [question, setQuestion] = useState('')
  const [contractId, setContractId] = useState<number | null>(null)
  const [turns, setTurns] = useState<Turn[]>([])
  const [busy, setBusy] = useState(false)
  const lastTurn = useRef<HTMLDivElement>(null)
  const form = useRef<HTMLFormElement>(null)

  // 새 질문과 답이 화면에 보이게 내린다. 답이 화면보다 길면 답의 처음부터 보여 준다.
  useEffect(() => {
    if (!lastTurn.current || !form.current) return
    const height = form.current.getBoundingClientRect().bottom - lastTurn.current.getBoundingClientRect().top
    const fits = height <= window.innerHeight - 24
    const target = fits ? form.current : lastTurn.current
    target.scrollIntoView({ behavior: 'smooth', block: fits ? 'end' : 'start' })
  }, [turns])

  const ask = async (text: string) => {
    const q = text.trim()
    if (!q || busy) return
    setQuestion('')
    setBusy(true)
    setTurns((current) => [...current, { question: q }])
    const update = (patch: Partial<Turn>) =>
      setTurns((current) => current.map((t, i) => (i === current.length - 1 ? { ...t, ...patch } : t)))
    try {
      update({ response: await api.ask(q, contractId) })
    } catch (e) {
      update({ error: (e as Error).message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel narrow">
      <div className="panel-head">
        <h2>질문하기</h2>
        <select aria-label="찾을 계약" value={contractId ?? ''} onChange={(e) => setContractId(e.target.value ? Number(e.target.value) : null)}>
          <option value="">모든 계약에서 찾기</option>
          {contracts.map((c) => (
            <option key={c.id} value={c.id}>
              {c.clientName ?? `계약 ${c.id}`}
            </option>
          ))}
        </select>
      </div>
      <p className="muted">올린 기록 안에서만 답하고, 답의 근거가 된 문장을 함께 보여 줍니다.</p>

      {turns.length === 0 && (
        <div className="examples">
          {EXAMPLES.map((example) => (
            <button key={example} onClick={() => ask(example)}>
              {example}
            </button>
          ))}
        </div>
      )}

      <div className="chat">
        {turns.map((turn, index) => (
          <div key={index} ref={index === turns.length - 1 ? lastTurn : undefined}>
            <div className="bubble user">{turn.question}</div>
            {turn.error && <div className="error-box">{turn.error}</div>}
            {!turn.response && !turn.error && (
              <div className="bubble ai muted loading">
                <span className="spinner" aria-hidden="true" />
                기록을 찾는 중입니다...
              </div>
            )}
            {turn.response && (
              <div className={turn.response.found ? 'bubble ai' : 'bubble ai not-found'}>
                {turn.response.answer}
                {turn.response.sources.map((source, i) =>
                  source.documentId === null ? (
                    <details className="source-card" key={i}>
                      <summary>{source.clientName} · {source.fileName} 보기</summary>
                      <blockquote className="quote">{source.text}</blockquote>
                    </details>
                  ) : (
                    <blockquote className="quote" key={i}>
                      “{source.text}”{' '}
                      <cite>
                        <a href={`/api/documents/${source.documentId}/file`} target="_blank" rel="noreferrer">
                          {source.clientName} · {source.fileName}
                        </a>
                      </cite>
                    </blockquote>
                  ),
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      <form
        ref={form}
        className="ask-form"
        onSubmit={(e) => {
          e.preventDefault()
          void ask(question)
        }}
      >
        <input placeholder="예: 하늘디자인 수정 몇 번까지야?" value={question} onChange={(e) => setQuestion(e.target.value)} />
        <button className="primary" disabled={busy || !question.trim()}>
          질문
        </button>
      </form>
    </section>
  )
}
