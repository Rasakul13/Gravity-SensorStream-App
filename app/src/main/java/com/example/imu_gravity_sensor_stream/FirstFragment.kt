package com.example.imu_gravity_sensor_stream

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.imu_gravity_sensor_stream.databinding.FragmentFirstBinding

import android.app.AlertDialog

import java.net.InetAddress
import kotlinx.coroutines.*

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PorterDuff
import android.net.wifi.WifiManager
import android.util.TypedValue
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.coroutineScope


/**
 * A [Fragment] subclass as the default destination in the navigation.
 * Used for setting target device and start/stop streaming
 */
class FirstFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var progressBar: ProgressBar

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var isStreaming = false
    private var selectedDevice: Pair<String, String>? = null // Holds selected device name and IP
    private var streamingJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.isWifiConnected.observe(viewLifecycleOwner) { isConnected ->
            updateStatus(isConnected)
        }

        sharedViewModel.isStreaming.observe(viewLifecycleOwner) { streaming ->
            isStreaming = streaming == true // Update local `isStreaming`
            updateStreamingButtonAppearance()
            updateStatus(true)
        }

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        // Set initial button state
        binding.streamingButton.isEnabled = false // Button disabled initially

        // Handle "streaming" button click
        binding.streamingButton.setOnClickListener {
            toggleStreaming()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = requireView().findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        streamingJob?.cancel()  // Cancel any running coroutine
    }

    fun toggleStreaming() {
        if (isStreaming) {
            onStopStreaming()
            sharedViewModel.isStreaming.postValue(false)
        } else {
            onStartStreaming()
            sharedViewModel.isStreaming.postValue(true)
        }

        // Update the flag and button text
        isStreaming = !isStreaming
        updateStreamingButtonAppearance()

        // Update the UI after toggling the streaming state
        updateStatus(true)  // Update the views based on the Wi-Fi status
    }

    private fun updateStreamingButtonAppearance() {
        if (isStreaming) {
            binding.streamingButton.text = getString(R.string.stop)
            binding.streamingButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.streaming_active_color))
        } else {
            binding.streamingButton.text = getString(R.string.start)

            val typedValue = TypedValue()
            val theme = requireContext().theme
            theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
            binding.streamingButton.setBackgroundColor(typedValue.data)
        }
    }

    // Method to update the TextView based on Wi-Fi status and selected device
    @SuppressLint("SetTextI18n")
    fun updateStatus(isWifiConnected: Boolean) {

        if (!isWifiConnected) {
            // No Wi-Fi connection
            binding.textview.text = getString(R.string.please_connect_wifi)
            binding.logo.setImageResource(R.drawable.wifi_logo_foreground)
            binding.streamingButton.isEnabled = false
            if (isStreaming) {
                toggleStreaming()  // Stop streaming if Wi-Fi disconnects
            }
        } else if (isStreaming) {
            // Currently streaming
            binding.textview.text = getString(R.string.streaming) + "${selectedDevice?.first}"
            binding.logo.setImageResource(R.drawable.sensor_launcher_foreground)
            binding.streamingButton.isEnabled = true
        }
        else {
            // Wi-Fi connected, update logo based on device selection
            binding.logo.setImageResource(R.drawable.computer_logo_foreground)
            binding.textview.text = if (selectedDevice == null) {
                getString(R.string.please_select_device)
            } else {
                getString(R.string.selected_device) + "${selectedDevice?.first}"
            }

            // Enable the button if Wi-Fi is connected and a device is selected
            binding.streamingButton.isEnabled = selectedDevice != null
        }
    }

    fun onFabClick() {

        if(isStreaming) {
            toggleStreaming()
        }

        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        progressBar.indeterminateDrawable.setColorFilter(typedValue.data, PorterDuff.Mode.SRC_IN)

        progressBar.visibility = View.VISIBLE
        binding.textview.text = getString(R.string.scanning_devices)

        // Launch a coroutine in the fragment's lifecycle scope
        lifecycleScope.launch {
            val devices = withContext(Dispatchers.IO) {
                scanNetworkForDevices(requireContext())
            }

            // Once the devices are scanned, show the dialog on the main thread
            if (devices.isNotEmpty()) {
                showDeviceListDialog(devices)
            } else {
                binding.textview.text = getString(R.string.please_select_device)
            }
            progressBar.visibility = View.GONE
        }
    }


    /**
     * functionality for listing network devices
     */

    // Method to show a dialog with a list of scanned devices
    @SuppressLint("SetTextI18n")
    private fun showDeviceListDialog(devices: List<Pair<String, String>>) {

        if (devices.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.no_devices))
                .setMessage("No devices found on the network. Please try again.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val deviceNames = devices.map { it.first }.toTypedArray()
        val selectedIndex = devices.indexOfFirst { it == selectedDevice }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Target Device")
            .setSingleChoiceItems(deviceNames, selectedIndex) { _, which ->
                // When a device is selected, update the selectedDevice variable
                selectedDevice = devices[which]
            }
            .setPositiveButton("OK") { _, _ ->
                // When OK is clicked, update the TextView and enable the streaming button
                //binding.textview.text = "Selected device: ${selectedDevice?.first}"
                binding.textview.text = getString(R.string.selected_device) + "${selectedDevice?.first}"
                binding.streamingButton.isEnabled = true
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Reset the TextView to "Please select device" if the user cancels
                if (selectedDevice == null) {
                    binding.textview.text = getString(R.string.please_select_device)
                }
                else {
                    binding.textview.text = getString(R.string.selected_device) + "${selectedDevice?.first}"
                }
            }
            .show()
    }

    private fun scanNetworkForDevices(context: Context): List<Pair<String, String>> {
        val devices = mutableListOf<Pair<String, String>>()
        val localIp = getLocalIpAddress(context)
        localIp?.let {
            val subnet = localIp.substring(0, localIp.lastIndexOf('.'))

            // Launch coroutines to scan network devices in parallel
            runBlocking {
                coroutineScope {
                    (1..254).map { i ->
                        launch(Dispatchers.IO) { // Launching a coroutine for each IP address
                            val ip = "$subnet.$i"
                            try {
                                val address = InetAddress.getByName(ip)
                                if (address.isReachable(200) || retryReachable(address, 2)) { // Ping with 100ms timeout
                                    // If reachable, add to the devices list
                                    devices.add(Pair(address.hostName ?: "Unknown", ip))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }.joinAll() // Wait for all coroutines to finish scanning
                }
            }
        }
        return devices
    }

    // Helper function to retry if initial reachability check fails
    private fun retryReachable(address: InetAddress, retries: Int, timeout: Int = 300): Boolean {
        repeat(retries) {
            if (address.isReachable(timeout)) return true
        }
        return false
    }

    private fun getLocalIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return InetAddress.getByAddress(
            byteArrayOf(
                (ipAddress and 0xff).toByte(),
                (ipAddress shr 8 and 0xff).toByte(),
                (ipAddress shr 16 and 0xff).toByte(),
                (ipAddress shr 24 and 0xff).toByte()
            )
        ).hostAddress
    }

    private fun onStartStreaming() {
        activity?.requestedOrientation = resources.configuration.orientation

        val selectedIp = selectedDevice?.second ?: return
        val port = sharedViewModel.portLiveData.value ?: 5555

        // Start the foreground service
        val serviceIntent = Intent(requireContext(), UdpStreamingService::class.java).apply {
            putExtra("ipAddress", selectedIp)
            putExtra("port", port)
        }
        ContextCompat.startForegroundService(requireContext(), serviceIntent)

        isStreaming = true
    }

    private fun onStopStreaming() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Stop the foreground service
        val serviceIntent = Intent(requireContext(), UdpStreamingService::class.java)
        requireContext().stopService(serviceIntent)

        isStreaming = false
    }

}
