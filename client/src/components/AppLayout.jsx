/*
 * File:        AppLayout.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Shell around every signed in page: the Bootstrap navigation bar,
 *              the side menu and the content area. Menu items are filtered by
 *              the signed in user's role, so a Grid Operator never sees the
 *              Backoffice administration links. This is a convenience only —
 *              the Web API refuses the request regardless of what is shown.
 */

import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLE_LABELS, ROLES } from '../utils/constants';
import { initialsOf } from '../utils/formatters';

// Every menu item, together with the roles allowed to see it. Each role has its
// own home page, so "Dashboard" points at a different address for each of them.
const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: '■', end: true, roles: [ROLES.BACKOFFICE] },
  { to: '/operations', label: 'Operations', icon: '■', roles: [ROLES.GRID_OPERATOR] },
  { to: '/users', label: 'Users', icon: '●', roles: [ROLES.BACKOFFICE] },
  { to: '/profile', label: 'My Profile', icon: '▲', roles: [ROLES.BACKOFFICE, ROLES.GRID_OPERATOR] },
];

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth();

  // Only the items this user's role is allowed to see are rendered
  const visibleItems = NAV_ITEMS.filter((item) => hasRole(item.roles));

  return (
    <div className="app-shell">
      <nav className="navbar navbar-expand-lg app-navbar sticky-top">
        <div className="container-fluid px-3 px-lg-4">
          <span className="navbar-brand d-flex align-items-center gap-2 mb-0">
            <span className="brand-mark" aria-hidden="true">&#9728;</span>
            <span className="fw-semibold">Smart Solar Microgrid</span>
          </span>

          <button
            className="navbar-toggler"
            type="button"
            data-bs-toggle="collapse"
            data-bs-target="#mainNav"
            aria-controls="mainNav"
            aria-expanded="false"
            aria-label="Toggle navigation"
          >
            <span className="navbar-toggler-icon" />
          </button>

          <div className="collapse navbar-collapse justify-content-end" id="mainNav">
            <div className="d-flex align-items-center gap-3 mt-3 mt-lg-0">
              <div className="d-flex align-items-center gap-2">
                <span className="avatar-circle" aria-hidden="true">{initialsOf(user?.fullName)}</span>
                <div className="lh-sm">
                  <div className="fw-semibold small">{user?.fullName}</div>
                  <span className="badge rounded-pill text-bg-light border">
                    {ROLE_LABELS[user?.role] || user?.role}
                  </span>
                </div>
              </div>

              <button type="button" className="btn btn-sm btn-outline-light" onClick={logout}>
                Sign out
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="container-fluid">
        <div className="row g-0">
          <aside className="col-lg-2 app-sidebar border-end">
            <ul className="nav flex-column p-3 gap-1">
              {visibleItems.map((item) => (
                <li className="nav-item" key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center gap-2 rounded ${isActive ? 'active' : ''}`
                    }
                  >
                    <span className="small" aria-hidden="true">{item.icon}</span>
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </aside>

          <main className="col-lg-10 p-3 p-lg-4">
            {/* The matched page is rendered here */}
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
