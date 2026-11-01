import { useEffect, useState } from 'react'
import { api, shortDate, won } from './api'
import type { Contract, Evidence, Payment } from './api'

type Props = {
  contracts: Contract[]
  selectedId: number | null
  onSelect: (id: number | null) => void
  onChanged: () => Promise<void>
  onUpload: () => void
}

export function ContractsPage({ contracts, selectedId, onSelect, onChanged, onUpload }: Props) {
  const selected = contracts.find((c) => c.id === selectedId) ?? contracts[0]

  if (contracts.length === 0) {
    return (
      <div className="empty">
        아직 계약이 없습니다.
        <button className="link" onClick={onUpload}>
          기록 올리러 가기
        </button>
      </div>
    )
  }

  return (
    <div className="split">
      <aside className="contract-list">
        {contracts.map((c) => (
          <button key={c.id} className={c.id === selected.id ? 'contract-item active' : 'contract-item'} onClick={() => onSelect(c.id)}>
            <b>{c.clientName ?? '고객 미정'}</b>
            <span>{c.projectName ?? '프로젝트 미정'}</span>
            {c.status === 'DRAFT' ? <em className="pill pill-warn">확인 필요</em> : <em className="pill pill-done">확인 완료</em>}
          </button>
        ))}
      </aside>
      {/* key: 다른 계약을 고르면 입력 폼을 새로 만든다 */}
      <ContractDetail key={selected.id} contract={selected} onChanged={onChanged} onDeleted={() => onSelect(null)} />
    </div>
  )
}

function ContractDetail({ contract, onChanged, onDeleted }: { contract: Contract; onChanged: () => Promise<void>; onDeleted: () => void }) {
  const [form, setForm] = useState(contract)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => setForm(contract), [contract])

  const set = <K extends keyof Contract>(key: K, value: Contract[K]) => setForm({ ...form, [key]: value })
  const evidenceOf = (field: string) => contract.evidences.find((e) => e.field === field)

  const setPayment = (index: number, patch: Partial<Payment>) =>
    set('payments', form.payments.map((p, i) => (i === index ? { ...p, ...patch } : p)))

  const addPayment = () =>
    set('payments', [
      ...form.payments,
      { id: null, label: '', amount: null, netAmount: null, ruleType: 'FIXED_DATE', fixedDate: null, offsetDays: null, dueDate: null, paid: false, quote: null, fileName: null },
    ])

  const save = async (confirm: boolean) => {
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await api.update(contract.id, {
        clientName: form.clientName ?? '',
        projectName: form.projectName,
        contractDate: form.contractDate,
        deliveryDate: form.deliveryDate,
        acceptanceDate: form.acceptanceDate,
        totalAmount: form.totalAmount,
        revisionLimit: form.revisionLimit,
        copyrightOwner: form.copyrightOwner,
        withholding: form.withholding,
        confirm,
        payments: form.payments.map((p) => ({
          id: p.id,
          label: p.label,
          amount: p.amount,
          ruleType: p.ruleType,
          fixedDate: p.fixedDate,
          offsetDays: p.offsetDays,
        })),
      })
      await onChanged()
      setMessage(confirm ? '확인했습니다. 일정에 등록되었습니다.' : '저장했습니다.')
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!window.confirm('이 계약과 올린 기록을 모두 삭제할까요?')) return
    try {
      await api.remove(contract.id)
      onDeleted()
      await onChanged()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{contract.clientName ?? '고객 미정'}</h2>
        {contract.status === 'DRAFT' && <span className="pill pill-warn">AI가 정리한 내용을 확인해 주세요</span>}
      </div>

      {contract.notes.length > 0 && (
        <div className="changes">
          <b>기록끼리 다른 부분이 있어요</b>
          <ul>
            {contract.notes.map((note) => (
              <li key={note}>{note}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="form-grid">
        <Field label="고객" evidence={evidenceOf('clientName')}>
          <input value={form.clientName ?? ''} onChange={(e) => set('clientName', e.target.value)} />
        </Field>
        <Field label="프로젝트" evidence={evidenceOf('projectName')}>
          <input value={form.projectName ?? ''} onChange={(e) => set('projectName', e.target.value)} />
        </Field>
        <Field label="총 금액 (원)" evidence={evidenceOf('totalAmount')}>
          <NumberInput value={form.totalAmount} onChange={(v) => set('totalAmount', v)} />
        </Field>
        <Field label="수정 횟수" evidence={evidenceOf('revisionLimit')}>
          <NumberInput value={form.revisionLimit} onChange={(v) => set('revisionLimit', v)} />
        </Field>
        <Field label="계약일" evidence={evidenceOf('contractDate')}>
          <input type="date" value={form.contractDate ?? ''} onChange={(e) => set('contractDate', e.target.value || null)} />
        </Field>
        <Field label="납품일" evidence={evidenceOf('deliveryDate')}>
          <input type="date" value={form.deliveryDate ?? ''} onChange={(e) => set('deliveryDate', e.target.value || null)} />
        </Field>
        <Field label="검수일" evidence={evidenceOf('acceptanceDate')}>
          <input type="date" value={form.acceptanceDate ?? ''} onChange={(e) => set('acceptanceDate', e.target.value || null)} />
        </Field>
        <Field label="저작권" evidence={evidenceOf('copyrightOwner')} wide>
          <input value={form.copyrightOwner ?? ''} onChange={(e) => set('copyrightOwner', e.target.value)} />
        </Field>
        <Field label="3.3% 원천징수" evidence={evidenceOf('withholding')} wide>
          <label className="check">
            <input type="checkbox" checked={form.withholding} onChange={(e) => set('withholding', e.target.checked)} /> 세금을 떼고 받음
          </label>
        </Field>
      </div>

      <h3>지급 조건</h3>
      <p className="muted small">지급일은 AI가 아니라 코드가 계산합니다. 조건을 고치면 저장할 때 다시 계산합니다.</p>
      <div className="payments">
        {form.payments.map((p, index) => (
          <div className="payment" key={p.id ?? `new-${index}`}>
            <input className="w-label" aria-label="항목" placeholder="항목" value={p.label} onChange={(e) => setPayment(index, { label: e.target.value })} />
            <NumberInput className="w-amount" ariaLabel="금액" placeholder="금액" value={p.amount} onChange={(v) => setPayment(index, { amount: v })} />
            <select aria-label="지급 기준" value={p.ruleType} onChange={(e) => setPayment(index, { ruleType: e.target.value })}>
              <option value="FIXED_DATE">날짜 지정</option>
              <option value="DAYS_AFTER_DELIVERY">납품 후 N일</option>
              <option value="DAYS_AFTER_CONTRACT">계약 후 N일</option>
              <option value="DAYS_AFTER_ACCEPTANCE">검수 후 N일</option>
              <option value="DELIVERY_MONTH_DAY">납품한 달 N일</option>
              <option value="NEXT_MONTH_DAY">납품 다음 달 N일</option>
              <option value="UNKNOWN">기록 없음</option>
            </select>
            {p.ruleType === 'FIXED_DATE' && (
              <input type="date" aria-label="지급일" value={p.fixedDate ?? ''} onChange={(e) => setPayment(index, { fixedDate: e.target.value || null })} />
            )}
            {(p.ruleType.startsWith('DAYS_AFTER') || p.ruleType.endsWith('_DAY')) && (
              <NumberInput
                className="w-days"
                ariaLabel={p.ruleType.endsWith('_DAY') ? '날짜 (말일은 31)' : '일수'}
                placeholder={p.ruleType.endsWith('_DAY') ? '말일 31' : '일수'}
                value={p.offsetDays}
                onChange={(v) => setPayment(index, { offsetDays: v })}
              />
            )}
            <span className="due">
              {shortDate(p.dueDate)} · 실수령 {won(p.netAmount)}
            </span>
            <button className="link danger" onClick={() => set('payments', form.payments.filter((_, i) => i !== index))}>
              빼기
            </button>
            {p.quote && (
              <blockquote className="quote">
                “{p.quote}” <cite>{p.fileName}</cite>
              </blockquote>
            )}
          </div>
        ))}
        <button className="link" onClick={addPayment}>
          + 지급 항목 추가
        </button>
      </div>

      <h3>올린 기록</h3>
      <ul className="doc-list">
        {contract.documents.map((d) => (
          <li key={d.id}>
            <a href={`/api/documents/${d.id}/file`} target="_blank" rel="noreferrer">
              {d.fileName}
            </a>
          </li>
        ))}
      </ul>

      {error && <div className="error-box">{error}</div>}
      {message && <div className="success-box">{message}</div>}
      <div className="actions">
        {contract.status === 'DRAFT' ? (
          <>
            <button className="primary" disabled={saving} onClick={() => save(true)}>
              확인하고 일정에 등록
            </button>
            <button disabled={saving} onClick={() => save(false)}>
              임시 저장
            </button>
          </>
        ) : (
          <button className="primary" disabled={saving} onClick={() => save(false)}>
            저장
          </button>
        )}
        <button className="link danger push" onClick={remove}>
          삭제
        </button>
      </div>
    </section>
  )
}

function Field({ label, evidence, wide, children }: { label: string; evidence?: Evidence; wide?: boolean; children: React.ReactNode }) {
  return (
    <div className={wide ? 'field wide' : 'field'}>
      <label>
        <span>{label}</span>
        {children}
      </label>
      {evidence ? (
        <blockquote className="quote">
          “{evidence.quote}” <cite>{evidence.fileName}</cite>
        </blockquote>
      ) : (
        <p className="no-evidence">근거 문장 없음</p>
      )}
    </div>
  )
}

function NumberInput({ value, onChange, className, placeholder, ariaLabel }: {
  value: number | null
  onChange: (value: number | null) => void
  className?: string
  placeholder?: string
  ariaLabel?: string
}) {
  return (
    <input
      type="number"
      min={0}
      className={className}
      placeholder={placeholder}
      aria-label={ariaLabel}
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
    />
  )
}
