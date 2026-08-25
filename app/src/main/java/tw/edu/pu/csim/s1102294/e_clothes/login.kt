package tw.edu.pu.csim.s1120336.e_fit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.s1120336.e_fit.home
import java.util.*
import tw.edu.pu.csim.s1120336.e_fit.R
class login : AppCompatActivity() {

    lateinit var btn_login: Button
    lateinit var txv_register: TextView
    lateinit var txv_forget: TextView
    lateinit var user_name: EditText
    lateinit var password: EditText
    lateinit var firebaseAuth: FirebaseAuth

    // 錯誤訊息翻譯保留
    private val errorTranslations = mapOf(
        "The email address is badly formatted." to "Email格式不正確",
        "The password is invalid or the user does not have a password." to "密碼錯誤",
        "There is no user record corresponding to this identifier. The user may have been deleted." to "此帳號尚未註冊，請進行註冊"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        // 綁定 XML 裡的 ID (兩邊完全一致了！)
        user_name = findViewById(R.id.user_name)
        password = findViewById(R.id.password)
        btn_login = findViewById(R.id.btn_login)
        txv_register = findViewById(R.id.txv_register)
        txv_forget = findViewById(R.id.txv_forget)

        firebaseAuth = FirebaseAuth.getInstance()

        btn_login.setOnClickListener {
            val email = user_name.text.toString().trim()
            val pass = password.text.toString().trim()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                signInUser(email, pass)
            } else {
                Toast.makeText(this, "請輸入有效的帳號密碼", Toast.LENGTH_SHORT).show()
            }
//            val timer = Timer()
//            timer.schedule(object : TimerTask() {
//                override fun run() {
//                    val intent1 = Intent(this@login, home::class.java)
//                    startActivity(intent1)
//                    finish()
//                }
//            }, 2000L) //3秒後跳轉頁面
        }

        txv_register.setOnClickListener {
            val intent2 = Intent(this, register::class.java)
            startActivity(intent2)
            finish()
        }

        txv_forget.setOnClickListener {
            val intent3 = Intent(this, forget_password::class.java)
            startActivity(intent3)
            finish()
        }
    }

    private fun signInUser(name: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(name, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        Toast.makeText(this, "登入成功，歡迎 ${user.email}", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, home::class.java)
                        startActivity(intent)
                        finish() // 登入成功後關閉登入頁面
                    }
                } else {
                    val errorMessage = task.exception?.message
                    val translatedError = errorTranslations[errorMessage]
                    val errorMsg = translatedError ?: "登入失敗: $errorMessage"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
    }
}