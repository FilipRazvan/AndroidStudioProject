package com.example.chatappv2

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatappv2.adapter.MessagesAdapter
import com.example.chatappv2.databinding.ActivityChatBinding
import com.example.chatappv2.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private var binding: ActivityChatBinding? = null
    private var adapter: MessagesAdapter? = null
    private var messages: ArrayList<Message>? = null
    private var receiverRoom: String? = null
    private var database: FirebaseDatabase? = null
    private var storage: FirebaseStorage? = null
    private var dialog: ProgressDialog? = null
    private var senderUid: String? = null
    private var receiverUid: String? = null
    private lateinit var senderRoom: String

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        setSupportActionBar(binding!!.toolbar)



        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        dialog = ProgressDialog(this@ChatActivity)
        dialog!!.setMessage("Uploading image...")
        dialog!!.setCancelable(false)

        messages = ArrayList()

        val name = intent.getStringExtra("name")
        val profile = intent.getStringExtra("image")
        binding!!.name.text = name
        Glide.with(this@ChatActivity)
            .load(profile)
            .placeholder(R.drawable.placeholder)
            .into(binding!!.profile01)

        binding!!.imageView.setOnClickListener {
            finish()
        }

        receiverUid = intent.getStringExtra("uid")
        senderUid = FirebaseAuth.getInstance().uid

        database!!.reference.child("Presence").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(String::class.java)
                        if (status == "offline") {
                            binding!!.status.visibility = View.GONE
                        } else {
                            binding!!.status.text = status
                            binding!!.status.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        senderRoom = "$senderUid$receiverUid"
        receiverRoom = "$receiverUid$senderUid"
        adapter = MessagesAdapter(this@ChatActivity, messages!!, senderRoom, receiverRoom!!)

        binding!!.recyclerView.layoutManager = LinearLayoutManager(this@ChatActivity)
        binding!!.recyclerView.adapter = adapter

        database!!.reference.child("chats")
            .child(senderRoom)
            .child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages!!.clear()
                    for (snapshot1 in snapshot.children) {
                        val message: Message? = snapshot1.getValue(Message::class.java)
                        message!!.messageId = snapshot1.key
                        messages!!.add(message)
                    }
                    adapter!!.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        binding!!.send.setOnClickListener {
            val messageTxt: String = binding!!.messageBox.text.toString()
            val date = Date()
            val message = Message(messageTxt, senderUid, date.time)

            binding!!.messageBox.setText("")

            val randomKey = database!!.reference.push().key
            val lastMsgObj = HashMap<String, Any>()
            lastMsgObj["lastMsg"] = message.message!!
            lastMsgObj["lastMsgTime"] = date.time

            database!!.reference.child("chats").child(senderRoom!!)
                .updateChildren(lastMsgObj)
            database!!.reference.child("chats").child(receiverRoom!!)
                .updateChildren(lastMsgObj)
            database!!.reference.child("chats").child(senderRoom!!)
                .child("messages")
                .child(randomKey!!)
                .setValue(message)
                .addOnSuccessListener {
                    database!!.reference.child("chats")
                        .child(receiverRoom!!)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message)
                        .addOnSuccessListener {
                            // Mesajul a fost trimis cu succes
                        }
                }
        }

        binding!!.attachment.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 25)
        }

        val handler = Handler()
        binding!!.messageBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                database!!.reference.child("Presence")
                    .child(senderUid!!)
                    .setValue("Typing...")

                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 1000)
            }

            private val userStoppedTyping = Runnable {
                database!!.reference.child("Presence")
                    .child(senderUid!!)
                    .setValue("Online")
            }
        })

        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding!!.camera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            } else {
                openCamera()
            }



        }
        binding!!.bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_item1 -> {
                    // Implementați acțiunea pentru butonul 1 (Exit) aici
                    finishAffinity()
                    System.exit(0)
                    true
                }
                R.id.navigation_item2 -> {
                    // Implementați acțiunea pentru butonul 2 (Redirectionare către meniul de ChatActivity) aici
                    val intent = Intent(this@ChatActivity, ChatActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_item3 -> {
                    // Implementați acțiunea pentru butonul 3 (Redirectionare către SetupProfileActivity) aici
                    val intent = Intent(this@ChatActivity, SetupProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 25 && resultCode == RESULT_OK && data != null) {
            val selectedImage = data.data
            val calendar = Calendar.getInstance()
            val reference = storage!!.reference.child("chats")
                .child(calendar.timeInMillis.toString())

            dialog!!.show()
            reference.putFile(selectedImage!!)
                .addOnCompleteListener { task ->
                    dialog!!.dismiss()
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            val messageText: String = binding!!.messageBox.text.toString()
                            val date = Date()
                            val message = Message(messageText, senderUid, date.time)
                            message.message = "photo"
                            message.imageUrl = imageUrl // actualizare atributei messageImageUrl
                            binding!!.messageBox.setText("")

                            val randomKey = database!!.reference.push().key
                            val lastMsgObj = HashMap<String, Any>()
                            lastMsgObj["lastMsg"] = message.message!!
                            lastMsgObj["lastMsgTime"] = date.time

                            database!!.reference.child("chats").child(senderRoom!!)
                                .updateChildren(lastMsgObj)
                            database!!.reference.child("chats").child(receiverRoom!!)
                                .updateChildren(lastMsgObj)
                            database!!.reference.child("chats").child(senderRoom!!)
                                .child("messages")
                                .child(randomKey!!)
                                .setValue(message)
                                .addOnSuccessListener {
                                    database!!.reference.child("chats")
                                        .child(receiverRoom!!)
                                        .child("messages")
                                        .child(randomKey)
                                        .setValue(message)
                                        .addOnSuccessListener {
                                            // Mesajul cu imagine a fost trimis cu succes
                                        }
                                }
                        }
                    }
                }
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val bitmap = data.extras!!.get("data") as Bitmap
            val calendar = Calendar.getInstance()
            val reference = storage!!.reference.child("chats")
                .child(calendar.timeInMillis.toString())

            dialog!!.show()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            reference.putBytes(imageData)
                .addOnCompleteListener { task ->
                    dialog!!.dismiss()
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            val messageText: String = binding!!.messageBox.text.toString()
                            val date = Date()
                            val message = Message(messageText, senderUid, date.time)
                            message.message = "photo"
                            message.imageUrl = imageUrl // actualizare atributei messageImageUrl
                            binding!!.messageBox.setText("")

                            val randomKey = database!!.reference.push().key
                            val lastMsgObj = HashMap<String, Any>()
                            lastMsgObj["lastMsg"] = message.message!!
                            lastMsgObj["lastMsgTime"] = date.time

                            database!!.reference.child("chats").child(senderRoom!!)
                                .updateChildren(lastMsgObj)
                            database!!.reference.child("chats").child(receiverRoom!!)
                                .updateChildren(lastMsgObj)
                            database!!.reference.child("chats").child(senderRoom!!)
                                .child("messages")
                                .child(randomKey!!)
                                .setValue(message)
                                .addOnSuccessListener {
                                    database!!.reference.child("chats")
                                        .child(receiverRoom!!)
                                        .child("messages")
                                        .child(randomKey)
                                        .setValue(message)
                                        .addOnSuccessListener {
                                            // Mesajul cu imagine realizată prin cameră a fost trimis cu succes
                                        }
                                }
                        }
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            database!!.reference.child("Presence").child(senderUid!!)
                .setValue("Online")
        }
    }

    // Functie pentru implementarea butonului de Exit (button1)
    fun onExitButtonClicked(view: View) {
        finish()
    }

    // Functie pentru implementarea butonului de redirectionare catre ChatActivity (button2)
    fun onChatButtonClicked(view: View) {
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
    }

    // Functie pentru implementarea butonului de redirectionare catre SetupProfileActivity (button3)
    fun onSetupProfileButtonClicked(view: View) {
        val intent = Intent(this, SetupProfileActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        database!!.reference.child("Presence").child(senderUid!!)
            .setValue("Offline")
    }
}
