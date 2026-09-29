/*
 * File:        AppLayout.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Shell around every signed in page: a fixed side menu, a top bar
 *              with the signed in user and the content area. Menu items are
 *              filtered by the signed in user's role, so a Grid Operator never
 *              sees the Backoffice administration links. This is a convenience
 *              only — the Web API refuses the request regardless of what is
 *              shown. On small screens the side menu slides in from the left.
 *              Signing out asks for confirmation first, so the session is never
 *              ended by an accidental click.
 */

import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import ConfirmModal from './ConfirmModal';
import Icon from './Icon';
import { useAuth } from '../auth/useAuth';
import { useToast } from '../toast/useToast';
import { ROLE_LABELS, ROLES } from '../utils/constants';
import { initialsOf } from '../utils/formatters';

// Every menu item, together with the roles allowed to see it. Each role has its
// own home page, so "Dashboard" points at a different address for each of them.
const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: 'dashboard', end: true, roles: [ROLES.BACKOFFICE] },
  { to: '/operations', label: 'Operations', icon: 'bolt', roles: [ROLES.GRID_OPERATOR] },
  { to: '/users', label: 'Users', icon: 'users', roles: [ROLES.BACKOFFICE] },
  { to: '/prosumers', label: 'Prosumers', icon: 'sun', roles: [ROLES.BACKOFFICE, ROLES.GRID_OPERATOR] },
  { to: '/nodes', label: 'Microgrid Nodes', icon: 'node', roles: [ROLES.BACKOFFICE, ROLES.GRID_OPERATOR] },
  { to: '/profile', label: 'My Profile', icon: 'user', roles: [ROLES.BACKOFFICE, ROLES.GRID_OPERATOR] },
];

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth();
  const { showSuccess } = useToast();
  const location = useLocation();

  // True while the "are you sure?" sign out dialog is on screen
  const [confirmingSignOut, setConfirmingSignOut] = useState(false);

  // True while the side menu is slid open on a small screen
  const [menuOpen, setMenuOpen] = useState(false);

  // Only the items this user's role is allowed to see are rendered
  const visibleItems = NAV_ITEMS.filter((item) => hasRole(item.roles));

  // The menu item for the page being shown, used as the top bar's title
  const currentItem = visibleItems.find((item) =>
    item.end ? location.pathname === item.to : location.pathname.startsWith(item.to),
  );

  /**
   * Ends the session once the user has confirmed the dialog.
   */
  function handleConfirmSignOut() {
    // Clearing the session sends the router back to the login page
    setConfirmingSignOut(false);
    logout();
    showSuccess('You have been signed out.');
  }

  return (
    <div className="app-shell">
      <aside id="sideMenu" className={`app-sidebar ${menuOpen ? 'is-open' : ''}`}>
        <div className="sidebar-brand">
          <span className="brand-mark" aria-hidden="true">&#9728;</span>
          <div className="lh-sm">
            <div className="sidebar-brand-name">Smart Solar</div>
            <div className="sidebar-brand-tagline">Microgrid Console</div>
          </div>

          {/* Only shown on small screens, where the menu covers the page */}
          <button
            type="button"
            className="btn sidebar-close d-lg-none ms-auto"
            aria-label="Close menu"
            onClick={() => setMenuOpen(false)}
          >
            <Icon name="close" size={20} />
          </button>
        </div>

        <nav className="sidebar-nav" aria-label="Main navigation">
          <p className="sidebar-section-label">Menu</p>
          <ul className="nav flex-column gap-1">
            {visibleItems.map((item) => (
              <li className="nav-item" key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMenuOpen(false)}
                >
                  <Icon name={item.icon} />
                  <span>{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="sidebar-footer">
          Signed in as {ROLE_LABELS[user?.role] || user?.role}
        </div>
      </aside>

      {/* Dark layer behind the open menu on small screens; tapping it closes the menu */}
      {menuOpen && (
        <div className="sidebar-backdrop d-lg-none" aria-hidden="true" onClick={() => setMenuOpen(false)} />
      )}

      <div className="app-main">
        <header className="app-topbar">
          <button
            type="button"
            className="btn topbar-menu-button d-lg-none"
            aria-label="Open menu"
            aria-controls="sideMenu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen(true)}
          >
            <Icon name="menu" size={22} />
          </button>

          <div className="topbar-title">{currentItem?.label}</div>

          <div className="d-flex align-items-center gap-3 ms-auto">
            <div className="d-flex align-items-center gap-2">
              <span className="avatar-circle" aria-hidden="true">{initialsOf(user?.fullName)}</span>
              <div className="lh-sm d-none d-sm-block">
                <div className="fw-semibold small">{user?.fullName}</div>
                <div className="text-secondary topbar-role">{ROLE_LABELS[user?.role] || user?.role}</div>
              </div>
            </div>

            {/* Opens the confirmation dialog instead of signing out at once */}
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary d-flex align-items-center gap-2"
              aria-label="Sign out"
              onClick={() => setConfirmingSignOut(true)}
            >
              <Icon name="logout" size={16} />
              <span className="d-none d-sm-inline">Sign out</span>
            </button>
          </div>
        </header>

        <main className="app-content">
          {/* The matched page is rendered here */}
          <Outlet />
        </main>
      </div>

      {/* Nothing happens unless the user chooses "Sign out" in this dialog */}
      <ConfirmModal
        show={confirmingSignOut}
        title="Sign out"
        message="Are you sure you want to sign out of the management console?"
        confirmLabel="Sign out"
        confirmVariant="danger"
        onConfirm={handleConfirmSignOut}
        onCancel={() => setConfirmingSignOut(false)}
      />
    </div>
  );
}
