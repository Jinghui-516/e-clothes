package tw.edu.pu.csim.s1120336.e_fit.Community

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1120336.e_fit.Match.Edit_Label
import tw.edu.pu.csim.s1120336.e_fit.Match.Match_home
import tw.edu.pu.csim.s1120336.e_fit.Match.Rank
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.clothes.Wardrobe
import tw.edu.pu.csim.s1120336.e_fit.home
import java.util.*

class Personal_Page : AppCompatActivity() {

    lateinit var nameTextView: TextView
    lateinit var birthdayTextView: TextView
    lateinit var signatureTextView: TextView
    lateinit var circularImageView: ShapeableImageView
    lateinit var chipGroup: ChipGroup

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // 🌟 用於挑選圖片的 Launchers
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                uploadProfileImage(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_page)

        // 1. 綁定 UI
        nameTextView = findViewById(R.id.nameTextView)
        birthdayTextView = findViewById(R.id.birthdayTextView)
        signatureTextView = findViewById(R.id.signatureTextView)
        circularImageView = findViewById(R.id.circularImageView)
        chipGroup = findViewById(R.id.chipGroup)
        val btnEditTags: ImageView = findViewById(R.id.btn_edit_tags)

        // 2. 設置點擊監聽 (點哪裡改哪裡)
        circularImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        nameTextView.setOnClickListener { showEditDialog("使用者名稱", "修改暱稱", nameTextView) }
        signatureTextView.setOnClickListener { showEditDialog("個性簽名", "修改心情語錄", signatureTextView) }
        birthdayTextView.setOnClickListener { showEditDialog("生日", "修改生日與性別", birthdayTextView) }

        // 🌟 完美接通風格選擇視窗：點擊小畫筆跳轉至編輯頁面
        btnEditTags.setOnClickListener {
            val intent = Intent(this, Edit_Label::class.java)
            startActivity(intent)
        }

        // 3. 初始讀取個人資料與風格標籤
        fetchUserData()
        loadUserStyles()

        // 4. 底部導覽列 (維持之前的邏輯)
        setupNavigation()
    }

    // 🌟 當使用者從編輯標籤頁面設定完返回時，自動重新載入最新標籤，無縫刷新畫面
    override fun onResume() {
        super.onResume()
        loadUserStyles()
    }

    // 🌟 通用編輯對話框
    private fun showEditDialog(field: String, title: String, targetView: TextView) {
        val editText = EditText(this)
        editText.setText(targetView.text.toString().replace("@", ""))

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("更新") { _, _ ->
                val newValue = editText.text.toString()
                updateFirestoreField(field, newValue) {
                    if (field == "使用者名稱") targetView.text = "@$newValue"
                    else targetView.text = newValue
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 🌟 上傳圖片到 Storage 並更新資料庫
    private fun uploadProfileImage(uri: Uri) {
        val email = auth.currentUser?.email ?: return
        val ref = storage.reference.child("profiles/$email/avatar.jpg")

        Toast.makeText(this, "正在上傳頭貼...", Toast.LENGTH_SHORT).show()

        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                updateFirestoreField("頭貼圖片", downloadUrl.toString()) {
                    Glide.with(this).load(downloadUrl).into(circularImageView)
                    Toast.makeText(this, "頭貼更新成功", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFirestoreField(field: String, value: String, onSuccess: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        db.collection(email).document("個人資料")
            .update(field, value)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { Toast.makeText(this, "更新失敗", Toast.LENGTH_SHORT).show() }
    }

    private fun fetchUserData() {
        val email = auth.currentUser?.email ?: return
        db.collection(email).document("個人資料").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                nameTextView.text = "@${doc.getString("使用者名稱") ?: "未設定"}"
                signatureTextView.text = doc.getString("個性簽名") ?: "點擊編輯簽名..."
                birthdayTextView.text = doc.getString("生日") ?: "點擊編輯生日..."

                val imgUrl = doc.getString("頭貼圖片")
                if (!imgUrl.isNullOrEmpty()) Glide.with(this).load(imgUrl).placeholder(R.drawable.user).into(circularImageView)
            }
        }
    }

    /**
     * 🌟 核心整合：從 Firestore 雲端資料庫的 profile 文件讀取風格標籤，並動態轉成圓角 Chip 放進 ChipGroup
     */
    private fun loadUserStyles() {
        val email = auth.currentUser?.email ?: return

        db.collection(email).document("profile").get()
            .addOnSuccessListener { document ->
                // 每次更新前先把舊的標籤清除，避免重複疊加框框
                chipGroup.removeAllViews()

                if (document != null && document.exists()) {
                    // 抓出儲存的風格字串陣列
                    val savedStyles = document.get("myStyles") as? List<*>

                    if (savedStyles != null && savedStyles.isNotEmpty()) {
                        for (style in savedStyles) {
                            val styleName = style.toString()

                            // 動態建立一個符合 Material 規範的圓角展示用 Chip
                            val chip = Chip(this).apply {
                                text = styleName
                                isClickable = false   // 主頁面只負責展示，不可重複點擊選取
                                isCheckable = false
                                setTextColor(Color.parseColor("#4A3E3D")) // 質感深咖啡色文字
                                setChipBackgroundColorResource(android.R.color.white) // 白色圓角背景
                            }

                            // 把做好的風格標籤塞進 chipGroup 容器
                            chipGroup.addView(chip)
                        }
                    } else {
                        showEmptyTagPrompt()
                    }
                } else {
                    showEmptyTagPrompt()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Personal_Page", "讀取風格標籤失敗: ${e.message}")
            }
    }

    /**
     * 🌟 沒有風格標籤時的防呆提示
     */
    private fun showEmptyTagPrompt() {
        chipGroup.removeAllViews()
        val emptyChip = Chip(this).apply {
            text = "尚未設定風格標籤，點擊右方編輯 📝"
            isClickable = false
            isCheckable = false
            setTextColor(Color.GRAY)
        }
        chipGroup.addView(emptyChip)
    }

    private fun setupNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = R.id.nav_profile
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> { startActivity(Intent(this, home::class.java)); finish(); true }
                R.id.nav_wardrobe -> { startActivity(Intent(this, Wardrobe::class.java)); finish(); true }
                R.id.nav_rank -> { startActivity(Intent(this, Rank::class.java)); finish(); true }
                R.id.nav_community -> { startActivity(Intent(this, Match_home::class.java)); finish(); true }
                else -> true
            }
        }
    }
}