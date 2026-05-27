package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
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
    lateinit var shoesImagesContainer: LinearLayout
    private val shoesImageViews = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wardrobe)

        fabAddClothes = findViewById(R.id.fab_add_clothes)
        fabAddClothes.setOnClickListener {
            startActivity(Intent(this, choose_add::class.java))
        }

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

        findViewById<LinearLayout>(R.id.hat_layout).setOnClickListener { startActivity(Intent(this, add_hat::class.java)) }
        findViewById<LinearLayout>(R.id.dress_layout).setOnClickListener { startActivity(Intent(this, add_dress::class.java)) }
        findViewById<LinearLayout>(R.id.clothes_layout).setOnClickListener { startActivity(Intent(this, add_clothes::class.java)) }
        findViewById<LinearLayout>(R.id.pants_layout).setOnClickListener { startActivity(Intent(this, add_pants::class.java)) }
        findViewById<LinearLayout>(R.id.shoes_layout).setOnClickListener { startActivity(Intent(this, add_shoes::class.java)) }

        hatImagesContainer = findViewById(R.id.imagesContainer)
        dressImagesContainer = findViewById(R.id.dressimagesContainer)
        clothesImagesContainer = findViewById(R.id.clothesimagesContainer)
        pantsImagesContainer = findViewById(R.id.pantsimagesContainer)
        shoesImagesContainer = findViewById(R.id.shoesimagesContainer)

        initializeImageViews(hatImagesContainer, hatImageViews)
        initializeImageViews(dressImagesContainer, dressImageViews)
        initializeImageViews(clothesImagesContainer, clothesImageViews)
        initializeImageViews(pantsImagesContainer, pantsImageViews)
        initializeImageViews(shoesImagesContainer, shoesImageViews)

        refreshWardrobe()
    }

    private fun refreshWardrobe() {
        downloadImages("頭飾", hatImageViews)
        downloadImages("洋裝", dressImageViews)
        downloadImages("上衣", clothesImageViews)
        downloadImages("褲子", pantsImageViews)
        downloadImages("鞋子", shoesImageViews)
    }

    private fun initializeImageViews(container: LinearLayout, imageViews: MutableList<ImageView>) {
        for (i in 1..4) {
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams((100 * resources.displayMetrics.density).toInt(), (100 * resources.displayMetrics.density).toInt())
            layoutParams.setMargins(0, 0, 16, 0)
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setBackgroundResource(R.drawable.corners_login)
            imageView.clipToOutline = true
            container.addView(imageView)
            imageViews.add(imageView)
        }
    }

    private fun downloadImages(type: String, imageViews: List<ImageView>) {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection(email)
            .whereEqualTo("服裝種類", type)
            .orderBy(FieldPath.documentId())
            .limit(4)
            .get()
            .addOnSuccessListener { documents ->
                var index = 0
                for (document in documents) {
                    val imageUrl = document.getString("圖片網址")
                    val docId = document.id

                    if (imageUrl != null && index < imageViews.size) {
                        val imageView = imageViews[index]
                        downloadFromFirebaseStorage(imageUrl, imageView)

                        // 🌟 核心：綁定「長按刪除」
                        imageView.setOnLongClickListener {
                            showDeleteDialog(email, docId, type)
                            true
                        }
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
                FirebaseFirestore.getInstance().collection(email).document(docId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "已刪除！", Toast.LENGTH_SHORT).show()
                        refreshWardrobe() // 🌟 重新整理畫面
                    }
            }
            .setNegativeButton("先留著", null)
            .show()
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