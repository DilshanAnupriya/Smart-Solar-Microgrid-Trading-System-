/*
 * File:        ErrorAlert.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Dismissible Bootstrap alert used to show an error message
 *              returned by the Web API.
 */

export default function ErrorAlert({ message, onDismiss }) {
  // Nothing is rendered when there is no error to report
  if (!message) return null;

  return (
    <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
      <span aria-hidden="true">&#9888;</span>
      <div className="flex-grow-1">{message}</div>
      {onDismiss && (
        <button type="button" className="btn-close" aria-label="Close" onClick={onDismiss} />
      )}
    </div>
  );
}
