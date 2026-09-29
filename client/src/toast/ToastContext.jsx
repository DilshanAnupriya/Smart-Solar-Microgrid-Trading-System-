/*
 * File:        ToastContext.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Toast
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-21
 * Description: Provides the application wide notification messages. Every
 *              success and error message raised by a page is collected here and
 *              rendered as a Bootstrap 5 toast fixed to the bottom left corner
 *              of the screen. Each toast closes itself after a few seconds and
 *              can also be dismissed by the user.
 */

import { createContext, useCallback, useMemo, useRef, useState } from 'react';

// Consumed through the useToast hook in ./useToast.js
export const ToastContext = createContext(null);

// How long a message stays on screen before it disappears, in milliseconds
const AUTO_DISMISS_MS = 4000;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  // Gives every toast a unique key without depending on the message text
  const nextId = useRef(0);

  /**
   * Removes one toast, used by the close button and by the auto dismiss timer.
   */
  const dismissToast = useCallback((id) => {
    // Filtering by id is safe even if the toast was already removed
    setToasts((previous) => previous.filter((toast) => toast.id !== id));
  }, []);

  /**
   * Adds a toast of the given kind and schedules it to disappear.
   */
  const showToast = useCallback(
    (message, variant) => {
      // Ignore an empty message rather than showing a blank toast
      if (!message) return;

      nextId.current += 1;
      const id = nextId.current;

      setToasts((previous) => [...previous, { id, message, variant }]);

      setTimeout(() => dismissToast(id), AUTO_DISMISS_MS);
    },
    [dismissToast],
  );

  /**
   * Shows a green confirmation message, for example after a user is created.
   */
  const showSuccess = useCallback((message) => showToast(message, 'success'), [showToast]);

  /**
   * Shows a red message for an invalid action or an error from the Web API.
   */
  const showError = useCallback((message) => showToast(message, 'danger'), [showToast]);

  // Memoised so consumers do not re-render on every provider render
  const value = useMemo(() => ({ showSuccess, showError }), [showSuccess, showError]);

  return (
    <ToastContext.Provider value={value}>
      {children}

      {/* Bootstrap toast container pinned to the bottom left of the viewport */}
      <div className="toast-container position-fixed bottom-0 start-0 p-3">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`toast show align-items-center text-bg-${toast.variant} border-0 mb-2`}
            role="alert"
            aria-live="assertive"
            aria-atomic="true"
          >
            <div className="d-flex">
              <div className="toast-body d-flex align-items-start gap-2">
                <span aria-hidden="true">{toast.variant === 'success' ? '✓' : '⚠'}</span>
                <span>{toast.message}</span>
              </div>

              <button
                type="button"
                className="btn-close btn-close-white me-2 m-auto"
                aria-label="Close"
                onClick={() => dismissToast(toast.id)}
              />
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
