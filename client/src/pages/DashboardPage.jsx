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
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import { useAuth } from '../auth/useAuth';
import { useToast } from '../toast/useToast';
import { getUsers } from '../api/usersApi';
import { getReservationStats } from '../api/reservationsApi';
import { ROLES } from '../utils/constants';

export default function DashboardPage() {
  const { user } = useAuth();
  const { showError } = useToast();

  const [users, setUsers] = useState([]);
  const [reservationStats, setReservationStats] = useState(null);
  const [loading, setLoading] = useState(true);

  /**
   * Loads web users and reservation stats so summary cards can be calculated.
   */
  const loadDashboardData = useCallback(async () => {
    setLoading(true);

    try {
      const [usersData, resStats] = await Promise.all([
        getUsers(),
        getReservationStats().catch(() => null),
      ]);
      setUsers(usersData);
      setReservationStats(resStats);
    } catch (fetchError) {
      showError(fetchError.message);
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Recalculated only when the user list actually changes
  const stats = useMemo(() => {
    return {
      total: users.length,
      backoffice: users.filter((u) => u.role === ROLES.BACKOFFICE).length,
      operators: users.filter((u) => u.role === ROLES.GRID_OPERATOR).length,
      deactivated: users.filter((u) => !u.isActive).length,
    };
  }, [users]);

  return (
    <>
      <PageHeader
        title={`Welcome back, ${user?.fullName?.split(' ')[0] || 'there'}`}
        subtitle="Backoffice administration overview"
      />

      {loading ? (
        <LoadingSpinner message="Loading dashboard…" />
      ) : (
        <>
          <div className="row g-3 mb-3">
            <StatCard label="Total web users" value={stats.total} tone="primary" />
            <StatCard label="Backoffice officers" value={stats.backoffice} tone="info" />
            <StatCard label="Grid Operators" value={stats.operators} tone="success" />
            <StatCard label="Deactivated accounts" value={stats.deactivated} tone="secondary" />
          </div>

          {/* Component 4: Power Trading Reservation Metrics */}
          {reservationStats && (
            <div className="row g-3 mb-4">
              <StatCard label="Active energy bookings" value={reservationStats.approvedFutureReservations} tone="primary" />
              <StatCard label="Pending slot approvals" value={reservationStats.pendingReservations} tone="warning" />
              <StatCard label="Today's scheduled transfers" value={reservationStats.todayReservations} tone="info" />
              <StatCard label="Completed transfers" value={reservationStats.completedReservations} tone="success" />
            </div>
          )}

          <div className="row g-3">
            <div className="col-lg-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <h2 className="h6 fw-semibold mb-3">Quick actions</h2>
                  <div className="d-flex flex-wrap gap-2">
                    <Link to="/reservations" className="btn btn-primary btn-sm">Manage reservations</Link>
                    <Link to="/reservations/new" className="btn btn-outline-primary btn-sm">+ New booking</Link>
                    <Link to="/users/new" className="btn btn-outline-secondary btn-sm">Add a user</Link>
                    <Link to="/users" className="btn btn-outline-secondary btn-sm">Manage users</Link>
                    <Link to="/profile" className="btn btn-outline-secondary btn-sm">My profile</Link>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-lg-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <h2 className="h6 fw-semibold mb-3">Recently added users</h2>

                  {users.length === 0 ? (
                    <p className="text-secondary small mb-0">No users yet.</p>
                  ) : (
                    <ul className="list-unstyled mb-0">
                      {users.slice(0, 5).map((item) => (
                        <li
                          key={item.id}
                          className="d-flex justify-content-between align-items-center py-2 border-bottom"
                        >
                          <span className="small">{item.fullName}</span>
                          <span className="text-secondary small">{item.email}</span>
                        </li>
                      ))}
                    </ul>
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
 * One summary tile on the dashboard.
 */
function StatCard({ label, value, tone }) {
  // Kept in this file because it is only ever used by the dashboard
  return (
    <div className="col-6 col-xl-3">
      <div className="card border-0 shadow-sm h-100">
        <div className="card-body">
          <p className="text-secondary small text-uppercase mb-1">{label}</p>
          <p className={`display-6 fw-semibold mb-0 text-${tone}`}>{value}</p>
        </div>
      </div>
    </div>
  );
}
