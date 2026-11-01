import { useState } from 'react'
import { api } from './api'
import type { Contract } from './api'

const ACCEPT = '.pdf,.png,.jpg,.jpeg,.txt'

export function UploadPage({ onDone }: { onDone: (contract: Contract) => Promise<void> }) {
  const [files, setFiles] = useState<File[]>([])
  const [busy, setBusy] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const add = (list: FileList | null) => {
    if (list) setFiles((current) => [...current, ...Array.from(list)])
  }

  const submit = async () => {
    setBusy(true)
    setError(null)
    try {
      await onDone(await api.extract(files))
    } catch (e) {
      setError((e as Error).message)
      setBusy(false)
    }
  }

  return (
    <section className="panel narrow">
      <h2>기록 올리기</h2>
      <p className="muted">한 계약에 관한 견적서, 계약서, 카카오톡 캡처, 메일을 함께 올려 주세요. AI가 시간순으로 읽고 최종 조건을 정리합니다.</p>

      <label
        className={dragging ? 'dropzone dragging' : 'dropzone'}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          add(e.dataTransfer.files)
        }}
      >
        <input type="file" multiple accept={ACCEPT} hidden onChange={(e) => add(e.target.files)} />
        <b>파일을 끌어다 놓거나 눌러서 선택</b>
        <span>PDF, PNG, JPG, TXT · 파일당 20MB까지</span>
      </label>

      {files.length > 0 && (
        <ul className="file-list">
          {files.map((file, index) => (
            <li key={`${file.name}-${index}`}>
              <span>{file.name}</span>
              <button className="link" disabled={busy} onClick={() => setFiles(files.filter((_, i) => i !== index))}>
                빼기
              </button>
            </li>
          ))}
        </ul>
      )}

      {error && <div className="error-box">{error}</div>}
      <button className="primary" disabled={files.length === 0 || busy} onClick={submit}>
        {busy ? 'AI가 기록을 읽는 중입니다...' : '계약 조건 정리하기'}
      </button>
      <p className="muted small">실제 고객 정보가 담긴 자료를 올릴 때는 다른 사람의 개인정보에 주의해 주세요.</p>
    </section>
  )
}
