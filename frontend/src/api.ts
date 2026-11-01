export type Evidence = { field: string; quote: string; documentId: number | null; fileName: string | null }
export type Payment = {
  id: number | null
  label: string
  amount: number | null
  netAmount: number | null
  ruleType: string
  fixedDate: string | null
  offsetDays: number | null
  dueDate: string | null
  paid: boolean
  quote: string | null
  fileName: string | null
}
export type Contract = {
  id: number
  clientName: string | null
  projectName: string | null
  contractDate: string | null
  deliveryDate: string | null
  acceptanceDate: string | null
  totalAmount: number | null
  revisionLimit: number | null
  copyrightOwner: string | null
  withholding: boolean
  status: 'DRAFT' | 'CONFIRMED'
  notes: string[]
  documents: { id: number; fileName: string; mediaType: string }[]
  evidences: Evidence[]
  payments: Payment[]
}
export type ScheduleItem = {
  contractId: number
  clientName: string
  projectName: string | null
  type: 'PAYMENT' | 'DELIVERY'
  label: string
  date: string | null
  daysLeft: number | null
  overdue: boolean
  amount: number | null
  netAmount: number | null
  paymentId: number | null
  paid: boolean
}
export type AskResponse = {
  found: boolean
  answer: string
  sources: { documentId: number | null; clientName: string; fileName: string; text: string; score: number }[]
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message ?? '요청을 처리하지 못했습니다. 서버가 켜져 있는지 확인해 주세요.')
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

export const api = {
  meta: () => request<{ aiMode: string }>('/api/meta'),
  contracts: () => request<Contract[]>('/api/contracts'),
  schedule: () => request<ScheduleItem[]>('/api/schedule'),
  extract: (files: File[]) => {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    return request<Contract>('/api/contracts/extract', { method: 'POST', body: form })
  },
  update: (id: number, body: unknown) => request<Contract>(`/api/contracts/${id}`, json('PUT', body)),
  remove: (id: number) => request<void>(`/api/contracts/${id}`, { method: 'DELETE' }),
  setPaid: (paymentId: number, paid: boolean) => request<void>(`/api/payments/${paymentId}`, json('PATCH', { paid })),
  ask: (question: string, contractId: number | null) =>
    request<AskResponse>('/api/ask', json('POST', { question, contractId })),
}

export const won = (value: number | null | undefined) =>
  value == null ? '금액 미정' : `${value.toLocaleString('ko-KR')}원`

export const shortDate = (value: string | null) => {
  if (!value) return '미정'
  const [, month, day] = value.split('-')
  return `${Number(month)}월 ${Number(day)}일`
}
