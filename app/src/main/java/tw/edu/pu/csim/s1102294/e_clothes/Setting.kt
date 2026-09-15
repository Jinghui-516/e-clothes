package tw.edu.pu.csim.s1120336.e_fit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Setting : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var layoutChangePassword: LinearLayout
    private lateinit var layoutLogout: LinearLayout
    private lateinit var layoutDeleteAccount: LinearLayout

    // 🌟 開關元件
    private lateinit var switchNotification: SwitchCompat
    private lateinit var switchPublic: SwitchCompat

    // 對話框元件
    private lateinit var dialogDeleteOverlay: FrameLayout
    private lateinit var btnCancelDelete: TextView
    private lateinit var btnConfirmDelete: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 1. 綁定一般元件
        btnBack = findViewById(R.id.btn_back)
        layoutChangePassword = findViewById(R.id.layout_change_password)
        layoutLogout = findViewById(R.id.layout_logout)
        layoutDeleteAccount = findViewById(R.id.layout_delete_account)

        // 2. 綁定開關元件
        switchNotification = findViewById(R.id.switch_notification)
        switchPublic = findViewById(R.id.switch_public)

        // 3. 綁定刪除對話框元件
        dialogDeleteOverlay = findViewById(R.id.dialog_delete_overlay)
        btnCancelDelete = findViewById(R.id.btn_cancel_delete)
        btnConfirmDelete = findViewById(R.id.btn_confirm_delete)

        // 4. 讀取使用者原本在 Firebase 上的開關設定
        loadUserSettings()

        // 5. 返回上一頁
        btnBack.setOnClickListener {
            finish()
        }

        // 6. 跳轉至修改密碼頁面
        layoutChangePassword.setOnClickListener {
            val intent = Intent(this, change_password::class.java)
            startActivity(intent)
        }

        // 7. 登出帳號
        layoutLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "已登出 e-fit 帳號", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 8. 點擊刪除帳號，顯示專屬對話框
        layoutDeleteAccount.setOnClickListener {
            dialogDeleteOverlay.visibility = View.VISIBLE
        }

        // 9. 取消刪除 (隱藏對話框)
        btnCancelDelete.setOnClickListener {
            dialogDeleteOverlay.visibility = View.GONE
        }

        // 10. 確定刪除 (先刪除發布過的貼文，再刪除帳號)
        btnConfirmDelete.setOnClickListener {
            deleteUserAccountAndPosts()
        }

        // 11. 監聽「推播通知」開關切換
        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            updateUserSetting("notificationsEnabled", isChecked, "已更新推播通知設定")
        }

        // 12. 監聽「公開帳號」開關切換
        switchPublic.setOnCheckedChangeListener { _, isChecked ->
            updateUserSetting("isPublic", isChecked, "已更新公開帳號設定")
        }
    }

    // 從 Firestore 讀取使用者的通知與公開設定
    private fun loadUserSettings() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isNotificationEnabled = document.getBoolean("notificationsEnabled") ?: true
                    val isPublicEnabled = document.getBoolean("isPublic") ?: true

                    switchNotification.isChecked = isNotificationEnabled
                    switchPublic.isChecked = isPublicEnabled
                }
            }
    }

    // 更新單一設定到 Firestore
    private fun updateUserSetting(field: String, value: Boolean, successMessage: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update(field, value)
            .addOnSuccessListener {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "設定儲存失敗，請檢查網路", Toast.LENGTH_SHORT).show()
            }
    }

    // 刪除帳號與關聯貼文的完整邏輯
    private fun deleteUserAccountAndPosts() {
        val user = auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            btnConfirmDelete.text = "資料清除中..."
            btnConfirmDelete.isEnabled = false

            db.collection("AllPosts").whereEqualTo("userEmail", email).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        db.collection("AllPosts").document(document.id).delete()
                    }

                    user.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "帳號與相關貼文已成功刪除", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, login::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "刪除失敗，請重新登入後再試", Toast.LENGTH_LONG).show()
                            dialogDeleteOverlay.visibility = View.GONE
                            btnConfirmDelete.text = "確定刪除"
                            btnConfirmDelete.isEnabled = true
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "清除貼文時發生錯誤，請稍後再試", Toast.LENGTH_SHORT).show()
                    dialogDeleteOverlay.visibility = View.GONE
                    btnConfirmDelete.text = "確定刪除"
                    btnConfirmDelete.isEnabled = true
                }
        } else {
            Toast.makeText(this, "無法取得使用者資訊，請重新登入", Toast.LENGTH_SHORT).show()
        }
    }
}