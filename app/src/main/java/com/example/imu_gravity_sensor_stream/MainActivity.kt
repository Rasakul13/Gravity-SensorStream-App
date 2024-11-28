package com.example.imu_gravity_sensor_stream

import android.app.AlertDialog
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.imu_gravity_sensor_stream.databinding.ActivityMainBinding

import android.content.Context

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null

    private lateinit var networkReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Initialize SensorManager and the Gravity Sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (gravitySensor == null) {
            Log.e("MainActivity", "Gravity sensor not available")
        }

        // Register Listener for gravity sensor updates
        gravitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Initialize network receiver
        networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Network state changed, update FAB appearance
                updateFabAppearance()

                // Notify FirstFragment if it's active
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                if (currentFragment is FirstFragment) {
                   currentFragment.updateStatus(isWifiConnected()) // Call the fragment's method to update the TextView
                }
            }
        }

        // Set FAB initial state
        updateFabAppearance()

        // Handle FAB click
        binding.fab.setOnClickListener { view ->
            if (isWifiConnected()) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                if (currentFragment is FirstFragment) {
                    // Trigger device scanning in FirstFragment
                    currentFragment.onFabClick()
                }
            } else {
                Snackbar.make(view, "Please connect to WiFi to set target device", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(binding.fab) // Set FAB as anchor view to make Snackbar appear above it
                    .show()
            }
        }
    }


    /**
     * sensor member implementations
     */

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // leave empty for now
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {

            val values = event.values

            // Update the ViewModel's gravityData LiveData
            sharedViewModel.gravityData.postValue(values)
            // Also update the Singleton's gravityData LiveData
            SharedViewModel.setGravityData(values)
        }

    }

    // Method to update WiFi status
    private fun updateNetworkStatus() {
        sharedViewModel.isWifiConnected.postValue(isWifiConnected())
    }

    /**
     * functionality for checking wifi connections
     */

   fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Updates FAB color and transparency based on WiFi connection
     */
    private fun updateFabAppearance() {
        if (isWifiConnected()) {
            // Normal appearance
            binding.fab.setColorFilter(ContextCompat.getColor(this, R.color.white))
            binding.fab.alpha = 1f // Fully opaque
        } else {
            // little transparent appearance with greyed icon
            binding.fab.alpha = 0.5f // Semi-transparent to indicate disabled state

            if (isDarkMode()) {
                binding.fab.setColorFilter(ContextCompat.getColor(this, R.color.lightGrey))
            } else {
                binding.fab.setColorFilter(ContextCompat.getColor(this, R.color.grey))
            }
        }
    }

    /**
     * functions for UI
     */

    private fun isDarkMode(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Loop through menu items and set text color
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val s = SpannableString(menuItem.title)

            // Set color dynamically based on the theme (for example, white for dark mode)
            val textColor = if (isDarkMode()) {
                ContextCompat.getColor(this, R.color.white)
            } else {
                ContextCompat.getColor(this, R.color.black)
            }

            // Apply color to the menu item text
            s.setSpan(ForegroundColorSpan(textColor), 0, s.length, 0)
            menuItem.title = s
        }

        return true
    }

    /**
     * functions for main menu
     */

    private fun showPortDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set UDP Port")

        // Create an EditText to input the port
        val input = EditText(this)
        input.hint = "Enter port number"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        // Add the input to a layout for better padding
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(input)

        builder.setView(layout)

        // Set current port value (default 5555) if available
        input.setText(sharedViewModel.portLiveData.value?.toString() ?: "5555")

        builder.setPositiveButton("OK") { dialog, _ ->
            val portValue = input.text.toString().toIntOrNull()
            if (portValue != null) {
                sharedViewModel.portLiveData.postValue(portValue)
                Snackbar.make(binding.root, "Port set to $portValue", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Invalid port value", Snackbar.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showPortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    // Update the network status whenever onResume() is called
    override fun onResume() {
        super.onResume()
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        updateNetworkStatus() // Update the ViewModel
    }

    override fun onPause() {
        super.onPause()
        // Unregister network receiver to avoid memory leaks
        unregisterReceiver(networkReceiver)
    }
}