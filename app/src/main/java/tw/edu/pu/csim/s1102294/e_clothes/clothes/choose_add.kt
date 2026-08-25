package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import tw.edu.pu.csim.s1120336.e_fit.Community.Friends
import tw.edu.pu.csim.s1120336.e_fit.Match.Match_home
import tw.edu.pu.csim.s1120336.e_fit.Match.Rank
import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.home

class choose_add : AppCompatActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerColor: Spinner
    lateinit var bottomNavigationView: BottomNavigationView

    // 相機拍照結果處理 (保留妳的原本邏輯)
    private val takePictureResult =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val intent = Intent(this, New_clothes::class.java).apply {
                    putExtra("capturedPhoto", bitmap)
                    putExtra("category", spinnerCategory.selectedItem.toString())
                    putExtra("color", spinnerColor.selectedItem.toString())
                }
                startActivity(intent)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_add)

        // 綁定選單
        spinnerCategory = findViewById(R.id.spinner_category)
        spinnerColor = findViewById(R.id.spinner_color)

        // 底部導覽列
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> { startActivity(Intent(this, home::class.java)); finish(); true }
                R.id.nav_wardrobe -> { startActivity(Intent(this, Wardrobe::class.java)); finish(); true }
                R.id.nav_community -> { startActivity(Intent(this, Match_home::class.java)); finish(); true }
                R.id.nav_rank -> { startActivity(Intent(this, Rank::class.java)); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, Personal_Page::class.java)); finish(); true }
                else -> false
            }
        }

        // 拍照按鈕
        findViewById<Button>(R.id.camera_btn).setOnClickListener {
            if (isSelectionValid()) checkPermission()
        }

        // 🌟 修改：從相簿挑選按鈕，改為呼叫「彈出作品集視窗」
        findViewById<Button>(R.id.photo_btn).setOnClickListener {
            if (isSelectionValid()) {
                showPortfolioDialog()
            }
        }
    }

    private fun isSelectionValid(): Boolean {
        val category = spinnerCategory.selectedItem.toString()
        val color = spinnerColor.selectedItem.toString()
        if (category == "請選擇種類" || color == "請選擇顏色") {
            Toast.makeText(this, "請先選擇服裝種類與顏色唷！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) takePictureResult.launch(null)
            }.launch(android.Manifest.permission.CAMERA)
        } else {
            takePictureResult.launch(null)
        }
    }

    // 🌟 新增：原地彈出「去背作品集」視窗的邏輯
    private fun showPortfolioDialog() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val dialog = android.app.Dialog(this)

        // 用純程式碼建構畫面，完全不干擾妳現有的 XML
        val rootLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 50, 40, 50)
            setBackgroundColor(android.graphics.Color.parseColor("#EBE6E2"))
        }

        val titleTv = android.widget.TextView(this).apply {
            text = "請選擇作品集照片"
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#5D4037"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        rootLayout.addView(titleTv)

        val recyclerView = androidx.recyclerview.widget.RecyclerView(this).apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@choose_add, 3)
        }
        rootLayout.addView(recyclerView)
        dialog.setContentView(rootLayout)
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        // 從 Firebase 抓取去背相簿的照片
        FirebaseFirestore.getInstance().collection(email).document("去背相簿").collection("images")
            .get()
            .addOnSuccessListener { documents ->
                val urls = mutableListOf<String>()
                for (doc in documents) {
                    doc.getString("imageUrl")?.let { urls.add(it) }
                }

                if (urls.isEmpty()) {
                    Toast.makeText(this, "作品集裡目前沒有照片唷！", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@addOnSuccessListener
                }

                // 🌟 點擊圖片後：關閉視窗，帶著雲端網址跳去 New_clothes！
                recyclerView.adapter = DialogAlbumAdapter(urls) { selectedUrl ->
                    dialog.dismiss()
                    val intent = Intent(this, New_clothes::class.java).apply {
                        putExtra("portfolioImageUrl", selectedUrl) // 傳遞雲端網址
                        putExtra("category", spinnerCategory.selectedItem.toString())
                        putExtra("color", spinnerColor.selectedItem.toString())
                    }
                    startActivity(intent)
                    finish()
                }
                dialog.show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "讀取作品集失敗", Toast.LENGTH_SHORT).show()
            }
    }

    // 視窗專用的圖片轉接器
    inner class DialogAlbumAdapter(
        private val urls: List<String>,
        private val onSelected: (String) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<DialogAlbumAdapter.ViewHolder>() {

        inner class ViewHolder(val iv: ImageView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(iv)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val size = (100 * parent.context.resources.displayMetrics.density).toInt()
            val imageView = ImageView(parent.context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, size)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(8, 8, 8, 8)
                setBackgroundColor(android.graphics.Color.parseColor("#D1CFCF"))
            }
            return ViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val url = urls[position]
            Glide.with(holder.itemView.context).load(url).into(holder.iv)
            holder.iv.setOnClickListener { onSelected(url) }
        }

        override fun getItemCount() = urls.size
    }
}