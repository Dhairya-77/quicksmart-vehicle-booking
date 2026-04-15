package com.quicksmart.android.common_activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.ConsumerMainActivity
import com.quicksmart.android.reusable_methods.showToast
import com.quicksmart.android.reusable_methods.putBool
import com.quicksmart.android.reusable_methods.putString
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.common_activities.saveFcmToken

class LoginActivity : AppCompatActivity() {

    //Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //Views
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPassword: TextView
    private lateinit var register: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialise Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

        // Bind views
        email           = findViewById(R.id.email)
        password        = findViewById(R.id.password)
        btnLogin        = findViewById(R.id.btn_login)
        progressBar     = findViewById(R.id.progress_bar)
        forgotPassword  = findViewById(R.id.forgot_password)
        register        = findViewById(R.id.register)

        // Login
        btnLogin.setOnClickListener {
            val emailText    = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty()) {
                showToast("Please enter your email")
                return@setOnClickListener
            }
            if (passwordText.isEmpty()) {
                showToast("Please enter your password")
                return@setOnClickListener
            }

            signInWithEmailPassword(emailText, passwordText)
        }

        // Forgot Password
        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Register
        register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Firebase: Email / Password Sign-In
    private fun signInWithEmailPassword(email: String, password: String) {
        showLoading()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                hideLoading()
                                if (document != null && document.exists()) {
                                    val role      = document.getString("role") ?: "consumer"
                                    val firstName = document.getString("firstName") ?: ""
                                    val lastName  = document.getString("lastName") ?: ""
                                    val mobile  = document.getString("mobile") ?: ""
                                    val profileImg  = document.getString("profileImg") ?: ""

                                    // Save to shared prefs
                                    putString("userId", userId)
                                    putString("firstName", firstName)
                                    putString("lastName", lastName)
                                    putString("email", email)
                                    putString("mobile", mobile)
                                    putString("role", role)
                                    putString("profileImg", profileImg)
                                    putBool("isLoggedIn", true)

                                    // Save/refresh FCM token for all roles.
                                    // Currently used by providers; consumer FCM support is planned.
                                    saveFcmToken(userId)

                                    showToast("Login Successful!")
                                    navigateToHome(role)
                                } else {
                                    showToast("User document not found")
                                }
                            }
                            .addOnFailureListener { e ->
                                hideLoading()
                                showToast("Failed to fetch user data: ${e.message}")
                            }
                    } else {
                        hideLoading()
                        showToast("User ID is null")
                    }
                } else {
                    hideLoading()
                    showToast("Login Failed: ${task.exception?.message}")
                }
            }
    }

    // Loading helpers
    private fun showLoading() {
        btnLogin.visibility    = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnLogin.visibility    = View.VISIBLE
    }

    // Navigation
    private fun navigateToHome(role: String) {
        val intent = when (role.lowercase()) {
            "admin" -> Intent(this, com.quicksmart.android.admin_activities.AdminMainActivity::class.java)
            "provider" -> Intent(this, com.quicksmart.android.provider_activities.ProviderMainActivity::class.java)
            else -> Intent(this, com.quicksmart.android.consumer_activities.ConsumerMainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}