/*
 * File:        useAuth.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Auth
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Hook that gives any component access to the signed in user and
 *              the login, logout and role checking helpers.
 */

import { useContext } from 'react';
import { AuthContext } from './AuthContext';

/**
 * Returns the authentication context, failing loudly if the provider is missing.
 */
export function useAuth() {
  // A null context means the component was rendered outside <AuthProvider>
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider.');
  }

  return context;
}
