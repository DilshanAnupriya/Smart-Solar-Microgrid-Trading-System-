package com.example.mobileapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

/**
 * Lets a prosumer change their name, email, phone and address.
 * The NIC is shown but can't be edited, because it is the account's primary key.
 * Receives the current profile in the intent extras and returns the updated one as the result.
 */
class EditProfileActivity : Activity() {

    private lateinit var original: Prosumer

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This screen only makes sense for an existing profile
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
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { attemptSave() }
    }

    private fun attemptSave() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        // Same rules as registration, minus the NIC and password, which aren't edited here
        val isValid = showFieldErrors(
            listOf(
                etFullName to ProsumerValidator.validateFullName(fullName),
                etEmail to ProsumerValidator.validateEmail(email),
                etPhone to ProsumerValidator.validatePhone(phone),
                etAddress to ProsumerValidator.validateAddress(address)
            )
        )
        if (!isValid) return

        // UI only for now: PUT /api/prosumers/{nic} goes here, and the result should be the server's response
        val updated = original.copy(fullName = fullName, email = email, phone = phone, address = address)
        setResult(RESULT_OK, Intent().putExtras(updated.toBundle()))
        finish()
    }
}
