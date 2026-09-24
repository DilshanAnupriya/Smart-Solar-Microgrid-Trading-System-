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
 *              attribute on the Web API's UsersController. The /prosumers
 *              routes are open to both staff roles, matching the
 *              [Authorize(Roles = "Backoffice,GridOperator")] attribute on
 *              ProsumersController.
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
import ProsumerDetailPage from './pages/prosumers/ProsumerDetailPage';
import ProsumerFormPage from './pages/prosumers/ProsumerFormPage';
import ProsumerListPage from './pages/prosumers/ProsumerListPage';
import NodeDetailsPage from './pages/nodes/NodeDetailsPage';
import NodeFormPage from './pages/nodes/NodeFormPage';
import NodeSchedulePage from './pages/nodes/NodeSchedulePage';
import NodeSlotsPage from './pages/nodes/NodeSlotsPage';
import NodesListPage from './pages/nodes/NodesListPage';
import UserFormPage from './pages/users/UserFormPage';
import UsersListPage from './pages/users/UsersListPage';
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

          {/* Prosumer management: both staff roles, matching
              [Authorize(Roles = "Backoffice,GridOperator")] on the API. The
              reactivate action inside the profile page is Backoffice only. */}
          <Route element={<RoleRoute allowed={[ROLES.BACKOFFICE, ROLES.GRID_OPERATOR]} />}>
            <Route path="prosumers" element={<ProsumerListPage />} />
            <Route path="prosumers/new" element={<ProsumerFormPage />} />
            <Route path="prosumers/:nic" element={<ProsumerDetailPage />} />
            <Route path="prosumers/:nic/edit" element={<ProsumerFormPage />} />
          </Route>

          {/* Node information is visible to both staff roles. */}
          <Route element={<RoleRoute allowed={[ROLES.BACKOFFICE, ROLES.GRID_OPERATOR]} />}>
            <Route path="nodes" element={<NodesListPage />} />
            <Route path="nodes/:id" element={<NodeDetailsPage />} />
          </Route>

          {/* Node creation, editing, schedules and status are Backoffice tasks. */}
          <Route element={<RoleRoute allowed={[ROLES.BACKOFFICE]} />}>
            <Route path="nodes/new" element={<NodeFormPage />} />
            <Route path="nodes/:id/edit" element={<NodeFormPage />} />
            <Route path="nodes/:id/schedule" element={<NodeSchedulePage />} />
          </Route>

          {/* Live battery availability is maintained by Grid Operators. */}
          <Route element={<RoleRoute allowed={[ROLES.GRID_OPERATOR]} />}>
            <Route path="nodes/:id/slots" element={<NodeSlotsPage />} />
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
