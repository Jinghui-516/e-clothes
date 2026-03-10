package tw.edu.pu.csim.s1102294.e_clothes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.s1102294.e_clothes.Match.edit_Profile

class change_password : AppCompatActivity() {
    lateinit var old_password: EditText
    lateinit var new_password: EditText
    lateinit var password_again: EditText
    lateinit var btn_submit: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        old_password = findViewById(R.id.old_password)
        new_password = findViewById(R.id.new_password)
        password_again = findViewById(R.id.password_again)

        btn_submit = findViewById(R.id.btn_submit)
        btn_submit.setOnClickListener {
            changePassword()
        }
    }
    private fun changePassword() {
        val user = auth.currentUser

        if (user != null) {
            val newPasswordText = new_password.text.toString()
            val confirmNewPasswordText = password_again.text.toString()
            val currentPasswordText = old_password.text.toString()

            // Check if the new password and confirmation password match
            if (newPasswordText != confirmNewPasswordText) {
                Toast.makeText(this, "請再次確認密碼", Toast.LENGTH_SHORT).show()
                return
            }

            // Re-authenticate the user before updating the password
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPasswordText)

            user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    // Re-authentication successful, proceed to change the password
                    user.updatePassword(newPasswordText).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "更新成功，請重新登入", Toast.LENGTH_SHORT).show()

                            FirebaseAuth.getInstance().signOut()

                            Handler(Looper.getMainLooper()).postDelayed({
                                // Redirect to login activity
                                val intent = Intent(this, login::class.java)
                                startActivity(intent)
                                finish()
                            }, 3000)

                        } else {
                            Toast.makeText(this, "Error updating password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Re-authentication failed
                    Toast.makeText(this, "Re-authentication failed: ${reAuthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }
}