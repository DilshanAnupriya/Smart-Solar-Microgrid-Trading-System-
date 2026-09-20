/*
 * File:        ConfirmModal.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Bootstrap 5 modal used to confirm an action before it is sent to
 *              the Web API, for example deactivating a user account. The modal
 *              is controlled by React state rather than Bootstrap's JavaScript
 *              so its visibility follows the component's props.
 */

export default function ConfirmModal({
  show,
  title,
  message,
  confirmLabel = 'Confirm',
  confirmVariant = 'danger',
  busy = false,
  onConfirm,
  onCancel,
}) {
  // Rendering nothing keeps the modal out of the DOM entirely when hidden
  if (!show) return null;

  return (
    <>
      <div className="modal fade show d-block" tabIndex="-1" role="dialog">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onCancel}
                disabled={busy}
              />
            </div>

            <div className="modal-body">
              <p className="mb-0">{message}</p>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-outline-secondary" onClick={onCancel} disabled={busy}>
                Cancel
              </button>
              <button
                type="button"
                className={`btn btn-${confirmVariant}`}
                onClick={onConfirm}
                disabled={busy}
              >
                {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                {confirmLabel}
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
