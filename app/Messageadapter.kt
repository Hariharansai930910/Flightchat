package com.flyhi.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flyhi.app.databinding.ItemMessageReceivedBinding
import com.flyhi.app.databinding.ItemMessageSentBinding
import com.flyhi.app.models.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying chat messages
 */
class MessageAdapter(
    private val messages: List<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByMe) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
            ReceivedMessageViewHolder(binding)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }
    
    override fun getItemCount() = messages.size
    
    // Sent message viewholder
    class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
            binding.tvTime.text = formatTime(message.timestamp)
        }
    }
    
    // Received message viewholder
    class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            binding.tvSenderName.text = "${message.senderSeat} - ${message.senderName}"
            binding.tvMessage.text = message.content
            binding.tvTime.text = formatTime(message.timestamp)
        }
    }
    
    companion object {
        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
