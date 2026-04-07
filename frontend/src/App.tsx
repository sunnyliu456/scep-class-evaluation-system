import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import type { UploadProps } from 'antd'
import http from './http'
import { notify } from './utils/notify'
import { shouldUseChunkUpload, uploadFileWithChunks, uploadSingleFile } from './upload/chunkUpload'
import { postFormData } from './upload/postFormData'
import { useUpload } from './hooks/useUpload'
import { useStepFlow } from './hooks/useStepFlow'
import { useDormFileState } from './hooks/useDormFileState'
import type {
  ClassRosterResponse,
  ScoreStats,
  StudentSummary,
  UploadApplyResult
} from './types'

const Step0 = lazy(() => import('./steps/Step0'))
const Step1 = lazy(() => import('./steps/Step1'))
const Step2 = lazy(() => import('./steps/Step2'))
const Step3 = lazy(() => import('./steps/Step3'))
const Step4 = lazy(() => import('./steps/Step4'))
const Step5 = lazy(() => import('./steps/Step5'))

const stepItems = ['智育', '奖励', '体育', '德育', '劳育', '汇总导出']

const App = () => {
  const { step, stepReady, nextStep, prevStep, markStepReady } = useStepFlow(5, 5)
  const [className, setClassName] = useState('')
  const [students, setStudents] = useState<StudentSummary[]>([])
  const [lastMessage, setLastMessage] = useState('')
  const {
    dormStudentFile,
    dormStarFile,
    dormStudentFileList,
    dormStarFileList,
    onSelectDormStudentFile,
    onSelectDormStarFile,
    onClearDormStudentFile,
    onClearDormStarFile
  } = useDormFileState()

  const handleCommonUploadApplied = useCallback(
    (payload: UploadApplyResult, msg: string, stepIndex: number) => {
      setStudents(payload.students)
      setLastMessage(msg)
      markStepReady(stepIndex)
    },
    [markStepReady]
  )

  const { commonUpload } = useUpload({ onApplied: handleCommonUploadApplied })

  const onUploadGradesZip: UploadProps['customRequest'] = async (options) => {
    const file = options.file as File

    try {
      const res =
        shouldUseChunkUpload(file)
          ? await uploadFileWithChunks<ClassRosterResponse>(file, {
              uploadScope: 'gradesZip',
              completeUrl: '/api/import/gradesZip/chunk/complete',
              options
            })
          : await uploadSingleFile<ClassRosterResponse>('/api/import/gradesZip', file)

      if (res.code !== 0) {
        await notify('error', res.msg || '上传失败')
        options.onError?.(new Error(res.msg || '上传失败'))
        return
      }

      const payload = res.data
      if (!payload) {
        await notify('error', '服务端未返回班级结果')
        options.onError?.(new Error('服务端未返回班级结果'))
        return
      }

      setClassName(payload.className)
      setStudents(payload.students)
      setLastMessage(res.msg)
      markStepReady(0)
      await notify('success', res.msg)
      options.onSuccess?.({}, file)
    } catch (error) {
      console.error(error)
      await notify('error', '上传失败，请检查后端是否启动')
      options.onError?.(error as Error)
    }
  }

  const onUploadReward: UploadProps['customRequest'] = async (options) => {
    const file = options.file as File
    try {
      const res =
        shouldUseChunkUpload(file)
          ? await uploadFileWithChunks<UploadApplyResult>(file, {
              uploadScope: 'reward',
              completeUrl: '/api/import/reward/chunk/complete',
              options
            })
          : await uploadSingleFile<UploadApplyResult>('/api/import/reward', file)

      if (res.code !== 0) {
        await notify('error', res.msg || '奖励上传失败')
        options.onError?.(new Error(res.msg || '奖励上传失败'))
        return
      }

      const payload = res.data
      if (!payload) {
        await notify('error', '服务端未返回奖励结果')
        options.onError?.(new Error('服务端未返回奖励结果'))
        return
      }

      setStudents(payload.students)
      setLastMessage(res.msg)
      markStepReady(1)
      await notify('success', res.msg)
      options.onSuccess?.({}, file)
    } catch (error) {
      console.error(error)
      await notify('error', '奖励上传失败，请检查后端是否启动')
      options.onError?.(error as Error)
    }
  }

  const onUploadPe: UploadProps['customRequest'] = async (options) => {
    await commonUpload('/api/import/pe', options, 2, '体育')
  }

  const onUploadMoral: UploadProps['customRequest'] = async (options) => {
    await commonUpload('/api/import/moral', options, 3, '德育')
  }

  const uploadDorm = async () => {
    if (!dormStudentFile || !dormStarFile) {
      await notify('warning', '请先选择“宿舍名单表”和“宿舍星级表”两个文件')
      return
    }

    const form = new FormData()
    form.append('studentDormFile', dormStudentFile)
    form.append('starFile', dormStarFile)

    try {
      const res = await postFormData<UploadApplyResult>('/api/import/dorm', form)

      if (res.code !== 0) {
        await notify('error', res.msg || '劳育上传失败')
        return
      }

      const payload = res.data
      setStudents(payload.students)
      setLastMessage(res.msg)
      markStepReady(4)
      await notify('success', res.msg)
    } catch (error) {
      console.error(error)
      await notify('error', '劳育上传失败，请检查后端是否启动')
    }
  }

  const exportExcel = async () => {
    try {
      const res = await http.get('/api/summary/export', {
        responseType: 'blob'
      })
      const blob = new Blob([res.data], { type: 'application/vnd.ms-excel' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${className || '班级'}-综测汇总.xlsx`
      a.click()
      window.URL.revokeObjectURL(url)
      await notify('success', '导出成功')
    } catch (error) {
      console.error(error)
      await notify('error', '导出失败')
    }
  }

  const stats = useMemo<ScoreStats | null>(() => {
    if (!students.length) {
      return null
    }

    const scores = students.map((item) => item.total)
    const sum = scores.reduce((acc, cur) => acc + cur, 0)
    const avg = Number((sum / students.length).toFixed(2))
    const max = Number(Math.max(...scores).toFixed(2))
    const min = Number(Math.min(...scores).toFixed(2))
    const warningCount = scores.filter((score) => score < 60).length

    return { count: students.length, avg, max, min, warningCount }
  }, [students])

  useEffect(() => {
    const preloadMap = {
      0: () => import('./steps/Step1'),
      1: () => import('./steps/Step2'),
      2: () => import('./steps/Step3'),
      3: () => import('./steps/Step4'),
      4: () => import('./steps/Step5')
    } as const

    const preload = preloadMap[step as keyof typeof preloadMap]
    if (preload) {
      preload()
    }
  }, [step])

  const renderStep = () => {
    switch (step) {
      case 0:
        return (
          <Step0
            className={className}
            students={students}
            ready={stepReady[0]}
            onUploadGradesZip={onUploadGradesZip}
            onNext={nextStep}
          />
        )
      case 1:
        return (
          <Step1
            students={students}
            lastMessage={lastMessage}
            ready={stepReady[1]}
            onUploadReward={onUploadReward}
            onPrev={prevStep}
            onNext={nextStep}
          />
        )
      case 2:
        return (
          <Step2
            students={students}
            lastMessage={lastMessage}
            ready={stepReady[2]}
            onUploadPe={onUploadPe}
            onPrev={prevStep}
            onNext={nextStep}
          />
        )
      case 3:
        return (
          <Step3
            students={students}
            lastMessage={lastMessage}
            ready={stepReady[3]}
            onUploadMoral={onUploadMoral}
            onPrev={prevStep}
            onNext={nextStep}
          />
        )
      case 4:
        return (
          <Step4
            students={students}
            lastMessage={lastMessage}
            ready={stepReady[4]}
            dormStudentFileList={dormStudentFileList}
            dormStarFileList={dormStarFileList}
            onSelectDormStudentFile={onSelectDormStudentFile}
            onSelectDormStarFile={onSelectDormStarFile}
            onClearDormStudentFile={onClearDormStudentFile}
            onClearDormStarFile={onClearDormStarFile}
            onUploadDorm={uploadDorm}
            onPrev={prevStep}
            onNext={nextStep}
          />
        )
      case 5:
        return (
          <Step5
            className={className}
            students={students}
            stats={stats}
            onPrev={prevStep}
            onExportExcel={exportExcel}
          />
        )
      default:
        return null
    }
  }

  return (
    <div className="wizard-container">
      <h2 className="page-title">班级综合测评批量计算</h2>

      <div className="steps-lite">
        {stepItems.map((title, index) => (
          <div key={title} className={`steps-lite-item ${index <= step ? 'active' : ''}`}>
            <div className="steps-lite-dot">{index + 1}</div>
            <div className="steps-lite-text">{title}</div>
          </div>
        ))}
      </div>

      <Suspense fallback={<div className="step-panel loading-block">加载中...</div>}>
        {renderStep()}
      </Suspense>
    </div>
  )
}

export default App
