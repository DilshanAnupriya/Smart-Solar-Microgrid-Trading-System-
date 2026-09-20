/*
 * File:        usersApi.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       API
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Wrapper around the web user management endpoints. Every one of
 *              these calls is refused by the API with 403 unless the signed in
 *              user holds the Backoffice role.
 */

import axiosClient from './axiosClient';

/**
 * GET api/users — lists users using the optional filters.
 */
export async function getUsers({ role, status, search } = {}) {
  // Empty filters are dropped so the query string stays clean
  const params = {};
  if (role) params.role = role;
  if (status && status !== 'all') params.status = status;
  if (search?.trim()) params.search = search.trim();

  const { data } = await axiosClient.get('/users', { params });

  return data;
}

/**
 * GET api/users/{id} — returns a single user.
 */
export async function getUserById(id) {
  const { data } = await axiosClient.get(`/users/${id}`);

  return data;
}

/**
 * POST api/users — creates a Backoffice or Grid Operator account.
 */
export async function createUser(payload) {
  // The API hashes the password and rejects a duplicate email address
  const { data } = await axiosClient.post('/users', payload);

  return data;
}

/**
 * PUT api/users/{id} — updates name, email and role.
 */
export async function updateUser(id, payload) {
  const { data } = await axiosClient.put(`/users/${id}`, payload);

  return data;
}

/**
 * PATCH api/users/{id}/deactivate — blocks an account from signing in.
 */
export async function deactivateUser(id) {
  // The API refuses this when the target is the signed in user's own account
  await axiosClient.patch(`/users/${id}/deactivate`);
}

/**
 * PATCH api/users/{id}/activate — restores a deactivated account.
 */
export async function activateUser(id) {
  await axiosClient.patch(`/users/${id}/activate`);
}
