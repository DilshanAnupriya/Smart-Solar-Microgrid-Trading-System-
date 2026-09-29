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

import { NODE_STATUS, PROSUMER_STATUS, ROLE_LABELS, ROLES } from '../utils/constants';

/**
 * Shows the user's role, with Backoffice highlighted as the administrative role.
 */
export function RoleBadge({ role }) {
  // Backoffice is the role that unlocks system administration
  const isBackoffice = role === ROLES.BACKOFFICE;

  return (
    <span
      className={`badge rounded-pill border ${
        isBackoffice
          ? 'bg-primary-subtle text-primary-emphasis border-primary-subtle'
          : 'bg-info-subtle text-info-emphasis border-info-subtle'
      }`}
    >
      {ROLE_LABELS[role] || role}
    </span>
  );
}

/**
 * Shows whether the account is allowed to sign in.
 */
export function StatusBadge({ isActive }) {
  // Deactivated accounts stay in the database but cannot sign in
  return <StatusPill isActive={isActive} />;
}

/**
 * Shows whether a prosumer account is active or has been deactivated.
 */
export function ProsumerStatusBadge({ status }) {
  // The API sends the status as the text "Active" or "Deactivated"
  const isActive = status === PROSUMER_STATUS.ACTIVE;

  return <StatusPill isActive={isActive} />;
}

/**
 * Green "Active" or grey "Deactivated" pill with a coloured dot, shared by the
 * web user and prosumer badges so both look identical.
 */
function StatusPill({ isActive }) {
  // The dot comes from the .status-badge rule in index.css
  return (
    <span
      className={`badge rounded-pill border status-badge ${
        isActive
          ? 'bg-success-subtle text-success-emphasis border-success-subtle'
          : 'bg-secondary-subtle text-secondary-emphasis border-secondary-subtle'
      }`}
    >
      {isActive ? 'Active' : 'Deactivated'}
    </span>
  );
}

/** Shows whether a microgrid node is available for operations and nearby search. */
export function NodeStatusBadge({ isActive }) {
  return (
    <span className={`badge rounded-pill ${isActive ? 'text-bg-success' : 'text-bg-secondary'}`}>
      {isActive ? NODE_STATUS.ACTIVE : NODE_STATUS.INACTIVE}
    </span>
  );
}
