/*
 * File:        OperationsHomePage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Home page for Grid Operators. Signing in as a Grid Operator lands
 *              here rather than on the Backoffice dashboard, which is how the
 *              requirement "Grid Operators access operational tools" is shown in
 *              the user interface. The operational cards themselves are added by
 *              the team mates who own node management and slot booking.
 */

import { Link } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import { useAuth } from '../auth/useAuth';

export default function OperationsHomePage() {
  const { user } = useAuth();

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
        <div className="col-lg-6">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 fw-semibold mb-2">Microgrid nodes</h2>
              <p className="text-secondary small mb-0">
                Battery slot availability and node schedules appear here.
              </p>
              <Link to="/nodes" className="btn btn-primary btn-sm mt-3">Manage node slots</Link>
            </div>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 fw-semibold mb-2">Power trading bookings</h2>
              <p className="text-secondary small mb-0">
                Reservation monitoring appears here.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
