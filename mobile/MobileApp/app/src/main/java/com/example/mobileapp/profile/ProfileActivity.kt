/*
 * File:        ProfileActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: The signed-in prosumer's profile and home screen. Loads the
 *              profile from the API, and offers Edit, Deactivate and Sign out.
 */

package com.example.mobileapp.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.ReservationsActivity
import com.example.mobileapp.models.LocalUser
import com.example.mobileapp.models.Prosumer
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ProsumerApi
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.DateTimeUtils
import com.example.mobileapp.utils.UiUtils
import kotlinx.coroutines.launch

/**
 * Shows the copy saved in SQLite straight away, then replaces it with the API's version.
 * If the API can't be reached, the saved copy stays on screen with a banner saying so.
 */
class ProfileActivity : BaseActivity() {

    private lateinit var prosumer: Prosumer

    private lateinit var progressLoading: ProgressBar
    private lateinit var tvOfflineBanner: TextView
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
    private lateinit var btnSignOut: Button
    private lateinit var cardReservations: View

    override fun onCreate(savedInstanceState: Bundle?) {
        // Only a signed-in user can see this screen
        super.onCreate(savedInstanceState)
        val user = SessionManager(this).getCurrentUser()
        if (user == null) {
            UiUtils.signOutToLogin(this, null)
            return
        }

        setContentView(R.layout.activity_profile)

        progressLoading = findViewById(R.id.progressLoading)
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
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
        btnSignOut = findViewById(R.id.btnSignOut)
        cardReservations = findViewById(R.id.cardReservations)

        // After a rotation keep what was on screen; otherwise start from the copy saved in SQLite
        prosumer = savedInstanceState?.let { Prosumer.fromBundle(it) } ?: savedProfile(user)

        btnEdit.setOnClickListener { openEditProfile() }
        btnDeactivate.setOnClickListener { confirmDeactivation() }
        btnSignOut.setOnClickListener { signOut() }
        tvOfflineBanner.setOnClickListener { loadProfile() }
        cardReservations.setOnClickListener {
            startActivity(Intent(this, ReservationsActivity::class.java))
        }

        showProsumer()
        loadProfile()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Keep the profile across rotation so it doesn't flash back to the saved copy
        super.onSaveInstanceState(outState)
        outState.putAll(prosumer.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // EditProfileActivity returns the profile exactly as the API saved it
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EDIT_PROFILE || resultCode != RESULT_OK) return

        val updated = data?.extras?.let { Prosumer.fromBundle(it) } ?: return
        prosumer = updated
        showProsumer()
        UiUtils.showToast(this, getString(R.string.profile_updated))
    }

    /**
     * The profile as saved in SQLite at login, shown until the API answers.
     */
    private fun savedProfile(user: LocalUser): Prosumer {
        // A signed-in prosumer is active; the registration date is only known once the API answers
        return Prosumer(
            nic = user.nic,
            fullName = user.fullName,
            email = user.email,
            phone = user.phone,
            address = user.address,
            status = Prosumer.STATUS_ACTIVE,
            createdAt = ""
        )
    }

    /**
     * GET /prosumers/{nic}: replaces the shown profile with the API's version.
     */
    private fun loadProfile() {
        // The saved copy stays on screen while this runs
        setLoading(true)
        uiScope.launch {
            val result = ProsumerApi(this@ProfileActivity).getProfile(prosumer.nic)
            setLoading(false)
            when (result) {
                is ApiResult.Success -> {
                    // Refresh the SQLite copy too, so the next launch starts from current data
                    prosumer = result.data
                    SessionManager(this@ProfileActivity).updateProfile(result.data)
                    UiUtils.showBanner(tvOfflineBanner, null)
                    showProsumer()
                }
                is ApiResult.Failure -> {
                    if (result.isUnauthorized) {
                        sessionExpired()
                    } else {
                        // Keep the saved copy and say why it couldn't be refreshed
                        UiUtils.showBanner(
                            tvOfflineBanner,
                            getString(R.string.profile_offline_banner, result.message)
                        )
                    }
                }
            }
        }
    }

    /**
     * Writes [prosumer] into the views.
     */
    private fun showProsumer() {
        // Every value shown here came from the API (or its copy in SQLite)
        tvAvatar.text = prosumer.initials()
        tvName.text = prosumer.fullName
        tvNicSubtitle.text = getString(R.string.profile_nic_subtitle, prosumer.nic)
        tvNic.text = prosumer.nic
        tvEmail.text = prosumer.email
        tvPhone.text = prosumer.phone
        tvAddress.text = prosumer.address
        tvMemberSince.text = DateTimeUtils.parseIsoToMillis(prosumer.createdAt)
            ?.let { DateTimeUtils.formatDate(it) }
            ?: getString(R.string.profile_member_since_unknown)

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

    /**
     * Opens the edit form with the current profile.
     */
    private fun openEditProfile() {
        // The result comes back in onActivityResult
        val intent = Intent(this, EditProfileActivity::class.java).putExtras(prosumer.toBundle())
        startActivityForResult(intent, REQUEST_EDIT_PROFILE)
    }

    /**
     * Asks for confirmation before deactivating the account.
     */
    private fun confirmDeactivation() {
        // Deactivation can only be undone by a Backoffice officer, so make sure it's intended
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

    /**
     * PATCH /prosumers/{nic}/deactivate, then clears SQLite and returns to the login screen.
     */
    private fun deactivateAccount() {
        // The API refuses to sign this account in from now on, so the session is ended here too
        setLoading(true)
        uiScope.launch {
            when (val result = ProsumerApi(this@ProfileActivity).deactivate(prosumer.nic)) {
                is ApiResult.Success ->
                    UiUtils.signOutToLogin(this@ProfileActivity, getString(R.string.deactivate_success))
                is ApiResult.Failure -> {
                    setLoading(false)
                    if (result.isUnauthorized) {
                        sessionExpired()
                    } else {
                        UiUtils.showToast(this@ProfileActivity, result.message)
                    }
                }
            }
        }
    }

    /**
     * Signs out on this phone; the account stays active on the server.
     */
    private fun signOut() {
        // Clears the token from SQLite and opens the login screen
        UiUtils.signOutToLogin(this, getString(R.string.profile_signed_out))
    }

    /**
     * The API no longer accepts the saved token (it expired or was revoked).
     */
    private fun sessionExpired() {
        // The only way forward is to sign in again
        UiUtils.signOutToLogin(this, getString(R.string.session_expired))
    }

    /**
     * Shows the spinner and locks the actions while a request is running.
     */
    private fun setLoading(loading: Boolean) {
        // Prevents a second tap from sending the same request again
        progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        btnEdit.isEnabled = !loading
        btnDeactivate.isEnabled = !loading
        btnSignOut.isEnabled = !loading
    }

    companion object {
        private const val REQUEST_EDIT_PROFILE = 1
    }
}
