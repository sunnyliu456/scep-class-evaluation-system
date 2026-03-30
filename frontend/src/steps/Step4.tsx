import type { UploadFile } from 'antd'
import { Alert, Button, Space, Upload } from 'antd'
import type { StudentSummary } from '../types'
import { baseColumns } from './columns'
import SimpleTable from '../components/SimpleTable'

interface Props {
  students: StudentSummary[]
  lastMessage: string
  ready: boolean
  dormStudentFileList: UploadFile[]
  dormStarFileList: UploadFile[]
  onSelectDormStudentFile: (file: UploadFile) => void
  onSelectDormStarFile: (file: UploadFile) => void
  onClearDormStudentFile: () => void
  onClearDormStarFile: () => void
  onUploadDorm: () => void
  onNext: () => void
  onPrev: () => void
}

const Step4 = ({
  students,
  lastMessage,
  ready,
  dormStudentFileList,
  dormStarFileList,
  onSelectDormStudentFile,
  onSelectDormStarFile,
  onClearDormStudentFile,
  onClearDormStarFile,
  onUploadDorm,
  onNext,
  onPrev
}: Props) => {
  return (
    <div className="step-panel">
      <Alert
        message="第五步：上传劳育寝室数据（需要两个 Excel：宿舍名单 + 宿舍星级表）"
        type="info"
        showIcon
        className="mb-16"
      />

      <Alert
        message="1）宿舍名单表：包含学号、学生姓名、楼栋、寝室号（用来确定每个人住在哪个宿舍）"
        type="warning"
        showIcon
        className="mb-16"
      />

      <Upload
        accept=".xlsx,.xls"
        maxCount={1}
        fileList={dormStudentFileList}
        beforeUpload={(file) => {
          onSelectDormStudentFile(file as UploadFile)
          return false
        }}
        onRemove={() => {
          onClearDormStudentFile()
        }}
      >
        <Button>选择宿舍名单 Excel</Button>
      </Upload>

      <Alert
        message="2）宿舍星级表：包含楼栋、寝室号、评定结果（如 一星级~五星级）"
        type="warning"
        showIcon
        className="mt-16 mb-16"
      />

      <Upload
        accept=".xlsx,.xls"
        maxCount={1}
        fileList={dormStarFileList}
        beforeUpload={(file) => {
          onSelectDormStarFile(file as UploadFile)
          return false
        }}
        onRemove={() => {
          onClearDormStarFile()
        }}
      >
        <Button>选择宿舍星级 Excel</Button>
      </Upload>

      <div className="mt-16">
        <Button type="primary" onClick={onUploadDorm}>
          开始上传劳育数据
        </Button>
        <span className="hint-text">需先选好两份文件，再点击按钮。</span>
      </div>

      {lastMessage && <Alert message={lastMessage} type="success" showIcon className="mt-16" />}

      {students.length > 0 && (
        <SimpleTable
          className="mt-16"
          data={students}
          columns={[
            ...baseColumns,
            { title: '劳育得分(<=5)', dataIndex: 'laborScore', key: 'laborScore', width: 140 },
            { title: '当前总分', dataIndex: 'total', key: 'total', width: 140 }
          ]}
        />
      )}

      <div className="footer-bar">
        <Space>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" disabled={!ready} onClick={onNext}>
            下一步：汇总导出
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default Step4
