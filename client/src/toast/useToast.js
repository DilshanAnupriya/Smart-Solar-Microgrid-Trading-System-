/*
 * File:        useToast.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Toast
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-21
 * Description: Hook that lets any page raise a success or error notification in
 *              the bottom left corner of the screen.
 */

import { useContext } from 'react';
import { ToastContext } from './ToastContext';

/**
 * Returns the toast helpers, failing loudly if the provider is missing.
 */
export function useToast() {
  // A null context means the component was rendered outside <ToastProvider>
  const context = useContext(ToastContext);

  if (!context) {
    throw new Error('useToast must be used inside a ToastProvider.');
  }

  return context;
}
