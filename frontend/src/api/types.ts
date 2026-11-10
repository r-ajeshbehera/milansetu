export type Role = 'FAMILY' | 'VOLUNTEER' | 'DESK' | 'POLICE' | 'ADMIN'
export type Gender = 'MALE' | 'FEMALE' | 'OTHER'
export type CaseStatus = 'REPORTED' | 'MATCHED' | 'AT_DESK' | 'REUNITED' | 'CLOSED'
export type ReportStatus = 'OPEN' | 'MATCHED' | 'RESOLVED' | 'CLOSED'

export interface Profile { id: number; fullName: string; email: string; phone: string | null; role: Role }
export interface LoginResponse { accessToken: string; expiresAt: string; user: Profile }
export interface UserRow { id: number; fullName: string; email: string; role: Role; active: boolean }
export interface MEvent { id: number; name: string; city: string; startDate: string; endDate: string; active: boolean }
export interface Desk { id: number; eventId: number; name: string; latitude: number; longitude: number; inChargeId: number | null; inChargeName: string | null; active: boolean }
export interface Staff { userId: number; fullName: string; role: Role; deskId: number | null }
export interface Member { id: number; eventId: number; eventName: string; fullName: string; age: number; gender: Gender; notes: string | null }
export interface ScanResponse { caseId: number; newCase: boolean; firstName: string; approxAge: number; gender: Gender; desk: { id: number; name: string; latitude: number; longitude: number; distanceKm: number | null }; instructions: string }
export interface MissingReport { id: number; eventId: number; memberId: number | null; personName: string; approxAge: number; gender: Gender; clothingColor: string; lastSeenLat: number; lastSeenLng: number; lastSeenAt: string; status: ReportStatus }
export interface FoundReport { id: number; eventId: number; deskId: number; deskName: string; source: 'QR_SCAN' | 'MANUAL'; registeredMember: boolean; approxAge: number; gender: Gender; clothingColor: string; foundLat: number | null; foundLng: number | null; foundAt: string; safeNotes: string | null; status: ReportStatus }
export interface CaseItem {
  id: number; eventId: number; status: CaseStatus; matchScore?: number; registeredMember: boolean; personName?: string
  missing?: MissingReport; found?: FoundReport; assignedDeskId?: number; assignedDeskName?: string; assignedOfficer?: string
  openedAt: string; matchedAt?: string; reunitedAt?: string; closedAt?: string; stale: boolean
}
export interface TimelineEntry { caseId: number; fromStatus: CaseStatus | null; status: CaseStatus; note: string | null; changedBy: string; changedAt: string }
export interface MatchSuggestion { foundReportId: number; foundCaseId: number | null; score: number; factors: { factor: string; points: number; detail: string }[]; distanceKm: number; minutesApart: number; found: FoundReport }
export interface Notification { id: number; caseId: number | null; type: string; message: string; read: boolean; createdAt: string }
export interface OutboxMessage { userId: number; subject: string; body: string; sentAt: string }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number }
export interface Dashboard {
  eventId: number; openCases: number; reunitedToday: number; totalMissing: number; totalFound: number
  awaitingConfirmation: number; awaitingHandover: number; avgMinutesToReunite: number | null; staleCases: number
  casesByStatus: Record<CaseStatus, number>; byDesk: { desk: string; foundReports: number }[]; trend: { date: string; opened: number; reunited: number }[]
}
export interface ApiError { status: number; code: string; message: string; fieldErrors?: Record<string, string>; details?: { attemptsLeft?: number } }
