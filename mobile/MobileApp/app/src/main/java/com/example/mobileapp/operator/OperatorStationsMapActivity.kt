/*
 * File:        OperatorStationsMapActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Operator Mode
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Interactive map plotting solar microgrid hub stations from their stored
 *              latitude and longitude. Integrates Google Maps API, selection bottom sheet,
 *              battery slot management, and GPS turn-by-turn navigation intents.
 */

package com.example.mobileapp.operator

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.R
import com.example.mobileapp.models.MicrogridStation
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.NodeApi
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import java.util.Locale

class OperatorStationsMapActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var btnBack: ImageButton
    private lateinit var tvStationsCount: TextView
    private lateinit var btnRefreshMap: TextView
    private lateinit var progressMapLoading: ProgressBar

    private lateinit var layoutStationsFallback: View
    private lateinit var containerStationPins: LinearLayout

    private lateinit var cardSelectedStation: View
    private lateinit var tvSelectedStationName: TextView
    private lateinit var tvSelectedStationAddress: TextView
    private lateinit var tvSelectedSlots: TextView
    private lateinit var tvSelectedCapacity: TextView
    private lateinit var tvGpsCoords: TextView
    private lateinit var btnCloseStationCard: ImageButton
    private lateinit var btnUpdateStationSlots: Button
    private lateinit var btnNavigateGps: Button

    private lateinit var nodeApi: NodeApi
    private var googleMap: GoogleMap? = null
    private val stationList = mutableListOf<MicrogridStation>()
    private val markerStationMap = HashMap<Marker, MicrogridStation>()
    private var selectedStation: MicrogridStation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_stations_map)

        nodeApi = NodeApi(this)

        initViews()
        setupListeners()
        setupGoogleMap()
        loadStations()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvStationsCount = findViewById(R.id.tvStationsCount)
        btnRefreshMap = findViewById(R.id.btnRefreshMap)
        progressMapLoading = findViewById(R.id.progressMapLoading)

        layoutStationsFallback = findViewById(R.id.layoutStationsFallback)
        containerStationPins = findViewById(R.id.containerStationPins)

        cardSelectedStation = findViewById(R.id.cardSelectedStation)
        tvSelectedStationName = findViewById(R.id.tvSelectedStationName)
        tvSelectedStationAddress = findViewById(R.id.tvSelectedStationAddress)
        tvSelectedSlots = findViewById(R.id.tvSelectedSlots)
        tvSelectedCapacity = findViewById(R.id.tvSelectedCapacity)
        tvGpsCoords = findViewById(R.id.tvGpsCoords)
        btnCloseStationCard = findViewById(R.id.btnCloseStationCard)
        btnUpdateStationSlots = findViewById(R.id.btnUpdateStationSlots)
        btnNavigateGps = findViewById(R.id.btnNavigateGps)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnRefreshMap.setOnClickListener { loadStations() }
        btnCloseStationCard.setOnClickListener { cardSelectedStation.visibility = View.GONE }

        btnNavigateGps.setOnClickListener {
            val station = selectedStation ?: return@setOnClickListener
            openExternalNavigation(station)
        }

        btnUpdateStationSlots.setOnClickListener {
            val station = selectedStation ?: return@setOnClickListener
            showUpdateSlotsDialog(station)
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true

        googleMap?.setOnMarkerClickListener { marker ->
            val station = markerStationMap[marker]
            if (station != null) {
                showStationDetails(station)
            }
            false
        }

        googleMap?.setOnMapClickListener {
            cardSelectedStation.visibility = View.GONE
        }

        plotStationsOnMap()
    }

    /**
     * Loads stations from the Web API with local SQLite cache fallback.
     */
    private fun loadStations() {
        progressMapLoading.visibility = View.VISIBLE
        tvStationsCount.text = "Loading stations…"

        lifecycleScope.launch {
            when (val result = nodeApi.getAll(isActive = true)) {
                is ApiResult.Success -> {
                    progressMapLoading.visibility = View.GONE
                    stationList.clear()
                    stationList.addAll(result.data)
                    tvStationsCount.text = "${stationList.size} microgrid hub(s) online"

                    plotStationsOnMap()
                    populateFallbackStationList()
                }
                is ApiResult.Failure -> {
                    progressMapLoading.visibility = View.GONE
                    tvStationsCount.text = "Using offline stations"
                    populateFallbackStationList()
                }
            }
        }
    }

    /**
     * Plots each microgrid station at its exact latitude and longitude on the Google Map.
     */
    private fun plotStationsOnMap() {
        val map = googleMap ?: return
        if (stationList.isEmpty()) return

        map.clear()
        markerStationMap.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var hasValidPoints = false

        for (station in stationList) {
            if (station.latitude != 0.0 && station.longitude != 0.0) {
                val position = LatLng(station.latitude, station.longitude)
                boundsBuilder.include(position)
                hasValidPoints = true

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(station.name)
                        .snippet("Slots: ${station.availableBatterySlots} / ${station.totalBatterySlots}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )

                if (marker != null) {
                    markerStationMap[marker] = station
                }
            }
        }

        if (hasValidPoints) {
            try {
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            } catch (_: Exception) {
                // If map container layout is not yet measured, fall back to first coordinate
                val first = stationList.firstOrNull { it.latitude != 0.0 }
                if (first != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 11f))
                }
            }
        }
    }

    /**
     * Renders visual station list with coordinates and slot management (guarantees functionality).
     */
    private fun populateFallbackStationList() {
        containerStationPins.removeAllViews()
        if (stationList.isEmpty()) return

        for (st in stationList) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(32, 28, 32, 28)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 24
                }
            }

            val titleView = TextView(this).apply {
                text = st.name
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.text_dark))
            }

            val addressView = TextView(this).apply {
                text = "${st.address} (${String.format(Locale.US, "%.4f, %.4f", st.latitude, st.longitude)})"
                textSize = 13f
                setTextColor(getColor(R.color.text_muted))
            }

            val slotsView = TextView(this).apply {
                text = "Available Slots: ${st.availableBatterySlots} of ${st.totalBatterySlots} · Cap: ${st.generationCapacityKw} kW"
                textSize = 13f
                setTextColor(getColor(R.color.primary))
            }

            item.addView(titleView)
            item.addView(addressView)
            item.addView(slotsView)

            item.setOnClickListener {
                showStationDetails(st)
                // Center on map as well
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(st.latitude, st.longitude), 13f))
            }

            containerStationPins.addView(item)
        }
    }

    /**
     * Shows detailed bottom card when a station marker or list item is selected.
     */
    private fun showStationDetails(station: MicrogridStation) {
        selectedStation = station
        cardSelectedStation.visibility = View.VISIBLE

        tvSelectedStationName.text = station.name
        tvSelectedStationAddress.text = station.address.ifBlank { station.nodeCode }
        tvSelectedSlots.text = getString(R.string.operator_map_available_slots, station.availableBatterySlots, station.totalBatterySlots)
        tvSelectedCapacity.text = getString(R.string.operator_map_capacity, station.generationCapacityKw, station.storageCapacityKWh)
        tvGpsCoords.text = String.format(Locale.US, "GPS: %.4f, %.4f", station.latitude, station.longitude)
    }

    /**
     * Prompts the operator to update available battery storage slots (business logic).
     */
    private fun showUpdateSlotsDialog(station: MicrogridStation) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(station.availableBatterySlots.toString())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Update Available Slots")
            .setMessage("Set currently available battery storage slots at ${station.name} (Total Capacity: ${station.totalBatterySlots}):")
            .setView(input)
            .setPositiveButton("Save Slots") { _, _ ->
                val newSlots = input.text.toString().toIntOrNull()
                if (newSlots != null && newSlots in 0..station.totalBatterySlots) {
                    performSlotUpdate(station, newSlots)
                } else {
                    Toast.makeText(this, "Slots must be between 0 and ${station.totalBatterySlots}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSlotUpdate(station: MicrogridStation, newSlots: Int) {
        progressMapLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val res = nodeApi.updateSlots(station.id, newSlots)) {
                is ApiResult.Success -> {
                    progressMapLoading.visibility = View.GONE
                    Toast.makeText(this@OperatorStationsMapActivity, "Battery slots updated successfully.", Toast.LENGTH_SHORT).show()
                    loadStations()
                }
                is ApiResult.Failure -> {
                    progressMapLoading.visibility = View.GONE
                    Toast.makeText(this@OperatorStationsMapActivity, "Failed to update slots: ${res.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Opens the station coordinates in Google Maps or the device's default navigation app.
     */
    private fun openExternalNavigation(station: MicrogridStation) {
        val geoUri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}(${Uri.encode(station.name)})")
        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // If Google Maps app is not installed, open with any available map app
            startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        }
    }
}
