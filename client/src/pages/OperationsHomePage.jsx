/*
 * File:        OperationsHomePage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Modified:    2026-09-22 by H. Bhathiya (IT22189530) — connected live power trading
 *              booking metrics and quick actions for Grid Operators.
 * Description: Home page for Grid Operators. Signing in as a Grid Operator lands
 *              here rather than on the Backoffice dashboard. Provides live operational
 *              monitoring of energy slot reservations and hub status.
 */

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import { useAuth } from '../auth/useAuth';
import { getReservationStats } from '../api/reservationsApi';

export default function OperationsHomePage() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);

  useEffect(() => {
    let isMounted = true;
    getReservationStats()
      .then((data) => {
        if (isMounted) setStats(data);
      })
      .catch(() => {
        // Silently handle if API is initializing
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <>
      <PageHeader
        title={`Welcome back, ${user?.fullName?.split(' ')[0] || 'there'}`}
        subtitle="Grid Operator operational tools"
      />

      <div className="alert alert-info border-0 shadow-sm" role="status">
        You are signed in as a <strong>Grid Operator</strong>. System administration
        functions, including user management, are available to Backoffice officers only.
      </div>

      <div className="row g-3">
        {/* Microgrid Nodes Card (Teammate component) */}
        <div className="col-lg-6">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 fw-semibold mb-2">Microgrid nodes</h2>
              <p className="text-secondary small mb-3">
                Battery slot availability, hub schedules, and capacity telemetry.
              </p>
              <div className="d-flex gap-2">
                <button type="button" className="btn btn-outline-secondary btn-sm" disabled>
                  Hub Schedules
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Power Trading Bookings Card (Component 4: Energy Slot Reservation Management) */}
        <div className="col-lg-6">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex flex-column justify-content-between">
              <div>
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <h2 className="h6 fw-semibold mb-0">Power trading bookings</h2>
                  <span className="badge text-bg-primary">Component 4</span>
                </div>
                <p className="text-secondary small mb-3">
                  Live monitoring of energy drop-off and charging slot reservations.
                </p>

                {stats && (
                  <div className="row g-2 mb-3">
                    <div className="col-4">
                      <div className="p-2 bg-light rounded text-center border">
                        <div className="h5 fw-bold text-info mb-0">{stats.todayReservations}</div>
                        <div className="text-muted smaller">Today</div>
                      </div>
                    </div>
                    <div className="col-4">
                      <div className="p-2 bg-light rounded text-center border">
                        <div className="h5 fw-bold text-warning mb-0">{stats.pendingReservations}</div>
                        <div className="text-muted smaller">Pending</div>
                      </div>
                    </div>
                    <div className="col-4">
                      <div className="p-2 bg-light rounded text-center border">
                        <div className="h5 fw-bold text-success mb-0">{stats.approvedFutureReservations}</div>
                        <div className="text-muted smaller">Approved</div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                <Link to="/reservations" className="btn btn-primary btn-sm">
                  Monitor Bookings
                </Link>
                <Link to="/reservations/new" className="btn btn-outline-secondary btn-sm">
                  + New Booking
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
