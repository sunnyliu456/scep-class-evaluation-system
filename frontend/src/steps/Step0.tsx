import type { UploadProps } from 'antd'
import { Alert, Button, Upload } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import type { StudentSummary } from '../types'
import { baseColumns } from './columns'
import SimpleTable from '../components/SimpleTable'

interface Props {
  className: string
  students: StudentSummary[]
  ready: boolean
  onUploadGradesZip: UploadProps['customRequest']
  onNext: () => void
}

const Step0 = ({ className, students, ready, onUploadGradesZip, onNext }: Props) => {
  return (
    <div className="step-panel">
      <Alert
        message="第一步：上传班级智育成绩 ZIP（文件名=班级名称，例如：信管2302.zip）"
        type="info"
        showIcon
        className="mb-16"
      />

      <Upload.Dragger customRequest={onUploadGradesZip} showUploadList={false} accept=".zip" className="upload-block">
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">将班级成绩 ZIP 拖到此处，或点击上传</p>
        <p className="ant-upload-hint">ZIP 名称 = 班级名称；内部为“学号+姓名.xlsx”，一人一表。</p>
      </Upload.Dragger>

      {className && (
        <Alert
          message={`已识别 ${students.length} 人的智育成绩，班级：${className}`}
          type="success"
          showIcon
          className="mt-16"
        />
      )}

      {students.length > 0 && (
        <SimpleTable
          className="mt-16"
          data={students}
          columns={[
            ...baseColumns,
            { title: '平均绩点', dataIndex: 'avgGpa', key: 'avgGpa', width: 120 },
            { title: '最低绩点', dataIndex: 'minGpa', key: 'minGpa', width: 120 },
            { title: '智育得分', dataIndex: 'gradeScore', key: 'gradeScore', width: 120 }
          ]}
        />
      )}

      <div className="footer-bar">
        <Button type="primary" disabled={!ready} onClick={onNext}>
          下一步：上传奖励
        </Button>
      </div>
    </div>
  )
}

export default Step0
