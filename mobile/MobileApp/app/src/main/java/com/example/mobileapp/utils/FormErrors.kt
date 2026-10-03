/*
 * File:        FormErrors.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Shows form errors on their fields, both from the quick checks on
 *              the phone and from the API's validation reply.
 */

package com.example.mobileapp.utils

import android.widget.EditText

/**
 * Shows each field's error message (or clears it when the value is valid), then focuses the
 * first invalid field, which scrolls it into view and opens its message.
 *
 * @param checks each field paired with its error string resource id, or null if it is valid
 * @return true when every field is valid
 */
fun showFieldErrors(checks: List<Pair<EditText, Int?>>): Boolean {
    // Checked in screen order, so the first invalid field is the one nearest the top
    var firstInvalidField: EditText? = null
    for ((field, errorRes) in checks) {
        field.error = errorRes?.let { field.context.getString(it) }
        if (errorRes != null && firstInvalidField == null) {
            firstInvalidField = field
        }
    }
    firstInvalidField?.requestFocus()
    return firstInvalidField == null
}

/**
 * Shows the API's validation messages on the matching fields and focuses the first one.
 * Fields the API did not complain about have their error cleared.
 *
 * @param fields each field name as the API reports it (e.g. "email") with its EditText
 * @param errors the first message per field from ApiResult.Failure.fieldErrors
 * @return true if at least one message was shown on a field
 */
fun showServerFieldErrors(fields: Map<String, EditText>, errors: Map<String, String>): Boolean {
    // Names are compared without case, so "fullName" also matches "FullName"
    var firstInvalidField: EditText? = null
    for ((name, field) in fields) {
        val message = errors.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
        field.error = message
        if (message != null && firstInvalidField == null) {
            firstInvalidField = field
        }
    }
    firstInvalidField?.requestFocus()
    return firstInvalidField != null
}
