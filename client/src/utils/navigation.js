/*
 * File:        navigation.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Decides where each role belongs in the application. Used after
 *              login to choose the right home page, and to make sure a user is
 *              never sent back to a page their role cannot open.
 */

import { ROLES } from './constants';

// Which roles may open each part of the application
const ROUTE_RULES = [
  { prefix: '/users', roles: [ROLES.BACKOFFICE] },
  { prefix: '/operations', roles: [ROLES.GRID_OPERATOR] },
  { prefix: '/prosumers', roles: [ROLES.BACKOFFICE, ROLES.GRID_OPERATOR] },
  { prefix: '/profile', roles: [ROLES.BACKOFFICE, ROLES.GRID_OPERATOR] },
];

/**
 * Returns the home page for a role: Backoffice officers get the administration
 * dashboard, Grid Operators get the operations home.
 */
export function homePathFor(role) {
  // Used by the login redirect, the sidebar and the Forbidden page
  return role === ROLES.BACKOFFICE ? '/' : '/operations';
}

/**
 * Returns true when the role is allowed to open the given address.
 */
export function canRoleAccess(role, pathname) {
  // An unknown address is treated as not allowed, so the caller falls back home
  if (!role || !pathname) return false;

  // The dashboard at the root is the Backoffice home page
  if (pathname === '/') return role === ROLES.BACKOFFICE;

  const rule = ROUTE_RULES.find((entry) => pathname.startsWith(entry.prefix));

  return rule ? rule.roles.includes(role) : false;
}
