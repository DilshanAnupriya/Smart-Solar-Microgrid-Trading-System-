/*
 * File:        LoginActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Prosumer sign-in with NIC (or email) and password. On success the
 *              token and profile are saved in SQLite and the profile screen opens.
 */

package com.example.mobileapp.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.AuthApi
import com.example.mobileapp.profile.ProfileActivity
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.UiUtils
import com.example.mobileapp.utils.showFieldErrors
import com.example.mobileapp.utils.showServerFieldErrors
import kotlinx.coroutines.launch

/**
 * The sign-in screen. The phone only checks that both fields are filled in;
 * the API decides whether the details are right and whether the account is active.
 */
class LoginActivity : BaseActivity() {

    private lateinit var etIdentifier: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: Button
    private lateinit var btnSignIn: Button
    private lateinit var tvFormError: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Find the views and connect the buttons
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etIdentifier = findViewById(R.id.etIdentifier)
        etPassword = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvFormError = findViewById(R.id.tvFormError)

        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        btnSignIn.setOnClickListener { attemptSignIn() }

        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Pressing "Done" on the keyboard in the password field acts like tapping Sign in
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSignIn()
                true
            } else {
                false
            }
        }
    }

    /**
     * Shows or hides the typed password.
     */
    private fun togglePasswordVisibility() {
        // Swap between plain text and dots
        isPasswordVisible = !isPasswordVisible

        etPassword.transformationMethod = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        btnTogglePassword.setText(
            if (isPasswordVisible) R.string.login_hide_password else R.string.login_show_password
        )

        // Changing the transformation method moves the cursor to the start; put it back at the end
        etPassword.setSelection(etPassword.text.length)
    }

    /**
     * Checks the fields are filled in, then calls POST /auth/prosumer-login.
     */
    private fun attemptSignIn() {
        // Only emptiness is checked here; everything else is the API's decision
        val identifier = etIdentifier.text.toString().trim()
        val password = etPassword.text.toString()

        val isValid = showFieldErrors(
            listOf(
                etIdentifier to (if (identifier.isEmpty()) R.string.login_error_identifier_required else null),
                etPassword to (if (password.isEmpty()) R.string.login_error_password_required else null)
            )
        )
        if (!isValid) return

        UiUtils.showBanner(tvFormError, null)
        setLoading(true)

        uiScope.launch {
            when (val result = AuthApi(this@LoginActivity).prosumerLogin(identifier, password)) {
                is ApiResult.Success -> {
                    // Keep the token and profile in SQLite, so the app stays signed in after closing
                    SessionManager(this@LoginActivity).saveSession(result.data)
                    openHome()
                }
                is ApiResult.Failure -> {
                    // Wrong details (401) and a deactivated account (403) both come with the API's reason
                    setLoading(false)
                    showServerFieldErrors(
                        mapOf("identifier" to etIdentifier, "password" to etPassword),
                        result.fieldErrors
                    )
                    UiUtils.showBanner(tvFormError, result.message)
                }
            }
        }
    }

    /**
     * Locks the form while the sign-in request is running.
     */
    private fun setLoading(loading: Boolean) {
        // Prevents a second tap from sending the request twice
        btnSignIn.isEnabled = !loading
        btnSignIn.setText(if (loading) R.string.login_signing_in else R.string.login_sign_in)
        etIdentifier.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    /**
     * Opens the prosumer's home screen and removes this screen from the back stack.
     */
    private fun openHome() {
        // A new task, so pressing Back on the profile does not return to the login screen
        startActivity(
            Intent(this, ProfileActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
