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
import android.view.inputmethod.EditorInfo
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

        val imageUriString = intent.getStringExtra("selectedImageUri")
        val imageBitmap = intent.getParcelableExtra<Bitmap>("capturedPhoto")
        portfolioImageUrl = intent.getStringExtra("portfolioImageUrl")

        val passedCategory = intent.getStringExtra("category")
        selectedColor = intent.getStringExtra("color") ?: "未指定"

        classificationTextView.text = passedCategory ?: "未分類"
        when {
            portfolioImageUrl != null -> Glide.with(this).load(portfolioImageUrl).into(clothesImageView)
            imageUriString != null -> clothesImageView.setImageURI(Uri.parse(imageUriString))
            imageBitmap != null -> clothesImageView.setImageBitmap(imageBitmap)
        }

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_wardrobe
        bottomNav.setOnItemSelectedListener { true }

        findViewById<Button>(R.id.handsome).setOnClickListener { addTagDirectly("帥氣") }
        findViewById<Button>(R.id.cute).setOnClickListener { addTagDirectly("可愛") }
        findViewById<Button>(R.id.daily).setOnClickListener { addTagDirectly("日常") }
        findViewById<Button>(R.id.easy).setOnClickListener { addTagDirectly("輕鬆") }
        findViewById<Button>(R.id.formal).setOnClickListener { addTagDirectly("正式") }

        val rv = findViewById<RecyclerView>(R.id.labelRecyclerView)
        adapter = LabelAdapter(labelList) { showDeleteDialog(it) }
        rv.layoutManager = GridLayoutManager(this, 4)
        rv.adapter = adapter

        // 🌟 貼心功能：如果使用者按手機鍵盤的「完成(Enter)」，也自動把字變成標籤 (方便一次打多個)
        labelInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = labelInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    addTagDirectly(text)
                    labelInput.text.clear()
                }
                true
            } else {
                false
            }
        }

        previousBtn.setOnClickListener { finish() }

        finishBtn.setOnClickListener {
            // 🌟 關鍵邏輯：在按下右上角儲存前，檢查輸入框裡是不是還有字沒送出
            // 如果有，就當作他最後輸入的一個標籤，自動幫他加進去！
            val pendingText = labelInput.text.toString().trim()
            if (pendingText.isNotEmpty()) {
                addTagDirectly(pendingText)
                labelInput.text.clear()
            }

            // 然後繼續執行儲存
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

        val selectedScenarios = mutableListOf<String>()
        if (findViewById<CheckBox>(R.id.cb_work).isChecked) selectedScenarios.add("職場商務")
        if (findViewById<CheckBox>(R.id.cb_school).isChecked) selectedScenarios.add("校園日常")
        if (findViewById<CheckBox>(R.id.cb_casual).isChecked) selectedScenarios.add("休閒娛樂")
        if (findViewById<CheckBox>(R.id.cb_sport).isChecked) selectedScenarios.add("戶外運動")
        if (findViewById<CheckBox>(R.id.cb_special).isChecked) selectedScenarios.add("特殊場合")

        val seasonTag = when (findViewById<RadioGroup>(R.id.rg_season).checkedRadioButtonId) {
            R.id.rb_hot -> "炎熱(薄)"
            R.id.rb_warm -> "涼爽(適中)"
            R.id.rb_cold -> "寒冷(厚)"
            else -> "涼爽(適中)"
        }

        val data = hashMapOf(
            "服裝種類" to category,
            "顏色" to selectedColor,
            "圖片網址" to imageUrl,
            "圖片完整網址" to fullUrl,
            "標籤" to labelTexts,
            "適合情境" to selectedScenarios,
            "適合氣溫" to seasonTag,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection(email).add(data).addOnSuccessListener {
            Toast.makeText(this, "新增完成！", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Wardrobe::class.java))
            finish()
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "存入失敗", e)
            Toast.makeText(this, "資料存入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTagDirectly(text: String) {
        if (!labelList.contains(text)) {
            labelList.add(text)
            labelTexts.add(text)
            adapter.notifyItemInserted(labelList.size - 1)
        }
    }

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