package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1120336.e_fit.R
import java.io.File
import java.io.FileOutputStream
import java.util.*

class New_clothes : AppCompatActivity() {

    // --- 內部類別與變數 ---
    class LabelAdapter(private val labels: MutableList<String>, private val onLabelLongPress: (Int) -> Unit) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {
        class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_label, parent, false)
            return LabelViewHolder(view)
        }
        override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
            holder.textView.text = labels[position]
            holder.itemView.setOnLongClickListener { onLabelLongPress(position); true }
        }
        override fun getItemCount(): Int = labels.size
    }

    private val labelList = mutableListOf<String>()
    private val labelTexts = mutableListOf<String>()
    private lateinit var adapter: LabelAdapter
    private lateinit var firebaseHelper: FirebaseHelper
    private var selectedColor: String = "未指定"
    private var imageUrl: String? = null

    // 🌟 接收作品集網址的變數
    private var portfolioImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_clothes)

        val db = FirebaseFirestore.getInstance()
        firebaseHelper = FirebaseHelper()

        val clothesImageView: ImageView = findViewById(R.id.clothes)
        val classificationTextView: TextView = findViewById(R.id.Classification_name)
        val labelInput: EditText = findViewById(R.id.label)
        val finishBtn: ImageView = findViewById(R.id.finish)
        val previousBtn: ImageView = findViewById(R.id.previous)

        // 2. 接收前一頁資料 (包含從作品集傳來的雲端網址)
        val imageUriString = intent.getStringExtra("selectedImageUri")
        val imageBitmap = intent.getParcelableExtra<Bitmap>("capturedPhoto")
        portfolioImageUrl = intent.getStringExtra("portfolioImageUrl") // 🌟 接收作品集網址

        val passedCategory = intent.getStringExtra("category")
        selectedColor = intent.getStringExtra("color") ?: "未指定"

        // 3. 設定畫面初始值
        classificationTextView.text = passedCategory ?: "未分類"
        when {
            // 🌟 如果是作品集來的，直接用 Glide 載入雲端圖片顯示在畫面上！
            portfolioImageUrl != null -> Glide.with(this).load(portfolioImageUrl).into(clothesImageView)
            imageUriString != null -> clothesImageView.setImageURI(Uri.parse(imageUriString))
            imageBitmap != null -> clothesImageView.setImageBitmap(imageBitmap)
        }

        // 4. 底部導覽列設定
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_wardrobe
        bottomNav.setOnItemSelectedListener { item -> true }

        // 5. 標籤快捷鍵
        findViewById<Button>(R.id.handsome).setOnClickListener { labelInput.setText("帥氣") }
        findViewById<Button>(R.id.cute).setOnClickListener { labelInput.setText("可愛") }
        findViewById<Button>(R.id.daily).setOnClickListener { labelInput.setText("日常") }

        // 6. 標籤 RecyclerView
        val rv = findViewById<RecyclerView>(R.id.labelRecyclerView)
        adapter = LabelAdapter(labelList) { showDeleteDialog(it) }
        rv.layoutManager = GridLayoutManager(this, 4)
        rv.adapter = adapter

        findViewById<ImageView>(R.id.add_label).setOnClickListener {
            if (labelInput.text.isNotEmpty()) {
                val text = labelInput.text.toString()
                labelList.add(text)
                labelTexts.add(text)
                adapter.notifyItemInserted(labelList.size - 1)
                labelInput.text.clear()
            }
        }

        // 7. 退出與完成
        previousBtn.setOnClickListener { finish() }

        finishBtn.setOnClickListener {
            // 🌟 判斷：如果是作品集照片，直接秒寫入資料庫；否則執行傳統的 Bitmap 上傳
            if (portfolioImageUrl != null) {
                imageUrl = portfolioImageUrl
                saveToFirestore(db, portfolioImageUrl!!)
            } else {
                uploadProcess(db, clothesImageView)
            }
        }
    }

    private fun uploadProcess(db: FirebaseFirestore, imageView: ImageView) {
        val bitmap = (imageView.drawable as? BitmapDrawable)?.bitmap ?: return
        val uri = getImageUriFromBitmap(this, bitmap) ?: return

        firebaseHelper.uploadImage(this, uri, onSuccess = { fullUrl ->
            val relativePath = "/" + fullUrl.substringAfter("/o/").substringBefore("?alt=media").replace("%2F", "/")
            imageUrl = relativePath
            saveToFirestore(db, fullUrl)
        }, onFailure = {
            Toast.makeText(this, "上傳失敗", Toast.LENGTH_SHORT).show()
        })
    }

    private fun saveToFirestore(db: FirebaseFirestore, fullUrl: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val category = findViewById<TextView>(R.id.Classification_name).text.toString()

        val data = hashMapOf(
            "服裝種類" to category,
            "顏色" to selectedColor,
            "圖片網址" to imageUrl,
            "圖片完整網址" to fullUrl,
            "標籤" to labelTexts,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection(email).add(data).addOnSuccessListener {
            Toast.makeText(this, "新增完成！", Toast.LENGTH_SHORT).show()
            // 🌟 已經幫妳改成跳轉到 Wardrobe (衣櫃) 了！
            startActivity(Intent(this, Wardrobe::class.java))
            finish()
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "存入失敗", e)
            Toast.makeText(this, "資料存入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 輔助方法 ---
    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri? {
        val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
        return try {
            val os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            Uri.fromFile(file)
        } catch (e: Exception) { null }
    }

    private fun showDeleteDialog(pos: Int) {
        AlertDialog.Builder(this).setTitle("刪除標籤").setMessage("確定刪除？")
            .setPositiveButton("是") { _, _ -> labelList.removeAt(pos); labelTexts.removeAt(pos); adapter.notifyItemRemoved(pos) }
            .setNegativeButton("否", null).show()
    }
}