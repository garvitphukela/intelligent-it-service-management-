import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '@/lib/auth'
import type { Role } from '@/lib/types'
import { AppShell } from '@/components/AppShell'
import { LoginPage, RegisterPage } from '@/pages/Auth'
import { IncidentsPage } from '@/pages/Incidents'
import { ReportIncidentPage } from '@/pages/ReportIncident'
import { IncidentDetailPage } from '@/pages/IncidentDetail'
import { QueuePage } from '@/pages/Queue'
import { DashboardPage } from '@/pages/Dashboard'
import { UsersPage, ServicesPage, CategoriesPage, SlaPage, SettingsPage, NotificationLogPage } from '@/pages/Admin'
import { AuditPage } from '@/pages/Audit'
import { ProfilePage } from '@/pages/Profile'

function Protected({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="flex h-screen items-center justify-center"><span className="size-5 animate-spin rounded-full border-2 border-muted border-r-transparent" /></div>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/app" replace />
  return <>{children}</>
}

function Home() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  switch (user.role) {
    case 'EMPLOYEE': return <Navigate to="/app/incidents" replace />
    case 'TECHNICIAN':
    case 'TEAM_LEAD': return <Navigate to="/app/queue" replace />
    default: return <Navigate to="/app/dashboard" replace />
  }
}

const MGMT: Role[] = ['ADMIN', 'MANAGER']
const STAFF: Role[] = ['TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'MANAGER']

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/app" element={<Protected><AppShell /></Protected>}>
        <Route index element={<Home />} />
        <Route path="incidents" element={<IncidentsPage />} />
        <Route path="incidents/new" element={<ReportIncidentPage />} />
        <Route path="incidents/:id" element={<IncidentDetailPage />} />
        <Route path="queue" element={<Protected roles={['TECHNICIAN', 'TEAM_LEAD']}><QueuePage /></Protected>} />
        <Route path="dashboard" element={<Protected roles={STAFF}><DashboardPage /></Protected>} />
        <Route path="audit" element={<Protected roles={STAFF}><AuditPage /></Protected>} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="admin/users" element={<Protected roles={MGMT}><UsersPage /></Protected>} />
        <Route path="admin/services" element={<Protected roles={MGMT}><ServicesPage /></Protected>} />
        <Route path="admin/categories" element={<Protected roles={MGMT}><CategoriesPage /></Protected>} />
        <Route path="admin/sla" element={<Protected roles={MGMT}><SlaPage /></Protected>} />
        <Route path="admin/settings" element={<Protected roles={MGMT}><SettingsPage /></Protected>} />
        <Route path="admin/notifications" element={<Protected roles={MGMT}><NotificationLogPage /></Protected>} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}
