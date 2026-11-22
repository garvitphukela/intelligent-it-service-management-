export type Role = 'EMPLOYEE' | 'TECHNICIAN' | 'TEAM_LEAD' | 'ADMIN' | 'MANAGER'
export type Priority = 'P1' | 'P2' | 'P3' | 'P4'
export type IncidentStatus = 'NEW' | 'CLASSIFIED' | 'ASSIGNED' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'AWAITING_INFO' | 'RESOLVED' | 'VERIFIED' | 'CLOSED' | 'CANCELLED'
export type SlaStatus = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET' | 'PAUSED' | 'NOT_APPLICABLE'
export type Impact = 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTENSIVE'
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface Ref { id: number; name: string }
export interface UserSummary { id: number; code: string; fullName: string; email: string; role: Role; title: string }
export interface UserView extends UserSummary {
  phone?: string; department?: string; active: boolean; createdAt: string
  skills?: string[]; maxWorkload?: number; experienceYears?: number; available?: boolean; workload?: number
  preferredChannels: string[]
}
export interface AuthResponse { token: string; expiresAt: string; user: UserView }
export interface ServiceView { id: number; name: string; description?: string; businessCriticality: number; userBase: number; ownerTeam?: string; active: boolean; dependsOn: Ref[] }
export interface CategoryView { id: number; name: string; description?: string; requiredSkill: string; keywords: string[]; active: boolean }
export interface SlaPolicyView { id: number; name: string; serviceId?: number; serviceName: string; priority: Priority; responseMinutes: number; resolutionMinutes: number; warningThresholdPercent: number }
export interface SlaView {
  policyName?: string; responseMinutes?: number; resolutionMinutes?: number; startedAt?: string
  responseDueAt?: string; resolutionDueAt?: string; respondedAt?: string; resolvedAt?: string; pausedAt?: string
  responseBreached: boolean; resolutionBreached: boolean; status: SlaStatus; elapsedPercent: number; timeScale: number
}
export interface NoteView { id: number; type: 'COMMENT' | 'WORK_NOTE' | 'INFO_REQUEST' | 'INFO_RESPONSE' | 'SYSTEM'; text: string; authorName: string; authorRole?: string; createdAt: string }
export interface IncidentSummary {
  id: number; number: string; title: string; status: IncidentStatus; priority?: Priority; category?: string; service: string; serviceId: number
  reporterName: string; reporterCode: string; technicianName?: string; technicianCode?: string; technicianId?: number
  slaStatus: SlaStatus; responseDueAt?: string; resolutionDueAt?: string; createdAt: string; updatedAt: string
  escalationLevel: number; majorIncident: boolean; masterNumber?: string
}
export interface IncidentDetail {
  id: number; number: string; title: string; description: string; status: IncidentStatus; priority?: Priority
  priorityScore: number; priorityRationale?: string; impact: Impact; urgency: Urgency; affectedUsers: number
  category?: Ref; categoryAutoAssigned: boolean; categoryConfidence: number; service: ServiceView
  reporter: UserSummary; technician?: UserSummary; sla: SlaView; escalationLevel: number; escalationLevelName: string
  resolution?: { summary: string; rootCause?: string; steps?: string; resolvedBy?: string; resolvedAt?: string }
  feedback?: { rating: number; comment?: string; submittedAt: string }
  closureNotes?: string; closedBy?: string; closedAt?: string; cancellationReason?: string
  masterIncident?: Ref; duplicates: Ref[]; majorIncident: boolean; notes: NoteView[]; allowedActions: string[]
  createdAt: string; updatedAt: string; version: number
}
export interface DuplicateView { id: number; number: string; title: string; status: IncidentStatus; service: string; similarity: number; likelyDuplicate: boolean; reasons: string[] }
export interface AuditView { id: number; timestamp: string; actorName: string; action: string; incidentId?: number; incidentNumber?: string; previousState?: string; newState?: string; description: string }
export interface TechnicianScoreView { technician: UserSummary; score: number; workload: number; maxWorkload: number; breakdown: Record<string, number>; reason: string }
export interface ResolutionSuggestion { incidentId: number; incidentNumber: string; title: string; similarity: number; summary: string; rootCause?: string; steps?: string }
export interface Analysis {
  categories: { id: number; name: string; confidence: number; matchedTerms: string[] }[]
  duplicates: DuplicateView[]
  resolutions: ResolutionSuggestion[]
  priority?: { priority: Priority; label: string; score: number; rationale: string; strategy: string; responseMinutes?: number; resolutionMinutes?: number }
}
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface NotificationView { id: number; channel: string; eventType: string; title: string; message: string; incidentId?: number; incidentNumber?: string; read: boolean; deliveryStatus?: string; createdAt: string; recipientName: string; recipientCode: string }
export interface Overview { total: number; open: number; critical: number; resolved: number; slaViolations: number; unassigned: number; awaitingInfo: number; majorIncidents: number; avgResponseMinutes?: number; avgResolutionMinutes?: number; slaCompliancePercent?: number; avgRating?: number; createdToday: number }
export interface TechnicianLoad { id: number; code: string; name: string; role: Role; active: number; maxWorkload: number; resolved: number; avgResolutionMinutes?: number; avgRating?: number; available: boolean; skills: string[] }
export interface ServiceStat { id: number; name: string; criticality: number; total: number; open: number; breached: number; p1: number; avgResolutionMinutes?: number }
export interface SlaCompliance { priority: Priority; total: number; met: number; breached: number; running: number; compliancePercent?: number; avgResponseMinutes?: number; avgResolutionMinutes?: number }
export interface TrendPoint { date: string; created: number; resolved: number; breached: number }
export interface Count { key: string; count: number }
export interface Correlation { rootService: Ref; incidents: IncidentSummary[]; affectedServices: string[]; directlyOnRoot: number; explanation: string }
export interface SlaRisk { incident: IncidentSummary; riskScore: number; predictedResolution: string; reason: string }
export interface StrategyView { key: string; name: string; description: string }
export interface SettingsView { assignmentStrategy: string; priorityStrategy: string; autoAssign: boolean; assignmentStrategies: StrategyView[]; priorityStrategies: StrategyView[]; slaTimeScale: number }
export interface WorkloadView { technician: UserView; active: number; maxWorkload: number; resolvedTotal: number; avgResolutionMinutes?: number; avgRating?: number; incidents: IncidentSummary[] }
export interface LiveEvent { type: string; incidentId: number; incidentNumber: string; title: string; description: string; priority?: Priority; status: IncidentStatus; service: string; actor: string; at: string }
