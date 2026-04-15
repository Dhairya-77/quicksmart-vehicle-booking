package com.quicksmart.android.admin_activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.quicksmart.android.R
import com.quicksmart.android.admin_activities.fragments.HomeFragment
import com.quicksmart.android.common_activities.LoginActivity
import com.quicksmart.android.reusable_methods.clearAll
import com.quicksmart.android.reusable_methods.showConfirmDialog
import com.quicksmart.android.reusable_methods.showToast

class AdminMainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_toolbar_menu, menu)
        // Tint the logout icon white to match the dark toolbar
        menu.findItem(R.id.action_logout)
            ?.icon
            ?.setTint(getColor(R.color.white))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showConfirmDialog(
                    title = "Logout",
                    message = "Are you sure you want to logout?",
                    positiveLabel = "Yes",
                    negativeLabel = "No"
                ) { confirmed ->
                    if (confirmed) {
                        showToast("Logging out...")
                        this.clearAll()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        showToast("Logout cancelled")
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
