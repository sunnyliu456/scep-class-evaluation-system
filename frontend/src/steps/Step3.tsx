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
  onUploadMoral: UploadProps['customRequest']
  onNext: () => void
  onPrev: () => void
}

const Step3 = ({ students, lastMessage, ready, onUploadMoral, onNext, onPrev }: Props) => {
  return (
    <div className="step-panel">
      <Alert
        message="第四步：上传德育处分表（未出现在表中的同学默认为满分 15 分）"
        type="info"
        showIcon
        className="mb-16"
      />

      <Upload.Dragger customRequest={onUploadMoral} showUploadList={false} accept=".xlsx,.xls" className="upload-block">
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">上传德育 Excel</p>
      </Upload.Dragger>

      {lastMessage && <Alert message={lastMessage} type="success" showIcon className="mt-16" />}

      {students.length > 0 && (
        <SimpleTable
          className="mt-16"
          data={students}
          columns={[
            ...baseColumns,
            { title: '德育得分(<=15)', dataIndex: 'moralScore', key: 'moralScore', width: 140 },
            { title: '当前总分', dataIndex: 'total', key: 'total', width: 140 }
          ]}
        />
      )}

      <div className="footer-bar">
        <Space>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" disabled={!ready} onClick={onNext}>
            下一步：上传劳育
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default Step3
