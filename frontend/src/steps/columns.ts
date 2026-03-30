import type { StudentSummary } from '../types'
import type { SimpleColumn } from '../components/SimpleTable'

export const baseColumns: SimpleColumn[] = [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_: StudentSummary, index: number) => index + 1
  },
  { title: '学号', dataIndex: 'studentNo', key: 'studentNo', width: 140 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 120 }
]
