package tw.edu.pu.csim.s1102294.e_clothes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class whether_to_login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whether_to_login)

        auth = FirebaseAuth.getInstance()

        // 檢查使用者是否已登入，如果是，則導航到主畫面
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToHomeScreen(currentUser.uid)
        } else {
            navigateToLogin()
        }

    }

    private fun navigateToHomeScreen(userUid: String) {
        val intent = Intent(this, home::class.java)
        intent.putExtra("userUid", userUid)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, login::class.java)
        startActivity(intent)
        finish()
    }
}