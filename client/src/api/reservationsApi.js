/*
 * File:        reservationsApi.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       API
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Client wrapper around the energy slot reservation management
 *              endpoints. Calls are authorized using the JWT stored in the
 *              browser and enforce the 7-day and 12-hour rules in the Web API.
 */

import axiosClient from './axiosClient';

/**
 * GET api/reservations — lists reservations using optional filters.
 */
export async function getReservations({ nic, nodeId, status, fromDate, toDate, search } = {}) {
  const params = {};
  if (nic?.trim()) params.nic = nic.trim();
  if (nodeId?.trim()) params.nodeId = nodeId.trim();
  if (status && status !== 'all') params.status = status;
  if (fromDate) params.fromDate = fromDate;
  if (toDate) params.toDate = toDate;
  if (search?.trim()) params.search = search.trim();

  const { data } = await axiosClient.get('/reservations', { params });
  return data;
}

/**
 * GET api/reservations/{id} — returns a single reservation by id.
 */
export async function getReservationById(id) {
  const { data } = await axiosClient.get(`/reservations/${id}`);
  return data;
}

/**
 * GET api/reservations/stats — returns dashboard summary statistics.
 */
export async function getReservationStats() {
  const { data } = await axiosClient.get('/reservations/stats');
  return data;
}

/**
 * POST api/reservations — creates a new power trading reservation.
 * Subject to the 7-day scheduling window rule.
 */
export async function createReservation(payload) {
  const { data } = await axiosClient.post('/reservations', payload);
  return data;
}

/**
 * PUT api/reservations/{id} — reschedules or updates an existing reservation.
 * Subject to the 7-day scheduling rule and 12-hour notice rule.
 */
export async function updateReservation(id, payload) {
  const { data } = await axiosClient.put(`/reservations/${id}`, payload);
  return data;
}

/**
 * PATCH api/reservations/{id}/cancel — cancels a reservation with reason.
 * Subject to the 12-hour notice rule.
 */
export async function cancelReservation(id, { reason }) {
  const { data } = await axiosClient.patch(`/reservations/${id}/cancel`, { reason });
  return data;
}

/**
 * PATCH api/reservations/{id}/status — advances reservation state (e.g. Approved, Completed).
 */
export async function updateReservationStatus(id, { status, notes }) {
  const { data } = await axiosClient.patch(`/reservations/${id}/status`, { status, notes });
  return data;
}

/**
 * GET api/reservations/node/{nodeId}/active-check — checks if active bookings exist on a node.
 */
export async function checkNodeActiveReservations(nodeId) {
  const { data } = await axiosClient.get(`/reservations/node/${nodeId}/active-check`);
  return data;
}
