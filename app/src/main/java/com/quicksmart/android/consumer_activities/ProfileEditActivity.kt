package com.quicksmart.android.consumer_activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.putString
import com.quicksmart.android.reusable_methods.showToast
import de.hdodenhof.circleimageview.CircleImageView

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imgProfile: CircleImageView
    private lateinit var btnChangePhoto: View
    private lateinit var editFirstName: EditText
    private lateinit var editLastName: EditText
    private lateinit var editMobile: EditText
    private lateinit var editEmail: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null

    // ── Image picker ──────────────────────────────────────────────────────────────
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).placeholder(R.drawable.ic_person).into(imgProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        // Initialise Firebase
        auth    = FirebaseAuth.getInstance()
        db      = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
        storage = FirebaseStorage.getInstance()

        toolbar       = findViewById(R.id.toolbar)
        imgProfile    = findViewById(R.id.img_profile)
        btnChangePhoto = findViewById(R.id.btn_change_photo)
        editFirstName = findViewById(R.id.edit_first_name)
        editLastName  = findViewById(R.id.edit_last_name)
        editMobile    = findViewById(R.id.edit_mobile)
        editEmail     = findViewById(R.id.edit_email)
        btnSave       = findViewById(R.id.btn_save)
        progressBar   = findViewById(R.id.progress_bar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Pre-fill from SharedPreferences
        editFirstName.setText(getString("firstName", ""))
        editLastName.setText(getString("lastName", ""))
        editMobile.setText(getString("mobile", ""))
        editEmail.setText(getString("email", ""))

        // Load current photo if any
        val profileImgUrl = getString("profileImg", "")
        if (profileImgUrl.isNotEmpty() && profileImgUrl.isNotBlank()) {
            Glide.with(this).load(profileImgUrl).placeholder(R.drawable.ic_person).into(imgProfile)
        }

        btnChangePhoto.setOnClickListener { imagePicker.launch("image/*") }
        imgProfile.setOnClickListener     { imagePicker.launch("image/*") }

        btnSave.setOnClickListener { saveProfile() }
    }

    private fun saveProfile() {
        val firstName = editFirstName.text.toString().trim()
        val lastName  = editLastName.text.toString().trim()
        val mobile    = editMobile.text.toString().trim()

        if (firstName.isEmpty()) { showToast("Please enter your first name"); return }
        if (lastName.isEmpty())  { showToast("Please enter your last name");  return }
        if (mobile.isEmpty() || mobile.length != 10) {
            showToast("Please enter a valid 10-digit mobile number")
            return
        }

        showLoading()
        val userId = auth.currentUser?.uid ?: run { hideLoading(); showToast("Not logged in"); return }

        if (selectedImageUri != null) {
            // Path: profile_imgs -> userId -> userId_profile.png
            val photoRef = storage.reference.child("profile_imgs/$userId/${userId}_profile.png")
            
            // Delete old one if exists (though putFile overwrites, user requested explicit removal)
            photoRef.delete().addOnCompleteListener {
                // Ignore failure if it didn't exist
                photoRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        photoRef.downloadUrl.addOnSuccessListener { url ->
                            updateFirestore(userId, firstName, lastName, mobile, url.toString())
                        }
                    }
                    .addOnFailureListener { showToast("Photo upload failed: ${it.message}"); hideLoading() }
            }
        } else {
            updateFirestore(userId, firstName, lastName, mobile, null)
        }
    }

    private fun updateFirestore(userId: String, firstName: String, lastName: String, mobile: String, profileImg: String?) {
        val updates = mutableMapOf<String, Any>(
            "firstName" to firstName,
            "lastName"  to lastName,
            "mobile"    to mobile
        )
        if (profileImg != null) updates["profileImg"] = profileImg

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                hideLoading()
                putString("firstName", firstName)
                putString("lastName",  lastName)
                putString("mobile",    mobile)
                if (profileImg != null) putString("profileImg", profileImg)
                showToast("Profile updated!")
                finish()
            }
            .addOnFailureListener { hideLoading(); showToast("Update failed: ${it.message}") }
    }

    private fun showLoading() { btnSave.visibility = View.GONE; progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE; btnSave.visibility = View.VISIBLE }
}
