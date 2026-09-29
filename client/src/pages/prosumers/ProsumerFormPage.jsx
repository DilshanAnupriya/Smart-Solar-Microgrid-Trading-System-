/*
 * File:        ProsumerFormPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Prosumers
 * Author:      B.D.A.Cooray (IT22189530)
 * Created:     2026-09-22
 * Description: One form used both to register a new prosumer through
 *              POST /api/prosumers and to edit an existing one through
 *              PUT /api/prosumers/{nic}. The NIC is the primary key of the
 *              record, so it is captured only when the account is created and
 *              shown read only afterwards; the password is likewise set once,
 *              at registration. A 409 from the API means the NIC or the email
 *              address already belongs to another prosumer, and that message is
 *              placed against the field that caused it.
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { useToast } from '../../toast/useToast';
import { createProsumer, getProsumerByNic, updateProsumer } from '../../api/prosumerApi';
import { validateProsumerForm } from '../../utils/validators';

const EMPTY_FORM = {
  nic: '',
  fullName: '',
  email: '',
  phone: '',
  address: '',
  password: '',
  confirmPassword: '',
};

export default function ProsumerFormPage() {
  const { nic } = useParams();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  // The presence of a NIC in the address decides create mode from edit mode
  const isEditMode = Boolean(nic);

  const [values, setValues] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);

  /**
   * Loads the prosumer being edited and fills the form with their details.
   */
  const loadProsumer = useCallback(async () => {
    // Only runs in edit mode; registration starts from a blank form
    setLoading(true);

    try {
      const existing = await getProsumerByNic(nic);

      setValues({
        nic: existing.nic,
        fullName: existing.fullName,
        email: existing.email,
        phone: existing.phone,
        address: existing.address,
        password: '',
        confirmPassword: '',
      });
    } catch (fetchError) {
      // A missing NIC or a refused profile leaves nothing to edit, so the
      // officer is sent back to the list with the API's explanation
      showError(fetchError.message);
      navigate('/prosumers', { replace: true });
    } finally {
      setLoading(false);
    }
  }, [nic, showError, navigate]);

  useEffect(() => {
    if (isEditMode) loadProsumer();
  }, [isEditMode, loadProsumer]);

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
   * Copies any per-field messages returned by the API onto the matching inputs.
   */
  function applyApiFieldErrors(submitError) {
    // Model validation returns an object keyed by field; the shared
    // ApiResponse envelope returns a plain list, which has no field to attach to
    const apiFieldErrors = submitError.fieldErrors;

    if (!apiFieldErrors || Array.isArray(apiFieldErrors)) return;

    setFieldErrors((previous) => ({
      ...previous,
      ...Object.fromEntries(
        Object.entries(apiFieldErrors).map(([key, messages]) => [
          // The API names fields in Pascal case, the form in camel case
          key.charAt(0).toLowerCase() + key.slice(1),
          Array.isArray(messages) ? messages[0] : messages,
        ]),
      ),
    }));
  }

  /**
   * Validates the form and sends it to the Web API.
   */
  async function handleSubmit(event) {
    event.preventDefault();

    // The same rules are enforced again by the API; this is only fast feedback
    const errors = validateProsumerForm(values, {
      requireNic: !isEditMode,
      requirePassword: !isEditMode,
    });
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);

    try {
      // The four fields that both create and update accept
      const common = {
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        address: values.address.trim(),
      };

      if (isEditMode) {
        // The NIC travels in the address, never in the body, because it
        // cannot be changed once the record exists
        await updateProsumer(nic, common);

        showSuccess(`${common.fullName} has been updated.`);
        navigate(`/prosumers/${nic}`, { replace: true });
      } else {
        const created = await createProsumer({
          ...common,
          nic: values.nic.trim().toUpperCase(),
          password: values.password,
        });

        showSuccess(`${common.fullName} has been registered.`);
        navigate(`/prosumers/${created.nic}`, { replace: true });
      }
    } catch (submitError) {
      // A 409 means the NIC or the email address is already taken; the API's
      // message says which, so it is shown against that field as well
      if (submitError.status === 409) {
        const clashingField = submitError.message.toLowerCase().includes('nic') ? 'nic' : 'email';

        setFieldErrors((previous) => ({ ...previous, [clashingField]: submitError.message }));
      }

      showError(submitError.message);
      applyApiFieldErrors(submitError);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <LoadingSpinner message="Loading prosumer…" />;
  }

  return (
    <>
      <PageHeader
        title={isEditMode ? 'Edit prosumer' : 'Add prosumer'}
        subtitle={
          isEditMode
            ? 'Update the contact details held for this prosumer'
            : 'Register a solar prosumer and create their account'
        }
      />

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit} noValidate>
            <div className="row g-3">
              <div className="col-md-6">
                <label htmlFor="nic" className="form-label">NIC number</label>
                <input
                  id="nic"
                  name="nic"
                  type="text"
                  className={`form-control ${fieldErrors.nic ? 'is-invalid' : ''}`}
                  placeholder="199012345678 or 901234567V"
                  value={values.nic}
                  onChange={handleChange}
                  /* The NIC is the primary key of the record and can never be
                     edited, so in edit mode it is shown for reference only */
                  disabled={submitting || isEditMode}
                  readOnly={isEditMode}
                />
                {fieldErrors.nic && <div className="invalid-feedback">{fieldErrors.nic}</div>}
                <div className="form-text">
                  {isEditMode
                    ? 'The NIC identifies the account and cannot be changed.'
                    : 'Old format: 9 digits then V or X. New format: 12 digits.'}
                </div>
              </div>

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
                  placeholder="name@example.lk"
                  value={values.email}
                  onChange={handleChange}
                  disabled={submitting}
                />
                {fieldErrors.email && <div className="invalid-feedback">{fieldErrors.email}</div>}
              </div>

              <div className="col-md-6">
                <label htmlFor="phone" className="form-label">Phone number</label>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  className={`form-control ${fieldErrors.phone ? 'is-invalid' : ''}`}
                  placeholder="0771234567"
                  value={values.phone}
                  onChange={handleChange}
                  disabled={submitting}
                />
                {fieldErrors.phone && <div className="invalid-feedback">{fieldErrors.phone}</div>}
              </div>

              <div className="col-12">
                <label htmlFor="address" className="form-label">Installation address</label>
                <textarea
                  id="address"
                  name="address"
                  rows="2"
                  className={`form-control ${fieldErrors.address ? 'is-invalid' : ''}`}
                  placeholder="House number, street, town"
                  value={values.address}
                  onChange={handleChange}
                  disabled={submitting}
                />
                {fieldErrors.address && <div className="invalid-feedback">{fieldErrors.address}</div>}
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
                      placeholder="At least 8 characters"
                      value={values.password}
                      onChange={handleChange}
                      disabled={submitting}
                    />
                    {fieldErrors.password && <div className="invalid-feedback">{fieldErrors.password}</div>}
                    <div className="form-text">The prosumer uses this to sign in to the mobile app.</div>
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
                {isEditMode ? 'Save changes' : 'Register prosumer'}
              </button>

              <button
                type="button"
                className="btn btn-outline-secondary"
                /* Cancelling in edit mode returns to the profile that was open */
                onClick={() => navigate(isEditMode ? `/prosumers/${nic}` : '/prosumers')}
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
