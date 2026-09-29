package com.example.mobileapp

import android.widget.EditText

/**
 * Shows each field's error message (or clears it when the value is valid), then focuses the
 * first invalid field, which scrolls it into view and opens its message.
 *
 * @param checks each field paired with its error string resource id, or null if it is valid
 * @return true when every field is valid
 */
fun showFieldErrors(checks: List<Pair<EditText, Int?>>): Boolean {
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
