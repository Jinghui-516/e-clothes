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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // 🌟 新增 Firestore 資料庫的匯入

class Setting : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var layoutChangePassword: LinearLayout
    private lateinit var layoutLogout: LinearLayout
    private lateinit var layoutDeleteAccount: LinearLayout

    // 對話框元件
    private lateinit var dialogDeleteOverlay: FrameLayout
    private lateinit var btnCancelDelete: TextView
    private lateinit var btnConfirmDelete: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 綁定元件
        btnBack = findViewById(R.id.btn_back)
        layoutChangePassword = findViewById(R.id.layout_change_password)
        layoutLogout = findViewById(R.id.layout_logout)
        layoutDeleteAccount = findViewById(R.id.layout_delete_account)

        dialogDeleteOverlay = findViewById(R.id.dialog_delete_overlay)
        btnCancelDelete = findViewById(R.id.btn_cancel_delete)
        btnConfirmDelete = findViewById(R.id.btn_confirm_delete)

        // 1. 返回上一頁
        btnBack.setOnClickListener {
            finish()
        }

        // 2. 跳轉至修改密碼頁面
        layoutChangePassword.setOnClickListener {
            val intent = Intent(this, change_password::class.java)
            startActivity(intent)
        }

        // 3. 登出帳號
        layoutLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "已登出 e-fit 帳號", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login::class.java)
            // 清除之前的 Activity 堆疊，避免按返回鍵又回到登入狀態
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 4. 點擊刪除帳號，顯示專屬對話框
        layoutDeleteAccount.setOnClickListener {
            dialogDeleteOverlay.visibility = View.VISIBLE
        }

        // 5. 取消刪除 (隱藏對話框)
        btnCancelDelete.setOnClickListener {
            dialogDeleteOverlay.visibility = View.GONE
        }

        // 6. 確定刪除 (🌟 升級版：先刪除發布過的貼文，再刪除帳號)
        btnConfirmDelete.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email
            val db = FirebaseFirestore.getInstance()

            if (user != null && email != null) {
                // 將按鈕文字改成提示狀態，並暫時停用按鈕，避免使用者狂按
                btnConfirmDelete.text = "資料清除中..."
                btnConfirmDelete.isEnabled = false

                // 步驟一：先去 AllPosts 集合裡，找出所有 userEmail 是自己的貼文
                db.collection("AllPosts").whereEqualTo("userEmail", email).get()
                    .addOnSuccessListener { documents ->
                        // 迴圈把找到的貼文一篇一篇刪掉
                        for (document in documents) {
                            db.collection("AllPosts").document(document.id).delete()
                        }

                        // 步驟二：貼文刪乾淨後，正式刪除 Firebase Auth 帳號
                        user.delete().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "帳號與相關貼文已成功刪除", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, login::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                // 若距離上次登入太久，Firebase 可能會要求重新驗證
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
}