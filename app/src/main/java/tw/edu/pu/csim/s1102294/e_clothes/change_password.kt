package tw.edu.pu.csim.s1120336.e_fit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.s1120336.e_fit.R

class change_password : AppCompatActivity() {

    lateinit var old_password: EditText
    lateinit var new_password: EditText
    lateinit var password_again: EditText
    lateinit var btn_submit: Button
    lateinit var btn_back: ImageView // 🌟 新增返回按鈕的宣告

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        // 🌟 更新這裡的 ID，對齊新版的 XML
        old_password = findViewById(R.id.et_old_password)
        new_password = findViewById(R.id.et_new_password)
        password_again = findViewById(R.id.et_confirm_password)
        btn_submit = findViewById(R.id.btn_confirm_change)

        btn_back = findViewById(R.id.btn_back) // 🌟 綁定返回按鈕

        // 🌟 處理返回上一頁的點擊事件
        btn_back.setOnClickListener {
            finish()
        }

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
                                // 🌟 避免使用者按返回鍵又回到上一頁，清除 activity 堆疊
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }, 3000)

                        } else {
                            Toast.makeText(this, "更新密碼失敗: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Re-authentication failed
                    Toast.makeText(this, "舊密碼驗證失敗，請檢查是否輸入正確", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "無法取得使用者資訊，請重新登入", Toast.LENGTH_SHORT).show()
        }
    }
}