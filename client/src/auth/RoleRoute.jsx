/*
 * File:        RoleRoute.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Auth
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Route guard that allows only the listed roles through. This is a
 *              convenience for the user interface: the real access control is
 *              the [Authorize(Roles = "Backoffice")] attribute on the Web API,
 *              which refuses the request even if a page is reached some other
 *              way.
 */

import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './useAuth';

export default function RoleRoute({ allowed = [] }) {
  const { hasRole } = useAuth();

  // A user without one of the allowed roles is shown the Forbidden page
  if (!hasRole(allowed)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
