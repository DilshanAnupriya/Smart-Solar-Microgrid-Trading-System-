/*
 * File:        NotFoundPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Shown for any address that does not match a known route.
 */

import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center text-center min-vh-100 p-3">
      <p className="display-4 fw-semibold text-primary mb-2">404</p>
      <h1 className="h4 fw-semibold mb-2">Page not found</h1>
      <p className="text-secondary mb-4">The page you asked for does not exist.</p>
      <Link to="/" className="btn btn-primary">Back to home</Link>
    </div>
  );
}
