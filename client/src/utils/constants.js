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
