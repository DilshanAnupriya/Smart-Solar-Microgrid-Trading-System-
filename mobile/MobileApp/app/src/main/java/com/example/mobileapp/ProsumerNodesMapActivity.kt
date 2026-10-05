/*
 * File:        ProsumerNodesMapActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Prosumer Mode
 * Author:      Vidura Hewaduwa
 * Created:     2026-10-05
 * Description: Read-only map for a signed-in prosumer. It gets the phone's
 *              location, loads active nearby nodes from the Web API and shows
 *              station availability and capacity when a marker is selected.
 */

package com.example.mobileapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.models.MicrogridStation
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.NodeApi
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.UiUtils
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

/**
 * Prosumer-only nearby-node map. Slot updates deliberately remain in the
 * operator activity and are not exposed from this screen.
 */
class ProsumerNodesMapActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var btnBack: ImageButton
    private lateinit var btnRefresh: TextView
    private lateinit var tvNearbySummary: TextView
    private lateinit var tvMapError: TextView
    private lateinit var progressNearbyNodes: ProgressBar
    private lateinit var mapContainer: View

    private lateinit var cardSelectedNode: View
    private lateinit var btnCloseNodeCard: ImageButton
    private lateinit var tvSelectedNodeName: TextView
    private lateinit var tvSelectedNodeAddress: TextView
    private lateinit var tvSelectedNodeDistance: TextView
    private lateinit var tvSelectedNodeSlots: TextView
    private lateinit var tvSelectedNodeCapacity: TextView
    private lateinit var btnNavigateToNode: Button

    private lateinit var nodeApi: NodeApi
    private lateinit var locationManager: LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var googleMap: GoogleMap? = null
    private var phoneLocation: Location? = null
    private var selectedNode: MicrogridStation? = null
    private var locationListener: LocationListener? = null
    private var waitingForLocation = false

    private val nearbyNodes = mutableListOf<MicrogridStation>()
    private val markerNodeMap = HashMap<Marker, MicrogridStation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This screen depends on the prosumer token used by NodeApi.getNearby.
        val sessionManager = SessionManager(this)
        val currentUser = sessionManager.getCurrentUser()
        if (!sessionManager.hasValidSession() || currentUser?.isProsumer != true) {
            UiUtils.signOutToLogin(this, null)
            return
        }

        setContentView(R.layout.activity_prosumer_nodes_map)
        nodeApi = NodeApi(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        initViews()
        setupListeners()
        setupGoogleMap()
        requestPhoneLocation()
    }

    /** Connects the read-only map controls and station detail card. */
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnRefresh = findViewById(R.id.btnRefreshNearbyNodes)
        tvNearbySummary = findViewById(R.id.tvNearbySummary)
        tvMapError = findViewById(R.id.tvNearbyMapError)
        progressNearbyNodes = findViewById(R.id.progressNearbyNodes)
        mapContainer = findViewById(R.id.prosumerMapContainer)

        cardSelectedNode = findViewById(R.id.cardSelectedNode)
        btnCloseNodeCard = findViewById(R.id.btnCloseNodeCard)
        tvSelectedNodeName = findViewById(R.id.tvSelectedNodeName)
        tvSelectedNodeAddress = findViewById(R.id.tvSelectedNodeAddress)
        tvSelectedNodeDistance = findViewById(R.id.tvSelectedNodeDistance)
        tvSelectedNodeSlots = findViewById(R.id.tvSelectedNodeSlots)
        tvSelectedNodeCapacity = findViewById(R.id.tvSelectedNodeCapacity)
        btnNavigateToNode = findViewById(R.id.btnNavigateToNode)
    }

    /** Sets navigation, retry and marker-card actions. No operator mutation exists here. */
    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { requestPhoneLocation() }
        tvMapError.setOnClickListener { requestPhoneLocation() }
        btnCloseNodeCard.setOnClickListener { cardSelectedNode.visibility = View.GONE }
        btnNavigateToNode.setOnClickListener {
            selectedNode?.let(::openExternalNavigation)
        }
    }

    /** Adds the Maps fragment in code, keeping the XML free of operator-map dependencies. */
    private fun setupGoogleMap() {
        var mapFragment = supportFragmentManager
            .findFragmentById(R.id.prosumerMapContainer) as? SupportMapFragment

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.prosumerMapContainer, mapFragment)
                .commitNow()
        }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = false
        enableMyLocationLayer()

        map.setOnMarkerClickListener { marker ->
            markerNodeMap[marker]?.let(::showNodeDetails)
            markerNodeMap.containsKey(marker)
        }
        map.setOnMapClickListener {
            cardSelectedNode.visibility = View.GONE
        }

        plotNearbyNodes()
    }

    /** Requests location permission at the point where the nearby feature needs it. */
    private fun requestPhoneLocation() {
        UiUtils.showBanner(tvMapError, null)
        cardSelectedNode.visibility = View.GONE
        progressNearbyNodes.visibility = View.VISIBLE
        tvNearbySummary.setText(R.string.prosumer_map_locating)

        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        findCurrentLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST) return

        if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            findCurrentLocation()
        } else {
            showLocationError(getString(R.string.prosumer_map_permission_required))
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests one fresh fix from Android's enabled provider. A last-known fix is
     * retained only as a timeout fallback so the screen remains usable indoors.
     */
    @SuppressLint("MissingPermission")
    private fun findCurrentLocation() {
        if (!hasLocationPermission()) return
        enableMyLocationLayer()

        val enabledProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (enabledProviders.isEmpty()) {
            showLocationError(getString(R.string.prosumer_map_location_disabled))
            return
        }

        val fallbackLocation = enabledProviders
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }

        stopLocationRequest()
        waitingForLocation = true

        val listener = LocationListener { location ->
            if (!waitingForLocation) return@LocationListener
            stopLocationRequest()
            loadNearbyNodes(location)
        }
        locationListener = listener

        val provider = enabledProviders.first()
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())

        mainHandler.postDelayed({
            if (!waitingForLocation) return@postDelayed
            stopLocationRequest()
            if (fallbackLocation != null) {
                loadNearbyNodes(fallbackLocation)
            } else {
                showLocationError(getString(R.string.prosumer_map_location_unavailable))
            }
        }, LOCATION_TIMEOUT_MS)
    }

    /** Calls the prosumer-authorized nearby endpoint using the phone coordinates. */
    private fun loadNearbyNodes(location: Location) {
        phoneLocation = location
        progressNearbyNodes.visibility = View.VISIBLE
        tvNearbySummary.text = getString(R.string.prosumer_map_searching, NEARBY_RADIUS_KM)

        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),
                DEFAULT_ZOOM
            )
        )

        lifecycleScope.launch {
            when (val result = nodeApi.getNearby(
                location.latitude,
                location.longitude,
                NEARBY_RADIUS_KM
            )) {
                is ApiResult.Success -> {
                    progressNearbyNodes.visibility = View.GONE
                    nearbyNodes.clear()
                    nearbyNodes.addAll(result.data.filter {
                        it.isActive && it.latitude in -90.0..90.0 &&
                            it.longitude in -180.0..180.0 &&
                            !(it.latitude == 0.0 && it.longitude == 0.0)
                    })
                    tvNearbySummary.text = getString(
                        R.string.prosumer_map_nodes_found,
                        nearbyNodes.size,
                        NEARBY_RADIUS_KM
                    )
                    plotNearbyNodes()
                }
                is ApiResult.Failure -> {
                    progressNearbyNodes.visibility = View.GONE
                    if (result.isUnauthorized) {
                        UiUtils.signOutToLogin(
                            this@ProsumerNodesMapActivity,
                            getString(R.string.session_expired)
                        )
                    } else {
                        showLocationError(getString(R.string.prosumer_map_load_error, result.message))
                    }
                }
            }
        }
    }

    /** Plots returned stations and frames them together with the phone location. */
    private fun plotNearbyNodes() {
        val map = googleMap ?: return
        val location = phoneLocation ?: return

        map.clear()
        markerNodeMap.clear()

        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(LatLng(location.latitude, location.longitude))

        nearbyNodes.forEach { node ->
            val position = LatLng(node.latitude, node.longitude)
            boundsBuilder.include(position)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(node.name)
                    .snippet(getString(
                        R.string.prosumer_map_marker_slots,
                        node.availableBatterySlots,
                        node.totalBatterySlots
                    ))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            if (marker != null) markerNodeMap[marker] = node
        }

        if (nearbyNodes.isEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    DEFAULT_ZOOM
                )
            )
            return
        }

        // Wait until the map container is measured before fitting all markers.
        mapContainer.post {
            runCatching {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), MAP_PADDING_PX)
                )
            }
        }
    }

    /** Displays only read-only node information when a marker is selected. */
    private fun showNodeDetails(node: MicrogridStation) {
        selectedNode = node
        cardSelectedNode.visibility = View.VISIBLE
        tvSelectedNodeName.text = node.name
        tvSelectedNodeAddress.text = node.address.ifBlank { node.nodeCode }
        tvSelectedNodeDistance.text = getString(
            R.string.prosumer_map_distance,
            node.distanceKm ?: calculateDistanceKm(node)
        )
        tvSelectedNodeSlots.text = getString(
            R.string.prosumer_map_available_slots,
            node.availableBatterySlots,
            node.totalBatterySlots
        )
        tvSelectedNodeCapacity.text = getString(
            R.string.prosumer_map_capacity,
            node.generationCapacityKw,
            node.storageCapacityKWh
        )
    }

    private fun calculateDistanceKm(node: MicrogridStation): Double {
        val location = phoneLocation ?: return 0.0
        val result = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            node.latitude,
            node.longitude,
            result
        )
        return result[0] / 1000.0
    }

    /** Opens navigation without exposing any operator-only node controls. */
    private fun openExternalNavigation(node: MicrogridStation) {
        val label = Uri.encode(node.name)
        val geoUri = Uri.parse(
            "geo:${node.latitude},${node.longitude}?q=${node.latitude},${node.longitude}($label)"
        )
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        runCatching { startActivity(googleMapsIntent) }
            .onFailure { startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        if (hasLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopLocationRequest() {
        waitingForLocation = false
        locationListener?.let { listener ->
            if (hasLocationPermission()) {
                runCatching { locationManager.removeUpdates(listener) }
            }
        }
        locationListener = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun showLocationError(message: String) {
        progressNearbyNodes.visibility = View.GONE
        tvNearbySummary.setText(R.string.prosumer_map_retry_help)
        UiUtils.showBanner(tvMapError, message)
    }

    override fun onDestroy() {
        stopLocationRequest()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 2101
        private const val LOCATION_TIMEOUT_MS = 8_000L
        private const val NEARBY_RADIUS_KM = 50.0
        private const val DEFAULT_ZOOM = 12f
        private const val MAP_PADDING_PX = 120
    }
}
