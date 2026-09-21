/*
 * File:        UserFormPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Users
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: One form used both to create a new web application user and to
 *              edit an existing one. The password is set only when the account
 *              is created; afterwards the owner changes it from their profile
 *              page. Field level errors returned by the Web API, such as a
 *              duplicate email, are shown against the field that caused them.
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { useToast } from '../../toast/useToast';
import { createUser, getUserById, updateUser } from '../../api/usersApi';
import { ROLE_OPTIONS, ROLES } from '../../utils/constants';
import { validateUserForm } from '../../utils/validators';

const EMPTY_FORM = {
  fullName: '',
  email: '',
  role: ROLES.GRID_OPERATOR,
  password: '',
  confirmPassword: '',
};

export default function UserFormPage() {
  const { id } = useParams();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  // The presence of an id in the address decides create mode from edit mode
  const isEditMode = Boolean(id);

  const [values, setValues] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);

  /**
   * Loads the account being edited and fills the form with its values.
   */
  const loadUser = useCallback(async () => {
    // Only runs in edit mode; create mode starts from a blank form
    setLoading(true);

    try {
      const existing = await getUserById(id);

      setValues({
        fullName: existing.fullName,
        email: existing.email,
        role: existing.role,
        password: '',
        confirmPassword: '',
      });
    } catch (fetchError) {
      showError(fetchError.message);
    } finally {
      setLoading(false);
    }
  }, [id, showError]);

  useEffect(() => {
    if (isEditMode) loadUser();
  }, [isEditMode, loadUser]);

  /**
   * Keeps the form state in step with what the user types.
   */
  function handleChange(event) {
    // The field's error is cleared as soon as it is edited
    const { name, value } = event.target;

    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  }

  /**
   * Validates the form and sends it to the Web API.
   */
  async function handleSubmit(event) {
    event.preventDefault();

    // The same rules are enforced again by the API; this is only fast feedback
    const errors = validateUserForm(values, { requirePassword: !isEditMode });
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);

    try {
      if (isEditMode) {
        await updateUser(id, {
          fullName: values.fullName.trim(),
          email: values.email.trim(),
          role: values.role,
        });

        showSuccess(`${values.fullName.trim()} has been updated.`);
      } else {
        await createUser({
          fullName: values.fullName.trim(),
          email: values.email.trim(),
          role: values.role,
          password: values.password,
        });

        showSuccess(`${values.fullName.trim()} has been created.`);
      }

      navigate('/users', { replace: true });
    } catch (submitError) {
      // 409 means the email is taken, so the message is shown both on that
      // field and as a notification in the bottom left corner
      if (submitError.status === 409) {
        setFieldErrors((previous) => ({ ...previous, email: submitError.message }));
      }

      showError(submitError.message);

      // Any per-field messages returned by model validation are shown too
      const apiFieldErrors = submitError.fieldErrors || {};

      if (Object.keys(apiFieldErrors).length > 0) {
        setFieldErrors((previous) => ({
          ...previous,
          ...Object.fromEntries(
            Object.entries(apiFieldErrors).map(([key, messages]) => [key, messages[0]]),
          ),
        }));
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <LoadingSpinner message="Loading account…" />;
  }

  return (
    <>
      <PageHeader
        title={isEditMode ? 'Edit user' : 'Add user'}
        subtitle={
          isEditMode
            ? 'Update the account details and role'
            : 'Create a Backoffice or Grid Operator account'
        }
      />

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit} noValidate>
            <div className="row g-3">
              <div className="col-md-6">
                <label htmlFor="fullName" className="form-label">Full name</label>
                <input
                  id="fullName"
                  name="fullName"
                  type="text"
                  className={`form-control ${fieldErrors.fullName ? 'is-invalid' : ''}`}
                  placeholder="e.g. Nimali Perera"
                  value={values.fullName}
                  onChange={handleChange}
                  disabled={submitting}
                />
                {fieldErrors.fullName && <div className="invalid-feedback">{fieldErrors.fullName}</div>}
              </div>

              <div className="col-md-6">
                <label htmlFor="email" className="form-label">Email address</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  className={`form-control ${fieldErrors.email ? 'is-invalid' : ''}`}
                  placeholder="name@smartsolar.lk"
                  value={values.email}
                  onChange={handleChange}
                  disabled={submitting}
                />
                {fieldErrors.email && <div className="invalid-feedback">{fieldErrors.email}</div>}
              </div>

              <div className="col-md-6">
                <label htmlFor="role" className="form-label">Role</label>
                <select
                  id="role"
                  name="role"
                  className={`form-select ${fieldErrors.role ? 'is-invalid' : ''}`}
                  value={values.role}
                  onChange={handleChange}
                  disabled={submitting}
                >
                  {ROLE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
                {fieldErrors.role && <div className="invalid-feedback">{fieldErrors.role}</div>}
                <div className="form-text">
                  Backoffice users reach system administration; Grid Operators reach operational tools.
                </div>
              </div>

              {/* The password is only ever set when the account is created */}
              {!isEditMode && (
                <>
                  <div className="col-md-6">
                    <label htmlFor="password" className="form-label">Password</label>
                    <input
                      id="password"
                      name="password"
                      type="password"
                      autoComplete="new-password"
                      className={`form-control ${fieldErrors.password ? 'is-invalid' : ''}`}
                      placeholder="At least 6 characters"
                      value={values.password}
                      onChange={handleChange}
                      disabled={submitting}
                    />
                    {fieldErrors.password && <div className="invalid-feedback">{fieldErrors.password}</div>}
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="confirmPassword" className="form-label">Confirm password</label>
                    <input
                      id="confirmPassword"
                      name="confirmPassword"
                      type="password"
                      autoComplete="new-password"
                      className={`form-control ${fieldErrors.confirmPassword ? 'is-invalid' : ''}`}
                      placeholder="Re-enter the password"
                      value={values.confirmPassword}
                      onChange={handleChange}
                      disabled={submitting}
                    />
                    {fieldErrors.confirmPassword && (
                      <div className="invalid-feedback">{fieldErrors.confirmPassword}</div>
                    )}
                  </div>
                </>
              )}
            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                {isEditMode ? 'Save changes' : 'Create user'}
              </button>

              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => navigate('/users')}
                disabled={submitting}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
