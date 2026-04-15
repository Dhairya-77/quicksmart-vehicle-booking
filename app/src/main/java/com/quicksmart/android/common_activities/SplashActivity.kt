package com.quicksmart.android.common_activities

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.ConsumerMainActivity
import com.quicksmart.android.reusable_methods.getBool
import com.quicksmart.android.reusable_methods.getString

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appName  = findViewById<TextView>(R.id.app_name)
        val tagLine  = findViewById<TextView>(R.id.tagline)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        //Step 1: Fade in app name (starts immediately)
        appName.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        //Step 2: Fade in tagline (after app name appears)
        tagLine.animate()
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(800)
            .start()

        //Step 3: Fade in progress bar (after tagline)
        progressBar.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1300)
            .withEndAction {
                //Step 4: Check login status & route correctly
                val isLoggedIn = getBool("isLoggedIn", false)
                if (isLoggedIn) {
                    val role = getString("role", "consumer")
                    val intent = when (role.lowercase()) {
                        "admin" -> Intent(this, com.quicksmart.android.admin_activities.AdminMainActivity::class.java)
                        "provider" -> Intent(this, com.quicksmart.android.provider_activities.ProviderMainActivity::class.java)
                        else -> Intent(this, com.quicksmart.android.consumer_activities.ConsumerMainActivity::class.java)
                    }
                    startActivity(intent)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }
            .setStartDelay(1400)
            .start()
    }
}
