package com.flyhi.app.adapters

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flyhi.app.databinding.ItemPeerBinding

/**
 * Adapter for displaying list of discovered WiFi Direct peers
 */
class PeerListAdapter(
    private val onPeerClick: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<PeerListAdapter.PeerViewHolder>() {
    
    private val peers = mutableListOf<WifiP2pDevice>()
    
    fun updatePeers(newPeers: List<WifiP2pDevice>) {
        peers.clear()
        peers.addAll(newPeers)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val binding = ItemPeerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PeerViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(peers[position])
    }
    
    override fun getItemCount() = peers.size
    
    inner class PeerViewHolder(private val binding: ItemPeerBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(device: WifiP2pDevice) {
            binding.tvDeviceName.text = device.deviceName
            binding.tvDeviceStatus.text = getDeviceStatus(device.status)
            
            binding.root.setOnClickListener {
                onPeerClick(device)
            }
        }
        
        private fun getDeviceStatus(status: Int): String {
            return when (status) {
                WifiP2pDevice.CONNECTED -> "Connected"
                WifiP2pDevice.INVITED -> "Invited"
                WifiP2pDevice.FAILED -> "Failed"
                WifiP2pDevice.AVAILABLE -> "Available"
                WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                else -> "Unknown"
            }
        }
    }
}
