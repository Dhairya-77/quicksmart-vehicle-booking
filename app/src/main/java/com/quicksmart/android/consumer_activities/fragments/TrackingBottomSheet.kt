package com.quicksmart.android.consumer_activities.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.RideBookingModel
import com.quicksmart.android.reusable_methods.DirectionsHelper

class TrackingBottomSheet : BottomSheetDialogFragment(), OnMapReadyCallback {

    private lateinit var booking: RideBookingModel
    private var googleMap: GoogleMap? = null
    private var providerMarker: Marker? = null
    private var routePolyline: Polyline? = null
    
    private var isBeforePickup = false
    private lateinit var targetLatLng: LatLng
    
    // UI elements
    private lateinit var titleText: TextView
    private lateinit var distanceText: TextView
    private lateinit var durationText: TextView
    private lateinit var progress: ProgressBar

    private val rtdbRef by lazy {
        FirebaseDatabase.getInstance().getReference("live_rides/${booking.providerId}")
    }
    
    private var locationListener: ValueEventListener? = null

    companion object {
        const val TAG = "TrackingBottomSheet"
        
        fun newInstance(b: RideBookingModel) = TrackingBottomSheet().apply {
            arguments = Bundle().apply {
                putString("booking_id",    b.bookingId)
                putString("provider_id",   b.providerId)
                putString("status",        b.status)
                putString("rider_name",    b.riderName)
                putDouble("fromLat",       b.fromLat)
                putDouble("fromLng",       b.fromLng)
                putDouble("toLat",         b.toLat)
                putDouble("toLng",         b.toLng)
                putString("to_address",    b.toAddress)
                putString("from_address",  b.fromAddress)
                putDouble("providerLat",   b.providerLat)
                putDouble("providerLng",   b.providerLng)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        // Determine phase based on status
        val status = args.getString("status") ?: "accepted"
        isBeforePickup = (status == "accepted")
        
        booking = RideBookingModel(
            bookingId   = args.getString("booking_id") ?: "",
            providerId  = args.getString("provider_id") ?: "",
            status      = status,
            riderName   = args.getString("rider_name") ?: "",
            fromLat     = args.getDouble("fromLat"),
            fromLng     = args.getDouble("fromLng"),
            toLat       = args.getDouble("toLat"),
            toLng       = args.getDouble("toLng"),
            toAddress   = args.getString("to_address") ?: "",
            fromAddress = args.getString("from_address") ?: "",
            providerLat = args.getDouble("providerLat"),
            providerLng = args.getDouble("providerLng")
        )
        
        targetLatLng = if (isBeforePickup) {
            LatLng(booking.fromLat, booking.fromLng)
        } else {
            LatLng(booking.toLat, booking.toLng)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_track_distance, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force full height layout behavior if needed
        val peekPx = (resources.displayMetrics.heightPixels * 0.85).toInt()
        val behavior = (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior
        behavior?.peekHeight = peekPx
        behavior?.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior?.isDraggable = false // Prevent map scroll from closing bottom sheet

        titleText    = view.findViewById(R.id.trackTitle)
        distanceText = view.findViewById(R.id.trackDistance)
        durationText = view.findViewById(R.id.trackDuration)
        progress     = view.findViewById(R.id.trackProgress)
        
        titleText.isSingleLine = false
        titleText.maxLines = 2
        titleText.text = if (isBeforePickup) "Waiting for Provider\n(route to your location)"
                         else "Tracking Ride\n(route destination)"

        val mapContainer = view.findViewById<View>(R.id.trackingMapView)
        mapContainer.visibility = View.VISIBLE
        
        // Hide legacy buttons/cards
        view.findViewById<Button>(R.id.btnCloseTracking).visibility = View.GONE
        view.findViewById<View>(R.id.reachedDestinationCard).visibility = View.GONE

        // Assign the main close button to dismiss
        view.findViewById<Button>(R.id.btnTrackClose).setOnClickListener { dismiss() }

        // Setup Map
        val mapFrag = childFragmentManager.findFragmentById(R.id.trackingMapView)
            as? SupportMapFragment ?: SupportMapFragment.newInstance().also { frag ->
            childFragmentManager.beginTransaction()
                .replace(R.id.trackingMapView, frag)
                .commitNow()
        }
        mapFrag.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled     = false
        map.uiSettings.isMyLocationButtonEnabled = false

        // Place target marker
        val titleStr = if (isBeforePickup) "Pickup Location" else "Destination"
        val targetColor = if (isBeforePickup) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE
        map.addMarker(
            MarkerOptions()
                .position(targetLatLng)
                .title(titleStr)
                .icon(BitmapDescriptorFactory.defaultMarker(targetColor))
        )

        // Default layout location
        val initialProvider = LatLng(booking.providerLat, booking.providerLng)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialProvider, 14f))

        startTrackingProvider()
    }

    private fun startTrackingProvider() {
        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val lat = snapshot.child("currentLat").getValue(Double::class.java) ?: return
                val lng = snapshot.child("currentLng").getValue(Double::class.java) ?: return
                
                val providerLatLng = LatLng(lat, lng)
                
                // Update marker
                if (providerMarker == null) {
                    providerMarker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(providerLatLng)
                            .title(booking.riderName)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    )
                } else {
                    providerMarker?.position = providerLatLng
                }

                // Update Camera bounds and Route
                updateRoute(providerLatLng, targetLatLng)
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    distanceText.text = "Error connecting to tracking system"
                }
            }
        }
        rtdbRef.addValueEventListener(locationListener!!)
    }

    private fun updateRoute(origin: LatLng, destination: LatLng) {
        DirectionsHelper.getRoute(origin, destination) { result ->
            if (!isAdded) return@getRoute
            progress.visibility = View.GONE
            
            if (result.errorMsg == null) {
                distanceText.text = if (isBeforePickup) "Provider is ${result.distanceText} away" 
                                    else "Distance to Destination: ${result.distanceText}"
                durationText.text = "ETA: ${result.durationText}"
                
                // Draw polyline
                routePolyline?.remove()
                routePolyline = googleMap?.addPolyline(
                    PolylineOptions()
                        .addAll(result.points)
                        .width(10f)
                        .color(android.graphics.Color.BLUE)
                )

                // Adjust camera bounds to show both
                try {
                    val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    builder.include(origin)
                    builder.include(destination)
                    val padding = 150 // pixels
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))
                } catch (e: Exception) {
                    // Ignore empty bounds error or uninitialized layout error
                }
            } else {
                distanceText.text = "Unable to route path"
                durationText.text = "ETA unknown"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationListener?.let { rtdbRef.removeEventListener(it) }
    }
}
