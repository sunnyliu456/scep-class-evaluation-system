import http from '../http'
import type { ApiResponse } from '../types'

export const postFormData = async <T,>(url: string, form: FormData) => {
  const res = await http.post<ApiResponse<T>>(url, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}
