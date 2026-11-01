import { useCallback, useEffect, useState } from 'react'
import { api, shortDate, won } from './api'
import type { ScheduleItem } from './api'

function dayLabel(item: ScheduleItem) {
  if (item.paid) return '받음'
  if (item.daysLeft === null) return '날짜 미정'
  if (item.overdue) return `${-item.daysLeft}일 지남`
  return item.daysLeft === 0 ? '오늘' : `D-${item.daysLeft}`
}

export function SchedulePage({ onOpen }: { onOpen: (contractId: number) => void }) {
  const [items, setItems] = useState<ScheduleItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    api.schedule().then(setItems).catch((e: Error) => setError(e.message))
  }, [])
  useEffect(load, [load])

  const togglePaid = async (item: ScheduleItem) => {
    try {
      await api.setPaid(item.paymentId!, !item.paid)
      load()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  if (error) return <div className="error-box">{error}</div>
  if (!items) return <p className="muted">불러오는 중입니다.</p>

  const payments = items.filter((i) => i.type === 'PAYMENT' && !i.paid)
  const overdue = payments.filter((i) => i.overdue)
  const upcoming = payments.filter((i) => !i.overdue)
  const sum = (list: ScheduleItem[]) => list.reduce((total, i) => total + (i.netAmount ?? 0), 0)

  return (
    <>
      <section className="stats">
        <div className={overdue.length ? 'stat danger' : 'stat'}>
          <span className="stat-label">기한 지난 돈</span>
          <strong>{won(sum(overdue))}</strong>
          <span className="stat-sub">{overdue.length}건</span>
        </div>
        <div className="stat">
          <span className="stat-label">앞으로 받을 돈</span>
          <strong>{won(sum(upcoming))}</strong>
          <span className="stat-sub">{upcoming.length}건</span>
        </div>
      </section>

      {overdue.map((i) => (
        <div className="alert" key={`alert-${i.paymentId}`}>
          <b>{i.clientName}</b> {i.label} {won(i.netAmount)} 지급일이 <b>{-(i.daysLeft ?? 0)}일</b> 지났어요.
          <button className="link" onClick={() => onOpen(i.contractId)}>
            근거 보기
          </button>
        </div>
      ))}

      <h2>일정</h2>
      {items.length === 0 ? (
        <div className="empty">아직 확인된 계약이 없습니다. 기록을 올리고 내용을 확인하면 일정이 여기에 나타납니다.</div>
      ) : (
        <ul className="schedule">
          {items.map((i) => (
            <li key={`${i.type}-${i.paymentId ?? i.contractId}`} className={i.paid ? 'done' : ''}>
              <span className={`pill ${i.overdue ? 'pill-danger' : i.paid ? 'pill-done' : i.date === null ? 'pill-warn' : ''}`}>{dayLabel(i)}</span>
              <span className="date">{shortDate(i.date)}</span>
              <button className="item-main" onClick={() => onOpen(i.contractId)}>
                <b>{i.clientName}</b> · {i.label}
                {i.projectName && <small>{i.projectName}</small>}
              </button>
              {i.type === 'PAYMENT' && (
                <>
                  <span className="amount">
                    {won(i.netAmount)}
                    {i.amount !== i.netAmount && <small>세전 {won(i.amount)}</small>}
                  </span>
                  <label className="check">
                    <input type="checkbox" checked={i.paid} onChange={() => togglePaid(i)} /> 받음
                  </label>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
