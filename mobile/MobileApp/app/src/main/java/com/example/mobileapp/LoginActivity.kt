package com.example.mobileapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class LoginActivity : Activity() {

    private lateinit var etIdentifier: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: Button

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etIdentifier = findViewById(R.id.etIdentifier)
        etPassword = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

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

    private fun togglePasswordVisibility() {
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

    private fun attemptSignIn() {
        val identifier = etIdentifier.text.toString().trim()
        val password = etPassword.text.toString()

        var isValid = true
        if (identifier.isEmpty()) {
            etIdentifier.error = getString(R.string.login_error_identifier_required)
            isValid = false
        }
        if (password.isEmpty()) {
            etPassword.error = getString(R.string.login_error_password_required)
            isValid = false
        }
        if (!isValid) return

        // UI only for now: the call to the auth API goes here once the prosumer login endpoint exists
        Toast.makeText(this, R.string.login_not_connected, Toast.LENGTH_SHORT).show()
    }
}
