/*
 * File:        UserDetailsModal.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-21
 * Description: Read only Bootstrap 5 modal that shows every stored detail of
 *              one web user. The user list keeps only the main columns, so this
 *              dialog is where the username, NIC, phone number and date of
 *              birth are read. Like the confirmation dialog it is driven by
 *              React state rather than Bootstrap's JavaScript.
 */

import { RoleBadge, StatusBadge } from './Badges';
import { formatDate, formatDateTime } from '../utils/formatters';

export default function UserDetailsModal({ user, onClose, onEdit }) {
  // Rendering nothing keeps the modal out of the DOM entirely when hidden
  if (!user) return null;

  return (
    <>
      <div className="modal fade show d-block" tabIndex="-1" role="dialog">
        <div className="modal-dialog modal-dialog-centered modal-lg">
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">User details</h5>
              <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
            </div>

            <div className="modal-body">
              {/* Same fields as the add user form, in the same order */}
              <div className="row g-3">
                <DetailItem label="Full name" value={user.fullName} />
                <DetailItem label="Email address" value={user.email} />
                <DetailItem label="Username" value={user.username} />
                <DetailItem label="NIC number" value={user.nic} />
                <DetailItem label="Phone number" value={user.phone} />
                <DetailItem label="Date of birth" value={formatDate(user.dateOfBirth)} />

                <div className="col-md-6">
                  <p className="text-secondary small mb-1">Role</p>
                  <RoleBadge role={user.role} />
                </div>

                <div className="col-md-6">
                  <p className="text-secondary small mb-1">Status</p>
                  <StatusBadge isActive={user.isActive} />
                </div>

                <DetailItem label="Created" value={formatDateTime(user.createdAt)} />
                <DetailItem label="Last updated" value={formatDateTime(user.updatedAt)} />
              </div>

              {/* The password is never returned by the API, so it cannot be shown */}
              <p className="text-body-tertiary small mt-3 mb-0">
                Passwords are stored as a one-way hash and can never be displayed.
              </p>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose}>
                Close
              </button>
              <button type="button" className="btn btn-primary" onClick={() => onEdit(user)}>
                Edit user
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Dimmed background behind the dialog */}
      <div className="modal-backdrop fade show" />
    </>
  );
}

/**
 * One label and value pair inside the details grid.
 */
function DetailItem({ label, value }) {
  // Older accounts may not have every field filled in
  return (
    <div className="col-md-6">
      <p className="text-secondary small mb-1">{label}</p>
      <p className="mb-0 fw-medium">{value || '—'}</p>
    </div>
  );
}
