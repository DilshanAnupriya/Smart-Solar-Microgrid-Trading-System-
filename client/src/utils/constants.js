/*
 * File:        constants.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Values shared across the client. The role names must match the
 *              strings used by the Web API exactly, so they are defined once
 *              here instead of being typed into each component.
 */

// The two web application roles, spelled exactly as the API stores them
export const ROLES = {
  BACKOFFICE: 'Backoffice',
  GRID_OPERATOR: 'GridOperator',
};

// Friendly labels shown in dropdowns, tables and badges
export const ROLE_LABELS = {
  [ROLES.BACKOFFICE]: 'Backoffice',
  [ROLES.GRID_OPERATOR]: 'Grid Operator',
};

// Every role, used to build the role dropdown and the role filter
export const ROLE_OPTIONS = [
  { value: ROLES.BACKOFFICE, label: ROLE_LABELS[ROLES.BACKOFFICE] },
  { value: ROLES.GRID_OPERATOR, label: ROLE_LABELS[ROLES.GRID_OPERATOR] },
];

// Keys used to remember the signed in session in the browser
export const STORAGE_KEYS = {
  TOKEN: 'smartsolar.token',
  USER: 'smartsolar.user',
};

// Reservation lifecycle states matching the API
export const RESERVATION_STATUS = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

export const RESERVATION_STATUS_LABELS = {
  [RESERVATION_STATUS.PENDING]: 'Pending Approval',
  [RESERVATION_STATUS.APPROVED]: 'Approved',
  [RESERVATION_STATUS.COMPLETED]: 'Completed',
  [RESERVATION_STATUS.CANCELLED]: 'Cancelled',
};

// Power trading transaction types
export const RESERVATION_TYPES = {
  DROP_OFF: 'DropOff',
  CHARGING: 'Charging',
};

export const RESERVATION_TYPE_LABELS = {
  [RESERVATION_TYPES.DROP_OFF]: 'Energy Drop-off (Feed-in)',
  [RESERVATION_TYPES.CHARGING]: 'Battery Charging (Draw)',
};

// Standard reference microgrid nodes for booking selection
export const REFERENCE_NODES = [
  { id: 'NODE-COLOMBO-01', name: 'Colombo Central Hub (120 kWh)', capacity: 120, slots: 8 },
  { id: 'NODE-KANDY-02', name: 'Kandy Hillcrest Station (80 kWh)', capacity: 80, slots: 6 },
  { id: 'NODE-GALLE-01', name: 'Galle Coastal Solar Grid (150 kWh)', capacity: 150, slots: 10 },
  { id: 'NODE-JAFFNA-03', name: 'Jaffna Northern Solar Park (200 kWh)', capacity: 200, slots: 12 },
];

// Prosumer account status, spelled exactly as the API's ProsumerStatus enum
// serialises it. The API rejects anything other than these three words.
// PendingDeactivation means the prosumer asked to deactivate from the mobile
// app: the account is blocked until a Backoffice officer approves or rejects.
export const PROSUMER_STATUS = {
  ACTIVE: 'Active',
  PENDING_DEACTIVATION: 'PendingDeactivation',
  DEACTIVATED: 'Deactivated',
};

// Used to build the status filter on the prosumer list page
export const PROSUMER_STATUS_OPTIONS = [
  { value: PROSUMER_STATUS.ACTIVE, label: 'Active' },
  { value: PROSUMER_STATUS.PENDING_DEACTIVATION, label: 'Deactivation requested' },
  { value: PROSUMER_STATUS.DEACTIVATED, label: 'Deactivated' },
];

// Microgrid nodes use soft deletion, represented by the boolean isActive field
export const NODE_STATUS = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
};
