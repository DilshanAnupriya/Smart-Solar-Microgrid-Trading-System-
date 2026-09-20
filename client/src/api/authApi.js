/*
 * File:        authApi.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       API
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Wrapper around the authentication endpoints of the Web API, so
 *              components never build URLs themselves.
 */

import axiosClient from './axiosClient';

/**
 * POST api/auth/login — exchanges credentials for a token.
 */
export async function login(email, password) {
  // The API decides whether the credentials are valid and whether the
  // account is still active
  const { data } = await axiosClient.post('/auth/login', { email, password });

  return data;
}

/**
 * GET api/auth/me — returns the signed in user's profile.
 */
export async function getCurrentUser() {
  // Used on start-up to confirm a stored token is still accepted
  const { data } = await axiosClient.get('/auth/me');

  return data;
}

/**
 * POST api/auth/change-password — changes the signed in user's password.
 */
export async function changePassword(currentPassword, newPassword) {
  // The API verifies the current password before applying the change
  await axiosClient.post('/auth/change-password', { currentPassword, newPassword });
}
