package com.example.chatappv2

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.chatappv2.databinding.ActivitySetupProfileBinding
import com.example.chatappv2.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class SetupProfileActivity : AppCompatActivity() {

    private var binding: ActivitySetupProfileBinding? = null
    private var auth: FirebaseAuth? = null
    private var database: FirebaseDatabase? = null
    private var storage: FirebaseStorage? = null
    private var selectedImage: Uri? = null
    private var dialog: ProgressDialog? = null

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        dialog = ProgressDialog(this@SetupProfileActivity)
        dialog!!.setMessage("Updating Profile...")
        dialog!!.setCancelable(false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        supportActionBar?.hide()

        binding!!.imageView.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,45)
        }

        binding!!.continueBtn02.setOnClickListener {
            val name: String = binding!!.profileBox.text.toString()
            if (name.isEmpty()) {
                binding!!.profileBox.error = "Please type a name"
            } else {
                dialog!!.show()
                if (selectedImage != null) {
                    uploadProfileImage()
                } else {
                    saveUserToDatabase("No Image")
                }
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        val result = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 45)
    }

    private fun uploadProfileImage() {
        val reference = storage!!.reference.child("Profile").child(auth!!.uid!!)
        reference.putFile(selectedImage!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reference.downloadUrl.addOnCompleteListener { uri ->
                        val imageUrl = uri.toString()
                        saveUserToDatabase(imageUrl)
                    }
                } else {
                    Log.e("SetupProfileActivity", "Image upload failed: ${task.exception}")
                    saveUserToDatabase("No Image")
                }
            }
    }

    private fun saveUserToDatabase(imageUrl: String) {
        val uid = auth!!.uid
        val phone = auth!!.currentUser!!.phoneNumber
        val name: String = binding!!.profileBox.text.toString()
        val user = User(uid, name, phone, imageUrl)
        database!!.reference.child("users").child(uid!!)
            .setValue(user)
            .addOnCompleteListener { task ->
                dialog!!.dismiss()
                if (task.isSuccessful) {
                    val intent = Intent(this@SetupProfileActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("SetupProfileActivity", "Saving user to database failed: ${task.exception}")
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 45 && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            binding!!.imageView.setImageURI(uri)
            selectedImage = uri
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
