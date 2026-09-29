/*
 * File:        SuccessAlert.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Dismissible Bootstrap alert shown after an action completes,
 *              for example after a user account is created or deactivated.
 */

export default function SuccessAlert({ message, onDismiss }) {
  // Nothing is rendered when there is no message to show
  if (!message) return null;

  return (
    <div className="alert alert-success d-flex align-items-start gap-2" role="status">
      <span aria-hidden="true">&#10003;</span>
      <div className="flex-grow-1">{message}</div>
      {onDismiss && (
        <button type="button" className="btn-close" aria-label="Close" onClick={onDismiss} />
      )}
    </div>
  );
}
