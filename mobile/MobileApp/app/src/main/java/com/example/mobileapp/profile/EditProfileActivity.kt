/*
 * File:        EditProfileActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Edits the prosumer's name, email, phone and address through
 *              PUT /prosumers/{nic}. The NIC is shown but cannot be changed.
 */

package com.example.mobileapp.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.models.Prosumer
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ProsumerApi
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.ProsumerValidator
import com.example.mobileapp.utils.UiUtils
import com.example.mobileapp.utils.showFieldErrors
import com.example.mobileapp.utils.showServerFieldErrors
import kotlinx.coroutines.launch

/**
 * Receives the current profile in the intent extras and, once the API has saved the changes,
 * returns the saved profile as the result.
 */
class EditProfileActivity : BaseActivity() {

    private lateinit var original: Prosumer

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var tvFormError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // This screen only makes sense for an existing profile
        super.onCreate(savedInstanceState)
        val prosumer = intent.extras?.let { Prosumer.fromBundle(it) }
        if (prosumer == null) {
            finish()
            return
        }
        original = prosumer

        setContentView(R.layout.activity_edit_profile)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        tvFormError = findViewById(R.id.tvFormError)

        findViewById<TextView>(R.id.tvNic).text = original.nic

        // Fill the form only the first time; after a rotation the fields restore what was typed
        if (savedInstanceState == null) {
            etFullName.setText(original.fullName)
            etEmail.setText(original.email)
            etPhone.setText(original.phone)
            etAddress.setText(original.address)
        }

        // Back and Cancel both leave without saving
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { attemptSave() }
    }

    /**
     * Checks nothing is left empty, then sends the changes to the API.
     */
    private fun attemptSave() {
        // The email and phone formats, and a duplicate email, are checked by the API
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        val isValid = showFieldErrors(
            listOf(
                etFullName to ProsumerValidator.validateFullName(fullName),
                etEmail to ProsumerValidator.validateEmail(email),
                etPhone to ProsumerValidator.validatePhone(phone),
                etAddress to ProsumerValidator.validateAddress(address)
            )
        )
        if (!isValid) return

        UiUtils.showBanner(tvFormError, null)
        setLoading(true)

        uiScope.launch {
            val result = ProsumerApi(this@EditProfileActivity)
                .updateProfile(original.nic, fullName, email, phone, address)
            when (result) {
                is ApiResult.Success -> {
                    // Keep the SQLite copy in step, then hand the saved profile back to the profile screen
                    SessionManager(this@EditProfileActivity).updateProfile(result.data)
                    setResult(RESULT_OK, Intent().putExtras(result.data.toBundle()))
                    finish()
                }
                is ApiResult.Failure -> {
                    setLoading(false)
                    if (result.isUnauthorized) {
                        UiUtils.signOutToLogin(this@EditProfileActivity, getString(R.string.session_expired))
                    } else {
                        showSaveError(result)
                    }
                }
            }
        }
    }

    /**
     * Shows why the API refused the changes.
     */
    private fun showSaveError(failure: ApiResult.Failure) {
        // Validation problems go next to their fields; the banner carries the API's message,
        // e.g. "This email address is already used by another prosumer."
        showServerFieldErrors(
            mapOf(
                "fullName" to etFullName,
                "email" to etEmail,
                "phone" to etPhone,
                "address" to etAddress
            ),
            failure.fieldErrors
        )
        UiUtils.showBanner(tvFormError, failure.message)
    }

    /**
     * Locks the form while the request is running.
     */
    private fun setLoading(loading: Boolean) {
        // Prevents a second tap from saving twice
        btnSave.isEnabled = !loading
        btnCancel.isEnabled = !loading
        btnSave.setText(if (loading) R.string.edit_profile_saving else R.string.edit_profile_save)
    }
}
