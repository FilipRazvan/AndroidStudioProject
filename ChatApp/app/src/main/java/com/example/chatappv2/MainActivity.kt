package com.example.chatappv2

import android.app.ProgressDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.GridLayoutManager
import com.example.chatappv2.adapter.UserAdapter
import com.example.chatappv2.databinding.ActivityMainBinding
import com.example.chatappv2.model.User
import com.example.chatappv2.ui.theme.ChatAppV2Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    var binding :ActivityMainBinding? = null
    var database: FirebaseDatabase? = null
    var users: ArrayList<User>? = null
    var dialog: ProgressDialog? = null
    var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        dialog = ProgressDialog(this@MainActivity)
        dialog!!.setMessage("Uploading Image ...")
        dialog!!.setCancelable(false)
        database = FirebaseDatabase.getInstance()
        users = ArrayList<User>()
        var usersAdapter = UserAdapter(this@MainActivity, users!!)
        val layoutManager = GridLayoutManager(this@MainActivity, 2)
        binding!!.mRec.layoutManager = layoutManager
        database!!.reference.child("users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addValueEventListener(object:ValueEventListener{
                override  fun onDataChange(snapshot: DataSnapshot){
                    user = snapshot.getValue((User::class.java))
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

        binding!!.mRec.adapter = usersAdapter
        database!!.reference.child("users").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                users!!.clear()
                for (snapshot1 in snapshot.children){
                    val user:User? = snapshot1.getValue((User::class.java))
                    if (!user!!.uid.equals((FirebaseAuth.getInstance()).uid)) users!!.add(user)
                }
                usersAdapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })



    }

    override fun onResume() {
        super.onResume()
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("presence")
            .child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("presence").child(currentId!!).setValue("Offline")
    }
}