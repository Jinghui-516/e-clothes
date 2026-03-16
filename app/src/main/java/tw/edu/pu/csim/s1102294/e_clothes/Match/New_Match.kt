package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1102294.e_clothes.R
import java.util.*

class New_Match : AppCompatActivity(), GestureDetector.OnGestureListener {

    lateinit var gDetector: GestureDetector
    lateinit var imghat: ImageView
    lateinit var imgclothes: ImageView
    lateinit var imgpants: ImageView
    lateinit var imgshoes: ImageView
    lateinit var btnDress: Button
    lateinit var next: ImageView
    lateinit var previous: ImageView
    lateinit var body_photo: ImageView
    var selectedImageUri = ""

    val hat = mutableListOf<String>()
    val clothes = mutableListOf<String>()
    val pants = mutableListOf<String>()
    val shoes = mutableListOf<String>()
    var currentImageIndex1 = 0
    var currentImageIndex2 = 0
    var currentImageIndex3 = 0
    var currentImageIndex4 = 0

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_match)

        imghat = findViewById(R.id.imghat)
        imgclothes = findViewById(R.id.imgclothes)
        imgpants = findViewById(R.id.imgpants)
        imgshoes = findViewById(R.id.imgshoes)
        btnDress = findViewById(R.id.btnDress)
        next = findViewById(R.id.next)
        previous = findViewById(R.id.previous)
        body_photo = findViewById(R.id.body_photo)

        body_photo.setOnClickListener {
            openPhotoAlbum()
        }

        loadHatImagesFromFirestore()
        loadClothesImagesFromFirestore()
        loadPantsImagesFromFirestore()
        loadShoesImagesFromFirestore()

        gDetector = GestureDetector(this, this)

        previous.setOnClickListener {
            val intent1 = Intent(this, Match_home::class.java)
            startActivity(intent1)
            finish()
        }

        btnDress.setOnClickListener {
            val intent1 = Intent(this, new_match_dress::class.java)
            startActivity(intent1)
            finish()
        }

        next.setOnClickListener {
            val intent1 = Intent(this, Edit_Label::class.java)

            // 確保有資料才傳送，避免 indexOutOfBounds 錯誤
            if (hat.isNotEmpty()) intent1.putExtra("hatUrl", hat[currentImageIndex1])
            if (clothes.isNotEmpty()) intent1.putExtra("clothesUrl", clothes[currentImageIndex2])
            if (pants.isNotEmpty()) intent1.putExtra("pantsUrl", pants[currentImageIndex3])
            if (shoes.isNotEmpty()) intent1.putExtra("shoesUrl", shoes[currentImageIndex4])
            intent1.putExtra("bodyPhotoUrl", selectedImageUri)

            startActivity(intent1)
            finish()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gDetector.onTouchEvent(event)
        return true
    }

    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = true
    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean = true
    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null) return false

        val density = Resources.getSystem().displayMetrics.density
        val dpValue = 200
        val pixels = (dpValue * density).toInt()

        if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imghat.top) && e1.y <= (pixels + imghat.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex1 = (currentImageIndex1 + 1) % hat.size
            } else {
                currentImageIndex1 = if (currentImageIndex1 > 0) currentImageIndex1 - 1 else hat.size - 1
            }
            updateHatImage()
        } else if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imgclothes.top) && e1.y <= (pixels + imgclothes.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex2 = (currentImageIndex2 + 1) % clothes.size
            } else {
                currentImageIndex2 = if (currentImageIndex2 > 0) currentImageIndex2 - 1 else clothes.size - 1
            }
            updateClothesImage()
        } else if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imgpants.top) && e1.y <= (pixels + imgpants.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex3 = (currentImageIndex3 + 1) % pants.size
            } else {
                currentImageIndex3 = if (currentImageIndex3 > 0) currentImageIndex3 - 1 else pants.size - 1
            }
            updatePantsImage()
        } else if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imgshoes.top) && e1.y <= (pixels + imgshoes.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex4 = (currentImageIndex4 + 1) % shoes.size
            } else {
                currentImageIndex4 = if (currentImageIndex4 > 0) currentImageIndex4 - 1 else shoes.size - 1
            }
            updateShoesImage()
        }
        return true
    }

    private fun openPhotoAlbum() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 修正這裡的 RESULT_OK 判斷
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                val imageUrl = uri.toString()
                body_photo.setImageURI(uri)
                this.selectedImageUri = imageUrl
            }
        }
    }

    private fun loadHatImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email != null) {
            db.collection(email).get().addOnSuccessListener { documents ->
                hat.clear()
                for (document in documents) {
                    if (document.id.contains("頭飾")) {
                        val imageUrl = document.getString("圖片完整網址")
                        if (!imageUrl.isNullOrEmpty()) hat.add(imageUrl)
                    }
                }
                if (hat.isNotEmpty()) updateHatImage()
            }
        }
    }

    private fun updateHatImage() {
        if (hat.isNotEmpty()) {
            Glide.with(this).load(hat[currentImageIndex1]).into(imghat)
            Log.d("Glide", "Loading Hat: ${hat[currentImageIndex1]}")
        }
    }

    private fun loadClothesImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email != null) {
            db.collection(email).get().addOnSuccessListener { documents ->
                clothes.clear()
                for (document in documents) {
                    if (document.id.contains("上衣")) {
                        val imageUrl = document.getString("圖片完整網址")
                        if (!imageUrl.isNullOrEmpty()) clothes.add(imageUrl)
                    }
                }
                if (clothes.isNotEmpty()) updateClothesImage()
            }
        }
    }

    private fun updateClothesImage() {
        if (clothes.isNotEmpty()) {
            Glide.with(this).load(clothes[currentImageIndex2]).into(imgclothes)
        }
    }

    private fun loadPantsImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email != null) {
            db.collection(email).get().addOnSuccessListener { documents ->
                pants.clear()
                for (document in documents) {
                    if (document.id.contains("褲子")) {
                        val imageUrl = document.getString("圖片完整網址")
                        if (!imageUrl.isNullOrEmpty()) pants.add(imageUrl)
                    }
                }
                if (pants.isNotEmpty()) updatePantsImage()
            }
        }
    }

    private fun updatePantsImage() {
        if (pants.isNotEmpty()) {
            Glide.with(this).load(pants[currentImageIndex3]).into(imgpants)
        }
    }

    private fun loadShoesImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email != null) {
            db.collection(email).get().addOnSuccessListener { documents ->
                shoes.clear()
                for (document in documents) {
                    if (document.id.contains("鞋子")) {
                        val imageUrl = document.getString("圖片完整網址")
                        if (!imageUrl.isNullOrEmpty()) shoes.add(imageUrl)
                    }
                }
                if (shoes.isNotEmpty()) updateShoesImage()
            }
        }
    }

    private fun updateShoesImage() {
        if (shoes.isNotEmpty()) {
            Glide.with(this).load(shoes[currentImageIndex4]).into(imgshoes)
        }
    }
}