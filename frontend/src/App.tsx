import { useCallback, useEffect, useState } from 'react'
import { api } from './api'
import type { Contract } from './api'
import { AskPage } from './AskPage'
import { ContractsPage } from './ContractsPage'
import { SchedulePage } from './SchedulePage'
import { UploadPage } from './UploadPage'

type Tab = 'schedule' | 'contracts' | 'upload' | 'ask'
const TABS: { id: Tab; label: string }[] = [
  { id: 'schedule', label: '일정' },
  { id: 'contracts', label: '계약' },
  { id: 'upload', label: '기록 올리기' },
  { id: 'ask', label: '질문하기' },
]

export default function App() {
  const [tab, setTab] = useState<Tab>('schedule')
  const [aiMode, setAiMode] = useState<string | null>(null)
  const [contracts, setContracts] = useState<Contract[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    try {
      setContracts(await api.contracts())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [])

  useEffect(() => {
    api.meta().then((m) => setAiMode(m.aiMode)).catch(() => undefined)
    void reload()
  }, [reload])

  const openContract = (id: number) => {
    setSelectedId(id)
    setTab('contracts')
  }

  const drafts = contracts.filter((c) => c.status === 'DRAFT').length

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">계약노트</span>
          <span className="tagline">카카오톡과 메일 속 계약을 정리하고 돈 받을 날을 챙겨 줍니다</span>
        </div>
        <nav className="tabs">
          {TABS.map((t) => (
            <button key={t.id} className={tab === t.id ? 'tab active' : 'tab'} onClick={() => setTab(t.id)}>
              {t.label}
              {t.id === 'contracts' && drafts > 0 && <span className="badge">{drafts}</span>}
            </button>
          ))}
        </nav>
      </header>

      {aiMode === 'sample' && (
        <div className="notice">
          샘플 AI 모드입니다. 올린 파일을 실제로 읽지 않고 미리 정해 둔 가상 결과를 보여 줍니다.
        </div>
      )}
      {error && <div className="error-box">{error}</div>}

      <main className="content">
        {tab === 'schedule' && <SchedulePage onOpen={openContract} />}
        {tab === 'contracts' && (
          <ContractsPage
            contracts={contracts}
            selectedId={selectedId}
            onSelect={setSelectedId}
            onChanged={reload}
            onUpload={() => setTab('upload')}
          />
        )}
        {tab === 'upload' && (
          <UploadPage
            onDone={async (contract) => {
              await reload()
              openContract(contract.id)
            }}
          />
        )}
        {tab === 'ask' && <AskPage contracts={contracts} />}
      </main>

      <footer className="footer">법률 판단은 하지 않습니다. 분쟁이 생기면 정리된 기록을 들고 공공 상담 창구를 이용하세요.</footer>
    </div>
  )
}
