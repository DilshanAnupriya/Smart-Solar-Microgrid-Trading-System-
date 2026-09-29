package com.example.mobileapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

/**
 * Shows the signed-in prosumer's profile, with Edit and Deactivate actions in the top bar.
 */
class ProfileActivity : Activity() {

    private lateinit var prosumer: Prosumer

    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvNicSubtitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDeactivatedBanner: TextView
    private lateinit var tvNic: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnDeactivate: Button
    private lateinit var cardReservations: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvAvatar = findViewById(R.id.tvAvatar)
        tvName = findViewById(R.id.tvName)
        tvNicSubtitle = findViewById(R.id.tvNicSubtitle)
        tvStatus = findViewById(R.id.tvStatus)
        tvDeactivatedBanner = findViewById(R.id.tvDeactivatedBanner)
        tvNic = findViewById(R.id.tvNic)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvAddress = findViewById(R.id.tvAddress)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        btnEdit = findViewById(R.id.btnEdit)
        btnDeactivate = findViewById(R.id.btnDeactivate)
        cardReservations = findViewById(R.id.cardReservations)

        // After a rotation, keep any edits made on this screen instead of reloading
        prosumer = savedInstanceState?.let { Prosumer.fromBundle(it) } ?: SAMPLE_PROSUMER

        btnEdit.setOnClickListener { openEditProfile() }
        btnDeactivate.setOnClickListener { confirmDeactivation() }
        cardReservations.setOnClickListener {
            startActivity(Intent(this, ReservationsActivity::class.java))
        }

        showProsumer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(prosumer.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EDIT_PROFILE || resultCode != RESULT_OK) return

        val updated = data?.extras?.let { Prosumer.fromBundle(it) } ?: return
        prosumer = updated
        showProsumer()
        Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show()
    }

    private fun showProsumer() {
        tvAvatar.text = prosumer.initials()
        tvName.text = prosumer.fullName
        tvNicSubtitle.text = getString(R.string.profile_nic_subtitle, prosumer.nic)
        tvNic.text = prosumer.nic
        tvEmail.text = prosumer.email
        tvPhone.text = prosumer.phone
        tvAddress.text = prosumer.address
        tvMemberSince.text = prosumer.memberSince

        if (prosumer.isActive) {
            tvStatus.setText(R.string.profile_status_active)
            tvStatus.setBackgroundResource(R.drawable.bg_badge_active)
            tvStatus.setTextColor(getColor(R.color.success_text))
        } else {
            tvStatus.setText(R.string.profile_status_deactivated)
            tvStatus.setBackgroundResource(R.drawable.bg_badge_deactivated)
            tvStatus.setTextColor(getColor(R.color.danger_text))
        }

        // A deactivated prosumer can't change their profile, so hide both actions and explain why
        val actionsVisibility = if (prosumer.isActive) View.VISIBLE else View.GONE
        btnEdit.visibility = actionsVisibility
        btnDeactivate.visibility = actionsVisibility
        // A deactivated account can't trade energy, so it can't reach reservations either
        cardReservations.visibility = actionsVisibility
        tvDeactivatedBanner.visibility = if (prosumer.isActive) View.GONE else View.VISIBLE
    }

    private fun openEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java).putExtras(prosumer.toBundle())
        startActivityForResult(intent, REQUEST_EDIT_PROFILE)
    }

    private fun confirmDeactivation() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.deactivate_title)
            .setMessage(R.string.deactivate_message)
            .setNegativeButton(R.string.deactivate_cancel, null)
            .setPositiveButton(R.string.deactivate_confirm) { _, _ -> deactivateAccount() }
            .create()
        dialog.show()

        // Colour the destructive action red so it stands out from Cancel
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.danger))
    }

    private fun deactivateAccount() {
        // UI only for now: PATCH /api/prosumers/{nic}/deactivate goes here
        prosumer = prosumer.copy(status = Prosumer.STATUS_DEACTIVATED)
        showProsumer()
        Toast.makeText(this, R.string.deactivate_not_connected, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_EDIT_PROFILE = 1

        // Placeholder until GET /api/prosumers/{nic} is connected
        private val SAMPLE_PROSUMER = Prosumer(
            nic = "991234567V",
            fullName = "Nimal Perera",
            email = "nimal.perera@example.lk",
            phone = "0771234567",
            address = "12 Galle Road, Colombo 03",
            status = Prosumer.STATUS_ACTIVE,
            memberSince = "29 Sep 2026"
        )
    }
}
