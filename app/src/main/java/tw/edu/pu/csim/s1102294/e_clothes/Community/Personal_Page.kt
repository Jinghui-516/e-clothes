package tw.edu.pu.csim.s1120336.e_fit.Community

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1120336.e_fit.home
import tw.edu.pu.csim.s1120336.e_fit.Match.*
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.clothes.Wardrobe
import tw.edu.pu.csim.s1120336.e_fit.Setting

class Personal_Page : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var birthdayTextView: TextView
    private lateinit var signatureTextView: TextView
    private lateinit var circularImageView: ShapeableImageView
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvPersonalGrid: RecyclerView
    private lateinit var btnEditTags: ImageView
    private lateinit var btnSettingsPage: ImageView
    private lateinit var ivCamera: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var targetEmail: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uploadProfileImage(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_page)

        targetEmail = intent.getStringExtra("TARGET_EMAIL") ?: auth.currentUser?.email ?: ""

        nameTextView = findViewById(R.id.nameTextView)
        birthdayTextView = findViewById(R.id.birthdayTextView)
        signatureTextView = findViewById(R.id.signatureTextView)
        circularImageView = findViewById(R.id.circularImageView)
        chipGroup = findViewById(R.id.chipGroup)
        rvPersonalGrid = findViewById(R.id.rv_personal_grid)
        btnEditTags = findViewById(R.id.btn_edit_tags)
        btnSettingsPage = findViewById(R.id.btn_settings_page)
        ivCamera = findViewById(R.id.iv_camera)

        rvPersonalGrid.layoutManager = GridLayoutManager(this, 3)

        val isMe = (targetEmail == auth.currentUser?.email)
        if (isMe) {
            ivCamera.visibility = View.VISIBLE
            btnSettingsPage.visibility = View.VISIBLE
            btnSettingsPage.setOnClickListener {
                startActivity(Intent(this, Setting::class.java))
            }

            circularImageView.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                pickImageLauncher.launch(intent)
            }
            nameTextView.setOnClickListener { showEditDialog("使用者名稱", "修改暱稱", nameTextView) }
            signatureTextView.setOnClickListener { showEditDialog("個性簽名", "修改心情語錄", signatureTextView) }
            birthdayTextView.setOnClickListener { showEditDialog("生日", "修改生日", birthdayTextView) }
            btnEditTags.setOnClickListener { startActivity(Intent(this, Edit_Label::class.java)) }
        } else {
            ivCamera.visibility = View.GONE
            btnEditTags.visibility = View.GONE
            btnSettingsPage.visibility = View.GONE

            // 🌟 確保看別人的時候，自介跟名字絕對不能被點擊修改
            nameTextView.isClickable = false
            signatureTextView.isClickable = false
            birthdayTextView.isClickable = false
        }

        fetchUserData(targetEmail)
        loadUserStyles(targetEmail)
        fetchMyHistoryPosts(targetEmail, isMe)

        // 🌟 將 isMe 傳入，讓導覽列知道目前處於誰的主頁
        setupNavigation(isMe)
    }

    private fun fetchMyHistoryPosts(email: String, isMe: Boolean) {
        db.collection("AllPosts")
            .whereEqualTo("userEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(this@Personal_Page, "⚠️ 缺少Firebase排序索引，啟用備用讀取模式！", Toast.LENGTH_LONG).show()

                    db.collection("AllPosts")
                        .whereEqualTo("userEmail", email)
                        .get()
                        .addOnSuccessListener { fallbackSnapshot ->
                            val posts = fallbackSnapshot.documents.mapNotNull { doc ->
                                val urls = doc.get("imageUrls") as? List<*>
                                if (!urls.isNullOrEmpty()) Pair(doc.id, urls[0].toString()) else null
                            }
                            rvPersonalGrid.adapter = PersonalGridAdapter(posts, isMe)
                        }
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    val urls = doc.get("imageUrls") as? List<*>
                    if (!urls.isNullOrEmpty()) Pair(doc.id, urls[0].toString()) else null
                } ?: emptyList()

                rvPersonalGrid.adapter = PersonalGridAdapter(posts, isMe)
            }
    }

    private fun loadUserStyles(email: String) {
        db.collection(email).document("profile").get().addOnSuccessListener { doc ->
            chipGroup.removeAllViews()
            val styles = doc.get("myStyles") as? List<*> ?: return@addOnSuccessListener
            for (style in styles) {
                val name = style.toString().replace("#", "")
                val chip = Chip(this).apply {
                    text = "#$name"
                    setTypeface(null, Typeface.BOLD)
                    chipCornerRadius = dpToPx(20f)
                    chipStrokeWidth = dpToPx(1f)
                    val color = when (name) {
                        "可愛" -> "#FF35B2"
                        "帥氣" -> "#1976D2"
                        "日常" -> "#E65100"
                        else -> "#6A1B9A"
                    }
                    chipStrokeColor = ColorStateList.valueOf(Color.parseColor(color))
                    setTextColor(Color.parseColor(color))
                    setChipBackgroundColorResource(android.R.color.white)
                }
                chipGroup.addView(chip)
            }
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    // 🌟 更新資料時，即時判斷是否為空字串並處理預設提示文字
    private fun showEditDialog(field: String, title: String, target: TextView) {
        var currentText = target.text.toString().replace("@", "")
        if (currentText == "✎ 點擊新增個人簡介..." || currentText == "這人很懶，什麼都沒留...") {
            currentText = ""
        }

        val et = EditText(this).apply { setText(currentText) }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("更新") { _, _ ->
                val v = et.text.toString().trim()
                db.collection(targetEmail).document("個人資料").update(field, v).addOnSuccessListener {
                    if (field == "使用者名稱") {
                        target.text = if (v.isEmpty()) "@未設定" else "@$v"
                    } else if (field == "個性簽名" && v.isEmpty()) {
                        target.text = "✎ 點擊新增個人簡介..."
                    } else {
                        target.text = v
                    }
                }
            }.show()
    }

    private fun uploadProfileImage(uri: Uri) {
        val ref = storage.reference.child("profiles/$targetEmail/avatar.jpg")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                db.collection(targetEmail).document("個人資料").update("頭貼圖片", url.toString()).addOnSuccessListener {
                    Glide.with(this).load(url).into(circularImageView)
                }
            }
        }
    }

    // 🌟 抓取資料時，如果自介是空的，給予相對應的提示
    private fun fetchUserData(email: String) {
        val isMe = (email == auth.currentUser?.email)
        db.collection(email).document("個人資料").get().addOnSuccessListener { doc ->
            nameTextView.text = "@${doc.getString("使用者名稱") ?: "未設定"}"

            val bio = doc.getString("個性簽名")
            if (bio.isNullOrEmpty()) {
                signatureTextView.text = if (isMe) "✎ 點擊新增個人簡介..." else "這人很懶，什麼都沒留..."
            } else {
                signatureTextView.text = bio
            }

            birthdayTextView.text = doc.getString("生日") ?: "未設定"
            doc.getString("頭貼圖片")?.let { Glide.with(this).load(it).into(circularImageView) }
        }
    }

    // 🌟 接收 isMe 參數，設定正確的選取狀態與點擊邏輯
    private fun setupNavigation(isMe: Boolean) {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 看自己主頁時亮「個人」，看好友主頁時亮「社群」
        if (isMe) {
            nav.selectedItemId = R.id.nav_profile
        } else {
            nav.selectedItemId = R.id.nav_community
        }

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> { startActivity(Intent(this, home::class.java)); finish(); true }
                R.id.nav_wardrobe -> { startActivity(Intent(this, Wardrobe::class.java)); finish(); true }
                R.id.nav_rank -> { startActivity(Intent(this, Rank::class.java)); finish(); true }
                R.id.nav_community -> {
                    startActivity(Intent(this, Match_home::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    // 🌟 如果現在是看朋友的主頁，點擊個人按鈕就會「跳回自己的主頁」！
                    if (!isMe) {
                        startActivity(Intent(this, Personal_Page::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }

    inner class PersonalGridAdapter(
        private val posts: List<Pair<String, String>>,
        private val isMe: Boolean
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_personal_grid, parent, false)) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            val docId = posts[pos].first
            val imageUrl = posts[pos].second

            Glide.with(holder.itemView.context).load(imageUrl).into(holder.itemView.findViewById(R.id.iv_grid_image))

            if (isMe) {
                holder.itemView.setOnLongClickListener {
                    AlertDialog.Builder(this@Personal_Page)
                        .setTitle("刪除貼文")
                        .setMessage("確定要刪除這篇歷史穿搭嗎？刪除後無法恢復喔！")
                        .setPositiveButton("確定刪除") { _, _ ->
                            db.collection("AllPosts").document(docId).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this@Personal_Page, "貼文已刪除！", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@Personal_Page, "刪除失敗，請稍後再試", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .setNegativeButton("先留著", null)
                        .show()
                    true
                }
            } else {
                holder.itemView.setOnLongClickListener(null)
            }
        }

        override fun getItemCount() = posts.size
    }
}