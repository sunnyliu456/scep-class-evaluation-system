import type { UploadProps } from 'antd'
import { Alert, Button, Space, Upload } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import type { StudentSummary } from '../types'
import { baseColumns } from './columns'
import SimpleTable from '../components/SimpleTable'

interface Props {
  students: StudentSummary[]
  lastMessage: string
  ready: boolean
  onUploadPe: UploadProps['customRequest']
  onNext: () => void
  onPrev: () => void
}

const Step2 = ({ students, lastMessage, ready, onUploadPe, onNext, onPrev }: Props) => {
  return (
    <div className="step-panel">
      <Alert message="第三步：上传体育（体测 + 锻炼）" type="info" showIcon className="mb-16" />

      <Upload.Dragger customRequest={onUploadPe} showUploadList={false} accept=".xlsx,.xls" className="upload-block">
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">上传体育 Excel</p>
      </Upload.Dragger>

      {lastMessage && <Alert message={lastMessage} type="success" showIcon className="mt-16" />}

      {students.length > 0 && (
        <SimpleTable
          className="mt-16"
          data={students}
          columns={[
            ...baseColumns,
            { title: '体育得分(<=5)', dataIndex: 'sportsScore', key: 'sportsScore', width: 140 },
            { title: '当前总分', dataIndex: 'total', key: 'total', width: 140 }
          ]}
        />
      )}

      <div className="footer-bar">
        <Space>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" disabled={!ready} onClick={onNext}>
            下一步：上传德育
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default Step2
