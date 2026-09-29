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

// Letters, digits, dot, underscore and hyphen; 3 to 30 characters
const USERNAME_PATTERN = /^[a-zA-Z0-9._-]{3,30}$/;

// Sri Lankan NIC: 9 digits then V or X (old format), or 12 digits (new format)
const NIC_PATTERN = /^(\d{9}[VvXx]|\d{12})$/;

// Sri Lankan telephone number: 0 then 9 digits, or +94 then 9 digits
const PHONE_PATTERN = /^(0\d{9}|\+94\d{9})$/;

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

  if (!values.username?.trim()) {
    errors.username = 'Username is required.';
  } else if (!USERNAME_PATTERN.test(values.username.trim())) {
    errors.username = 'Use 3 to 30 letters, numbers, dot, underscore or hyphen.';
  }

  if (!values.nic?.trim()) {
    errors.nic = 'NIC number is required.';
  } else if (!NIC_PATTERN.test(values.nic.trim())) {
    errors.nic = 'Enter 9 digits followed by V or X, or 12 digits.';
  }

  if (!values.phone?.trim()) {
    errors.phone = 'Phone number is required.';
  } else if (!PHONE_PATTERN.test(values.phone.trim())) {
    errors.phone = 'Enter 10 digits starting with 0, or +94 followed by 9 digits.';
  }

  // The date of birth must be a real date in the past
  if (!values.dateOfBirth) {
    errors.dateOfBirth = 'Date of birth is required.';
  } else {
    const dob = new Date(values.dateOfBirth);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (Number.isNaN(dob.getTime())) {
      errors.dateOfBirth = 'Enter a valid date.';
    } else if (dob >= today) {
      errors.dateOfBirth = 'Date of birth must be in the past.';
    } else if (dob < new Date('1900-01-01')) {
      errors.dateOfBirth = 'Date of birth must be on or after 1 January 1900.';
    }
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
 * Validates the login form and returns an errors object. The identifier may be
 * an email address, a username or a phone number, so only its presence is
 * checked here; the API decides which one it matches.
 */
export function validateLoginForm(values) {
  const errors = {};

  if (!values.identifier?.trim()) {
    errors.identifier = 'Enter your email, username or phone number.';
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

// The prosumer API accepts only a local 10 digit number starting with 0, so
// the +94 form allowed for web users is deliberately not permitted here
const PROSUMER_PHONE_PATTERN = /^0\d{9}$/;

/**
 * Validates the create/edit prosumer form and returns an errors object. These
 * rules mirror ProsumerCreateRequest, ProsumerUpdateRequest and NicValidator on
 * the API, which stay the single place where they are actually enforced.
 */
export function validateProsumerForm(values, { requireNic, requirePassword }) {
  // An empty object means the form passed every check
  const errors = {};

  // The NIC is the primary key, so it is only asked for when creating
  if (requireNic) {
    if (!values.nic?.trim()) {
      errors.nic = 'NIC number is required.';
    } else if (!NIC_PATTERN.test(values.nic.trim())) {
      errors.nic = 'Enter 9 digits followed by V or X, or 12 digits.';
    }
  }

  if (!values.fullName?.trim()) {
    errors.fullName = 'Full name is required.';
  } else if (values.fullName.trim().length < 3) {
    errors.fullName = 'Full name must be at least 3 characters.';
  } else if (values.fullName.trim().length > 100) {
    errors.fullName = 'Full name must be 100 characters or fewer.';
  }

  if (!values.email?.trim()) {
    errors.email = 'Email is required.';
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Enter a valid email address.';
  }

  if (!values.phone?.trim()) {
    errors.phone = 'Phone number is required.';
  } else if (!PROSUMER_PHONE_PATTERN.test(values.phone.trim())) {
    errors.phone = 'Phone number must be 10 digits starting with 0.';
  }

  if (!values.address?.trim()) {
    errors.address = 'Address is required.';
  } else if (values.address.trim().length > 250) {
    errors.address = 'Address must be 250 characters or fewer.';
  }

  // The password is set once, when the account is created
  if (requirePassword) {
    if (!values.password) {
      errors.password = 'Password is required.';
    } else if (values.password.length < 8) {
      errors.password = 'Password must be at least 8 characters.';
    }

    if (values.password !== values.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }
  }

  return errors;
}
