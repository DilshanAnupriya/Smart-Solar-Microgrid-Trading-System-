/*
 * File:        DashboardPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Home page for Backoffice officers. The counts are calculated
 *              from the user list returned by the Web API, so the figures are
 *              always live and never hard coded. Team mates add their own cards
 *              for microgrid nodes and reservations into the same grid.
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from '../components/Icon';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import { RoleBadge, StatusBadge } from '../components/Badges';
import { useAuth } from '../auth/useAuth';
import { useToast } from '../toast/useToast';
import { getUsers } from '../api/usersApi';
import { ROLES } from '../utils/constants';
import { initialsOf } from '../utils/formatters';

// Shortcuts shown in the "Quick actions" card
const QUICK_ACTIONS = [
  { to: '/users/new', icon: 'plus', title: 'Add a user', text: 'Create a Backoffice or Grid Operator account' },
  { to: '/users', icon: 'users', title: 'Manage users', text: 'Search, edit, deactivate or reactivate accounts' },
  { to: '/prosumers', icon: 'sun', title: 'Prosumers', text: 'Review solar prosumer accounts' },
  { to: '/profile', icon: 'user', title: 'My profile', text: 'Your details and password' },
];

export default function DashboardPage() {
  const { user } = useAuth();
  const { showError } = useToast();

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  /**
   * Loads every web user so the summary cards can be calculated.
   */
  const loadUsers = useCallback(async () => {
    // The dashboard reads the same endpoint as the user list page
    setLoading(true);

    try {
      const data = await getUsers();
      setUsers(data);
    } catch (fetchError) {
      showError(fetchError.message);
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // Recalculated only when the user list actually changes
  const stats = useMemo(() => {
    return {
      total: users.length,
      active: users.filter((u) => u.isActive).length,
      backoffice: users.filter((u) => u.role === ROLES.BACKOFFICE).length,
      operators: users.filter((u) => u.role === ROLES.GRID_OPERATOR).length,
      deactivated: users.filter((u) => !u.isActive).length,
    };
  }, [users]);

  // Share of each role as a percentage, for the split bar
  const backofficeShare = stats.total ? Math.round((stats.backoffice / stats.total) * 100) : 0;

  // Today's date, shown under the greeting
  const today = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  return (
    <>
      <PageHeader
        title={`Welcome back, ${user?.fullName?.split(' ')[0] || 'there'}`}
        subtitle={`Backoffice administration overview · ${today}`}
        actions={
          <Link to="/users/new" className="btn btn-primary d-flex align-items-center gap-2">
            <Icon name="plus" size={16} strokeWidth={2.2} />
            Add user
          </Link>
        }
      />

      {loading ? (
        <LoadingSpinner message="Loading dashboard…" />
      ) : (
        <>
          <div className="row g-3 mb-4">
            <StatCard
              label="Total web users"
              value={stats.total}
              hint={`${stats.active} active`}
              icon="users"
              tone="primary"
            />
            <StatCard
              label="Backoffice officers"
              value={stats.backoffice}
              hint="Administration access"
              icon="shield"
              tone="info"
            />
            <StatCard
              label="Grid Operators"
              value={stats.operators}
              hint="Operational access"
              icon="bolt"
              tone="success"
            />
            <StatCard
              label="Deactivated accounts"
              value={stats.deactivated}
              hint="Cannot sign in"
              icon="userOff"
              tone="secondary"
            />
          </div>

          <div className="row g-3">
            <div className="col-xl-7">
              <div className="card h-100">
                <div className="card-body">
                  <div className="card-heading">
                    <h2 className="card-title-sm">Recently added users</h2>
                    <Link to="/users" className="small text-decoration-none d-flex align-items-center gap-1">
                      View all
                      <Icon name="arrowRight" size={14} />
                    </Link>
                  </div>

                  {users.length === 0 ? (
                    <p className="text-secondary small mb-0">No users yet.</p>
                  ) : (
                    <ul className="list-unstyled mb-0">
                      {users.slice(0, 5).map((item) => (
                        <li
                          key={item.id}
                          className="d-flex align-items-center gap-3 py-2 border-bottom"
                        >
                          <span className="avatar-circle avatar-circle-sm" aria-hidden="true">
                            {initialsOf(item.fullName)}
                          </span>
                          <div className="flex-grow-1 min-w-0 lh-sm">
                            <div className="fw-semibold small text-truncate">{item.fullName}</div>
                            <div className="text-secondary small text-truncate">{item.email}</div>
                          </div>
                          <div className="d-none d-sm-flex gap-2">
                            <RoleBadge role={item.role} />
                            <StatusBadge isActive={item.isActive} />
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </div>

            <div className="col-xl-5 d-flex flex-column gap-3">
              <div className="card">
                <div className="card-body">
                  <h2 className="card-title-sm mb-3">Quick actions</h2>
                  <div className="d-flex flex-column gap-2">
                    {QUICK_ACTIONS.map((action) => (
                      <Link key={action.to} to={action.to} className="action-link">
                        <span className="stat-icon bg-primary-subtle text-primary-emphasis">
                          <Icon name={action.icon} size={20} />
                        </span>
                        <span className="lh-sm">
                          <span className="d-block fw-semibold small">{action.title}</span>
                          <span className="d-block text-secondary small">{action.text}</span>
                        </span>
                        <Icon name="chevronRight" size={18} className="action-arrow" />
                      </Link>
                    ))}
                  </div>
                </div>
              </div>

              <div className="card">
                <div className="card-body">
                  <h2 className="card-title-sm mb-3">Accounts by role</h2>
                  {stats.total === 0 ? (
                    <p className="text-secondary small mb-0">No users yet.</p>
                  ) : (
                    <>
                      <div
                        className="role-split mb-3"
                        role="img"
                        aria-label={`${stats.backoffice} Backoffice officers and ${stats.operators} Grid Operators`}
                      >
                        <div className="bg-info" style={{ width: `${backofficeShare}%` }} />
                        <div className="bg-success flex-grow-1" />
                      </div>
                      <div className="d-flex flex-wrap gap-4 small">
                        <span className="d-flex align-items-center gap-2">
                          <span className="legend-dot bg-info" />
                          Backoffice <strong>{stats.backoffice}</strong>
                        </span>
                        <span className="d-flex align-items-center gap-2">
                          <span className="legend-dot bg-success" />
                          Grid Operators <strong>{stats.operators}</strong>
                        </span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}

/**
 * One summary tile on the dashboard: an icon, the figure and a short hint.
 */
function StatCard({ label, value, hint, icon, tone }) {
  // Kept in this file because it is only ever used by the dashboard
  return (
    <div className="col-6 col-xl-3">
      <div className="card h-100">
        <div className="card-body stat-card">
          <span className={`stat-icon bg-${tone}-subtle text-${tone}-emphasis`}>
            <Icon name={icon} size={22} />
          </span>
          <div className="min-w-0">
            <p className="stat-label text-truncate">{label}</p>
            <p className="stat-value">{value}</p>
            <p className="stat-hint">{hint}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
