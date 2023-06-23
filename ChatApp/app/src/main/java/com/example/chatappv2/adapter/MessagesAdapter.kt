package com.example.chatappv2.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatappv2.R
import com.example.chatappv2.databinding.DeleteLayoutBinding
import com.example.chatappv2.databinding.ReceiveMsgBinding
import com.example.chatappv2.databinding.SendMsgBinding
import com.example.chatappv2.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MessagesAdapter(
    private val context: Context,
    private var messageList: List<Message>,
    private val senderRoom: String,
    private val receiverRoom: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SENT = 1
    private val ITEM_RECEIVE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            val view = LayoutInflater.from(context).inflate(
                R.layout.send_msg, parent, false
            )
            SentMsgHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(
                R.layout.receive_msg, parent, false
            )
            ReceiveMsgHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (FirebaseAuth.getInstance().uid == message.senderId) {
            ITEM_SENT
        } else {
            ITEM_RECEIVE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is SentMsgHolder) {
            if (message.message == "photo") {
                holder.binding.image.visibility = View.VISIBLE
                holder.binding.message.visibility = View.GONE
                holder.binding.mLinear.visibility = View.GONE
                Glide.with(context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(holder.binding.image)
            } else {
                holder.binding.message.text = message.message
            }

            holder.itemView.setOnLongClickListener {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.delete_layout, null)
                val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)

                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()

                binding.everyone.setOnClickListener {
                    message.message = "This message is removed"
                    message.messageId?.let { messageId ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(senderRoom)
                            .child("message")
                            .child(messageId).setValue(message)
                    }

                    dialog.dismiss()
                }

                binding.delete.setOnClickListener {
                    message.messageId?.let { messageId ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(senderRoom)
                            .child("message")
                            .child(messageId).setValue(null)
                    }

                    dialog.dismiss()
                }

                binding.cancel.setOnClickListener { dialog.dismiss() }
                dialog.show()

                false
            }
        } else if (holder is ReceiveMsgHolder) {
            if (message.message == "photo") {
                holder.binding.image.visibility = View.VISIBLE
                holder.binding.message.visibility = View.GONE
                holder.binding.mLinear.visibility = View.GONE
                Glide.with(context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(holder.binding.image)
            } else {
                holder.binding.message.text = message.message
            }

            holder.itemView.setOnLongClickListener {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.delete_layout, null)
                val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)

                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()

                binding.everyone.setOnClickListener {
                    message.message = "This message is removed"
                    message.messageId?.let { messageId ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(senderRoom)
                            .child("message")
                            .child(messageId).setValue(message)
                    }

                    dialog.dismiss()
                }

                binding.delete.setOnClickListener {
                    message.messageId?.let { messageId ->
                        FirebaseDatabase.getInstance().reference.child("chats")
                            .child(senderRoom)
                            .child("message")
                            .child(messageId).setValue(null)
                    }

                    dialog.dismiss()
                }

                binding.cancel.setOnClickListener { dialog.dismiss() }
                dialog.show()

                false
            }
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun updateMessages(messages: List<Message>) {
        messageList = messages
        notifyDataSetChanged()
    }

    inner class SentMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: SendMsgBinding = SendMsgBinding.bind(itemView)
    }

    inner class ReceiveMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ReceiveMsgBinding = ReceiveMsgBinding.bind(itemView)
    }
}
