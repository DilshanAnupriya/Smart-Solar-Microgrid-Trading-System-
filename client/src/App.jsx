/*
 * File:        App.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Application
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Declares every route in the web application and wraps them in
 *              the guards that enforce sign in and role based access. The
 *              /users routes are wrapped in RoleRoute so only Backoffice users
 *              reach them, matching the [Authorize(Roles = "Backoffice")]
 *              attribute on the Web API's UsersController.
 */

import { Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './auth/ProtectedRoute';
import RoleRoute from './auth/RoleRoute';
import DashboardPage from './pages/DashboardPage';
import ForbiddenPage from './pages/ForbiddenPage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import OperationsHomePage from './pages/OperationsHomePage';
import ProfilePage from './pages/ProfilePage';
import UserFormPage from './pages/users/UserFormPage';
import UsersListPage from './pages/users/UsersListPage';
import ReservationsListPage from './pages/reservations/ReservationsListPage';
import ReservationFormPage from './pages/reservations/ReservationFormPage';
import { ROLES } from './utils/constants';

export default function App() {
  return (
    <Routes>
      {/* Public route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Everything below requires a signed in user */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          {/* Backoffice home: the administration dashboard */}
          <Route element={<RoleRoute allowed={[ROLES.BACKOFFICE]} />}>
            <Route index element={<DashboardPage />} />
          </Route>

          {/* Grid Operator home: the operational tools */}
          <Route element={<RoleRoute allowed={[ROLES.GRID_OPERATOR]} />}>
            <Route path="operations" element={<OperationsHomePage />} />
          </Route>

          {/* User management: Backoffice only */}
          <Route element={<RoleRoute allowed={[ROLES.BACKOFFICE]} />}>
            <Route path="users" element={<UsersListPage />} />
            <Route path="users/new" element={<UserFormPage />} />
            <Route path="users/:id/edit" element={<UserFormPage />} />
          </Route>

          {/* Energy Slot Reservation Management: Backoffice and Grid Operator */}
          <Route element={<RoleRoute allowed={[ROLES.BACKOFFICE, ROLES.GRID_OPERATOR]} />}>
            <Route path="reservations" element={<ReservationsListPage />} />
            <Route path="reservations/new" element={<ReservationFormPage />} />
            <Route path="reservations/:id/edit" element={<ReservationFormPage />} />
          </Route>

          {/* Available to both roles */}
          <Route path="profile" element={<ProfilePage />} />
          <Route path="forbidden" element={<ForbiddenPage />} />
        </Route>
      </Route>

      {/* Any unknown address */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
