package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Community.Liked_Post
import tw.edu.pu.csim.s1102294.e_clothes.Community.Personal_Page
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.change_password
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class edit_Profile : AppCompatActivity() {

    private lateinit var Match: ImageView
    private lateinit var Home: ImageView
    private lateinit var Friend: ImageView
    private lateinit var Clothes: ImageView
    private lateinit var Personal_page: ImageView
    private lateinit var ok: Button
    private lateinit var txv_change: TextView
    private lateinit var circularImageView: ShapeableImageView

    private var imageUri: Uri? = null // Variable to hold the selected image URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val user_name: EditText = findViewById(R.id.user_name)
        val birthday: EditText = findViewById(R.id.birthday)
        val gender: EditText = findViewById(R.id.gender)
        val sign: EditText = findViewById(R.id.sign)
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val email = FirebaseAuth.getInstance().currentUser?.email

        circularImageView = findViewById(R.id.circularImageView)

        // Load user data from Firestore on page load
        if (email != null) {
            val documentRef = db.collection(email).document("個人資料")

            documentRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Retrieve user data
                        val name = document.getString("使用者名稱")
                        val birthdayStr = document.getString("生日")
                        val genderStr = document.getString("性別")
                        val signature = document.getString("個性簽名")
                        val profileImageUrl = document.getString("頭貼圖片")

                        // Update the UI with the retrieved data
                        user_name.setText(name ?: "")
                        birthday.setText(birthdayStr ?: "")
                        gender.setText(genderStr ?: "")
                        sign.setText(signature ?: "")

                        // Load the profile image into the circular ImageView using Picasso
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Picasso.get()
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_launcher_foreground) // 可選的佔位符
                                .error(R.drawable.user) // 加載失敗時顯示的圖片
                                .into(circularImageView)
                        } else {
                            // URL 為空時設置預設圖片
                            circularImageView.setImageResource(R.drawable.user)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "無法載入個人資料: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Set up click listeners for other UI elements
        circularImageView.setOnClickListener {
            openGallery() // Open the gallery to select a new profile image
        }

        // Set up the other navigation and button click listeners (Home, Match, etc.)
        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
            startActivity(Intent(this, home::class.java))
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
            startActivity(Intent(this, Match_home::class.java))
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
            startActivity(Intent(this, choose_add::class.java))
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            startActivity(Intent(this, Friends::class.java))
            finish()
        }

        Personal_page = findViewById(R.id.Personal_page)
        Personal_page.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
            finish()
        }

        // Handle the OK button click to update user data
        ok = findViewById(R.id.ok)
        ok.setOnClickListener {
            updateProfileData(userId, user_name.text.toString(), birthday.text.toString(), gender.text.toString(), sign.text.toString())
        }

        txv_change = findViewById(R.id.txv_change)
        txv_change.setOnClickListener {
            startActivity(Intent(this, change_password::class.java))
            finish()
        }

        // Menu button logic
        val menu = findViewById<ImageView>(R.id.menu)
        menu.setOnClickListener {
            val popup = PopupMenu(this, menu)
            popup.menuInflater.inflate(R.menu.menu_share, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        startActivity(Intent(this, edit_Profile::class.java))
                        finish()
                        true
                    }
                    R.id.check -> {
                        startActivity(Intent(this, edit_Chosen_Match::class.java))
                        finish()
                        true
                    }
                    R.id.share -> {
                        startActivity(Intent(this, share_Match::class.java))
                        finish()
                        true
                    }
                    R.id.like -> {
                        startActivity(Intent(this, Liked_Post::class.java))
                        finish()
                        true
                    }
                    R.id.settings -> {
                        startActivity(Intent(this, Setting::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*" // Only select images
        }
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            imageUri = data?.data // Save the selected image URI
            circularImageView.setImageURI(imageUri) // Set the selected image
        }
    }

    private fun updateProfileData(userId: String?, name: String, birthday: String, gender: String, sign: String) {
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val email = FirebaseAuth.getInstance().currentUser?.email

            // 更新使用者的個人資料
            val user = hashMapOf(
                "email" to email,
                "使用者名稱" to name,
                "生日" to birthday,
                "性別" to gender,
                "個性簽名" to sign
            )

            // 更新個人資料
            db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener {
                    // 更新 users 集合
                    val userUpdate = hashMapOf(
                        "email" to email,
                        "使用者名稱" to name,
                        "生日" to birthday,
                        "性別" to gender,
                        "個性簽名" to sign
                    )

                    if (email != null) {
                        db.collection(email).document("個人資料")
                            .set(userUpdate, SetOptions.merge()) // 使用 merge 以避免覆蓋其他欄位
                            .addOnSuccessListener {
                                // 若有圖片更新，則上傳圖片
                                if (imageUri != null) {
                                    uploadImageToFirebase(imageUri!!)
                                } else {
                                    Toast.makeText(this, "更新成功", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "更新 users 失敗: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "更新個人資料失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }


    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${System.currentTimeMillis()}.png")

        storageReference.putFile(imageUri)
            .addOnSuccessListener {
                Log.d("EditProfile", "Image uploaded successfully")
                // 获取下载链接
                storageReference.downloadUrl.addOnSuccessListener { fullUrl ->
                    Log.d("EditProfile", "Image URL: $fullUrl")
                    saveImageUrlToFirestore(fullUrl.toString())
                }.addOnFailureListener { e ->
                    Log.e("EditProfile", "Failed to get download URL: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Image upload failed: ${e.message}")
                Toast.makeText(this, "頭貼上傳失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val email = FirebaseAuth.getInstance().currentUser?.email

        if (email != null) {
            db.collection(email).document("個人資料")
                .update("頭貼圖片", imageUrl)
                .addOnSuccessListener {
                    Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "更新失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            if (userId != null) {
                db.collection("users").document(userId)
                    .update("頭貼圖片", imageUrl)
                    .addOnSuccessListener {
                        Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "更新失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
