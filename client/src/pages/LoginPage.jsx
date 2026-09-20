/*
 * File:        LoginPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Sign in screen for Backoffice officers and Grid Operators. The
 *              credentials are checked by the Web API, and on success the user
 *              is redirected to the home page that matches their role.
 */

import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import ErrorAlert from '../components/ErrorAlert';
import { useAuth } from '../auth/useAuth';
import { canRoleAccess, homePathFor } from '../utils/navigation';
import { validateLoginForm } from '../utils/validators';

export default function LoginPage() {
  const { isAuthenticated, user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [values, setValues] = useState({ email: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Someone who is already signed in is sent to their own home page instead
  if (isAuthenticated) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }

  /**
   * Keeps the form state in step with what the user types.
   */
  function handleChange(event) {
    // Clearing the field's error as soon as it is edited keeps the form calm
    const { name, value } = event.target;

    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  }

  /**
   * Validates the form, signs in and redirects based on the returned role.
   */
  async function handleSubmit(event) {
    event.preventDefault();

    // Quick client side check first, so an obvious mistake costs no round trip
    const errors = validateLoginForm(values);
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    setFormError('');

    try {
      const user = await login(values.email.trim(), values.password);

      // Return the user to the page they originally asked for, but only when
      // their role is actually allowed to open it
      const requested = location.state?.from?.pathname;

      if (requested && requested !== '/login' && canRoleAccess(user.role, requested)) {
        navigate(requested, { replace: true });
        return;
      }

      // Otherwise send each role to its own home page: Backoffice officers get
      // the administration dashboard, Grid Operators get the operations home
      navigate(homePathFor(user.role), { replace: true });
    } catch (error) {
      // The API returns 401 for bad credentials and 403 for a deactivated account
      setFormError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page d-flex align-items-center justify-content-center min-vh-100 p-3">
      <div className="card login-card border-0 shadow-lg">
        <div className="card-body p-4 p-md-5">
          <div className="text-center mb-4">
            <span className="brand-mark brand-mark-lg d-inline-flex mb-3" aria-hidden="true">&#9728;</span>
            <h1 className="h4 fw-semibold mb-1">Smart Solar Microgrid</h1>
            <p className="text-secondary small mb-0">Sign in to the management console</p>
          </div>

          <ErrorAlert message={formError} onDismiss={() => setFormError('')} />

          <form onSubmit={handleSubmit} noValidate>
            <div className="mb-3">
              <label htmlFor="email" className="form-label">Email address</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                className={`form-control ${fieldErrors.email ? 'is-invalid' : ''}`}
                placeholder="you@smartsolar.lk"
                value={values.email}
                onChange={handleChange}
                disabled={submitting}
              />
              {fieldErrors.email && <div className="invalid-feedback">{fieldErrors.email}</div>}
            </div>

            <div className="mb-4">
              <label htmlFor="password" className="form-label">Password</label>
              <div className="input-group">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className={`form-control ${fieldErrors.password ? 'is-invalid' : ''}`}
                  placeholder="Enter your password"
                  value={values.password}
                  onChange={handleChange}
                  disabled={submitting}
                />
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setShowPassword((previous) => !previous)}
                  disabled={submitting}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
                {fieldErrors.password && <div className="invalid-feedback">{fieldErrors.password}</div>}
              </div>
            </div>

            <button type="submit" className="btn btn-primary w-100 py-2" disabled={submitting}>
              {submitting && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
              Sign in
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
