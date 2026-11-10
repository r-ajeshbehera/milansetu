import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import RequireRole from './components/RequireRole'
import Login from './pages/Login'
import NotificationsPage from './pages/NotificationsPage'
import Register from './pages/Register'
import EventsAdmin from './pages/admin/EventsAdmin'
import UsersAdmin from './pages/admin/UsersAdmin'
import FamilyHome from './pages/family/FamilyHome'
import MembersPage from './pages/family/MembersPage'
import ReportMissing from './pages/family/ReportMissing'
import CaseDetail from './pages/staff/CaseDetail'
import CasesPage from './pages/staff/CasesPage'
import DeskHome from './pages/staff/DeskHome'
import FoundForm from './pages/staff/FoundForm'
import PoliceDashboard from './pages/staff/PoliceDashboard'
import VolunteerPage from './pages/staff/VolunteerPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<RequireRole />}>
        <Route element={<Layout />}>
          <Route path="/cases" element={<CasesPage />} />
          <Route path="/cases/:id" element={<CaseDetail />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route element={<RequireRole roles={['FAMILY']} />}>
            <Route path="/family" element={<FamilyHome />} />
            <Route path="/family/members" element={<MembersPage />} />
            <Route path="/family/report-missing" element={<ReportMissing />} />
          </Route>
          <Route element={<RequireRole roles={['VOLUNTEER']} />}>
            <Route path="/volunteer" element={<VolunteerPage />} />
          </Route>
          <Route element={<RequireRole roles={['DESK']} />}>
            <Route path="/desk" element={<DeskHome />} />
            <Route path="/desk/found" element={<div className="max-w-2xl"><FoundForm goToCase /></div>} />
          </Route>
          <Route element={<RequireRole roles={['POLICE', 'ADMIN']} />}>
            <Route path="/police" element={<PoliceDashboard />} />
          </Route>
          <Route element={<RequireRole roles={['ADMIN']} />}>
            <Route path="/admin/events" element={<EventsAdmin />} />
            <Route path="/admin/users" element={<UsersAdmin />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
