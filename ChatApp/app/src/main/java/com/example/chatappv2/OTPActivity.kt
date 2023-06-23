package com.example.chatappv2

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.chatappv2.databinding.ActivityOtpactivityBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import java.util.concurrent.TimeUnit

class OTPActivity : AppCompatActivity() {

    private var binding: ActivityOtpactivityBinding? = null
    private var verificationId: String? = null
    private var auth: FirebaseAuth? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpactivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        dialog = ProgressDialog(this@OTPActivity)
        dialog!!.setMessage("Sending OTP....")
        dialog!!.setCancelable(false)
        dialog!!.show()
        auth = FirebaseAuth.getInstance()
        supportActionBar?.hide()
        val phoneNumber = intent.getStringExtra("phoneNumber")
        binding!!.phoneLble.text = "Verify $phoneNumber"

        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phoneNumber!!)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this@OTPActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(p0)
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    Toast.makeText(this@OTPActivity, "Verification failed: ${p0.message}", Toast.LENGTH_SHORT).show()
                    dialog!!.dismiss()
                }

                override fun onCodeSent(
                    verifyId: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verifyId, forceResendingToken)
                    dialog!!.dismiss()
                    verificationId = verifyId
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    binding!!.otpView.requestFocus()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
        binding!!.otpView.setOtpCompletionListener { otp ->
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth!!.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@OTPActivity, SetupProfileActivity::class.java)
                    Log.d("OTPActivity", "Launching SetupProfileActivity")

                    startActivity(intent)
                    finishAffinity()
                } else {
                    Toast.makeText(this@OTPActivity, "Failed to sign in: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        dialog?.dismiss()
    }
}
