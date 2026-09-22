/*
 * File:        ForbiddenPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Shown when a signed in user reaches a page their role does not
 *              allow, for example a Grid Operator opening /users directly.
 */

import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { homePathFor } from '../utils/navigation';

export default function ForbiddenPage() {
  const { user } = useAuth();

  // Send each role back to its own home page
  const homePath = homePathFor(user?.role);

  return (
    <div className="text-center py-5">
      <p className="display-4 fw-semibold text-warning mb-2">403</p>
      <h1 className="h4 fw-semibold mb-2">Access denied</h1>
      <p className="text-secondary mb-4">
        System administration functions are available to Backoffice officers only.
      </p>
      <Link to={homePath} className="btn btn-primary">Back to home</Link>
    </div>
  );
}
