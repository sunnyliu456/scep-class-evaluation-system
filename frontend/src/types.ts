export interface StudentSummary {
  className: string
  studentNo: string
  name: string
  avgGpa: number
  minGpa: number
  gradeScore: number
  rewardScore: number
  sportsScore: number
  moralScore: number
  laborScore: number
  aestheticScore: number
  total: number
}

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface ClassRosterResponse {
  className: string
  studentCount: number
  students: StudentSummary[]
}

export interface UploadApplyResult {
  step: string
  matched: number
  unknown: number
  unknownStudentNos: string[]
  students: StudentSummary[]
}

export interface ScoreStats {
  count: number
  avg: number
  max: number
  min: number
  warningCount: number
}
