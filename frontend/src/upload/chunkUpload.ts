import type { UploadProps } from 'antd'
import http from '../http'
import type { ApiResponse } from '../types'

// 超过该阈值的文件启用分片上传（20MB）
const CHUNK_THRESHOLD = 20 * 1024 * 1024
// 单片大小（5MB）
const CHUNK_SIZE = 5 * 1024 * 1024
// 并发上传分片的 worker 数
const CHUNK_CONCURRENCY = 3
// localStorage 中缓存上传会话的 key，用于断点续传
const CHUNK_SESSION_KEY = 'scep_chunk_upload_sessions_v1'

// 后端分片状态查询接口返回的数据结构
type ChunkStatusPayload = {
  // 分片上传会话 ID
  uploadId: string
  // 该会话对应的原始文件名
  fileName: string
  // 该文件总分片数
  totalChunks: number
  // 已上传完成的分片索引列表
  uploadedChunks: number[]
}

// 分片上传主流程所需参数
type ChunkUploadOptions = {
  // 上传作用域：用于和文件信息一起构成唯一指纹，避免不同业务间会话串用
  uploadScope: string
  // 上传完成后触发服务端合并的接口地址
  completeUrl: string
  // antd Upload 的 customRequest 回调参数（用于进度与状态回传）
  options: Parameters<NonNullable<UploadProps['customRequest']>>[0]
}

// 生成文件指纹：同一 scope 下，同名/同大小/同修改时间视为同一文件会话
const buildFileFingerprint = (file: File, scope: string) => {
  return `${scope}:${file.name}:${file.size}:${file.lastModified}`
}

// 读取本地缓存的上传会话表（fingerprint -> uploadId）
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

// 全量写回上传会话表
const writeChunkSessions = (sessions: Record<string, string>) => {
  window.localStorage.setItem(CHUNK_SESSION_KEY, JSON.stringify(sessions))
}

// 记录某个文件指纹对应的 uploadId，供断点续传复用
const setChunkSession = (fingerprint: string, uploadId: string) => {
  const sessions = readChunkSessions()
  sessions[fingerprint] = uploadId
  writeChunkSessions(sessions)
}

// 上传成功后移除本地会话，避免下次错误复用
const removeChunkSession = (fingerprint: string) => {
  const sessions = readChunkSessions()
  if (!(fingerprint in sessions)) {
    return
  }
  delete sessions[fingerprint]
  writeChunkSessions(sessions)
}

// 查询后端记录的已上传分片，用于断点续传
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
  // 额外做一次会话校验，防止误把其他文件会话当成当前文件
  if (payload.fileName !== file.name || payload.totalChunks !== totalChunks) {
    throw new Error('分片会话与当前文件不匹配')
  }
  // 转为 Set，便于 O(1) 判断某个分片是否已上传
  return new Set(payload.uploadedChunks)
}

// 初始化新的分片上传会话，向后端申请 uploadId
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

// 上传单个分片
const uploadChunkPart = async (file: File, uploadId: string, index: number) => {
  // 计算当前分片在原文件中的字节区间
  const start = index * CHUNK_SIZE
  const end = Math.min(file.size, start + CHUNK_SIZE)
  const blob = file.slice(start, end)
  // 构造分片文件对象，便于后端按 multipart/form-data 接收
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

// 通知后端执行分片合并，并返回最终业务接口结果
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

// 小文件直接普通上传，不走分片逻辑
export const uploadSingleFile = async <T,>(url: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  const res = await http.post<ApiResponse<T>>(url, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

// 分片上传主流程：会话恢复 -> 并发补传缺失分片 -> 合并
export const uploadFileWithChunks = async <T,>(file: File, payload: ChunkUploadOptions) => {
  // 总分片数向上取整，例如 10.1MB / 5MB => 3 片
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  const fingerprint = buildFileFingerprint(file, payload.uploadScope)
  const sessions = readChunkSessions()
  // 尝试从本地缓存恢复 uploadId
  let uploadId: string | undefined = sessions[fingerprint]
  // 已上传分片集合，默认空
  let uploadedSet = new Set<number>()

  if (uploadId) {
    try {
      // 如果后端仍保存该会话状态，则继续断点续传
      uploadedSet = await fetchUploadStatus(uploadId, file, totalChunks)
    } catch {
      // 任一异常都视为会话失效，后续重新初始化
      uploadId = undefined
    }
  }

  if (!uploadId) {
    // 首次上传或旧会话失效时，创建新会话并写入本地缓存
    uploadId = await initUploadSession(file.name, totalChunks)
    setChunkSession(fingerprint, uploadId)
    uploadedSet = new Set<number>()
  }

  // 计算仍需上传的分片索引
  const pendingIndices: number[] = []
  for (let i = 0; i < totalChunks; i++) {
    if (!uploadedSet.has(i)) {
      pendingIndices.push(i)
    }
  }

  // 上传阶段进度最多展示到 95%，预留给服务端合并阶段
  let uploadedCount = uploadedSet.size
  payload.options.onProgress?.({ percent: Number(((uploadedCount / totalChunks) * 95).toFixed(2)) })

  // 通过共享游标分配任务给多个并发 worker
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

  // 分片上传完成后将进度置为 100%，随后调用合并接口
  payload.options.onProgress?.({ percent: 100 })
  const completeRes = await completeChunkUpload<T>(uploadId as string, payload.completeUrl)
  // 合并成功后清理会话缓存，避免污染后续上传
  removeChunkSession(fingerprint)
  return completeRes
}

// 根据文件体积判断是否启用分片上传
export const shouldUseChunkUpload = (file: File) => file.size >= CHUNK_THRESHOLD
