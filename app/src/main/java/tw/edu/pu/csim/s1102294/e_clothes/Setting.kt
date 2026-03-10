package tw.edu.pu.csim.s1102294.e_clothes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import tw.edu.pu.csim.s1102294.e_clothes.Community.Liked_Post
//import tw.edu.pu.csim.s1102294.e_clothes.Community.Liked_Post
import tw.edu.pu.csim.s1102294.e_clothes.Match.edit_Chosen_Match
import tw.edu.pu.csim.s1102294.e_clothes.Match.edit_Profile
import tw.edu.pu.csim.s1102294.e_clothes.Match.share_Match

class Setting : AppCompatActivity() {

    lateinit var btn_Delete: Button
    lateinit var Sign_out: Button
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)


        btn_Delete = findViewById(R.id.btn_Delete)

        val menu = findViewById<ImageView>(R.id.menu)
        menu.setOnClickListener {
            val popup = PopupMenu(this, menu)
            popup.menuInflater.inflate(R.menu.menu_share, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        Toast.makeText(this, "編輯個人資料", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, edit_Profile::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.check -> {
                        Toast.makeText(this, "編輯精選穿搭", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, edit_Chosen_Match::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.share -> {
                        Toast.makeText(this, "分享搭配", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, share_Match::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.like -> {
                        Toast.makeText(this, "喜歡的貼文", Toast.LENGTH_SHORT).show()
                        val intent3 = Intent(this, Liked_Post::class.java)
                        startActivity(intent3)
                        finish()
                        true
                    }
                    R.id.settings -> {
                        Toast.makeText(this, "設定", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, Setting::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }


        firebaseAuth = FirebaseAuth.getInstance()

        Sign_out = findViewById(R.id.Sign_out)
        Sign_out.setOnClickListener {
            logoutUser()
        }

        btn_Delete.setOnClickListener {
            AlertDialog.Builder(this@Setting)
                .setTitle("title")
                .setMessage("確定刪掉本帳號嗎?")
                .setPositiveButton("否") { dialog, _ ->
                    // 可以在这里处理确定按钮的点击事件
                    dialog.dismiss()
                }
                .setNegativeButton("是") { dialog, _ ->
                    // 可以在这里处理取消按钮的点击事件
                    AlertDialog.Builder(this)
                        .setTitle("title")
                        .setMessage("刪除後將無法復原!!")
                        .setPositiveButton("否") { dialog2, _ ->

                            dialog2.dismiss()
                        }
                        .setNegativeButton("是") { dialog2, _ ->
                            dialog2.dismiss()
                            deleteAccount()
                            val intent = Intent(this, login::class.java) // 更換為您的目標Activity
                            startActivity(intent)
                        }
                        .show()
                    dialog.dismiss()
                }
                .show()
        }
    }
    private fun logoutUser() {
        // 使用 Firebase 身分驗證進行登出
        firebaseAuth.signOut()
        // 轉到 Login
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser

        user?.let {
            // 刪除帳號
            it.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "帳號已刪除", Toast.LENGTH_SHORT).show()
                    Log.d("DeleteAccount", "User account deleted.")
                    // 帳號刪除後，可以重定向到登入頁面或其他頁面
                } else {
                    // 如果需要重新驗證
                    Toast.makeText(this, "帳號刪除失敗，可能需要重新驗證", Toast.LENGTH_SHORT).show()
                    Log.e("DeleteAccount", "Failed to delete user", task.exception)
                }
            }
        } ?: run {
            // 如果當前沒有使用者登入
            Toast.makeText(this, "尚未登入", Toast.LENGTH_SHORT).show()
        }
    }
}