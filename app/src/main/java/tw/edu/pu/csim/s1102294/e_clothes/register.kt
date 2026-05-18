package tw.edu.pu.csim.s1120336.e_fit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1120336.e_fit.R
class register : AppCompatActivity() {
    lateinit var btn_back: ImageView
    lateinit var btn_register: Button
    lateinit var username: EditText // 新增：綁定用戶名稱
    lateinit var email: EditText
    lateinit var password: EditText
    lateinit var password_again: EditText
    lateinit var firebaseAuth: FirebaseAuth

    private val errorTranslations = mapOf(
        "The email address is badly formatted." to "Email格式不正確",
        "The given password is invalid. [ Password should be at least 6 characters ]" to "密碼無效（密碼應至少為6個字符）",
        "The email address is already in use by another account." to "該電子郵件地址已被使用",
        "The given password is too weak." to "密碼太弱"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 綁定 UI 元件
        btn_back = findViewById(R.id.btn_back)
        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        password_again = findViewById(R.id.password_again)
        btn_register = findViewById(R.id.btn_register)

        firebaseAuth = FirebaseAuth.getInstance()

        // 返回按鈕邏輯：點擊後關閉註冊頁，回到登入頁
        btn_back.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }

        // 註冊按鈕邏輯
        btn_register.setOnClickListener {
            val nameText = username.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val confirmPasswordText = password_again.text.toString().trim()

            if (validateInputs(nameText, emailText, passwordText, confirmPasswordText)) {
                // 將 username 也傳入註冊方法中
                registerUser(nameText, emailText, passwordText)
            }
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "請輸入完整的註冊資訊", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "密碼和確認密碼不相符", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    val db = FirebaseFirestore.getInstance()

                    if (uid != null) {
                        // 🌟 這裡把原本的 "" 替換成了剛才輸入的 name
                        val user = hashMapOf(
                            "email" to email,
                            "頭貼圖片" to "",
                            "使用者名稱" to name, // 成功寫入資料庫！
                            "生日" to "",
                            "性別" to "",
                            "個性簽名" to ""
                        )

                        db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener {
                                // 第一個寫入成功不顯示 Toast，避免跳兩次訊息干擾用戶
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "用戶資料儲存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        db.collection(email)
                            .document("個人資料")
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "註冊成功！", Toast.LENGTH_SHORT).show()
                                clearUserAuthState()
                                navigateToLoginScreen()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "用戶個人資料儲存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "用戶未登入", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMessage = task.exception?.message
                    val translatedError = errorTranslations[errorMessage]
                    val displayMessage = translatedError ?: "註冊失敗：$errorMessage"
                    Toast.makeText(this, displayMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun clearUserAuthState() {
        firebaseAuth.signOut()
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, login::class.java)
        startActivity(intent)
        finish()
    }
}