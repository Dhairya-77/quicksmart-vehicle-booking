package com.quicksmart.android.provider_activities

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showToast

class AddVehicleActivity : AppCompatActivity() {

    // ── Firebase ──────────────────────────────────────────────────────────────
    private val db      = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val storage = FirebaseStorage.getInstance()

    // ── State ─────────────────────────────────────────────────────────────────
    private var rcFileUri: Uri? = null

    // ── Views (late-init to avoid repeated findViewById calls) ────────────────
    private lateinit var vehicleTypeSpinner   : Spinner
    private lateinit var vehiclePurposeSpinner: Spinner
    private lateinit var seatCountSpinner    : Spinner
    private lateinit var layoutPricing       : View
    private lateinit var priceInput          : EditText
    private lateinit var vehicleNumView       : EditText
    private lateinit var rcFileName           : TextView
    private lateinit var btnSubmit            : Button
    private lateinit var progressBar          : ProgressBar

    // ── File picker — same pattern as RegisterActivity ────────────────────────
    private val rcPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            rcFileUri = uri
            rcFileName.text = getFileName(uri) ?: "rc_document.pdf"
            rcFileName.setTextColor(getColor(R.color.darkPurple))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        vehicleTypeSpinner    = findViewById(R.id.vehicle_type_spinner)
        vehiclePurposeSpinner = findViewById(R.id.vehicle_purpose_spinner)
        seatCountSpinner      = findViewById(R.id.seat_count_spinner)
        layoutPricing         = findViewById(R.id.layout_pricing)
        priceInput            = findViewById(R.id.price_per_km_input)
        vehicleNumView        = findViewById(R.id.vehicle_number_input)
        rcFileName            = findViewById(R.id.rc_file_name)
        btnSubmit             = findViewById(R.id.btn_submit_vehicle)
        progressBar           = findViewById(R.id.progress_bar)

        // ── Spinners ──────────────────────────────────────────────────────────
        ArrayAdapter(this, R.layout.spinner_selected_item, listOf("Car", "Bike / Scooter", "Rickshaw")).also { a ->
            a.setDropDownViewResource(R.layout.spinner_dropdown_item)
            vehicleTypeSpinner.adapter = a
        }
        ArrayAdapter(this, R.layout.spinner_selected_item, listOf("For Rent", "For Ride")).also { a ->
            a.setDropDownViewResource(R.layout.spinner_dropdown_item)
            vehiclePurposeSpinner.adapter = a
        }

        // ── Pricing visibility ───────────────────────────────────────────────
        vehiclePurposeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val purpose = vehiclePurposeSpinner.selectedItem.toString()
                layoutPricing.visibility = if (purpose == "For Rent") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // ── Seat Count (1-20) ────────────────────────────────────────────────
        val seatOptions = (1..20).map { it.toString() }
        ArrayAdapter(this, R.layout.spinner_selected_item, seatOptions).also { a ->
            a.setDropDownViewResource(R.layout.spinner_dropdown_item)
            seatCountSpinner.adapter = a
        }

        // ── Dynamic Seats calculation ────────────────────────────────────────
        vehicleTypeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val type = vehicleTypeSpinner.selectedItem.toString()
                when (type) {
                    "Bike / Scooter" -> {
                        seatCountSpinner.setSelection(0) // 1 seat
                        seatCountSpinner.isEnabled = false
                        findViewById<CardView>(R.id.card_seat_count).alpha = 0.5f
                    }
                    "Rickshaw" -> {
                        seatCountSpinner.setSelection(2) // 3 seats
                        seatCountSpinner.isEnabled = false
                        findViewById<CardView>(R.id.card_seat_count).alpha = 0.5f
                    }
                    else -> {
                        seatCountSpinner.isEnabled = true
                        findViewById<CardView>(R.id.card_seat_count).alpha = 1.0f
                    }
                }
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // ── RC PDF upload card ────────────────────────────────────────────────
        findViewById<CardView>(R.id.card_upload_rc).setOnClickListener {
            rcPicker.launch("application/pdf")
        }

        // ── Submit ────────────────────────────────────────────────────────────
        btnSubmit.setOnClickListener { validateAndSubmit() }
    }

    // ── Input validation ──────────────────────────────────────────────────────

    private fun validateAndSubmit() {
        val vehicleNum  = vehicleNumView.text.toString().trim().uppercase()
        val vehicleType = vehicleTypeSpinner.selectedItem?.toString() ?: ""
        val purpose     = vehiclePurposeSpinner.selectedItem?.toString() ?: ""
        val seatCount   = seatCountSpinner.selectedItem?.toString()?.toIntOrNull() ?: 1
        val pricePerKm  = if (purpose == "For Rent") priceInput.text.toString().toDoubleOrNull() ?: 0.0 else 0.0

        when {
            vehicleNum.isEmpty() -> { showToast("Please enter the vehicle number."); return }
            purpose == "For Rent" && pricePerKm <= 0 -> { showToast("Please enter a valid price per KM."); return }
            rcFileUri == null    -> { showToast("Please upload the RC document (PDF)."); return }
        }

        val ownerId = getString("userId")
        if (ownerId.isEmpty()) { showToast("Session expired. Please login again."); return }

        showLoading()
        uploadRcAndSave(ownerId, vehicleNum, vehicleType, purpose, seatCount, pricePerKm)
    }

    // ── Step 1: Upload RC PDF to Firebase Storage ─────────────────────────────
    //
    // Storage path: vehicle_rc/{ownerId}/{vehicleNo}.pdf
    //
    private fun uploadRcAndSave(
        ownerId    : String,
        vehicleNum : String,
        vehicleType: String,
        purpose    : String,
        seatCount  : Int,
        pricePerKm : Double
    ) {
        val rcRef = storage.reference
            .child("vehicle_rc/$ownerId/${vehicleNum}.pdf")

        rcRef.putFile(rcFileUri!!)
            .addOnSuccessListener {
                rcRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveVehicleToFirestore(
                        ownerId      = ownerId,
                        vehicleNum   = vehicleNum,
                        vehicleType  = vehicleType,
                        purpose      = purpose,
                        seatCount    = seatCount,
                        pricePerKm   = pricePerKm,
                        rcDocLink    = downloadUrl.toString()
                    )
                }
                .addOnFailureListener { e ->
                    hideLoading()
                    showToast("Failed to get RC download URL: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                showToast("RC upload failed: ${e.message}")
            }
    }

    // ── Step 2: Save vehicle document to Firestore ────────────────────────────
    //
    // Collection : vehicles
    // Document   : auto-ID
    // Fields     : vehicleNumber, vehicleType, purpose, rcDocLink, ownerId,
    //              ownerName, status (pending), createdAt
    //
    private fun saveVehicleToFirestore(
        ownerId    : String,
        vehicleNum : String,
        vehicleType: String,
        purpose    : String,
        seatCount  : Int,
        pricePerKm : Double,
        rcDocLink  : String
    ) {
        val ownerName = "${getString("firstName")} ${getString("lastName")}".trim()

        val vehicleDoc = hashMapOf(
            "vehicleNumber" to vehicleNum,
            "vehicleType"   to vehicleType,
            "purpose"       to purpose,
            "seatCount"     to seatCount,
            "pricePerKm"    to pricePerKm,
            "rcDocLink"     to rcDocLink,
            "ownerId"       to ownerId,
            "ownerName"     to ownerName,
            "status"        to "pending",
            "createdAt"     to Timestamp.now()
        )

        db.collection("vehicles")
            .add(vehicleDoc)
            .addOnSuccessListener {
                hideLoading()
                showToast("Vehicle submitted! Awaiting admin approval.")
                finish()
            }
            .addOnFailureListener { e ->
                hideLoading()
                showToast("Failed to submit vehicle: ${e.message}")
            }
    }

    // ── Loading helpers ───────────────────────────────────────────────────────

    private fun showLoading() {
        btnSubmit.isEnabled    = false
        btnSubmit.visibility   = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnSubmit.visibility   = View.VISIBLE
        btnSubmit.isEnabled    = true
    }

    // ── Extract display filename from URI ─────────────────────────────────────

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col != -1) result = cursor.getString(col)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }
}
