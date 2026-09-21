/*
 * File:        AuthContext.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Auth
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Holds the signed in user for the whole application. The token
 *              and the user summary are kept in localStorage so a page refresh
 *              does not sign the user out, and the stored token is re-checked
 *              against the API when the application starts.
 */

import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/authApi';
import { setUnauthorisedHandler } from '../api/axiosClient';
import { STORAGE_KEYS } from '../utils/constants';

// Consumed through the useAuth hook in ../auth/useAuth.js
export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);

  // Starts true so the router waits instead of flashing the login page
  const [loading, setLoading] = useState(true);

  /**
   * Clears the session from both React state and localStorage.
   */
  const logout = useCallback(() => {
    // Called by the logout button and by the 401 response interceptor
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
    localStorage.removeItem(STORAGE_KEYS.USER);
    setUser(null);
  }, []);

  /**
   * Signs in with an email address, username or phone number, stores the
   * token, and returns the user so the caller can redirect to the home page
   * that matches their role.
   */
  const login = useCallback(async (identifier, password) => {
    // The API performs the actual credential and status checks
    const result = await authApi.login(identifier, password);

    localStorage.setItem(STORAGE_KEYS.TOKEN, result.token);
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(result.user));
    setUser(result.user);

    return result.user;
  }, []);

  /**
   * Returns true when the signed in user holds one of the supplied roles.
   */
  const hasRole = useCallback(
    (allowed) => {
      // Used to hide navigation items; the API still enforces the real rule
      if (!user) return false;
      if (!allowed || allowed.length === 0) return true;

      return allowed.includes(user.role);
    },
    [user],
  );

  // Let the axios interceptor end the session when the API rejects the token
  useEffect(() => {
    setUnauthorisedHandler(logout);
  }, [logout]);

  // On start-up, restore the stored session and confirm it is still valid
  useEffect(() => {
    async function restoreSession() {
      const token = localStorage.getItem(STORAGE_KEYS.TOKEN);

      if (!token) {
        setLoading(false);
        return;
      }

      // Show the cached user immediately, then verify it against the API
      const cached = localStorage.getItem(STORAGE_KEYS.USER);
      if (cached) {
        try {
          setUser(JSON.parse(cached));
        } catch {
          localStorage.removeItem(STORAGE_KEYS.USER);
        }
      }

      try {
        const current = await authApi.getCurrentUser();
        localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(current));
        setUser(current);
      } catch {
        // An expired or revoked token ends the session
        logout();
      } finally {
        setLoading(false);
      }
    }

    restoreSession();
  }, [logout]);

  // Memoised so consumers do not re-render on every provider render
  const value = useMemo(
    () => ({ user, loading, isAuthenticated: Boolean(user), login, logout, hasRole }),
    [user, loading, login, logout, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
