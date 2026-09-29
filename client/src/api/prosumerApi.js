/*
 * File:        prosumerApi.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       API
 * Author:      B.D.A.Cooray (IT22189530)
 * Created:     2026-09-22
 * Description: Every call the web application makes to the prosumer management
 *              endpoints of the Web API, collected in one file so no page ever
 *              builds a URL of its own. ProsumersController wraps each result
 *              in the shared ApiResponse envelope, so the helper below unwraps
 *              it and the pages receive plain prosumer objects. Access rules
 *              (staff only listing, Backoffice only reactivation, prosumers
 *              limited to their own profile) are enforced by the API; the
 *              screens simply hide what the signed in role cannot use.
 */

import axiosClient from './axiosClient';

/**
 * Pulls the payload out of the ApiResponse envelope returned by the API.
 */
function unwrap(response) {
  // Falls back to the raw body so the caller still works if an endpoint is
  // ever changed to return the object without the envelope
  const body = response?.data;

  return body && typeof body === 'object' && 'data' in body ? body.data : body;
}

/**
 * GET api/prosumers?search=&status= — lists prosumers for Backoffice and Grid
 * Operator staff.
 */
export async function getProsumers({ search, status } = {}) {
  // Empty filters are left out so the query string stays clean, and "all" is a
  // value used only by the dropdown rather than something the API understands
  const params = {};
  if (search?.trim()) params.search = search.trim();
  if (status && status !== 'all') params.status = status;

  const response = await axiosClient.get('/prosumers', { params });

  return unwrap(response) ?? [];
}

/**
 * GET api/prosumers/{nic} — returns one prosumer profile.
 */
export async function getProsumerByNic(nic) {
  // The API refuses this with 403 when a prosumer asks for another NIC
  const response = await axiosClient.get(`/prosumers/${encodeURIComponent(nic)}`);

  return unwrap(response);
}

/**
 * POST api/prosumers — staff create a prosumer profile from the web console.
 */
export async function createProsumer(payload) {
  // The API validates the NIC format, hashes the password and rejects a
  // duplicate NIC or email address with 409
  const response = await axiosClient.post('/prosumers', payload);

  return unwrap(response);
}

/**
 * PUT api/prosumers/{nic} — updates name, email, phone and address.
 */
export async function updateProsumer(nic, payload) {
  // The NIC is the primary key, so it travels in the address and is never
  // part of the request body
  const response = await axiosClient.put(`/prosumers/${encodeURIComponent(nic)}`, payload);

  return unwrap(response);
}

/**
 * PATCH api/prosumers/{nic}/deactivate — deactivates an active account.
 */
export async function deactivateProsumer(nic) {
  // The API records who deactivated the account and when, and refuses the
  // request when the account is already deactivated
  const response = await axiosClient.patch(`/prosumers/${encodeURIComponent(nic)}/deactivate`);

  return unwrap(response);
}

/**
 * PATCH api/prosumers/{nic}/reactivate — restores a deactivated account.
 */
export async function reactivateProsumer(nic) {
  // Backoffice only: a Grid Operator receives 403 from the API even if this
  // call is somehow reached from the interface
  const response = await axiosClient.patch(`/prosumers/${encodeURIComponent(nic)}/reactivate`);

  return unwrap(response);
}

/**
 * POST api/prosumers/register — self-registration, used by the mobile app.
 */
export async function registerProsumer(payload) {
  // Kept here for completeness: the same endpoint the Android client calls,
  // the only prosumer route that needs no token
  const response = await axiosClient.post('/prosumers/register', payload);

  return unwrap(response);
}
