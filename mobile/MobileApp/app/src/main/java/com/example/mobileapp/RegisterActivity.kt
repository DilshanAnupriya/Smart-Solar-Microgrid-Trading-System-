package com.example.mobileapp

import android.app.Activity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

/**
 * Prosumer self-registration. The NIC entered here becomes the account's primary key
 * on the server, so it cannot be changed after the account is created.
 */
class RegisterActivity : Activity() {

    private lateinit var etNic: EditText
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnTogglePassword: Button

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Both go back to the sign-in screen, which is still underneath this one
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener { finish() }

        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        findViewById<Button>(R.id.btnRegister).setOnClickListener { attemptRegister() }

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

    private fun togglePasswordVisibility() {
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

    private fun attemptRegister() {
        val nic = ProsumerValidator.normalizeNic(etNic.text.toString())
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

        // Show the NIC in the same normalised form the server will store (e.g. 991234567V)
        etNic.setText(nic)

        // UI only for now: POST /api/prosumers/register goes here
        Toast.makeText(this, R.string.register_not_connected, Toast.LENGTH_SHORT).show()
    }
}
