/*
 * File:        ProtectedRoute.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Auth
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Route guard that allows only a signed in user through. The
 *              requested address is remembered so the user returns to it after
 *              signing in.
 */

import { Navigate, Outlet, useLocation } from 'react-router-dom';
import LoadingSpinner from '../components/LoadingSpinner';
import { useAuth } from './useAuth';

export default function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  // Wait for the stored session to be restored before deciding
  if (loading) {
    return <LoadingSpinner message="Checking your session…" fullPage />;
  }

  // Send an anonymous visitor to the login page, remembering where they wanted to go
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
