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
  onUploadReward: UploadProps['customRequest']
  onNext: () => void
  onPrev: () => void
}

const Step1 = ({ students, lastMessage, ready, onUploadReward, onNext, onPrev }: Props) => {
  return (
    <div className="step-panel">
      <Upload.Dragger customRequest={onUploadReward} showUploadList={false} accept=".zip,.xlsx,.xls" className="upload-block">
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">上传奖励 ZIP 或 Excel（ZIP 内为“学号+姓名.xlsx”，一人一表）</p>
      </Upload.Dragger>

      {lastMessage && <Alert message={lastMessage} type="success" showIcon className="mt-16" />}

      {students.length > 0 && (
        <SimpleTable
          className="mt-16"
          data={students}
          columns={[
            ...baseColumns,
            { title: '智育得分', dataIndex: 'gradeScore', key: 'gradeScore', width: 120 },
            { title: '奖励加分(<=5)', dataIndex: 'rewardScore', key: 'rewardScore', width: 140 },
            { title: '当前总分', dataIndex: 'total', key: 'total', width: 140 }
          ]}
        />
      )}

      <div className="footer-bar">
        <Space>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" disabled={!ready} onClick={onNext}>
            下一步：上传体育
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default Step1
