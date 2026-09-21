/*
 * File:        validators.js
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Client side form checks that mirror the rules already enforced
 *              by the Web API. These exist only to give the user quick feedback;
 *              the API remains the single place where the rules are actually
 *              enforced, as required by the FAT service pattern.
 */

// Simple pattern that catches obvious typing mistakes in an email address
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Validates the create/edit user form and returns an errors object.
 */
export function validateUserForm(values, { requirePassword }) {
  // An empty object means the form passed every check
  const errors = {};

  if (!values.fullName?.trim()) {
    errors.fullName = 'Full name is required.';
  } else if (values.fullName.trim().length < 3) {
    errors.fullName = 'Full name must be at least 3 characters.';
  }

  if (!values.email?.trim()) {
    errors.email = 'Email is required.';
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Enter a valid email address.';
  }

  if (!values.role) {
    errors.role = 'Select a role.';
  }

  // The password is only set when the account is first created
  if (requirePassword) {
    if (!values.password) {
      errors.password = 'Password is required.';
    } else if (values.password.length < 6) {
      errors.password = 'Password must be at least 6 characters.';
    }

    if (values.password !== values.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }
  }

  return errors;
}

/**
 * Validates the login form and returns an errors object.
 */
export function validateLoginForm(values) {
  const errors = {};

  if (!values.email?.trim()) {
    errors.email = 'Email is required.';
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Enter a valid email address.';
  }

  if (!values.password) {
    errors.password = 'Password is required.';
  }

  return errors;
}

/**
 * Validates the change password form and returns an errors object.
 */
export function validateChangePasswordForm(values) {
  const errors = {};

  if (!values.currentPassword) {
    errors.currentPassword = 'Current password is required.';
  }

  if (!values.newPassword) {
    errors.newPassword = 'New password is required.';
  } else if (values.newPassword.length < 6) {
    errors.newPassword = 'New password must be at least 6 characters.';
  } else if (values.newPassword === values.currentPassword) {
    errors.newPassword = 'The new password must be different.';
  }

  if (values.newPassword !== values.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.';
  }

  return errors;
}
