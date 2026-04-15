package com.quicksmart.android.consumer_activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.LiveRideModel
import com.quicksmart.android.consumer_activities.adapter_model.RiderModel
import com.quicksmart.android.consumer_activities.adapter_model.RideBookingModel
import com.quicksmart.android.consumer_activities.adapter_model.RiderAdapter
import com.quicksmart.android.reusable_methods.DirectionsHelper
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.longToast
import com.quicksmart.android.reusable_methods.showConfirmDialog
import com.quicksmart.android.reusable_methods.showPermissionDialog
import com.quicksmart.android.reusable_methods.showSettingsDialog
import com.quicksmart.android.reusable_methods.showToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsumerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    // Map & Location
    private val INDIA_CENTER = LatLng(20.5937, 78.9629)
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var destinationMarker : Marker?   = null
    private var routePolyline     : Polyline? = null
    private var providerMarker    : Marker?   = null
    private var providerPolyline  : Polyline? = null
    private var userLatLng        : LatLng?   = null
    private var destinationLatLng : LatLng?   = null
    private var userAddress       : String    = ""

    // received from HomeFragment via Intent extra
    private var vehicleType: String = "bike"

    // Firebase
    private val db   = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val rtdb = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Views
    private lateinit var destinationInput       : EditText
    private lateinit var clearDestinationButton : ImageView
    private lateinit var backButton             : ImageView
    private lateinit var confirmDestinationPanel: LinearLayout
    private lateinit var selectedDestinationText: TextView
    private lateinit var confirmDestinationButton: Button
    private lateinit var myLocationFab          : View

    // Location Permission checking
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
        else { longToast("Location is off. Please enable GPS to use this feature."); finish() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consumer_map)
        vehicleType = intent.getStringExtra("vehicle_type") ?: "bike"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        bindViews()
        setupMapFragment()
        setupListeners()
    }

    private fun bindViews() {
        destinationInput        = findViewById(R.id.destinationInput)
        clearDestinationButton  = findViewById(R.id.clearDestinationButton)
        backButton              = findViewById(R.id.backButton)
        confirmDestinationPanel = findViewById(R.id.confirmDestinationPanel)
        selectedDestinationText = findViewById(R.id.selectedDestinationText)
        confirmDestinationButton= findViewById(R.id.confirmDestinationButton)
        myLocationFab           = findViewById(R.id.myLocationFab)
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Map ready
    // ─────────────────────────────────────────────────────────────────────
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled     = false
            isMyLocationButtonEnabled = false
            isCompassEnabled          = true
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(INDIA_CENTER, 5f))
        googleMap.setOnMapLongClickListener { latLng -> setDestination(latLng, geocodeLatLng(latLng)) }
        googleMap.setOnMarkerClickListener { marker ->
            if (marker == destinationMarker) clearDestination()
            true
        }
        checkPermissionAndStart()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Permission & GPS flow
    // ─────────────────────────────────────────────────────────────────────
    private fun checkPermissionAndStart() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndProceed()
        } else {
            showPermissionDialog(
                title   = "Location Permission Needed",
                message = "QuickSmart needs your location to show the map and calculate your ride.",
                onAllow = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                onDeny  = { finish() }
            )
        }
    }

    private fun checkGpsAndProceed() {
        if (isGpsEnabled()) moveToUserLocation()
        else showSettingsDialog(
            title          = "Enable Location Services",
            message        = "Please turn on GPS so we can find your current location.",
            onOpenSettings = { gpsSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            onCancel       = { showToast("Location is off. Redirecting to Home."); finish() }
        )
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
                    userLatLng  = LatLng(location.latitude, location.longitude)
                    userAddress = geocodeLatLng(userLatLng!!)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, 16f))
                } else showToast("Unable to fetch location. Try again.")
            }
            .addOnFailureListener { showToast("Location error: ${it.message}") }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Destination helpers
    // ─────────────────────────────────────────────────────────────────────
    private fun setDestination(latLng: LatLng, address: String) {
        destinationMarker?.remove()
        destinationMarker = googleMap.addMarker(
            MarkerOptions().position(latLng).title(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )
        destinationLatLng = latLng
        drawRoute(latLng)
        val origin = userLatLng
        if (origin != null) {
            val bounds  = com.google.android.gms.maps.model.LatLngBounds.Builder().include(origin).include(latLng).build()
            val padding = (60 * resources.displayMetrics.density).toInt()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
        selectedDestinationText.text = address
        destinationInput.setText(address)
        destinationInput.setSelection(address.length)
        confirmDestinationPanel.visibility = View.VISIBLE
        hideKeyboard()
    }

    private fun drawRoute(destination: LatLng) {
        routePolyline?.remove()
        val origin = userLatLng ?: return
        DirectionsHelper.getRoute(origin, destination) { result ->
            val points = result.points
            routePolyline?.remove()
            routePolyline = if (points.isEmpty()) {
                googleMap.addPolyline(PolylineOptions().add(origin, destination)
                    .color(ContextCompat.getColor(this, R.color.darkPurple)).width(8f).geodesic(true))
            } else {
                googleMap.addPolyline(PolylineOptions().addAll(points)
                    .color(ContextCompat.getColor(this, R.color.darkPurple)).width(10f).geodesic(false))
            }
        }
    }

    private fun clearDestination() {
        destinationMarker?.remove(); destinationMarker = null
        routePolyline?.remove();     routePolyline     = null
        selectedDestinationText.text = "—"
        destinationInput.text.clear()
        confirmDestinationPanel.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────────────
    // Passenger limits per vehicle type
    // ─────────────────────────────────────────────────────────────────────
    private fun passengerOptions(): List<String> = when (vehicleType) {
        "bike"     -> listOf("1")
        "rickshaw" -> (1..3).map { "$it" }
        else       -> (1..4).map { "$it" }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vehicle list bottom sheet — fetches live_rides from RTDB
    // ─────────────────────────────────────────────────────────────────────
    private fun showVehicleList() {
        val sheet     = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_vehicle_list, null)
        sheet.setContentView(sheetView)
        sheet.behavior.apply {
            peekHeight = (360 * resources.displayMetrics.density).toInt()
        }

        val sheetTitle       = sheetView.findViewById<TextView>(R.id.sheetTitle)
        val passengerSpinner = sheetView.findViewById<Spinner>(R.id.passengerSpinner)
        val perPersonNotice  = sheetView.findViewById<TextView>(R.id.perPersonRateNotice)
        val noRidersMessage  = sheetView.findViewById<TextView>(R.id.noRidersMessage)
        val ridersList       = sheetView.findViewById<RecyclerView>(R.id.ridersList)
        val progressBar      = sheetView.findViewById<ProgressBar?>(R.id.progressBar)

        val typeLabel = when (vehicleType) {
            "bike" -> "Bike / Scooter"; "rickshaw" -> "Rickshaw"; else -> "Car"
        }
        sheetTitle.text = "Available $typeLabel Riders"

        // Passenger spinner
        val options = passengerOptions()
        passengerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        if (vehicleType == "bike") passengerSpinner.isEnabled = false

        var selectedPassengerCount = 1
        passengerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedPassengerCount = pos + 1
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Fetch from Realtime Database
        progressBar?.visibility = View.VISIBLE
        noRidersMessage.visibility = View.GONE
        ridersList.visibility      = View.GONE
        perPersonNotice.visibility = View.GONE

        val liveRides = mutableListOf<LiveRideModel>()

        rtdb.getReference("live_rides").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar?.visibility = View.GONE
                liveRides.clear()

                snapshot.children.forEach { child ->
                    val isActive = child.child("isActive").getValue(Boolean::class.java) ?: false
                    val vType    = child.child("vehicleType").getValue(String::class.java) ?: ""

                    // Match vehicle type to what consumer selected
                    val matches = when (vehicleType) {
                        "bike"     -> vType.lowercase().contains("bike") || vType.lowercase().contains("scooter")
                        "rickshaw" -> vType.lowercase().contains("rickshaw") || vType.lowercase().contains("auto")
                        else       -> vType.lowercase().contains("car") || vType.lowercase().contains("suv")
                    }

                    if (isActive && matches) {
                        val available = child.child("seatsAvailable").getValue(Int::class.java) ?: 0
                        if (available > 0) {
                            val ride = LiveRideModel(
                                providerId      = child.child("providerId").getValue(String::class.java) ?: "",
                                providerName    = child.child("providerName").getValue(String::class.java) ?: "",
                                vehicleNo       = child.child("vehicleNo").getValue(String::class.java) ?: "",
                                vehicleType     = vType,
                                perPersonPrice  = child.child("perPersonPrice").getValue(Int::class.java) ?: 0,
                                totalSeats      = child.child("totalSeats").getValue(Int::class.java) ?: 0,
                                seatsOccupied   = child.child("seatsOccupied").getValue(Int::class.java) ?: 0,
                                seatsAvailable  = available,
                                currentLocation = child.child("currentLocation").getValue(String::class.java) ?: "",
                                currentLat      = child.child("currentLat").getValue(Double::class.java) ?: 0.0,
                                currentLng      = child.child("currentLng").getValue(Double::class.java) ?: 0.0,
                                destination     = child.child("destination").getValue(String::class.java) ?: "",
                                isActive        = true
                            )
                            liveRides.add(ride)
                        }
                    }
                }

                if (liveRides.isEmpty()) {
                    noRidersMessage.visibility = View.VISIBLE
                    ridersList.visibility      = View.GONE
                    perPersonNotice.visibility = View.GONE
                } else {
                    noRidersMessage.visibility = View.GONE
                    ridersList.visibility      = View.VISIBLE
                    perPersonNotice.visibility = View.VISIBLE

                    // Convert LiveRideModel → RiderModel for existing adapter
                    val riderModels = liveRides.map { lr ->
                        RiderModel(
                            riderId       = lr.providerId,
                            riderName     = lr.providerName,
                            vehicleType   = vehicleType,
                            vehicleNo     = lr.vehicleNo,
                            perPersonRate = "₹${lr.perPersonPrice}",
                            maxPassengers = lr.seatsAvailable,
                            eta           = 0
                        )
                    }

                    ridersList.layoutManager = LinearLayoutManager(this@ConsumerMapActivity)
                    val riderAdapter = RiderAdapter(riderModels, selectedPassengerCount) { selectedRider ->
                        val selectedLive = liveRides.firstOrNull { it.providerId == selectedRider.riderId }
                        if (selectedLive != null) {
                            sheet.dismiss()
                            confirmBooking(selectedLive, selectedPassengerCount)
                        }
                    }
                    ridersList.adapter = riderAdapter

                    // Fetch real-time ETAs from Directions API
                    val userPos = userLatLng
                    if (userPos != null) {
                        riderModels.forEachIndexed { index, rider ->
                            val providerPos = LatLng(liveRides[index].currentLat, liveRides[index].currentLng)
                            DirectionsHelper.getRoute(providerPos, userPos) { result ->
                                if (result.errorMsg == null) {
                                    val durationMin = result.durationSec / 60
                                    rider.eta = durationMin
                                    riderAdapter.notifyItemChanged(index)
                                }
                            }
                        }
                    }

                    passengerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                            selectedPassengerCount = pos + 1
                            riderAdapter.setPassengers(selectedPassengerCount)
                        }
                        override fun onNothingSelected(p: AdapterView<*>?) {}
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar?.visibility = View.GONE
                showToast("Error loading riders: ${error.message}")
                sheet.dismiss()
            }
        })

        sheet.show()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Booking confirmation flow
    // ─────────────────────────────────────────────────────────────────────
    private fun confirmBooking(ride: LiveRideModel, personCount: Int) {
        val totalFare = ride.perPersonPrice * personCount
        showConfirmDialog(
            title   = "Confirm Booking",
            message = "Book a ride with ${ride.providerName}?\n" +
                      "Vehicle: ${ride.vehicleNo}\n" +
                      "Persons: $personCount\n" +
                      "Fare: ₹$totalFare (₹${ride.perPersonPrice} × $personCount)\n" +
                      "Destination: ${ride.destination}",
            positiveLabel = "Yes, Book",
            negativeLabel = "Cancel"
        ) { confirmed ->
            if (confirmed) placeBooking(ride, personCount, totalFare)
        }
    }

    private fun placeBooking(ride: LiveRideModel, personCount: Int, totalFare: Int) {
        val consumerId   = auth.currentUser?.uid ?: run { showToast("Please login first."); return }
        val consumerName = "${getString("firstName")} ${getString("lastName")}".trim()
        val consumerPhone = getString("mobile")

        val bookingData = hashMapOf(
            "consumerId"       to consumerId,
            "consumerName"     to consumerName,
            "consumerPhone"    to consumerPhone,
            "providerId"       to ride.providerId,
            "providerName"     to ride.providerName,
            "vehicleNo"        to ride.vehicleNo,
            "vehicleType"      to ride.vehicleType,
            "fromAddress"      to (userAddress.ifEmpty { "Consumer Location" }),
            "fromLat"          to (userLatLng?.latitude  ?: 0.0),
            "fromLng"          to (userLatLng?.longitude ?: 0.0),
            "toAddress"        to ride.destination,
            "toLat"            to (destinationLatLng?.latitude  ?: 0.0),
            "toLng"            to (destinationLatLng?.longitude ?: 0.0),
            "providerLat"      to ride.currentLat,
            "providerLng"      to ride.currentLng,
            "totalPersons"     to personCount,
            "perPersonPrice"   to ride.perPersonPrice,
            "fare"             to "₹$totalFare",
            "status"           to "pending",
            "bookingType"      to "ride",
            "bookingDate"      to Timestamp.now()
        )

        db.collection("bookings").add(bookingData)
            .addOnSuccessListener { docRef ->
                // Update seat count in RTDB
                val ref = rtdb.getReference("live_rides/${ride.providerId}")
                val newOccupied   = ride.seatsOccupied + personCount
                val newAvailable  = ride.totalSeats   - newOccupied
                ref.updateChildren(mapOf(
                    "seatsOccupied"  to newOccupied,
                    "seatsAvailable" to newAvailable
                ))

                // Add booking reference to RTDB under provider's bookings
                rtdb.getReference("live_rides/${ride.providerId}/bookings/${docRef.id}")
                    .setValue(mapOf(
                        "bookingId"    to docRef.id,
                        "consumerId"   to consumerId,
                        "consumerName" to consumerName,
                        "persons"      to personCount,
                        "createdAt"    to System.currentTimeMillis()
                    ))

                notifyProvider(ride.providerId, consumerName, personCount, ride.vehicleNo)

                showToast("Booking confirmed! Waiting for provider to accept.")
                finish()
            }
            .addOnFailureListener { showToast("Booking failed: ${it.message}") }
    }

    private fun notifyProvider(providerId: String, consumerName: String, personCount: Int, vehicleNo: String) {
        val notification = hashMapOf(
            "userId"     to providerId,
            "title"      to "New Ride Request",
            "message"    to "New ride request from $consumerName for $personCount person(s) on your vehicle ($vehicleNo).",
            "type"       to "ride_request",
            "isRead"     to false,
            "createdAt"  to Timestamp.now(),
            "expiryDate" to com.quicksmart.android.common_activities.NotificationsActivity.expiryTimestamp()
        )
        db.collection("notifications").add(notification)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────
    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        myLocationFab.setOnClickListener {
            userLatLng?.let { googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f)) }
                ?: moveToUserLocation()
        }

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
                    else showToast("Could not find that location. Try again.")
                }
                true
            } else false
        }

        confirmDestinationButton.setOnClickListener {
            if (destinationMarker != null) showVehicleList()
            else showToast("Please select a destination first.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Geocoding
    // ─────────────────────────────────────────────────────────────────────
    private fun geocodeLatLng(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) ?: "Pinned Location"
            else "Pinned Location"
        } catch (e: Exception) { "Pinned Location" }
    }

    private fun geocodeAddress(query: String): LatLng? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) LatLng(addresses[0].latitude, addresses[0].longitude) else null
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(destinationInput.windowToken, 0)
    }
}
