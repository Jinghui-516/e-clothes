package tw.edu.pu.csim.s1102294.e_clothes.clothes

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.home

class Wardrobe : AppCompatActivity() {

    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var New_clothes: ImageView
    lateinit var set: ImageView

    lateinit var hatImagesContainer: LinearLayout
    private val hatImageViews = mutableListOf<ImageView>()
    lateinit var dressImagesContainer: LinearLayout // 將髮飾容器改為洋裝
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

        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
//            Match.text = ""
            val intent2 = Intent(this, Match_home::class.java)
            startActivity(intent2)
            finish()
        }

        New_clothes = findViewById(R.id.New_clothes)
        New_clothes.setOnClickListener {
//            textView9.text = "123"
//            checkPermission()
            val intent = Intent(this, choose_add::class.java)
            startActivity(intent)
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }

        set = findViewById(R.id.set)
        set.setOnClickListener {
            val intent1 = Intent(this, Setting::class.java)
            startActivity(intent1)
            finish()
        }

        hat_layout = findViewById(R.id.hat_layout)
        hat_layout.setOnClickListener {
            val intent1 = Intent(this, add_hat::class.java)
            startActivity(intent1)
            finish()
        }

        dress_layout = findViewById(R.id.dress_layout)
        dress_layout.setOnClickListener {
            val intent1 = Intent(this, add_dress::class.java)
            startActivity(intent1)
            finish()
        }

        clothes_loayout = findViewById(R.id.clothes_layout)
        clothes_loayout.setOnClickListener {
            val intent1 = Intent(this, add_clothes::class.java)
            startActivity(intent1)
            finish()
        }

        pants_layout = findViewById(R.id.pants_layout)
        pants_layout.setOnClickListener {
            val intent1 = Intent(this, add_pants::class.java)
            startActivity(intent1)
            finish()
        }

        shoes_layout = findViewById(R.id.shoes_layout)
        shoes_layout.setOnClickListener {
            val intent1 = Intent(this, add_shoes::class.java)
            startActivity(intent1)
            finish()
        }

        hatImagesContainer = findViewById(R.id.imagesContainer)
        dressImagesContainer = findViewById(R.id.dressimagesContainer) // 更新容器ID
        clothesImagesContainer = findViewById(R.id.clothesimagesContainer)
        pantsImagesContainer = findViewById(R.id.pantsimagesContainer)
        shoesImagesContainer = findViewById(R.id.shoesimagesContainer)

        initializeImageViews(hatImagesContainer, hatImageViews) // 初始化帽子
        initializeImageViews(dressImagesContainer, dressImageViews) // 初始化洋裝
        initializeImageViews(clothesImagesContainer, clothesImageViews) // 初始化上衣
        initializeImageViews(pantsImagesContainer, pantsImageViews) // 初始化褲子
        initializeImageViews(shoesImagesContainer, shoesImageViews) // 初始化鞋子

        downloadImages("頭飾", hatImageViews) // 下載帽子圖片
        downloadImages("洋裝", dressImageViews) // 下載洋裝圖片
        downloadImages("上衣", clothesImageViews) // 下載上衣圖片
        downloadImages("褲子", pantsImageViews) // 下載褲子圖片
        downloadImages("鞋子", shoesImageViews) // 下載鞋子圖片
    }

    private fun initializeImageViews(container: LinearLayout, imageViews: MutableList<ImageView>) {
        for (i in 1..4) {
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                (100 * resources.displayMetrics.density).toInt(),
                (100 * resources.displayMetrics.density).toInt()
            )
            imageView.layoutParams = layoutParams
            container.addView(imageView) // 添加到容器中
            imageViews.add(imageView) // 將 ImageView 添加到列表中
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
                                    addImageView(imageUrl, imageViews[index])
                                    index++
                                }
                            }
                        } else {
                            Log.d("Firebase", "没有找到文件")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firebase", "获取文档时出错", exception)
                    }
            }
        } else {
            Log.e("Firebase", "用户未登录")
        }
    }

    private fun addImageView(imageUrl: String, imageView: ImageView) {
        downloadFromFirebaseStorage(imageUrl, imageView)
    }

    private fun downloadFromFirebaseStorage(relativePath: String, imageView: ImageView) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child(relativePath)

        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val bmp: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bmp)
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "下载图片时出错", exception)
        }
    }
}

