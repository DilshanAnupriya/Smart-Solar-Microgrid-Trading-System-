/*
 * File:        formatters.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Small display helpers used by the tables and detail panels.
 */

/**
 * Formats an ISO date string from the API as a readable local date.
 */
export function formatDate(value) {
  // Guard against a missing or invalid date rather than showing "Invalid Date"
  if (!value) return '—';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  return date.toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

/**
 * Formats an ISO date string as a readable local date and time.
 */
export function formatDateTime(value) {
  // Used where the exact time matters, such as the profile page
  if (!value) return '—';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  return date.toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Builds the two letter initials shown in the navigation bar avatar.
 */
export function initialsOf(fullName) {
  // Falls back to a single character when only one word is supplied
  if (!fullName) return '?';

  const parts = fullName.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();

  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}
