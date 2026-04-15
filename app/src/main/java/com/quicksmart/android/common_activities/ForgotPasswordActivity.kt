package com.quicksmart.android.common_activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.showToast

class ForgotPasswordActivity : AppCompatActivity() {

    // ─── Firebase Auth ──────────────────────────────────────────────────────────
    private lateinit var auth: FirebaseAuth

    // ─── Views ──────────────────────────────────────────────────────────────────
    private lateinit var email: EditText
    private lateinit var btnSendLink: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var backToLogin: TextView

    // ────────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialise Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Bind views
        email       = findViewById(R.id.email)
        btnSendLink = findViewById(R.id.btn_send_link)
        progressBar = findViewById(R.id.progress_bar)
        backToLogin = findViewById(R.id.back_to_login)

        // ── Send Reset Link ───────────────────────────────────────────────────
        btnSendLink.setOnClickListener {
            val emailText = email.text.toString().trim()

            if (emailText.isEmpty()) {
                showToast("Please enter your email")
                return@setOnClickListener
            }

            sendPasswordResetEmail(emailText)
        }

        // ── Back to Login ─────────────────────────────────────────────────────
        backToLogin.setOnClickListener {
            finish()   // pop back to LoginActivity
        }
    }

    // ─── Firebase: Password Reset Email ──────────────────────────────────────────
    private fun sendPasswordResetEmail(email: String) {
        showLoading()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                hideLoading()

                if (task.isSuccessful) {
                    showToast("Reset link sent to $email")
                    finish()
                } else {
                    showToast("Failed: ${task.exception?.message}")
                }
            }
    }

    // ─── Loading helpers ─────────────────────────────────────────────────────────
    private fun showLoading() {
        btnSendLink.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnSendLink.visibility = View.VISIBLE
    }
}
