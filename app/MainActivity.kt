
package com.flyhi.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.flyhi.app.adapters.PeerListAdapter
import com.flyhi.app.databinding.ActivityMainBinding
import com.flyhi.app.wifi.WiFiDirectManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiDirectManager: WiFiDirectManager
    private lateinit var peerAdapter: PeerListAdapter
    
    private val PERMISSION_REQUEST_CODE = 100
    
    private var mySeatNumber = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize WiFi Direct Manager
        wifiDirectManager = WiFiDirectManager(this)
        wifiDirectManager.initialize()
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        // Setup RecyclerView for peer list
        peerAdapter = PeerListAdapter { device ->
            onPeerSelected(device)
        }
        
        binding.peersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = peerAdapter
        }
        
        // Scan button
        binding.btnScan.setOnClickListener {
            if (mySeatNumber.isEmpty()) {
                showSeatNumberDialog()
            } else {
                startScanning()
            }
        }
        
        // Set seat button
        binding.btnSetSeat.setOnClickListener {
            showSeatNumberDialog()
        }
        
        // WiFi Direct callbacks
        wifiDirectManager.onPeersChanged = { peers ->
            runOnUiThread {
                peerAdapter.updatePeers(peers)
                binding.tvStatus.text = "Found ${peers.size} passenger(s)"
            }
        }
        
        wifiDirectManager.onDeviceChanged = { device ->
            runOnUiThread {
                binding.tvMyDevice.text = "Device: ${device?.deviceName ?: "Unknown"}"
            }
        }
    }
    
    private fun showSeatNumberDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "e.g., 25A"
            setText(mySeatNumber)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Enter Your Seat Number")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val seat = editText.text.toString().trim().uppercase()
                if (seat.isNotEmpty()) {
                    mySeatNumber = seat
                    binding.tvMySeat.text = "My Seat: $mySeatNumber"
                    Toast.makeText(this, "Seat saved: $mySeatNumber", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startScanning() {
        binding.tvStatus.text = "Scanning for passengers..."
        wifiDirectManager.discoverPeers()
        Toast.makeText(this, "Scanning for nearby passengers", Toast.LENGTH_SHORT).show()
    }
    
    private fun onPeerSelected(device: WifiP2pDevice) {
        AlertDialog.Builder(this)
            .setTitle("Connect to ${device.deviceName}?")
            .setMessage("Start chatting with this passenger?")
            .setPositiveButton("Connect") { _, _ ->
                connectToPeer(device)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun connectToPeer(device: WifiP2pDevice) {
        binding.tvStatus.text = "Connecting to ${device.deviceName}..."
        
        wifiDirectManager.connectToPeer(device) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Connection initiated!", Toast.LENGTH_SHORT).show()
                    
                    // Wait a moment for connection to establish, then open chat
                    binding.root.postDelayed({
                        openChatActivity(device)
                    }, 2000)
                } else {
                    binding.tvStatus.text = "Connection failed"
                    Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun openChatActivity(device: WifiP2pDevice) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("PEER_NAME", device.deviceName)
            putExtra("MY_SEAT", mySeatNumber)
        }
        startActivity(intent)
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // Location permission (required for WiFi Direct discovery)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // Android 13+ nearby WiFi devices permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            onPermissionsGranted()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionsGranted()
            } else {
                Toast.makeText(this, "Permissions required for WiFi Direct", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun onPermissionsGranted() {
        wifiDirectManager.registerReceiver()
        binding.tvStatus.text = "Ready to scan"
    }
    
    override fun onResume() {
        super.onResume()
        wifiDirectManager.registerReceiver()
    }
    
    override fun onPause() {
        super.onPause()
        wifiDirectManager.unregisterReceiver()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        wifiDirectManager.cleanup()
    }
}
