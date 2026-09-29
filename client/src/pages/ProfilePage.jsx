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
import Icon from '../components/Icon';
import PageHeader from '../components/PageHeader';
import { RoleBadge, StatusBadge } from '../components/Badges';
import { useAuth } from '../auth/useAuth';
import { useToast } from '../toast/useToast';
import { changePassword } from '../api/authApi';
import { formatDate, formatDateTime, initialsOf } from '../utils/formatters';
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

  // Read-only account details, shown as icon + label + value rows
  const details = [
    { icon: 'user', label: 'Full name', value: user?.fullName },
    { icon: 'mail', label: 'Email', value: user?.email },
    { icon: 'at', label: 'Username', value: user?.username || '—' },
    { icon: 'idCard', label: 'NIC number', value: user?.nic || '—' },
    { icon: 'phone', label: 'Phone', value: user?.phone || '—' },
    { icon: 'calendar', label: 'Date of birth', value: formatDate(user?.dateOfBirth) },
    { icon: 'clock', label: 'Account created', value: formatDateTime(user?.createdAt) },
  ];

  return (
    <>
      <PageHeader title="My profile" subtitle="Your account details and password" />

      {/* Summary card: who is signed in, their role and account status */}
      <div className="card mb-3">
        <div className="card-body profile-hero">
          <span className="avatar-circle avatar-circle-lg" aria-hidden="true">
            {initialsOf(user?.fullName)}
          </span>
          <div className="min-w-0">
            <h2 className="h4 fw-bold mb-1 text-truncate">{user?.fullName}</h2>
            <p className="text-secondary mb-2 text-truncate">{user?.email}</p>
            <div className="d-flex flex-wrap gap-2">
              <RoleBadge role={user?.role} />
              <StatusBadge isActive={user?.isActive} />
            </div>
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-lg-5">
          <div className="card h-100">
            <div className="card-body">
              <h2 className="card-title-sm mb-3">Account details</h2>

              <dl className="detail-list">
                {details.map((item) => (
                  <div className="detail-row" key={item.label}>
                    <Icon name={item.icon} size={18} />
                    <div className="min-w-0">
                      <dt>{item.label}</dt>
                      <dd>{item.value}</dd>
                    </div>
                  </div>
                ))}
              </dl>
            </div>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex align-items-start gap-3 mb-4">
                <span className="stat-icon bg-warning-subtle text-warning-emphasis">
                  <Icon name="key" size={20} />
                </span>
                <div>
                  <h2 className="card-title-sm mb-1">Change password</h2>
                  <p className="text-secondary small mb-0">
                    Use at least 6 characters, and choose something different from your current password.
                  </p>
                </div>
              </div>

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

                <div className="row g-3 mb-4">
                  <div className="col-md-6">
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

                  <div className="col-md-6">
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
                </div>

                <button type="submit" className="btn btn-primary px-4" disabled={submitting}>
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
