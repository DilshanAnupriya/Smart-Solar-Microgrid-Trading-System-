/*
 * File:        RegisterActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Prosumer self-registration through POST /prosumers/register,
 *              followed by an automatic sign-in with the new account.
 */

package com.example.mobileapp.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.AuthApi
import com.example.mobileapp.network.ProsumerApi
import com.example.mobileapp.profile.ProfileActivity
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.ProsumerValidator
import com.example.mobileapp.utils.UiUtils
import com.example.mobileapp.utils.showFieldErrors
import com.example.mobileapp.utils.showServerFieldErrors
import kotlinx.coroutines.launch

/**
 * Prosumer self-registration. The NIC entered here becomes the account's primary key
 * on the server, so it cannot be changed after the account is created.
 */
class RegisterActivity : BaseActivity() {

    private lateinit var etNic: EditText
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnTogglePassword: Button
    private lateinit var btnRegister: Button
    private lateinit var tvFormError: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Find the views and connect the buttons
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etNic = findViewById(R.id.etNic)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvFormError = findViewById(R.id.tvFormError)

        // Both go back to the sign-in screen, which is still underneath this one
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener { finish() }

        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        btnRegister.setOnClickListener { attemptRegister() }

        // Pressing "Done" on the keyboard in the last field acts like tapping Create account
        etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister()
                true
            } else {
                false
            }
        }
    }

    /**
     * Shows or hides both password fields together.
     */
    private fun togglePasswordVisibility() {
        // Swap between plain text and dots
        isPasswordVisible = !isPasswordVisible

        val method = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        for (field in listOf(etPassword, etConfirmPassword)) {
            field.transformationMethod = method
            // Changing the transformation method moves the cursor to the start; put it back at the end
            field.setSelection(field.text.length)
        }

        btnTogglePassword.setText(
            if (isPasswordVisible) R.string.login_hide_password else R.string.login_show_password
        )
    }

    /**
     * Checks nothing is left empty and the passwords match, then calls the API.
     */
    private fun attemptRegister() {
        // The NIC format, email, phone and password length are checked by the API
        val nic = etNic.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Check every field so all problems show at once, in the order they appear on screen
        val isValid = showFieldErrors(
            listOf(
                etNic to ProsumerValidator.validateNic(nic),
                etFullName to ProsumerValidator.validateFullName(fullName),
                etEmail to ProsumerValidator.validateEmail(email),
                etPhone to ProsumerValidator.validatePhone(phone),
                etAddress to ProsumerValidator.validateAddress(address),
                etPassword to ProsumerValidator.validatePassword(password),
                etConfirmPassword to ProsumerValidator.validateConfirmPassword(password, confirmPassword)
            )
        )
        if (!isValid) return

        UiUtils.showBanner(tvFormError, null)
        setLoading(true)

        uiScope.launch {
            val result = ProsumerApi(this@RegisterActivity)
                .register(nic, fullName, email, phone, address, password)
            when (result) {
                // Use the NIC exactly as the API saved it (trimmed and uppercased)
                is ApiResult.Success -> signInNewAccount(result.data.nic, password)
                is ApiResult.Failure -> {
                    setLoading(false)
                    showRegistrationError(result)
                }
            }
        }
    }

    /**
     * Signs the new account in straight away, with the NIC and password just registered.
     */
    private suspend fun signInNewAccount(nic: String, password: String) {
        // Same call as the login screen, so the session is saved the same way
        when (val login = AuthApi(this).prosumerLogin(nic, password)) {
            is ApiResult.Success -> {
                SessionManager(this).saveSession(login.data)
                UiUtils.showToast(this, getString(R.string.register_success))
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
            is ApiResult.Failure -> {
                // The account exists even though this sign-in failed, so send the user to sign in
                UiUtils.showToast(this, getString(R.string.register_success_sign_in))
                finish()
            }
        }
    }

    /**
     * Shows why the API refused the registration.
     */
    private fun showRegistrationError(failure: ApiResult.Failure) {
        // Validation problems go next to their fields; the banner always carries the API's
        // message, e.g. "A prosumer with this NIC is already registered."
        showServerFieldErrors(
            mapOf(
                "nic" to etNic,
                "fullName" to etFullName,
                "email" to etEmail,
                "phone" to etPhone,
                "address" to etAddress,
                "password" to etPassword
            ),
            failure.fieldErrors
        )
        UiUtils.showBanner(tvFormError, failure.message)
    }

    /**
     * Locks the form while the request is running.
     */
    private fun setLoading(loading: Boolean) {
        // Prevents a second tap from registering twice
        btnRegister.isEnabled = !loading
        btnRegister.setText(if (loading) R.string.register_creating else R.string.register_submit)
    }
}
