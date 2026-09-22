/*
 * File:        ProfilePage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Shows the signed in user's own account details and lets them
 *              change their password. The Web API verifies the current password
 *              before it applies the change.
 */

import { useState } from 'react';
import PageHeader from '../components/PageHeader';
import { RoleBadge, StatusBadge } from '../components/Badges';
import { useAuth } from '../auth/useAuth';
import { useToast } from '../toast/useToast';
import { changePassword } from '../api/authApi';
import { formatDate, formatDateTime } from '../utils/formatters';
import { validateChangePasswordForm } from '../utils/validators';

const EMPTY_FORM = { currentPassword: '', newPassword: '', confirmPassword: '' };

export default function ProfilePage() {
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();

  const [values, setValues] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  /**
   * Keeps the form state in step with what the user types.
   */
  function handleChange(event) {
    const { name, value } = event.target;

    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  }

  /**
   * Validates the form and sends the password change to the Web API.
   */
  async function handleSubmit(event) {
    event.preventDefault();

    const errors = validateChangePasswordForm(values);
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);

    try {
      await changePassword(values.currentPassword, values.newPassword);

      // Clear the form so the typed passwords do not stay on screen
      setValues(EMPTY_FORM);
      showSuccess('Your password has been changed.');
    } catch (submitError) {
      showError(submitError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <PageHeader title="My profile" subtitle="Your account details and password" />

      <div className="row g-3">
        <div className="col-lg-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 fw-semibold mb-3">Account details</h2>

              <dl className="row mb-0 small">
                <dt className="col-5 text-secondary fw-normal">Full name</dt>
                <dd className="col-7">{user?.fullName}</dd>

                <dt className="col-5 text-secondary fw-normal">Email</dt>
                <dd className="col-7">{user?.email}</dd>

                <dt className="col-5 text-secondary fw-normal">Username</dt>
                <dd className="col-7">{user?.username || '—'}</dd>

                <dt className="col-5 text-secondary fw-normal">NIC number</dt>
                <dd className="col-7">{user?.nic || '—'}</dd>

                <dt className="col-5 text-secondary fw-normal">Phone</dt>
                <dd className="col-7">{user?.phone || '—'}</dd>

                <dt className="col-5 text-secondary fw-normal">Date of birth</dt>
                <dd className="col-7">{formatDate(user?.dateOfBirth)}</dd>

                <dt className="col-5 text-secondary fw-normal">Role</dt>
                <dd className="col-7"><RoleBadge role={user?.role} /></dd>

                <dt className="col-5 text-secondary fw-normal">Status</dt>
                <dd className="col-7"><StatusBadge isActive={user?.isActive} /></dd>

                <dt className="col-5 text-secondary fw-normal">Created</dt>
                <dd className="col-7">{formatDateTime(user?.createdAt)}</dd>
              </dl>
            </div>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 fw-semibold mb-3">Change password</h2>

              <form onSubmit={handleSubmit} noValidate>
                <div className="mb-3">
                  <label htmlFor="currentPassword" className="form-label">Current password</label>
                  <input
                    id="currentPassword"
                    name="currentPassword"
                    type="password"
                    autoComplete="current-password"
                    className={`form-control ${fieldErrors.currentPassword ? 'is-invalid' : ''}`}
                    value={values.currentPassword}
                    onChange={handleChange}
                    disabled={submitting}
                  />
                  {fieldErrors.currentPassword && (
                    <div className="invalid-feedback">{fieldErrors.currentPassword}</div>
                  )}
                </div>

                <div className="mb-3">
                  <label htmlFor="newPassword" className="form-label">New password</label>
                  <input
                    id="newPassword"
                    name="newPassword"
                    type="password"
                    autoComplete="new-password"
                    className={`form-control ${fieldErrors.newPassword ? 'is-invalid' : ''}`}
                    value={values.newPassword}
                    onChange={handleChange}
                    disabled={submitting}
                  />
                  {fieldErrors.newPassword && (
                    <div className="invalid-feedback">{fieldErrors.newPassword}</div>
                  )}
                </div>

                <div className="mb-4">
                  <label htmlFor="confirmPassword" className="form-label">Confirm new password</label>
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type="password"
                    autoComplete="new-password"
                    className={`form-control ${fieldErrors.confirmPassword ? 'is-invalid' : ''}`}
                    value={values.confirmPassword}
                    onChange={handleChange}
                    disabled={submitting}
                  />
                  {fieldErrors.confirmPassword && (
                    <div className="invalid-feedback">{fieldErrors.confirmPassword}</div>
                  )}
                </div>

                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                  Change password
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
