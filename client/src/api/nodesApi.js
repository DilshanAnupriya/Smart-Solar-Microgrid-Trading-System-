/*
 * File:        nodesApi.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       API
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: All web-client requests for microgrid node management. Pages
 *              receive plain data because the shared ApiResponse is unwrapped here.
 */

import axiosClient from './axiosClient';

/** Pulls the useful payload out of the API's standard response envelope. */
function unwrap(response) {
  const body = response?.data;
  return body && typeof body === 'object' && 'data' in body ? body.data : body;
}

/** Lists nodes using optional text and active-status filters. */
export async function getNodes({ search, isActive } = {}) {
  const params = {};
  if (search?.trim()) params.search = search.trim();
  if (isActive !== undefined && isActive !== null && isActive !== 'all') {
    params.isActive = isActive;
  }

  return unwrap(await axiosClient.get('/nodes', { params })) ?? [];
}

/** Loads one node for its details or edit page. */
export async function getNodeById(id) {
  return unwrap(await axiosClient.get(`/nodes/${encodeURIComponent(id)}`));
}

/** Creates a node; the API enforces Backoffice permission. */
export async function createNode(payload) {
  return unwrap(await axiosClient.post('/nodes', payload));
}

/** Updates general node information. */
export async function updateNode(id, payload) {
  return unwrap(await axiosClient.put(`/nodes/${encodeURIComponent(id)}`, payload));
}

/** Replaces all embedded weekly schedule entries. */
export async function updateNodeSchedule(id, operatingSchedule) {
  return unwrap(await axiosClient.put(`/nodes/${encodeURIComponent(id)}/schedule`, { operatingSchedule }));
}

/** Updates only the Grid Operator-managed available-slot value. */
export async function updateNodeSlots(id, availableBatterySlots) {
  return unwrap(await axiosClient.patch(`/nodes/${encodeURIComponent(id)}/slots`, { availableBatterySlots }));
}

/** Soft-deactivates a node after server-side reservation checks. */
export async function deactivateNode(id) {
  return unwrap(await axiosClient.patch(`/nodes/${encodeURIComponent(id)}/deactivate`));
}

/** Restores an inactive node. */
export async function activateNode(id) {
  return unwrap(await axiosClient.patch(`/nodes/${encodeURIComponent(id)}/activate`));
}

/** Used by the mobile/prosumer interface to locate active nearby nodes. */
export async function getNearbyNodes(latitude, longitude, radiusKm = 10) {
  const params = { latitude, longitude, radiusKm };
  return unwrap(await axiosClient.get('/nodes/nearby', { params })) ?? [];
}
