/*
 * File:        Badges.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Small Bootstrap badges used to show a user's role and whether
 *              their account is active. The prosumer badge is kept here as well
 *              so every status pill in the console looks the same.
 */

import { PROSUMER_STATUS, ROLE_LABELS, ROLES } from '../utils/constants';

/**
 * Shows the user's role, with Backoffice highlighted as the administrative role.
 */
export function RoleBadge({ role }) {
  // Backoffice is the role that unlocks system administration
  const isBackoffice = role === ROLES.BACKOFFICE;

  return (
    <span className={`badge rounded-pill ${isBackoffice ? 'text-bg-primary' : 'text-bg-info'}`}>
      {ROLE_LABELS[role] || role}
    </span>
  );
}

/**
 * Shows whether the account is allowed to sign in.
 */
export function StatusBadge({ isActive }) {
  // Deactivated accounts stay in the database but cannot sign in
  return (
    <span className={`badge rounded-pill ${isActive ? 'text-bg-success' : 'text-bg-secondary'}`}>
      {isActive ? 'Active' : 'Deactivated'}
    </span>
  );
}

/**
 * Shows whether a prosumer account is active or has been deactivated.
 */
export function ProsumerStatusBadge({ status }) {
  // The API sends the status as the text "Active" or "Deactivated"
  const isActive = status === PROSUMER_STATUS.ACTIVE;

  return (
    <span className={`badge rounded-pill ${isActive ? 'text-bg-success' : 'text-bg-secondary'}`}>
      {isActive ? 'Active' : 'Deactivated'}
    </span>
  );
}
