import type { UploadProps } from 'antd'
import http from '../http'
import type { ApiResponse } from '../types'

const CHUNK_THRESHOLD = 20 * 1024 * 1024
const CHUNK_SIZE = 5 * 1024 * 1024
const CHUNK_CONCURRENCY = 3
const CHUNK_SESSION_KEY = 'scep_chunk_upload_sessions_v1'

type ChunkStatusPayload = {
  uploadId: string
  fileName: string
  totalChunks: number
  uploadedChunks: number[]
}

type ChunkUploadOptions = {
  uploadScope: string
  completeUrl: string
  options: Parameters<NonNullable<UploadProps['customRequest']>>[0]
}

const buildFileFingerprint = (file: File, scope: string) => {
  return `${scope}:${file.name}:${file.size}:${file.lastModified}`
}

const readChunkSessions = (): Record<string, string> => {
  try {
    const raw = window.localStorage.getItem(CHUNK_SESSION_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw) as unknown
    if (!parsed || typeof parsed !== 'object') {
      return {}
    }
    return parsed as Record<string, string>
  } catch {
    return {}
  }
}

const writeChunkSessions = (sessions: Record<string, string>) => {
  window.localStorage.setItem(CHUNK_SESSION_KEY, JSON.stringify(sessions))
}

const setChunkSession = (fingerprint: string, uploadId: string) => {
  const sessions = readChunkSessions()
  sessions[fingerprint] = uploadId
  writeChunkSessions(sessions)
}

const removeChunkSession = (fingerprint: string) => {
  const sessions = readChunkSessions()
  if (!(fingerprint in sessions)) {
    return
  }
  delete sessions[fingerprint]
  writeChunkSessions(sessions)
}

const fetchUploadStatus = async (targetUploadId: string, file: File, totalChunks: number) => {
  const statusForm = new FormData()
  statusForm.append('uploadId', targetUploadId)
  const statusRes = await http.post<ApiResponse<ChunkStatusPayload>>('/api/upload/chunk/status', statusForm, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  if (statusRes.data.code !== 0 || !statusRes.data.data) {
    throw new Error(statusRes.data.msg || '查询分片状态失败')
  }
  const payload = statusRes.data.data
  if (payload.fileName !== file.name || payload.totalChunks !== totalChunks) {
    throw new Error('分片会话与当前文件不匹配')
  }
  return new Set(payload.uploadedChunks)
}

const initUploadSession = async (fileName: string, totalChunks: number) => {
  const initForm = new FormData()
  initForm.append('fileName', fileName)
  initForm.append('totalChunks', String(totalChunks))
  const initRes = await http.post<ApiResponse<{ uploadId: string }>>('/api/upload/chunk/init', initForm, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  if (initRes.data.code !== 0 || !initRes.data.data?.uploadId) {
    throw new Error(initRes.data.msg || '分片上传初始化失败')
  }
  return initRes.data.data.uploadId
}

const uploadChunkPart = async (file: File, uploadId: string, index: number) => {
  const start = index * CHUNK_SIZE
  const end = Math.min(file.size, start + CHUNK_SIZE)
  const blob = file.slice(start, end)
  const part = new File([blob], `${file.name}.part${index}`, { type: file.type || 'application/octet-stream' })

  const partForm = new FormData()
  partForm.append('uploadId', uploadId)
  partForm.append('chunkIndex', String(index))
  partForm.append('chunk', part)

  const partRes = await http.post<ApiResponse<Record<string, number>>>('/api/upload/chunk/part', partForm, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  if (partRes.data.code !== 0) {
    throw new Error(partRes.data.msg || `第 ${index + 1} 片上传失败`)
  }
}

const completeChunkUpload = async <T,>(uploadId: string, completeUrl: string) => {
  const completeForm = new FormData()
  completeForm.append('uploadId', uploadId)
  const completeRes = await http.post<ApiResponse<T>>(completeUrl, completeForm, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  if (completeRes.data.code !== 0) {
    throw new Error(completeRes.data.msg || '分片合并失败')
  }

  return completeRes.data
}

export const uploadSingleFile = async <T,>(url: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  const res = await http.post<ApiResponse<T>>(url, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

export const uploadFileWithChunks = async <T,>(file: File, payload: ChunkUploadOptions) => {
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  const fingerprint = buildFileFingerprint(file, payload.uploadScope)
  const sessions = readChunkSessions()
  let uploadId: string | undefined = sessions[fingerprint]
  let uploadedSet = new Set<number>()

  if (uploadId) {
    try {
      uploadedSet = await fetchUploadStatus(uploadId, file, totalChunks)
    } catch {
      uploadId = undefined
    }
  }

  if (!uploadId) {
    uploadId = await initUploadSession(file.name, totalChunks)
    setChunkSession(fingerprint, uploadId)
    uploadedSet = new Set<number>()
  }

  const pendingIndices: number[] = []
  for (let i = 0; i < totalChunks; i++) {
    if (!uploadedSet.has(i)) {
      pendingIndices.push(i)
    }
  }

  let uploadedCount = uploadedSet.size
  payload.options.onProgress?.({ percent: Number(((uploadedCount / totalChunks) * 95).toFixed(2)) })

  let cursor = 0
  const workerCount = Math.min(CHUNK_CONCURRENCY, pendingIndices.length || 1)
  const workers = Array.from({ length: workerCount }).map(async () => {
    while (cursor < pendingIndices.length) {
      const idx = pendingIndices[cursor]
      cursor += 1
      await uploadChunkPart(file, uploadId as string, idx)
      uploadedCount += 1
      const percent = Number(((uploadedCount / totalChunks) * 95).toFixed(2))
      payload.options.onProgress?.({ percent })
    }
  })
  await Promise.all(workers)

  payload.options.onProgress?.({ percent: 100 })
  const completeRes = await completeChunkUpload<T>(uploadId as string, payload.completeUrl)
  removeChunkSession(fingerprint)
  return completeRes
}

export const shouldUseChunkUpload = (file: File) => file.size >= CHUNK_THRESHOLD
