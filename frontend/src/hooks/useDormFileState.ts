import { useCallback, useState } from 'react'
import type { UploadFile } from 'antd'

const toRawFile = (file: UploadFile) =>
  ((file.originFileObj as File | undefined) ?? (file as unknown as File)) as File

export const useDormFileState = () => {
  const [dormStudentFile, setDormStudentFile] = useState<File | null>(null)
  const [dormStarFile, setDormStarFile] = useState<File | null>(null)
  const [dormStudentFileList, setDormStudentFileList] = useState<UploadFile[]>([])
  const [dormStarFileList, setDormStarFileList] = useState<UploadFile[]>([])

  const onSelectDormStudentFile = useCallback((file: UploadFile) => {
    setDormStudentFile(toRawFile(file))
    setDormStudentFileList([file])
  }, [])

  const onSelectDormStarFile = useCallback((file: UploadFile) => {
    setDormStarFile(toRawFile(file))
    setDormStarFileList([file])
  }, [])

  const onClearDormStudentFile = useCallback(() => {
    setDormStudentFile(null)
    setDormStudentFileList([])
  }, [])

  const onClearDormStarFile = useCallback(() => {
    setDormStarFile(null)
    setDormStarFileList([])
  }, [])

  return {
    dormStudentFile,
    dormStarFile,
    dormStudentFileList,
    dormStarFileList,
    onSelectDormStudentFile,
    onSelectDormStarFile,
    onClearDormStudentFile,
    onClearDormStarFile
  }
}
