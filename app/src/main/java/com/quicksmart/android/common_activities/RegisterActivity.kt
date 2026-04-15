package com.quicksmart.android.common_activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.showToast
import com.quicksmart.android.reusable_methods.putBool
import com.quicksmart.android.reusable_methods.putString
import android.content.Intent
import android.provider.OpenableColumns
import com.quicksmart.android.common_activities.saveFcmToken

class RegisterActivity : AppCompatActivity() {

    // ─── Firebase ────────────────────────────────────────────────────────────────
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // ─── Views ───────────────────────────────────────────────────────────────────
    private lateinit var spinnerRole: Spinner
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var mobile: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var login: TextView

    // Provider-only cards
    private lateinit var cardMobile: CardView
    private lateinit var cardAadhaar: CardView
    private lateinit var cardLicence: CardView

    // File upload labels & buttons
    private lateinit var aadhaarName: TextView
    private lateinit var licenceName: TextView
    private lateinit var btnUploadAadhaar: ImageButton
    private lateinit var btnUploadLicence: ImageButton

    // ─── URIs for selected PDFs ──────────────────────────────────────────────────
    private var aadhaarUri: Uri? = null
    private var licenceUri: Uri? = null

    // Selected role
    private var selectedRole: String = "Consumer"

    // ─── File Pickers ────────────────────────────────────────────────────────────
    private val aadhaarPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                aadhaarUri = uri
                val fileName = getFileName(uri)
                aadhaarName.text = fileName ?: "aadhaar.pdf"
                aadhaarName.setTextColor(getColor(R.color.darkPurple))
            }
        }

    private val licencePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                licenceUri = uri
                val fileName = getFileName(uri)
                licenceName.text = fileName ?: "licence.pdf"
                licenceName.setTextColor(getColor(R.color.darkPurple))
            }
        }

    // ─────────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialise Firebase
        auth    = FirebaseAuth.getInstance()
        db      = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
        storage = FirebaseStorage.getInstance()

        // Bind views
        spinnerRole     = findViewById(R.id.spinner_role)
        firstName       = findViewById(R.id.first_name)
        lastName        = findViewById(R.id.last_name)
        email           = findViewById(R.id.email)
        mobile          = findViewById(R.id.mobile)
        password        = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirm_password)
        btnRegister     = findViewById(R.id.btn_register)
        progressBar     = findViewById(R.id.progress_bar)
        login           = findViewById(R.id.login)

        // Provider-only cards
        cardMobile   = findViewById(R.id.card_mobile)
        cardAadhaar  = findViewById(R.id.card_aadhaar)
        cardLicence  = findViewById(R.id.card_licence)

        // Upload helpers
        aadhaarName    = findViewById(R.id.aadhaar_name)
        licenceName    = findViewById(R.id.licence_name)
        btnUploadAadhaar = findViewById(R.id.btn_upload_aadhaar)
        btnUploadLicence = findViewById(R.id.btn_upload_licence)

        // ── Role Spinner ──────────────────────────────────────────────────────
        val roles = arrayOf("Consumer", "Provider")
        val spinnerAdapter = ArrayAdapter(this, R.layout.spinner_selected_item, roles)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerRole.adapter = spinnerAdapter

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedRole = parent.getItemAtPosition(position).toString()
                toggleProviderFields(selectedRole == "Provider")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // ── Upload PDF buttons ────────────────────────────────────────────────
        btnUploadAadhaar.setOnClickListener { aadhaarPicker.launch("application/pdf") }
        btnUploadLicence.setOnClickListener { licencePicker.launch("application/pdf") }

        // ── Register button ───────────────────────────────────────────────────
        btnRegister.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener
            registerUser()
        }

        // ── Back to Login ─────────────────────────────────────────────────────
        login.setOnClickListener { finish() }
    }

    // ─── Input Validation ────────────────────────────────────────────────────────
    private fun validateInputs(): Boolean {
        val firstNameText = firstName.text.toString().trim()
        val lastNameText  = lastName.text.toString().trim()
        val emailText     = email.text.toString().trim()
        val mobileText    = mobile.text.toString().trim()
        val passwordText  = password.text.toString().trim()
        val confirmText   = confirmPassword.text.toString().trim()

        if (firstNameText.isEmpty()) { showToast("Please enter your first name"); return false }
        if (lastNameText.isEmpty())  { showToast("Please enter your last name");  return false }
        if (emailText.isEmpty())     { showToast("Please enter your email");       return false }

        // Mobile validation for all roles
        if (mobileText.isEmpty() || mobileText.length != 10) {
            showToast("Please enter a valid 10-digit mobile number")
            return false
        }

        if (selectedRole == "Provider") {
            if (aadhaarUri == null) { showToast("Please upload Aadhaar Card PDF"); return false }
            if (licenceUri == null) { showToast("Please upload Licence PDF");      return false }
        }

        if (passwordText.isEmpty())     { showToast("Please enter a password");                   return false }
        if (passwordText.length < 6)    { showToast("Password must be at least 6 characters");    return false }
        if (confirmText != passwordText) { showToast("Passwords do not match");                    return false }

        return true
    }

    // ─── Step 1 : Create Firebase Auth account ────────────────────────────────────
    private fun registerUser() {
        showLoading()

        val emailText    = email.text.toString().trim()
        val passwordText = password.text.toString().trim()

        auth.createUserWithEmailAndPassword(emailText, passwordText)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user!!.uid

                if (selectedRole == "Provider") {
                    // Upload both PDFs first, then save to Firestore
                    uploadFilesAndSaveUser(userId)
                } else {
                    // Consumer: no files → save directly to Firestore
                    saveUserToFirestore(
                        userId       = userId,
                        aadhaarUrl   = null,
                        licenceUrl   = null
                    )
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                showToast("Registration Failed: ${e.message}")
            }
    }

    // ─── Step 2 (Provider only): Upload PDFs to Firebase Storage ─────────────────
    //
    // Storage paths:
    //   documents/{userId}/{userId}_aadhaar.pdf
    //   documents/{userId}/{userId}_licence.pdf
    //
    private fun uploadFilesAndSaveUser(userId: String) {
        val storageRef = storage.reference.child("documents/$userId")

        val aadhaarRef = storageRef.child("${userId}_aadhaar.pdf")
        val licenceRef = storageRef.child("${userId}_licence.pdf")

        // Upload Aadhaar first
        aadhaarRef.putFile(aadhaarUri!!)
            .addOnSuccessListener {
                aadhaarRef.downloadUrl.addOnSuccessListener { aadhaarUrl ->
                    // Upload Licence after Aadhaar succeeds
                    licenceRef.putFile(licenceUri!!)
                        .addOnSuccessListener {
                            licenceRef.downloadUrl.addOnSuccessListener { licenceUrl ->
                                saveUserToFirestore(
                                    userId     = userId,
                                    aadhaarUrl = aadhaarUrl.toString(),
                                    licenceUrl = licenceUrl.toString()
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            hideLoading()
                            showToast("Licence upload failed: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                showToast("Aadhaar upload failed: ${e.message}")
            }
    }

    // ─── Step 3: Save user document to Firestore ──────────────────────────────────
    //
    // Collection : users
    // Document ID: userId (Firebase Auth UID)
    // Database   : quicksmart-db
    //
    private fun saveUserToFirestore(
        userId: String,
        aadhaarUrl: String?,
        licenceUrl: String?
    ) {
        val userMap = mutableMapOf<String, Any?>(
            "userId"     to userId,
            "firstName"  to firstName.text.toString().trim(),
            "lastName"   to lastName.text.toString().trim(),
            "email"      to email.text.toString().trim(),
            "mobile"     to mobile.text.toString().trim(),
            "role"       to selectedRole.lowercase(),
            "profileImg" to " ",
            "createdAt"  to com.google.firebase.Timestamp.now()
        )

        if (selectedRole == "Provider") {
            userMap["aadhaarUrl"] = aadhaarUrl
            userMap["licenceUrl"] = licenceUrl
            userMap["status"] = "pending"
        }

        db.collection("users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                hideLoading()
                
                // Save to shared prefs
                putString("userId", userId)
                putString("firstName", firstName.text.toString().trim())
                putString("lastName", lastName.text.toString().trim())
                putString("email", email.text.toString().trim())
                putString("mobile", mobile.text.toString().trim())
                putString("role", selectedRole.lowercase())
                putString("profileImg", " ")
                putBool("isLoggedIn", true)

                // Save FCM token for all roles.
                // Currently used by providers; consumer FCM support is planned.
                saveFcmToken(userId)

                showToast("Registration Successful!")
                
                val intent = when (selectedRole.lowercase()) {
                    "admin" -> Intent(this, com.quicksmart.android.admin_activities.AdminMainActivity::class.java)
                    "provider" -> Intent(this, com.quicksmart.android.provider_activities.ProviderMainActivity::class.java)
                    else -> Intent(this, com.quicksmart.android.consumer_activities.ConsumerMainActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                hideLoading()
                showToast("Failed to save user data: ${e.message}")
            }
    }

    // ─── Toggle Provider-specific fields ─────────────────────────────────────────────────
    private fun toggleProviderFields(isProvider: Boolean) {
        val visibility = if (isProvider) View.VISIBLE else View.GONE
        // cardMobile is always visible (all users)
        cardAadhaar.visibility = visibility
        cardLicence.visibility = visibility
    }

    // ─── Loading helpers ──────────────────────────────────────────────────────────
    private fun showLoading() {
        btnRegister.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnRegister.visibility = View.VISIBLE
    }

    // ─── Utility to extract true file name from URI ───────────────────────────────
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
