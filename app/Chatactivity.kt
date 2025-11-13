package com.flyhi.app

import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.flyhi.app.adapters.MessageAdapter
import com.flyhi.app.databinding.ActivityChatBinding
import com.flyhi.app.models.Message
import com.flyhi.app.models.MessageType
import com.flyhi.app.models.NetworkMessage
import com.flyhi.app.wifi.ConnectionManager
import com.flyhi.app.wifi.WiFiDirectManager

class ChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var connectionManager: ConnectionManager
    private lateinit var wifiDirectManager: WiFiDirectManager
    
    private val messages = mutableListOf<Message>()
    
    private var peerName = ""
    private var mySeat = ""
    private var myName = ""
    private var isConnected = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get extras
        peerName = intent.getStringExtra("PEER_NAME") ?: "Unknown"
        mySeat = intent.getStringExtra("MY_SEAT") ?: "?"
        myName = android.os.Build.MODEL // Use device model as name
        
        setupUI()
        initializeConnection()
    }
    
    private fun setupUI() {
        // Title
        supportActionBar?.title = "Chat with $peerName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Setup RecyclerView
        messageAdapter = MessageAdapter(messages)
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
        }
        
        // Send button
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
        
        binding.tvStatus.text = "Connecting..."
    }
    
    private fun initializeConnection() {
        connectionManager = ConnectionManager()
        wifiDirectManager = WiFiDirectManager(this)
        wifiDirectManager.initialize()
        wifiDirectManager.registerReceiver()
        
        // Setup callbacks
        connectionManager.onConnectionEstablished = {
            runOnUiThread {
                isConnected = true
                binding.tvStatus.text = "Connected ✓"
                Toast.makeText(this, "Connected! Start chatting", Toast.LENGTH_SHORT).show()
                
                // Send initial user info
                sendUserInfo()
            }
        }
        
        connectionManager.onMessageReceived = { networkMessage ->
            runOnUiThread {
                when (networkMessage.type) {
                    MessageType.TEXT -> {
                        val message = Message(
                            senderName = networkMessage.senderName,
                            senderSeat = networkMessage.senderSeat,
                            content = networkMessage.content,
                            timestamp = networkMessage.timestamp,
                            isSentByMe = false
                        )
                        addMessage(message)
                    }
                    MessageType.USER_INFO -> {
                        // Update peer info
                        peerName = networkMessage.senderName
                        supportActionBar?.title = "Chat with $peerName (${networkMessage.senderSeat})"
                    }
                    else -> { /* Handle other types */ }
                }
            }
        }
        
        connectionManager.onConnectionLost = {
            runOnUiThread {
                isConnected = false
                binding.tvStatus.text = "Disconnected ✗"
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Request connection info to determine if we're server or client
        wifiDirectManager.wifiP2pManager?.requestConnectionInfo(
            wifiDirectManager.channel,
            object : WifiP2pManager.ConnectionInfoListener {
                override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
                    info?.let {
                        if (it.groupFormed) {
                            if (it.isGroupOwner) {
                                // We are the server (Group Owner)
                                connectionManager.startServer()
                            } else {
                                // We are the client
                                it.groupOwnerAddress?.let { address ->
                                    connectionManager.connectToServer(address)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
    
    private fun sendUserInfo() {
        val userInfo = NetworkMessage(
            type = MessageType.USER_INFO,
            senderName = myName,
            senderSeat = mySeat,
            content = "Hello"
        )
        connectionManager.sendMessage(userInfo)
    }
    
    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isConnected) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create message
        val message = Message(
            senderName = myName,
            senderSeat = mySeat,
            content = text,
            isSentByMe = true
        )
        
        // Add to UI
        addMessage(message)
        
        // Send over network
        val networkMessage = NetworkMessage(
            type = MessageType.TEXT,
            senderName = myName,
            senderSeat = mySeat,
            content = text
        )
        
        val sent = connectionManager.sendMessage(networkMessage)
        if (!sent) {
            Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
        }
        
        // Clear input
        binding.etMessage.text.clear()
    }
    
    private fun addMessage(message: Message) {
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        connectionManager.disconnect()
        wifiDirectManager.cleanup()
    }
}
