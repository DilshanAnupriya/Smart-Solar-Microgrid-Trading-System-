/*
 * File:        LoadingSpinner.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Bootstrap 5 spinner shown while data is being fetched from the
 *              Web API.
 */

export default function LoadingSpinner({ message = 'Loading…', fullPage = false }) {
  // fullPage centres the spinner in the viewport, used by the route guards
  const wrapperClass = fullPage
    ? 'd-flex flex-column align-items-center justify-content-center vh-100'
    : 'd-flex flex-column align-items-center justify-content-center py-5';

  return (
    <div className={wrapperClass}>
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Loading</span>
      </div>
      <p className="text-secondary mt-3 mb-0">{message}</p>
    </div>
  );
}
