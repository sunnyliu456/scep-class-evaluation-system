import { useCallback } from 'react'
import type { UploadProps } from 'antd'
import { postFormData } from '../upload/postFormData'
import type { UploadApplyResult } from '../types'
import { notify } from '../utils/notify'

type UploadRequestOptions = Parameters<NonNullable<UploadProps['customRequest']>>[0]

type UseUploadParams = {
  onApplied: (payload: UploadApplyResult, msg: string, stepIndex: number) => void
}

export const useUpload = ({ onApplied }: UseUploadParams) => {
  const commonUpload = useCallback(
    async (url: string, options: UploadRequestOptions, stepIndex: number, label: string) => {
      const file = options.file as File
      const form = new FormData()
      form.append('file', file)

      try {
        const res = await postFormData<UploadApplyResult>(url, form)

        if (res.code !== 0) {
          await notify('error', res.msg || `${label}上传失败`)
          options.onError?.(new Error(res.msg || `${label}上传失败`))
          return
        }

        const payload = res.data
        onApplied(payload, res.msg, stepIndex)
        await notify('success', res.msg)
        options.onSuccess?.({}, file)
      } catch (error) {
        console.error(error)
        await notify('error', `${label}上传失败，请检查后端是否启动`)
        options.onError?.(error as Error)
      }
    },
    [onApplied]
  )

  return { commonUpload }
}
