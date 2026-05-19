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
import tw.edu.pu.csim.s1120336.e_fit.Match.*
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.clothes.Wardrobe
import tw.edu.pu.csim.s1120336.e_fit.home

class Personal_Page : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var birthdayTextView: TextView
    private lateinit var signatureTextView: TextView
    private lateinit var circularImageView: ShapeableImageView
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvPersonalGrid: RecyclerView
    private lateinit var btnEditTags: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // 🌟 核心變數：儲存當前查看的 Email (預設為自己)
    private var targetEmail: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uploadProfileImage(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_page)

        // 接收外部傳來的 Email，若沒傳則預設為自己
        targetEmail = intent.getStringExtra("TARGET_EMAIL") ?: auth.currentUser?.email ?: ""

        nameTextView = findViewById(R.id.nameTextView)
        birthdayTextView = findViewById(R.id.birthdayTextView)
        signatureTextView = findViewById(R.id.signatureTextView)
        circularImageView = findViewById(R.id.circularImageView)
        chipGroup = findViewById(R.id.chipGroup)
        rvPersonalGrid = findViewById(R.id.rv_personal_grid)
        btnEditTags = findViewById(R.id.btn_edit_tags)
        rvPersonalGrid.layoutManager = GridLayoutManager(this, 3)

        // 🌟 權限控制：只有查看自己頁面時才允許編輯
        val isMe = (targetEmail == auth.currentUser?.email)
        if (isMe) {
            circularImageView.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                pickImageLauncher.launch(intent)
            }
            nameTextView.setOnClickListener { showEditDialog("使用者名稱", "修改暱稱", nameTextView) }
            signatureTextView.setOnClickListener { showEditDialog("個性簽名", "修改心情語錄", signatureTextView) }
            birthdayTextView.setOnClickListener { showEditDialog("生日", "修改生日", birthdayTextView) }
            btnEditTags.setOnClickListener { startActivity(Intent(this, Edit_Label::class.java)) }
        } else {
            // 查看他人檔案時，隱藏編輯按鈕
            btnEditTags.visibility = View.GONE
        }

        fetchUserData(targetEmail)
        loadUserStyles(targetEmail)
        fetchMyHistoryPosts(targetEmail)
        setupNavigation()
    }

    private fun fetchMyHistoryPosts(email: String) {
        db.collection("AllPosts").whereEqualTo("userEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val images = snapshot?.documents?.mapNotNull { it.get("imageUrls") as? List<*> }
                    ?.filter { it.isNotEmpty() }?.map { it[0].toString() } ?: emptyList()
                rvPersonalGrid.adapter = PersonalGridAdapter(images)
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

    private fun showEditDialog(field: String, title: String, target: TextView) {
        val et = EditText(this).apply { setText(target.text.toString().replace("@", "")) }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("更新") { _, _ ->
                val v = et.text.toString()
                db.collection(targetEmail).document("個人資料").update(field, v).addOnSuccessListener {
                    target.text = if (field == "使用者名稱") "@$v" else v
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

    private fun fetchUserData(email: String) {
        db.collection(email).document("個人資料").get().addOnSuccessListener { doc ->
            nameTextView.text = "@${doc.getString("使用者名稱") ?: "未設定"}"
            signatureTextView.text = doc.getString("個性簽名") ?: "這人很懶，什麼都沒留..."
            birthdayTextView.text = doc.getString("生日") ?: "未設定"
            doc.getString("頭貼圖片")?.let { Glide.with(this).load(it).into(circularImageView) }
        }
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
                else -> false
            }
        }
    }

    inner class PersonalGridAdapter(private val images: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_personal_grid, parent, false)) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            Glide.with(holder.itemView.context).load(images[pos]).into(holder.itemView.findViewById(R.id.iv_grid_image))
        }
        override fun getItemCount() = images.size
    }
}