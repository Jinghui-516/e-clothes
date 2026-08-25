package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.Match.Match_home
import tw.edu.pu.csim.s1120336.e_fit.Match.Rank
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.home

class Wardrobe : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddClothes: FloatingActionButton
    private lateinit var fabAiRobot: FloatingActionButton

    lateinit var hatImagesContainer: LinearLayout
    private val hatImageViews = mutableListOf<ImageView>()
    lateinit var dressImagesContainer: LinearLayout
    private val dressImageViews = mutableListOf<ImageView>()
    lateinit var clothesImagesContainer: LinearLayout
    private val clothesImageViews = mutableListOf<ImageView>()
    lateinit var pantsImagesContainer: LinearLayout
    private val pantsImageViews = mutableListOf<ImageView>()
    lateinit var skirtImagesContainer: LinearLayout
    private val skirtImageViews = mutableListOf<ImageView>()
    lateinit var shoesImagesContainer: LinearLayout
    private val shoesImageViews = mutableListOf<ImageView>()

    // 存放所有搭配資料的暫存變數
    private var allOutfits = listOf<Triple<String, String, String>>()
    private lateinit var layoutTitleSelector: LinearLayout
    private lateinit var tvMainTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var layoutWardrobeContent: LinearLayout
    private lateinit var layoutOutfitsContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wardrobe)

        fabAddClothes = findViewById(R.id.fab_add_clothes)
        fabAddClothes.setOnClickListener { startActivity(Intent(this, choose_add::class.java)) }

        layoutTitleSelector = findViewById(R.id.layout_title_selector)
        tvMainTitle = findViewById(R.id.tv_main_title)
        tvSubtitle = findViewById(R.id.tv_subtitle)
        layoutWardrobeContent = findViewById(R.id.layout_wardrobe_content)
        layoutOutfitsContent = findViewById(R.id.layout_outfits_content)

        // 初始化標題下拉功能
        enableTitleDropdown(true)

        fabAiRobot = findViewById(R.id.fab_ai_robot)
        fabAiRobot.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "🤖 詢問 AI 穿搭意見")
            popup.menu.add(0, 2, 1, "✂️ AI 照片一鍵去背")
            popup.menu.add(0, 3, 2, "📁 我的去背作品集")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { startActivity(Intent(this, Chat_AI::class.java)); true }
                    2 -> { startActivity(Intent(this, RemoveBgActivity::class.java)); true }
                    3 -> { startActivity(Intent(this, RemoveBgAlbumActivity::class.java)); true }
                    else -> false
                }
            }
            popup.show()
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_wardrobe
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> { startActivity(Intent(this, home::class.java)); overridePendingTransition(0, 0); finish(); true }
                R.id.nav_wardrobe -> true
                R.id.nav_community -> { startActivity(Intent(this, Match_home::class.java)); overridePendingTransition(0, 0); finish(); true }
                R.id.nav_rank -> { startActivity(Intent(this, Rank::class.java)); overridePendingTransition(0, 0); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, Personal_Page::class.java)); overridePendingTransition(0, 0); finish(); true }
                else -> false
            }
        }

        // 點擊文字標題區塊可以展開
        findViewById<LinearLayout>(R.id.hat_layout).setOnClickListener { openCategoryGrid("頭飾") }
        findViewById<LinearLayout>(R.id.dress_layout).setOnClickListener { openCategoryGrid("洋裝") }
        findViewById<LinearLayout>(R.id.clothes_layout).setOnClickListener { openCategoryGrid("上衣") }
        findViewById<LinearLayout>(R.id.pants_layout).setOnClickListener { openCategoryGrid("褲子") }
        findViewById<LinearLayout>(R.id.skirt_layout).setOnClickListener { openCategoryGrid("裙子") }
        findViewById<LinearLayout>(R.id.shoes_layout).setOnClickListener { openCategoryGrid("鞋子") }

        hatImagesContainer = findViewById(R.id.imagesContainer)
        dressImagesContainer = findViewById(R.id.dressimagesContainer)
        clothesImagesContainer = findViewById(R.id.clothesimagesContainer)
        pantsImagesContainer = findViewById(R.id.pantsimagesContainer)
        skirtImagesContainer = findViewById(R.id.skirtimagesContainer)
        shoesImagesContainer = findViewById(R.id.shoesimagesContainer)

        initializeImageViews(hatImagesContainer, hatImageViews, "頭飾")
        initializeImageViews(dressImagesContainer, dressImageViews, "洋裝")
        initializeImageViews(clothesImagesContainer, clothesImageViews, "上衣")
        initializeImageViews(pantsImagesContainer, pantsImageViews, "褲子")
        initializeImageViews(skirtImagesContainer, skirtImageViews, "裙子")
        initializeImageViews(shoesImagesContainer, shoesImageViews, "鞋子")

        refreshWardrobe()
        fetchMyOutfitsData()
    }

    // ==========================================
    // 🌟 控制標題下拉箭頭與點擊事件的開關
    // ==========================================
    private fun enableTitleDropdown(enable: Boolean) {
        // 自動尋找並隱藏/顯示標題旁的倒三角形圖示
        for (i in 0 until layoutTitleSelector.childCount) {
            val child = layoutTitleSelector.getChildAt(i)
            if (child.id != R.id.tv_main_title) {
                child.visibility = if (enable) View.VISIBLE else View.GONE
            }
        }

        if (enable) {
            layoutTitleSelector.isClickable = true
            layoutTitleSelector.setOnClickListener { view ->
                val popup = PopupMenu(this@Wardrobe, view)
                popup.menu.add(0, 1, 0, "我的衣櫃")
                popup.menu.add(0, 2, 1, "我的搭配")
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> { switchToWardrobe(); true }
                        2 -> { switchToOutfits(); true }
                        else -> false
                    }
                }
                popup.show()
            }
        } else {
            layoutTitleSelector.setOnClickListener(null)
            layoutTitleSelector.isClickable = false
        }
    }

    private fun switchToWardrobe() {
        tvMainTitle.text = "我的衣櫃"
        tvSubtitle.text = "要記得定期整理唷！"
        tvSubtitle.setOnClickListener(null)
        layoutWardrobeContent.visibility = View.VISIBLE
        layoutOutfitsContent.visibility = View.GONE
        fabAddClothes.show()
        enableTitleDropdown(true)
    }

    private fun switchToOutfits() {
        tvMainTitle.text = "我的搭配"
        layoutWardrobeContent.visibility = View.GONE
        layoutOutfitsContent.visibility = View.VISIBLE
        fabAddClothes.hide()
        enableTitleDropdown(true)
        showCategoryView()
    }

    // ==========================================
    // 🌟 展開特定類別的完整網格
    // ==========================================
    private fun openCategoryGrid(categoryName: String) {
        layoutWardrobeContent.visibility = View.GONE
        layoutOutfitsContent.visibility = View.VISIBLE
        fabAddClothes.hide()

        // 更改標題並停用下拉選單
        tvMainTitle.text = "我的$categoryName"
        enableTitleDropdown(false) // 🌟 隱藏倒三角形並停用下拉

        tvSubtitle.text = "🔙 返回我的衣櫃"
        tvSubtitle.setOnClickListener {
            switchToWardrobe() // 點擊返回時會恢復倒三角形與下拉功能
        }

        fetchClothesInCategory(categoryName)
    }

    private fun fetchClothesInCategory(categoryName: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = FirebaseFirestore.getInstance()
        val rvOutfitsGrid = findViewById<RecyclerView>(R.id.rv_outfits_grid)

        rvOutfitsGrid.layoutManager = GridLayoutManager(this, 3)

        db.collection(email)
            .whereEqualTo("服裝種類", categoryName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val clothesList = snapshot?.documents?.mapNotNull { doc ->
                    val url = doc.getString("圖片網址")
                    if (url != null) Pair(doc.id, url) else null
                } ?: emptyList()

                rvOutfitsGrid.adapter = ClothesGridAdapter(clothesList)
            }
    }

    inner class ClothesGridAdapter(private val items: List<Pair<String, String>>) : RecyclerView.Adapter<ClothesGridAdapter.ViewHolder>() {
        inner class ViewHolder(val layout: LinearLayout, val imageView: ImageView) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(12, 12, 12, 12) }
                gravity = android.view.Gravity.CENTER
            }

            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (120 * context.resources.displayMetrics.density).toInt())
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(android.graphics.Color.parseColor("#F9F9F9"))
                setBackgroundResource(R.drawable.corners_login)
                clipToOutline = true
            }

            layout.addView(imageView)
            return ViewHolder(layout, imageView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (docId, url) = items[position]
            Glide.with(holder.imageView.context).load(url).into(holder.imageView)

            holder.imageView.setOnClickListener {
                val dialog = Dialog(this@Wardrobe, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                val fullScreenImageView = ImageView(this@Wardrobe).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
                Glide.with(this@Wardrobe).load(url).into(fullScreenImageView)
                fullScreenImageView.setOnClickListener { dialog.dismiss() }
                dialog.setContentView(fullScreenImageView)
                dialog.show()
            }

            holder.imageView.setOnLongClickListener {
                AlertDialog.Builder(this@Wardrobe)
                    .setTitle("丟棄衣物")
                    .setMessage("確定要把這件衣物從衣櫃丟棄嗎？")
                    .setPositiveButton("確定丟棄") { _, _ ->
                        val email = FirebaseAuth.getInstance().currentUser?.email ?: return@setPositiveButton
                        FirebaseFirestore.getInstance().collection(email).document(docId).delete()
                            .addOnSuccessListener {
                                Toast.makeText(this@Wardrobe, "已刪除！", Toast.LENGTH_SHORT).show()
                                refreshWardrobe()
                            }
                    }.setNegativeButton("先留著", null).show()
                true
            }
        }
        override fun getItemCount() = items.size
    }

    private fun fetchMyOutfitsData() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = FirebaseFirestore.getInstance()
        val rvOutfitsGrid = findViewById<RecyclerView>(R.id.rv_outfits_grid)
        rvOutfitsGrid.layoutManager = GridLayoutManager(this, 2)

        db.collection(email).document("我的搭配").collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                allOutfits = snapshot?.documents?.mapNotNull { doc ->
                    val url = doc.getString("imageUrl")
                    val category = doc.getString("category") ?: "未分類"
                    if (url != null) Triple(doc.id, url, category) else null
                } ?: emptyList()
            }
    }

    private fun showCategoryView() {
        tvSubtitle.text = "請選擇一個穿搭情境 📁"
        tvSubtitle.setOnClickListener(null)
        enableTitleDropdown(true)

        val rvOutfitsGrid = findViewById<RecyclerView>(R.id.rv_outfits_grid)
        rvOutfitsGrid.layoutManager = GridLayoutManager(this, 2)
        val categories = allOutfits.map { it.third }.distinct()

        rvOutfitsGrid.adapter = CategoryAdapter(categories) { selectedCategory ->
            showOutfitsInCategory(selectedCategory)
        }
    }

    private fun showOutfitsInCategory(category: String) {
        tvSubtitle.text = "🔙 返回分類列表 ｜ 📍目前顯示: #$category"
        tvSubtitle.setOnClickListener { showCategoryView() }
        enableTitleDropdown(false) // 進入特定搭配清單時也隱藏下拉箭頭

        val rvOutfitsGrid = findViewById<RecyclerView>(R.id.rv_outfits_grid)
        rvOutfitsGrid.layoutManager = GridLayoutManager(this, 2)
        val filteredOutfits = allOutfits.filter { it.third == category }

        rvOutfitsGrid.adapter = OutfitsAdapter(filteredOutfits)
    }

    inner class CategoryAdapter(private val categories: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
        inner class CategoryViewHolder(val layout: LinearLayout, val tvCategory: TextView, val tvCount: TextView) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val context = parent.context
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 16, 16, 16) }
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#F5F0E6"))
                setPadding(0, 60, 0, 60)
            }

            val icon = TextView(context).apply { text = "📁"; textSize = 40f; gravity = android.view.Gravity.CENTER }
            val tvCategory = TextView(context).apply { textSize = 18f; setTextColor(android.graphics.Color.DKGRAY); setTypeface(null, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER }
            val tvCount = TextView(context).apply { textSize = 14f; setTextColor(android.graphics.Color.GRAY); gravity = android.view.Gravity.CENTER; setPadding(0, 10, 0, 0) }

            layout.addView(icon); layout.addView(tvCategory); layout.addView(tvCount)
            return CategoryViewHolder(layout, tvCategory, tvCount)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.tvCategory.text = category
            val count = allOutfits.count { it.third == category }
            holder.tvCount.text = "$count 套搭配"
            holder.layout.setOnClickListener { onClick(category) }
        }
        override fun getItemCount() = categories.size
    }

    inner class OutfitsAdapter(private val outfits: List<Triple<String, String, String>>) : RecyclerView.Adapter<OutfitsAdapter.OutfitViewHolder>() {
        inner class OutfitViewHolder(val layout: LinearLayout, val imageView: ImageView) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
            val context = parent.context
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 16, 16, 32) }
                gravity = android.view.Gravity.CENTER
            }

            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (220 * context.resources.displayMetrics.density).toInt())
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#F9F9F9"))
            }

            layout.addView(imageView)
            return OutfitViewHolder(layout, imageView)
        }

        override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
            val (docId, url, category) = outfits[position]
            Glide.with(holder.imageView.context).load(url).into(holder.imageView)

            holder.imageView.setOnClickListener {
                val dialog = Dialog(this@Wardrobe, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                val fullScreenImageView = ImageView(this@Wardrobe).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
                Glide.with(this@Wardrobe).load(url).into(fullScreenImageView)
                fullScreenImageView.setOnClickListener { dialog.dismiss() }
                dialog.setContentView(fullScreenImageView)
                dialog.show()
            }

            holder.imageView.setOnLongClickListener {
                AlertDialog.Builder(this@Wardrobe)
                    .setTitle("刪除搭配")
                    .setMessage("確定要刪除這套穿搭嗎？")
                    .setPositiveButton("確定刪除") { _, _ ->
                        val email = FirebaseAuth.getInstance().currentUser?.email ?: return@setPositiveButton
                        FirebaseFirestore.getInstance().collection(email).document("我的搭配").collection("items").document(docId).delete()
                            .addOnSuccessListener { Toast.makeText(this@Wardrobe, "已刪除搭配！", Toast.LENGTH_SHORT).show() }
                    }.setNegativeButton("保留", null).show()
                true
            }
        }
        override fun getItemCount() = outfits.size
    }

    private fun refreshWardrobe() {
        downloadImages("頭飾", hatImageViews)
        downloadImages("洋裝", dressImageViews)
        downloadImages("上衣", clothesImageViews)
        downloadImages("褲子", pantsImageViews)
        downloadImages("裙子", skirtImageViews)
        downloadImages("鞋子", shoesImageViews)
    }

    private fun initializeImageViews(container: LinearLayout, imageViews: MutableList<ImageView>, categoryName: String) {
        container.setOnClickListener { openCategoryGrid(categoryName) }

        for (i in 1..4) {
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams((100 * resources.displayMetrics.density).toInt(), (100 * resources.displayMetrics.density).toInt())
            layoutParams.setMargins(0, 0, 16, 0)
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setBackgroundResource(R.drawable.corners_login)
            imageView.clipToOutline = true

            imageView.setOnClickListener { openCategoryGrid(categoryName) }

            container.addView(imageView)
            imageViews.add(imageView)
        }
    }

    private fun downloadImages(type: String, imageViews: List<ImageView>) {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection(email).whereEqualTo("服裝種類", type).orderBy(FieldPath.documentId()).limit(4)
            .get().addOnSuccessListener { documents ->
                var index = 0
                for (document in documents) {
                    val imageUrl = document.getString("圖片網址")
                    val docId = document.id
                    if (imageUrl != null && index < imageViews.size) {
                        val imageView = imageViews[index]
                        downloadFromFirebaseStorage(imageUrl, imageView)

                        imageView.setOnLongClickListener { showDeleteDialog(email, docId, type); true }
                        index++
                    }
                }
                for (i in index until imageViews.size) {
                    imageViews[i].setImageBitmap(null)
                    imageViews[i].setOnLongClickListener(null)
                }
            }
    }

    private fun showDeleteDialog(email: String, docId: String, type: String) {
        AlertDialog.Builder(this)
            .setTitle("丟棄衣物")
            .setMessage("確定要把這件「$type」從衣櫃丟棄嗎？")
            .setPositiveButton("確定丟棄") { _, _ ->
                FirebaseFirestore.getInstance().collection(email).document(docId).delete()
                    .addOnSuccessListener { Toast.makeText(this, "已刪除！", Toast.LENGTH_SHORT).show(); refreshWardrobe() }
            }.setNegativeButton("先留著", null).show()
    }

    private fun downloadFromFirebaseStorage(path: String, imageView: ImageView) {
        val storage = FirebaseStorage.getInstance()
        if (path.isEmpty()) return
        val storageRef = if (path.startsWith("http")) storage.getReferenceFromUrl(path) else storage.reference.child(path)
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val bmp: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bmp)
        }
    }
}