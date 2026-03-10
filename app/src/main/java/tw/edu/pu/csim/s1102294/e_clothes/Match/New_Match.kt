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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.R
import kotlin.math.log

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

    val hat = mutableListOf<String>() // Firestore URLs for hats
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

            // Pass the current selected image URLs
            intent1.putExtra("hatUrl", hat[currentImageIndex1])
            intent1.putExtra("clothesUrl", clothes[currentImageIndex2])
            intent1.putExtra("pantsUrl", pants[currentImageIndex3])
            intent1.putExtra("shoesUrl", shoes[currentImageIndex4])
            intent1.putExtra("bodyPhotoUrl", selectedImageUri)  // Pass selected image URL

            startActivity(intent1)
            finish()
        }
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        gDetector.onTouchEvent(event)
        return true
    }

    override fun onDown(p0: MotionEvent): Boolean = true

    override fun onShowPress(p0: MotionEvent) {}

    override fun onSingleTapUp(p0: MotionEvent): Boolean = true

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean = true

    override fun onLongPress(p0: MotionEvent) {}

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val density = Resources.getSystem().displayMetrics.density
        val dpValue = 200
        val pixels = (dpValue * density).toInt()

        if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imghat.top) && e1.y <= (pixels + imghat.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex1++
                if (currentImageIndex1 >= hat.size) currentImageIndex1 = 0
            } else {
                currentImageIndex1--
                if (currentImageIndex1 < 0) currentImageIndex1 = hat.size - 1
            }
            updateHatImage()
        } else if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imgclothes.top) && e1.y <= (pixels + imgclothes.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex2++
                if (currentImageIndex2 >= clothes.size) currentImageIndex2 = 0
            } else {
                currentImageIndex2--
                if (currentImageIndex2 < 0) currentImageIndex2 = clothes.size - 1
            }
            updateClothesImage()
        } else if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imgpants.top) && e1.y <= (pixels + imgpants.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex3++
                if (currentImageIndex3 >= pants.size) currentImageIndex3 = 0
            } else {
                currentImageIndex3--
                if (currentImageIndex3 < 0) currentImageIndex3 = pants.size - 1
            }
            updatePantsImage()
        } else if (Math.abs(velocityX) > Math.abs(velocityY) && e1.y >= (pixels + imgshoes.top) && e1.y <= (pixels + imgshoes.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex4++
                if (currentImageIndex4 >= shoes.size) currentImageIndex4 = 0
            } else {
                currentImageIndex4--
                if (currentImageIndex4 < 0) currentImageIndex4 = shoes.size - 1
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                // Convert the URI to String and store it
                val imageUrl = selectedImageUri.toString()
                Log.e("PhotoSelection", "Selected Image URL: $imageUrl")

                // Set the selected image to the body_photo ImageView
                body_photo.setImageURI(selectedImageUri)
                this.selectedImageUri = imageUrl // Store the image URL for passing to the next activity
            } else {
                Log.e("PhotoSelection", "No image URI found in the selected data")
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load and update images for different categories from Firestore
    private fun loadHatImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val email = FirebaseAuth.getInstance().currentUser?.email
            Log.d("Firestore", "Loading images for user: $email")

            if (email != null) {
                db.collection(email)
                    .get()
                    .addOnSuccessListener { documents ->
                        hat.clear() // Clear existing URLs to prevent duplication
                        for (document in documents) {
                            if (document.id.contains("頭飾")) {
                                val imageUrl = document.getString("圖片完整網址")
                                if (!imageUrl.isNullOrEmpty()) {
                                    hat.add(imageUrl)
                                    Log.d("Firestore", "Added image URL: $imageUrl")
                                }
                            }
                        }
                        if (hat.isNotEmpty()) {
                            updateHatImage()
                        } else {
                            Log.e("Firestore", "No valid hat images found.")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error loading images: ${exception.message}")
                    }
            }
        }
    }

    private fun updateHatImage() {
        if (hat.isNotEmpty()) {
            Picasso.get().load(hat[currentImageIndex1])
                .into(imghat, object : Callback {
                    override fun onSuccess() {
                        Log.d("PicassoHatImage", "Image loaded successfully")
                    }

                    override fun onError(e: Exception?) {
                        Log.e("PicassoHatImage", "Error loading image: ${e?.message}")
                    }
                })
        }
    }

    private fun loadClothesImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val email = FirebaseAuth.getInstance().currentUser?.email
            Log.d("Firestore", "Loading images for user: $email")

            if (email != null) {
                db.collection(email)
                    .get()
                    .addOnSuccessListener { documents ->
                        clothes.clear() // Clear existing URLs to prevent duplication
                        for (document in documents) {
                            if (document.id.contains("上衣")) {
                                val imageUrl = document.getString("圖片完整網址")
                                if (!imageUrl.isNullOrEmpty()) {
                                    clothes.add(imageUrl)
                                    Log.d("Firestore", "Added image URL: $imageUrl")
                                }
                            }
                        }
                        if (clothes.isNotEmpty()) {
                            updateClothesImage()
                        } else {
                            Log.e("Firestore", "No valid clothes images found.")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error loading images: ${exception.message}")
                    }
            }
        }
    }

    private fun updateClothesImage() {
        if (clothes.isNotEmpty()) {
            Picasso.get().load(clothes[currentImageIndex2])
                .into(imgclothes, object : Callback {
                    override fun onSuccess() {
                        Log.d("PicassoClothesImage", "Image loaded successfully")
                    }

                    override fun onError(e: Exception?) {
                        Log.e("PicassoClothesImage", "Error loading image: ${e?.message}")
                    }
                })
        }
    }

    private fun loadPantsImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val email = FirebaseAuth.getInstance().currentUser?.email
            Log.d("Firestore", "Loading images for user: $email")

            if (email != null) {
                db.collection(email)
                    .get()
                    .addOnSuccessListener { documents ->
                        pants.clear() // Clear existing URLs to prevent duplication
                        for (document in documents) {
                            if (document.id.contains("褲子")) {
                                val imageUrl = document.getString("圖片完整網址")
                                if (!imageUrl.isNullOrEmpty()) {
                                    pants.add(imageUrl)
                                    Log.d("Firestore", "Added image URL: $imageUrl")
                                }
                            }
                        }
                        if (pants.isNotEmpty()) {
                            updatePantsImage()
                        } else {
                            Log.e("Firestore", "No valid pants images found.")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error loading images: ${exception.message}")
                    }
            }
        }
    }

    private fun updatePantsImage() {
        if (pants.isNotEmpty()) {
            Picasso.get().load(pants[currentImageIndex3])
                .into(imgpants, object : Callback {
                    override fun onSuccess() {
                        Log.d("PicassoPantsImage", "Image loaded successfully")
                    }

                    override fun onError(e: Exception?) {
                        Log.e("PicassoPantsImage", "Error loading image: ${e?.message}")
                    }
                })
        }
    }

    private fun loadShoesImagesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val email = FirebaseAuth.getInstance().currentUser?.email
            Log.d("Firestore", "Loading images for user: $email")

            if (email != null) {
                db.collection(email)
                    .get()
                    .addOnSuccessListener { documents ->
                        shoes.clear() // Clear existing URLs to prevent duplication
                        for (document in documents) {
                            if (document.id.contains("鞋子")) {
                                val imageUrl = document.getString("圖片完整網址")
                                if (!imageUrl.isNullOrEmpty()) {
                                    shoes.add(imageUrl)
                                    Log.d("Firestore", "Added image URL: $imageUrl")
                                }
                            }
                        }
                        if (shoes.isNotEmpty()) {
                            updateShoesImage()
                        } else {
                            Log.e("Firestore", "No valid shoes images found.")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error loading images: ${exception.message}")
                    }
            }
        }
    }

    private fun updateShoesImage() {
        if (shoes.isNotEmpty()) {
            Picasso.get().load(shoes[currentImageIndex4])
                .into(imgshoes, object : Callback {
                    override fun onSuccess() {
                        Log.d("PicassoShoesImage", "Image loaded successfully")
                    }

                    override fun onError(e: Exception?) {
                        Log.e("PicassoShoesImage", "Error loading image: ${e?.message}")
                    }
                })
        }
    }

}
