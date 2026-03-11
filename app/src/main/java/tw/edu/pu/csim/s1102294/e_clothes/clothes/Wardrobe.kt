package tw.edu.pu.csim.s1102294.e_clothes.clothes

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Community.Personal_Page
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.Match.Rank
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.home
import kotlin.jvm.java

class Wardrobe : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView

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

    lateinit var hat_layout: LinearLayout
    lateinit var dress_layout: LinearLayout
    lateinit var clothes_loayout: LinearLayout
    lateinit var pants_layout: LinearLayout
    lateinit var shoes_layout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wardrobe)

        // 🌟 導航列設定
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_wardrobe
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> {
                    startActivity(Intent(this, home::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_wardrobe -> true
                R.id.nav_community -> {
                    startActivity(Intent(this, Match_home::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_rank -> {
                    // 🌟 這裡就是接通跳轉的地方！
                    val intent = Intent(this, Rank::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // 讓切換更順滑，沒有跳動感
                    finish() // 關閉目前這一頁
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Personal_Page::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        // 分類點擊跳轉 (保留你原本功能)
        findViewById<LinearLayout>(R.id.hat_layout).setOnClickListener {
            startActivity(Intent(this, add_hat::class.java))
        }
        findViewById<LinearLayout>(R.id.dress_layout).setOnClickListener {
            startActivity(Intent(this, add_dress::class.java))
        }
        findViewById<LinearLayout>(R.id.clothes_layout).setOnClickListener {
            startActivity(Intent(this, add_clothes::class.java))
        }
        findViewById<LinearLayout>(R.id.pants_layout).setOnClickListener {
            startActivity(Intent(this, add_pants::class.java))
        }
        findViewById<LinearLayout>(R.id.shoes_layout).setOnClickListener {
            startActivity(Intent(this, add_shoes::class.java))
        }

        // 容器綁定與初始化 (延用你原本邏輯)
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

        downloadImages("頭飾", hatImageViews)
        downloadImages("洋裝", dressImageViews)
        downloadImages("上衣", clothesImageViews)
        downloadImages("褲子", pantsImageViews)
        downloadImages("鞋子", shoesImageViews)
    }

    private fun initializeImageViews(container: LinearLayout, imageViews: MutableList<ImageView>) {
        for (i in 1..4) {
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                (100 * resources.displayMetrics.density).toInt(),
                (100 * resources.displayMetrics.density).toInt()
            )
            layoutParams.setMargins(0, 0, 16, 0) // 增加圖片間距
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setBackgroundResource(R.drawable.corners_login) // 使用你之前的圓角資源
            imageView.clipToOutline = true
            container.addView(imageView)
            imageViews.add(imageView)
        }
    }

    private fun downloadImages(type: String, imageViews: List<ImageView>) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val email = currentUser.email
            if (email != null) {
                db.collection(email)
                    .whereEqualTo("服裝種類", type)
                    .orderBy(FieldPath.documentId())
                    .limit(4)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            var index = 0
                            for (document in documents) {
                                val imageUrl = document.getString("圖片網址")
                                if (imageUrl != null && index < imageViews.size) {
                                    downloadFromFirebaseStorage(imageUrl, imageViews[index])
                                    index++
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun downloadFromFirebaseStorage(relativePath: String, imageView: ImageView) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child(relativePath)
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val bmp: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bmp)
        }
    }
}