package tw.edu.pu.csim.s1120336.e_fit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class FittingRoomActivity : AppCompatActivity() {

    private lateinit var rvHats: RecyclerView
    private lateinit var rvTops: RecyclerView
    private lateinit var rvBottoms: RecyclerView
    private lateinit var rvShoes: RecyclerView
    private lateinit var outfitCaptureArea: ConstraintLayout
    private lateinit var btnSaveOutfit: FloatingActionButton

    data class ClothesItem(val type: String, val url: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fitting_room)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        rvHats = findViewById(R.id.rv_hats)
        rvTops = findViewById(R.id.rv_tops)
        rvBottoms = findViewById(R.id.rv_bottoms)
        rvShoes = findViewById(R.id.rv_shoes)

        outfitCaptureArea = findViewById(R.id.outfit_capture_area)
        btnSaveOutfit = findViewById(R.id.btn_save_outfit)

        setupHorizontalRecyclerView(rvHats)
        setupHorizontalRecyclerView(rvTops)
        setupHorizontalRecyclerView(rvBottoms)
        setupHorizontalRecyclerView(rvShoes)

        loadClothesData()

        btnSaveOutfit.setOnClickListener {
            showSaveOutfitDialog()
        }
    }

    private fun showSaveOutfitDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val spinner = Spinner(this)
        val categories = arrayOf("日常", "約會", "上班", "校園", "自訂義類別...")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter
        layout.addView(spinner)

        val customInput = EditText(this).apply {
            hint = "請輸入自訂類別名稱"
            visibility = View.GONE
        }
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 20, 0, 0)
        customInput.layoutParams = layoutParams
        layout.addView(customInput)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == categories.size - 1) customInput.visibility = View.VISIBLE
                else customInput.visibility = View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("儲存穿搭")
            .setMessage("請選擇要儲存到哪個搭配類別：")
            .setView(layout)
            .setPositiveButton("儲存") { _, _ ->
                val selectedCategory = if (spinner.selectedItemPosition == categories.size - 1) {
                    customInput.text.toString().ifEmpty { "未分類" }
                } else {
                    spinner.selectedItem.toString()
                }

                val rawBitmap = captureViewToBitmap(outfitCaptureArea)
                val croppedBitmap = cropCenterBitmap(rawBitmap)
                uploadOutfitToFirebase(croppedBitmap, selectedCategory)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun captureViewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun cropCenterBitmap(original: Bitmap): Bitmap {
        val keepRatio = 0.5f
        val newWidth = (original.width * keepRatio).toInt()
        val startX = (original.width - newWidth) / 2
        return Bitmap.createBitmap(original, startX, 0, newWidth, original.height)
    }

    private fun uploadOutfitToFirebase(bitmap: Bitmap, category: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val timestamp = System.currentTimeMillis()
        val storageRef = FirebaseStorage.getInstance().reference.child("outfits/$email/$timestamp.png")

        Toast.makeText(this, "正在產生穿搭照並上傳...", Toast.LENGTH_SHORT).show()
        btnSaveOutfit.isEnabled = false

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val db = FirebaseFirestore.getInstance()
                val outfitData = hashMapOf(
                    "imageUrl" to uri.toString(),
                    "category" to category,
                    "timestamp" to timestamp
                )

                db.collection(email).document("我的搭配").collection("items").document(timestamp.toString())
                    .set(outfitData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "成功儲存至「$category」！", Toast.LENGTH_LONG).show()
                        btnSaveOutfit.isEnabled = true
                    }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "上傳失敗: ${exception.message}", Toast.LENGTH_LONG).show()
            btnSaveOutfit.isEnabled = true
        }
    }

    private fun setupHorizontalRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadClothesData() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection(email).get().addOnSuccessListener { documents ->
            val hatList = mutableListOf(ClothesItem("Empty", ""))
            val topList = mutableListOf(ClothesItem("Empty", ""))
            val bottomList = mutableListOf(ClothesItem("Empty", ""))
            val shoeList = mutableListOf(ClothesItem("Empty", ""))

            for (document in documents) {
                val type = document.getString("服裝種類") ?: ""
                val url = document.getString("圖片網址") ?: ""

                if (url.isNotEmpty()) {
                    val item = ClothesItem(type, url)
                    when (type) {
                        "頭飾", "帽子" -> hatList.add(item)
                        "上衣", "洋裝", "外套" -> topList.add(item) // 🌟 確保外套被分發到上衣軌道
                        "褲子", "裙子" -> bottomList.add(item)
                        "鞋子" -> shoeList.add(item)
                    }
                }
            }

            val density = resources.displayMetrics.density

            rvHats.adapter = HorizontalClothesAdapter(hatList, (75 * density).toInt(), (75 * density).toInt())
            rvTops.adapter = HorizontalClothesAdapter(topList, (165 * density).toInt(), (175 * density).toInt())
            rvBottoms.adapter = HorizontalClothesAdapter(bottomList, (155 * density).toInt(), (230 * density).toInt())
            rvShoes.adapter = HorizontalClothesAdapter(shoeList, (140 * density).toInt(), (110 * density).toInt())
        }
    }

    inner class HorizontalClothesAdapter(
        private val items: List<ClothesItem>,
        private val defaultWidth: Int,
        private val defaultHeight: Int
    ) : RecyclerView.Adapter<HorizontalClothesAdapter.ViewHolder>() {

        inner class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(defaultWidth, defaultHeight)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
            return ViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val density = holder.imageView.context.resources.displayMetrics.density
            val layoutParams = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams

            // 🌟 加入外套的專屬尺寸判斷
            when (item.type) {
                "洋裝" -> {
                    layoutParams.width = (165 * density).toInt()
                    layoutParams.height = (330 * density).toInt()
                    layoutParams.setMargins((40 * density).toInt(), 0, (40 * density).toInt(), 0)
                }
                "上衣" -> {
                    layoutParams.width = (165 * density).toInt()
                    layoutParams.height = (175 * density).toInt()
                    layoutParams.setMargins((40 * density).toInt(), 0, (40 * density).toInt(), 0)
                }
                "外套" -> {
                    layoutParams.width = (175 * density).toInt() // 外套稍微寬一點
                    layoutParams.height = (180 * density).toInt()
                    layoutParams.setMargins((40 * density).toInt(), 0, (40 * density).toInt(), 0)
                }
                "裙子" -> {
                    layoutParams.width = (155 * density).toInt()
                    layoutParams.height = (160 * density).toInt()
                    layoutParams.setMargins((40 * density).toInt(), 0, (40 * density).toInt(), 0)
                }
                "褲子" -> {
                    layoutParams.width = (145 * density).toInt()
                    layoutParams.height = (230 * density).toInt()
                    layoutParams.setMargins((40 * density).toInt(), (10 * density).toInt(), (40 * density).toInt(), 0)
                }
                else -> {
                    layoutParams.width = defaultWidth
                    layoutParams.height = defaultHeight
                    layoutParams.setMargins((40 * density).toInt(), 0, (40 * density).toInt(), 0)
                }
            }
            holder.imageView.layoutParams = layoutParams

            if (item.url.isEmpty()) {
                holder.imageView.setImageDrawable(null)
            } else {
                Glide.with(holder.imageView.context).load(item.url).into(holder.imageView)
            }
        }

        override fun getItemCount() = items.size
    }
}