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
 * POST api/auth/login — exchanges credentials for a token. The identifier may
 * be the user's email address, username or phone number.
 */
export async function login(identifier, password) {
  // The API decides which field the identifier matched, whether the password
  // is correct, and whether the account is still active
  const { data } = await axiosClient.post('/auth/login', { identifier, password });

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
