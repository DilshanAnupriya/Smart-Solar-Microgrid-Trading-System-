/*
 * File:        axiosClient.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       API
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: The single axios instance used for every call to the Web API.
 *              A request interceptor attaches the JWT, and a response
 *              interceptor turns the API's error shape into a plain message and
 *              signs the user out when the token is no longer accepted.
 */

import axios from 'axios';
import { STORAGE_KEYS } from '../utils/constants';

// Callback registered by AuthContext so the interceptor can clear the session
let onUnauthorised = null;

/**
 * Lets AuthContext react when the API rejects the stored token.
 */
export function setUnauthorisedHandler(handler) {
  // Stored in a module variable because interceptors run outside React
  onUnauthorised = handler;
}

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach the bearer token to every outgoing request
axiosClient.interceptors.request.use((config) => {
  // The token is read fresh each time so a new login is picked up immediately
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN);

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// Convert API errors into a consistent Error object for the pages to display
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // No response at all means the API is not running or is unreachable
    if (!error.response) {
      return Promise.reject(new Error('Cannot reach the server. Please check that the API is running.'));
    }

    const { status, data } = error.response;

    // The token is missing, expired or invalid, so end the local session
    if (status === 401 && onUnauthorised) {
      onUnauthorised();
    }

    // Build a readable message from the API's shared error shape
    const normalised = new Error(data?.message || 'Something went wrong. Please try again.');
    normalised.status = status;
    normalised.fieldErrors = data?.errors || {};

    return Promise.reject(normalised);
  },
);

export default axiosClient;
