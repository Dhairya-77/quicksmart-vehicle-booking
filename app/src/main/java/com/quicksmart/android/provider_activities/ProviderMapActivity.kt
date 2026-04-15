package com.quicksmart.android.provider_activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.provider_activities.adapter_model.AcceptedRideModel
import com.quicksmart.android.provider_activities.adapter_model.AcceptedRidePopupAdapter
import com.quicksmart.android.provider_activities.adapter_model.NewRideRequestModel
import com.quicksmart.android.provider_activities.adapter_model.NewRideRequestPopupAdapter
import com.quicksmart.android.provider_activities.adapter_model.ProviderVehicleModel
import com.quicksmart.android.reusable_methods.DirectionsHelper
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.longToast
import com.quicksmart.android.reusable_methods.showPermissionDialog
import com.quicksmart.android.reusable_methods.showSettingsDialog
import com.quicksmart.android.reusable_methods.showToast
import java.util.Locale

class ProviderMapActivity : AppCompatActivity(), OnMapReadyCallback {

    // ── Map state ──────────────────────────────────────────────────────────────
    private val INDIA_CENTER = LatLng(20.5937, 78.9629)
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var destinationMarker: Marker? = null
    private var consumerMarker   : Marker? = null
    private var consumerPolyline : Polyline? = null
    private var routePolyline    : Polyline? = null
    private var userLatLng       : LatLng? = null
    private var userAddress      : String = "—"
    private var destinationAddress: String = "Not set"

    // ── Firebase ───────────────────────────────────────────────────────────────
    private val db      = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val rtdb    = FirebaseDatabase.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private var bookingsListener: ValueEventListener? = null

    // ── Provider vehicles for Ride (fetched from Firestore) ───────────────────
    private val rideVehicles = mutableListOf<ProviderVehicleModel>()
    private var rideIsActive      = false
    private var restoredPrice     = ""
    private var restoredOccupied  = 0
    private var restoredVehicleNo = ""

    // ── Accepted rides ─────────────────────────────────────────────────────────
    private val acceptedRides = mutableListOf<AcceptedRideModel>()

    // ── Views ──────────────────────────────────────────────────────────────────
    private lateinit var destinationInput      : EditText
    private lateinit var clearDestinationButton: ImageView
    private lateinit var backButton            : ImageView
    private lateinit var myLocationFab         : FloatingActionButton
    private lateinit var bookingsFab           : FloatingActionButton
    private lateinit var bookingsBadge         : TextView
    private lateinit var rideSettingsFab       : FloatingActionButton

    // ── Permission launchers ───────────────────────────────────────────────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) checkGpsAndProceed()
        else longToast("Location permission is required to use this feature.")
    }

    private val gpsSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isGpsEnabled()) moveToUserLocation()
        else { longToast("Location is off. Please enable GPS."); finish() }
    }

    // ──────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        bindViews()
        setupMapFragment()
        setupListeners()
        fetchRideVehicles()
    }

    override fun onStart() {
        super.onStart()
        startBookingsBadgeListener()
    }

    override fun onStop() {
        super.onStop()
        bookingsListener?.let {
            val userId = auth.currentUser?.uid ?: return
            rtdb.getReference("live_rides/$userId/bookings").removeEventListener(it)
        }
        bookingsListener = null
    }

    // ── View binding ──────────────────────────────────────────────────────────
    private fun bindViews() {
        destinationInput       = findViewById(R.id.destinationInput)
        clearDestinationButton = findViewById(R.id.clearDestinationButton)
        backButton             = findViewById(R.id.backButton)
        myLocationFab          = findViewById(R.id.myLocationFab)
        bookingsFab            = findViewById(R.id.bookingsFab)
        bookingsBadge          = findViewById(R.id.bookingsBadge)
        rideSettingsFab        = findViewById(R.id.rideSettingsFab)
    }

    // ── Fetch provider's For‑Ride vehicles from Firestore ─────────────────────
    private fun fetchRideVehicles() {
        val userId = getString("userId")
        if (userId.isEmpty()) return

        db.collection("vehicles")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("purpose", "For Ride")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                rideVehicles.clear()
                snapshot.documents.forEach { doc ->
                    val v = doc.toObject(ProviderVehicleModel::class.java)
                    if (v != null) rideVehicles.add(v.copy(docId = doc.id))
                }
                // After vehicles loaded, restore active ride state if app was killed
                restoreRideState()
            }
    }

    // ── Restore active ride from RTDB if app was killed while online ──────────
    private fun restoreRideState() {
        val userId = auth.currentUser?.uid ?: return
        rtdb.getReference("live_rides/$userId")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: false
                    if (isActive) {
                        rideIsActive     = true
                        restoredPrice    = (snapshot.child("perPersonPrice").getValue(Int::class.java) ?: 0).toString()
                        restoredOccupied = snapshot.child("seatsOccupied").getValue(Int::class.java) ?: 0
                        restoredVehicleNo = snapshot.child("vehicleNo").getValue(String::class.java) ?: ""
                        // Also restore destination if saved
                        val savedDest = snapshot.child("destination").getValue(String::class.java) ?: ""
                        if (savedDest.isNotEmpty() && savedDest != "Not set") {
                            destinationAddress = savedDest
                        }
                    }
                }
            }
    }

    // ── Realtime Database badge listener ──────────────────────────────────────
    private fun startBookingsBadgeListener() {
        val userId = auth.currentUser?.uid ?: return

        bookingsListener?.let {
            rtdb.getReference("live_rides/$userId/bookings").removeEventListener(it)
        }

        val ref = rtdb.getReference("live_rides/$userId/bookings")
        bookingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                updateBookingsBadge(count)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(bookingsListener!!)
    }

    private fun updateBookingsBadge(count: Int) {
        bookingsBadge.apply {
            if (count > 0) {
                text = if (count > 99) "99+" else count.toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    // ── Google Map ────────────────────────────────────────────────────────────
    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled     = false
            isMyLocationButtonEnabled = false
            isCompassEnabled          = true
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(INDIA_CENTER, 5f))

        googleMap.setOnMapLongClickListener { latLng ->
            setDestination(latLng, geocodeLatLng(latLng))
        }
        googleMap.setOnMarkerClickListener { marker ->
            if (marker == destinationMarker) clearDestination()
            true
        }
        checkPermissionAndStart()
    }

    // ── Location permission + GPS ─────────────────────────────────────────────
    private fun checkPermissionAndStart() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndProceed()
        } else {
            showPermissionDialog(
                title   = "Location Permission Needed",
                message = "QuickSmart needs your location to show the map.",
                onAllow = {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onDeny = { finish() }
            )
        }
    }

    private fun checkGpsAndProceed() {
        if (isGpsEnabled()) {
            moveToUserLocation()
        } else {
            showSettingsDialog(
                title          = "Enable Location Services",
                message        = "Please turn on GPS to use this feature.",
                onOpenSettings = {
                    gpsSettingsLauncher.launch(
                        Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                },
                onCancel = {
                    showToast("Location is off. Redirecting to Home.")
                    finish()
                }
            )
        }
    }

    private fun isGpsEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun moveToUserLocation() {
        googleMap.isMyLocationEnabled = true
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLatLng = LatLng(location.latitude, location.longitude)
                    userAddress = geocodeLatLng(userLatLng!!)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, 16f))
                } else showToast("Unable to fetch location. Try again.")
            }
            .addOnFailureListener { showToast("Location error: ${it.message}") }
    }

    // ── Destination handling ──────────────────────────────────────────────────
    private fun setDestination(latLng: LatLng, address: String) {
        destinationMarker?.remove()
        destinationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )
        destinationAddress = address
        drawRoute(latLng)

        val origin = userLatLng
        if (origin != null) {
            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                .include(origin).include(latLng).build()
            val padding = (60 * resources.displayMetrics.density).toInt()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }

        destinationInput.setText(address)
        destinationInput.setSelection(address.length)
        hideKeyboard()
    }

    private fun drawRoute(destination: LatLng) {
        routePolyline?.remove()
        val origin = userLatLng ?: return
        DirectionsHelper.getRoute(origin, destination) { result ->
            val points = result.points
            routePolyline?.remove()
            val opts = PolylineOptions()
                .color(ContextCompat.getColor(this, R.color.darkPurple))
                .width(if (points.isEmpty()) 8f else 10f)
                .geodesic(points.isEmpty())
            routePolyline = if (points.isEmpty()) {
                googleMap.addPolyline(opts.add(origin, destination))
            } else {
                googleMap.addPolyline(opts.addAll(points))
            }
        }
    }

    private fun clearDestination() {
        destinationMarker?.remove(); destinationMarker = null
        routePolyline?.remove();    routePolyline = null
        destinationInput.text.clear()
        destinationAddress = "Not set"
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        myLocationFab.setOnClickListener {
            userLatLng?.let { googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f)) }
                ?: moveToUserLocation()
        }

        bookingsFab.setOnClickListener { showBookingsPopup() }

        rideSettingsFab.setOnClickListener { showRideSettingsPopup() }

        destinationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearDestinationButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        clearDestinationButton.setOnClickListener { clearDestination() }

        destinationInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = destinationInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    val latLng = geocodeAddress(query)
                    if (latLng != null) setDestination(latLng, query)
                    else showToast("Could not find that location.")
                }
                true
            } else false
        }
    }

    // ── Ride Settings popup ───────────────────────────────────────────────────
    private fun showRideSettingsPopup() {
        val dialog = BottomSheetDialog(this)
        val view   = LayoutInflater.from(this).inflate(R.layout.popup_ride_settings, null)
        dialog.setContentView(view)

        // Allow scrolling over the keyboard
        dialog.behavior.apply {
            state            = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed    = true
            isHideable       = true
        }

        // Read-only fields
        view.findViewById<TextView>(R.id.settings_current_location).text =
            if (userAddress != "—") userAddress else "Fetching location…"
        view.findViewById<TextView>(R.id.settings_destination).text = destinationAddress

        val priceInput    = view.findViewById<EditText>(R.id.settings_per_person_price)
        val occupiedInput = view.findViewById<EditText>(R.id.settings_seats_occupied)
        val totalSeatsTv  = view.findViewById<TextView>(R.id.settings_total_seats)
        val saveBtn       = view.findViewById<android.widget.Button>(R.id.settings_save_btn)
        val progressBar   = view.findViewById<android.widget.ProgressBar>(R.id.settings_progress)

        // Pre-fill restored values if ride was active
        if (rideIsActive) {
            priceInput.setText(restoredPrice)
            occupiedInput.setText(restoredOccupied.toString())
        }

        // ── Vehicle Spinner ────────────────────────────────────────────────────
        val vehicleSpinner = view.findViewById<Spinner>(R.id.settings_vehicle_spinner)
        val vehicleNos = if (rideVehicles.isEmpty())
            listOf("No vehicles added")
        else
            rideVehicles.map { "${it.vehicleNumber} (${it.vehicleType})" }

        vehicleSpinner.adapter = ArrayAdapter(this, R.layout.spinner_selected_item, vehicleNos)
            .also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }

        // Update total seats when vehicle selection changes
        vehicleSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (rideVehicles.isNotEmpty()) {
                    val selected = rideVehicles[position]
                    totalSeatsTv.text = selected.seatCount.toString()
                } else {
                    totalSeatsTv.text = "—"
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // ── Ride switch ────────────────────────────────────────────────────────
        val rideSwitch  = view.findViewById<MaterialSwitch>(R.id.settings_ride_switch)
        val statusLabel = view.findViewById<TextView>(R.id.settings_ride_status_label)
        val statusHint  = view.findViewById<TextView>(R.id.settings_ride_status_hint)

        rideSwitch.isChecked = rideIsActive
        updateSwitchUI(rideIsActive, statusLabel, statusHint)

        rideSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchUI(isChecked, statusLabel, statusHint)
        }

        // ── Save / Activate / Deactivate ──────────────────────────────────────
        saveBtn.setOnClickListener {
            val isOn        = rideSwitch.isChecked
            val price       = priceInput.text.toString().trim()
            val occupiedStr = occupiedInput.text.toString().trim()

            if (isOn) {
                // Validate fields only when going online
                if (destinationAddress == "Not set") {
                    showToast("Please set a destination on the map first."); return@setOnClickListener
                }
                if (price.isEmpty()) {
                    showToast("Please enter a per-person price."); return@setOnClickListener
                }
                val priceVal = price.toIntOrNull()
                if (priceVal == null || priceVal <= 0) {
                    showToast("Please enter a valid price."); return@setOnClickListener
                }
                if (rideVehicles.isEmpty()) {
                    showToast("No 'For Ride' vehicles found. Please add a vehicle first."); return@setOnClickListener
                }
                val selectedIdx     = vehicleSpinner.selectedItemPosition
                val selectedVehicle = rideVehicles[selectedIdx]
                val totalSeats      = selectedVehicle.seatCount
                val occupiedSeats   = occupiedStr.toIntOrNull() ?: 0
                if (occupiedSeats < 0 || occupiedSeats >= totalSeats) {
                    showToast("Seats occupied must be between 0 and ${totalSeats - 1}."); return@setOnClickListener
                }

                // Cache for restore on next open
                restoredPrice     = price
                restoredOccupied  = occupiedSeats
                restoredVehicleNo = selectedVehicle.vehicleNumber

                // Show loading, save to RTDB
                saveBtn.visibility     = View.GONE
                progressBar.visibility = View.VISIBLE
                saveToRealtimeDatabase(price, selectedVehicle, totalSeats, occupiedSeats,
                    saveBtn, progressBar, dialog)
            } else {
                // Deactivate — remove from Realtime Database
                saveBtn.visibility     = View.GONE
                progressBar.visibility = View.VISIBLE
                removeFromRealtimeDatabase(saveBtn, progressBar, dialog)
            }
        }

        dialog.show()
    }

    private fun updateSwitchUI(isOn: Boolean, label: TextView, hint: TextView) {
        if (isOn) {
            label.text = "Online – Accepting Rides"
            hint.text  = "You are visible to nearby consumers"
        } else {
            label.text = "Offline"
            hint.text  = "Toggle to go online"
        }
    }

    private fun saveToRealtimeDatabase(
        price        : String,
        vehicle      : ProviderVehicleModel,
        totalSeats   : Int,
        occupiedSeats: Int,
        saveBtn      : android.widget.Button,
        progressBar  : android.widget.ProgressBar,
        dialog       : BottomSheetDialog
    ) {
        val userId    = auth.currentUser?.uid ?: run {
            showLoadingOff(saveBtn, progressBar); return
        }
        val firstName = getString("firstName")
        val lastName  = getString("lastName")

        // All numeric fields stored as proper types (Int / Double / Long), not String
        val data = mapOf(
            "providerId"      to userId,
            "providerName"    to "$firstName $lastName".trim(),
            "perPersonPrice"  to price.toInt(),            // Int
            "vehicleNo"       to vehicle.vehicleNumber,
            "vehicleType"     to vehicle.vehicleType,
            "totalSeats"      to totalSeats,               // Int
            "seatsOccupied"   to occupiedSeats,            // Int
            "seatsAvailable"  to (totalSeats - occupiedSeats), // Int
            "currentLocation" to userAddress,
            "currentLat"      to (userLatLng?.latitude  ?: 0.0), // Double
            "currentLng"      to (userLatLng?.longitude ?: 0.0), // Double
            "destination"     to destinationAddress,
            "isActive"        to true,                     // Boolean
            "updatedAt"       to System.currentTimeMillis()  // Long
        )

        rtdb.getReference("live_rides/$userId")
            .setValue(data)
            .addOnSuccessListener {
                showLoadingOff(saveBtn, progressBar)
                rideIsActive = true
                showToast("You are now online!")
                dialog.dismiss()
            }
            .addOnFailureListener {
                showLoadingOff(saveBtn, progressBar)
                showToast("Failed to go online: ${it.message}")
            }
    }

    private fun removeFromRealtimeDatabase(
        saveBtn    : android.widget.Button,
        progressBar: android.widget.ProgressBar,
        dialog     : BottomSheetDialog
    ) {
        val userId = auth.currentUser?.uid ?: run {
            showLoadingOff(saveBtn, progressBar); return
        }
        rtdb.getReference("live_rides/$userId")
            .removeValue()
            .addOnSuccessListener {
                showLoadingOff(saveBtn, progressBar)
                rideIsActive     = false
                restoredPrice    = ""
                restoredOccupied = 0
                restoredVehicleNo = ""
                showToast("You are now offline.")
                dialog.dismiss()
            }
            .addOnFailureListener {
                showLoadingOff(saveBtn, progressBar)
                showToast("Failed to go offline: ${it.message}")
            }
    }

    private fun showLoadingOff(saveBtn: android.widget.Button, progressBar: android.widget.ProgressBar) {
        progressBar.visibility = View.GONE
        saveBtn.visibility     = View.VISIBLE
    }

    // ── Bookings popup — Firestore driven ────────────────────────────────────
    private fun showBookingsPopup() {
        val providerId = auth.currentUser?.uid ?: return
        val dialog     = BottomSheetDialog(this)
        val popupView  = LayoutInflater.from(this).inflate(R.layout.popup_ride_bookings, null)
        dialog.setContentView(popupView)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val newRecycler      = popupView.findViewById<RecyclerView>(R.id.recycler_new_requests)
        val acceptedRecycler = popupView.findViewById<RecyclerView>(R.id.recycler_accepted_rides)
        val noNewText        = popupView.findViewById<TextView>(R.id.no_new_requests_text)
        val noAcceptedText   = popupView.findViewById<TextView>(R.id.no_accepted_rides_text)
        newRecycler.layoutManager      = LinearLayoutManager(this)
        acceptedRecycler.layoutManager = LinearLayoutManager(this)

        val pendingList  = mutableListOf<NewRideRequestModel>()
        val acceptedList = mutableListOf<AcceptedRideModel>()

        val acceptedAdapter = AcceptedRidePopupAdapter(
            items      = acceptedList,
            onLocate   = { latLng -> dialog.dismiss(); showConsumerOnMap(latLng) },
            onPickedUp = { item, pos ->
                updateBookingStatus(item.bookingId, "picked_up")
                sendNotificationToUser(item.consumerId, "Provider on the way",
                    "Your provider has picked you up! Please pay when you arrive.")
                acceptedList[pos] = item.copy(status = "picked_up")
                acceptedRecycler.adapter?.notifyItemChanged(pos)
                showToast("Marked as picked up")
            },
            onComplete = { item, pos ->
                completeRideIfPaid(item) {
                    sendNotificationToUser(item.consumerId, "Ride Completed",
                        "Your ride has been completed. Thank you for using QuickSmart!")
                    rtdb.getReference("live_rides/$providerId").get().addOnSuccessListener { snap ->
                        val occ   = ((snap.child("seatsOccupied").getValue(Int::class.java) ?: 0) - item.totalPersons).coerceAtLeast(0)
                        val total = snap.child("totalSeats").getValue(Int::class.java) ?: 0
                        rtdb.getReference("live_rides/$providerId").updateChildren(mapOf(
                            "seatsOccupied" to occ, "seatsAvailable" to (total - occ)))
                        rtdb.getReference("live_rides/$providerId/bookings/${item.bookingId}").removeValue()
                    }
                    acceptedList.removeAt(pos)
                    acceptedRecycler.adapter?.notifyItemRemoved(pos)
                    noAcceptedText.visibility   = if (acceptedList.isEmpty()) View.VISIBLE else View.GONE
                    acceptedRecycler.visibility = if (acceptedList.isEmpty()) View.GONE    else View.VISIBLE
                    showToast("Ride completed!")
                }
            }
        )

        val newAdapter = NewRideRequestPopupAdapter(
            items    = pendingList,
            onAccept = { item, pos ->
                updateBookingStatus(item.bookingId, "accepted")
                sendNotificationToUser(item.consumerId, "Booking Accepted",
                    "Your ride booking has been accepted! Provider is on the way.")
                // Remove from RTDB to refresh badge
                rtdb.getReference("live_rides/$providerId/bookings/${item.bookingId}").removeValue()
                
                pendingList.removeAt(pos)
                newRecycler.adapter?.notifyItemRemoved(pos)
                acceptedList.add(AcceptedRideModel(
                    bookingId = item.bookingId, consumerId = item.consumerId,
                    consumerName = item.consumerName, consumerPhone = item.consumerPhone,
                    pickupAddress = item.pickupAddress, destination = item.destination, vehicleType = item.vehicleType,
                    totalPersons = item.totalPersons, perPersonPrice = item.perPersonPrice,
                    pickupLat = item.pickupLat, pickupLng = item.pickupLng, status = "accepted"
                ))
                acceptedRecycler.adapter?.notifyItemInserted(acceptedList.lastIndex)
                noNewText.visibility        = if (pendingList.isEmpty())  View.VISIBLE else View.GONE
                newRecycler.visibility      = if (pendingList.isEmpty())  View.GONE    else View.VISIBLE
                noAcceptedText.visibility   = if (acceptedList.isEmpty()) View.VISIBLE else View.GONE
                acceptedRecycler.visibility = if (acceptedList.isEmpty()) View.GONE    else View.VISIBLE
                showToast("Accepted: ${item.consumerName}")
            },
            onReject = { item, pos ->
                updateBookingStatus(item.bookingId, "rejected")
                rtdb.getReference("live_rides/$providerId").get().addOnSuccessListener { snap ->
                    val occ   = ((snap.child("seatsOccupied").getValue(Int::class.java) ?: 0) - item.totalPersons).coerceAtLeast(0)
                    val total = snap.child("totalSeats").getValue(Int::class.java) ?: 0
                    rtdb.getReference("live_rides/$providerId").updateChildren(mapOf(
                        "seatsOccupied" to occ, "seatsAvailable" to (total - occ)))
                    rtdb.getReference("live_rides/$providerId/bookings/${item.bookingId}").removeValue()
                }
                sendNotificationToUser(item.consumerId, "Booking Rejected",
                    "Your ride booking was not accepted. Please try another provider.")
                pendingList.removeAt(pos)
                newRecycler.adapter?.notifyItemRemoved(pos)
                noNewText.visibility   = if (pendingList.isEmpty()) View.VISIBLE else View.GONE
                newRecycler.visibility = if (pendingList.isEmpty()) View.GONE    else View.VISIBLE
                showToast("Rejected: ${item.consumerName}")
            },
            onLocate = { latLng -> dialog.dismiss(); showConsumerOnMap(latLng) }
        )

        newRecycler.adapter      = newAdapter
        acceptedRecycler.adapter = acceptedAdapter
        noNewText.visibility = View.VISIBLE; newRecycler.visibility = View.GONE
        noAcceptedText.visibility = View.VISIBLE; acceptedRecycler.visibility = View.GONE

        db.collection("bookings")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("bookingType", "ride")
            .whereIn("status", listOf("pending", "accepted", "picked_up"))
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val status = doc.getString("status") ?: ""
                    when (status) {
                        "pending" -> pendingList.add(NewRideRequestModel(
                            bookingId = doc.id,
                            consumerId = doc.getString("consumerId") ?: "",
                            consumerName = doc.getString("consumerName") ?: "",
                            consumerPhone = doc.getString("consumerPhone") ?: "",
                            vehicleType = doc.getString("vehicleType") ?: "",
                            totalPersons = (doc.getLong("totalPersons") ?: 1L).toInt(),
                            pickupAddress = doc.getString("fromAddress") ?: "",
                            destination = doc.getString("toAddress") ?: "",
                            pickupLat = doc.getDouble("fromLat") ?: 0.0,
                            pickupLng = doc.getDouble("fromLng") ?: 0.0,
                            perPersonPrice = (doc.getLong("perPersonPrice") ?: 0L).toInt()
                        ))
                        "accepted", "picked_up" -> acceptedList.add(AcceptedRideModel(
                            bookingId = doc.id,
                            consumerId = doc.getString("consumerId") ?: "",
                            consumerName = doc.getString("consumerName") ?: "",
                            consumerPhone = doc.getString("consumerPhone") ?: "",
                            pickupAddress = doc.getString("fromAddress") ?: "",
                            destination = doc.getString("toAddress") ?: "",
                            vehicleType = doc.getString("vehicleType") ?: "",
                            totalPersons = (doc.getLong("totalPersons") ?: 1L).toInt(),
                            perPersonPrice = (doc.getLong("perPersonPrice") ?: 0L).toInt(),
                            pickupLat = doc.getDouble("fromLat") ?: 0.0,
                            pickupLng = doc.getDouble("fromLng") ?: 0.0,
                            status = status,
                            paymentDone = doc.getBoolean("paymentDone") ?: false
                        ))
                    }
                }
                newAdapter.notifyDataSetChanged(); acceptedAdapter.notifyDataSetChanged()
                noNewText.visibility        = if (pendingList.isEmpty())  View.VISIBLE else View.GONE
                newRecycler.visibility      = if (pendingList.isEmpty())  View.GONE    else View.VISIBLE
                noAcceptedText.visibility   = if (acceptedList.isEmpty()) View.VISIBLE else View.GONE
                acceptedRecycler.visibility = if (acceptedList.isEmpty()) View.GONE    else View.VISIBLE
            }
            .addOnFailureListener { showToast("Failed to load bookings: ${it.message}") }

        dialog.show()
    }

    private fun updateBookingStatus(bookingId: String, status: String) {
        db.collection("bookings").document(bookingId).update("status", status)
    }

    private fun completeRideIfPaid(item: AcceptedRideModel, onSuccess: () -> Unit) {
        db.collection("bookings").document(item.bookingId)
            .get()
            .addOnSuccessListener { doc ->
                val paymentDone = doc.getBoolean("paymentDone") ?: false
                val currentStatus = doc.getString("status") ?: ""

                if (!paymentDone) {
                    showToast("Cannot complete: payment not received yet.")
                    return@addOnSuccessListener
                }
                if (currentStatus == "completed") {
                    showToast("Ride is already completed.")
                    return@addOnSuccessListener
                }

                db.collection("bookings").document(item.bookingId)
                    .update("status", "completed")
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { showToast("Failed to complete ride: ${it.message}") }
            }
            .addOnFailureListener {
                showToast("Failed to verify payment status: ${it.message}")
            }
    }

    private fun sendNotificationToUser(userId: String, title: String, body: String) {
        if (userId.isBlank()) return
        db.collection("notifications").add(hashMapOf<String, Any>(
            "userId"     to userId,
            "title"      to title,
            "message"    to body,
            "type"       to "ride",
            "isRead"     to false,
            "createdAt"  to com.google.firebase.Timestamp.now(),
            "expiryDate" to com.quicksmart.android.common_activities.NotificationsActivity.expiryTimestamp()
        ))
    }

    // ── Consumer locate: green pin + route line to pickup ─────────────────────
    private fun showConsumerOnMap(pickupLatLng: LatLng) {
        consumerMarker?.remove()
        consumerPolyline?.remove()

        consumerMarker = googleMap.addMarker(
            MarkerOptions()
                .position(pickupLatLng)
                .title("Consumer Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        val origin = userLatLng
        if (origin != null) {
            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                .include(origin).include(pickupLatLng).build()
            val padding = (60 * resources.displayMetrics.density).toInt()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

            showToast("Fetching route to consumer…")
            DirectionsHelper.getRoute(origin, pickupLatLng) { result ->
                val points = result.points
                consumerPolyline?.remove()
                val opts = PolylineOptions()
                    .color(ContextCompat.getColor(this, R.color.green))
                    .width(if (points.isEmpty()) 8f else 10f)
                    .geodesic(points.isEmpty())
                consumerPolyline = if (points.isEmpty()) {
                    googleMap.addPolyline(opts.add(origin, pickupLatLng))
                } else {
                    googleMap.addPolyline(opts.addAll(points))
                }
            }
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 16f))
        }
    }

    // ── Geocoding ────────────────────────────────────────────────────────────
    private fun geocodeLatLng(latLng: LatLng): String {
        return try {
            val gc = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!list.isNullOrEmpty()) list[0].getAddressLine(0) ?: "Pinned Location"
            else "Pinned Location"
        } catch (e: Exception) { "Pinned Location" }
    }

    private fun geocodeAddress(query: String): LatLng? {
        return try {
            val gc = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = gc.getFromLocationName(query, 1)
            if (!list.isNullOrEmpty()) LatLng(list[0].latitude, list[0].longitude) else null
        } catch (e: Exception) { null }
    }

    // ── Keyboard ─────────────────────────────────────────────────────────────
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(destinationInput.windowToken, 0)
    }
}
