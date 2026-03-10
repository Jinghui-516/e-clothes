package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import tw.edu.pu.csim.s1102294.e_clothes.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import android.widget.TextView
import android.widget.ImageView
import com.bumptech.glide.Glide // Or your preferred image loading library
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class Matching_details : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    // UI elements
    private lateinit var weatherTextView: TextView
    private lateinit var tagsTextView: TextView
//    private lateinit var hatImageView: ImageView
//    private lateinit var clothesImageView: ImageView
//    private lateinit var pantsImageView: ImageView
//    private lateinit var shoesImageView: ImageView

    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var Clothes: ImageView
    lateinit var set: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching_details)  // Assuming layout name is correct

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

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

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
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

        // Initialize UI elements
        weatherTextView = findViewById(R.id.textView22)  // Assuming the ID for weather text view
        tagsTextView = findViewById(R.id.textView20)  // Assuming the ID for tags text view
//        hatImageView = findViewById(R.id.imghat)  // Assuming the ID for hat image view
//        clothesImageView = findViewById(R.id.imgclothes)  // Assuming the ID for clothes image view
//        pantsImageView = findViewById(R.id.imgpants)  // Assuming the ID for pants image view
//        shoesImageView = findViewById(R.id.imgshoes)  // Assuming the ID for shoes image view

        // Get data from Firestore
//        val matchData = intent.getStringExtra("matchData") // Assuming you passed the matchId in the intent
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email
        val documentName = intent.getStringExtra("documentName")
        if (currentUser != null && email != null) {
            if (documentName != null) {
                db.collection(email).document(documentName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            // 取得天氣種類欄位
                            val weather = document.getString("天氣種類") ?: "Unknown"

                            // 取得標籤，並處理為安全類型
                            val tags = (document["標籤"] as? List<*>)?.filterIsInstance<String>()
                                ?: listOf("No tags")

                            val tagCount = tags.size
                            Log.d("FirestoreDebug", "Total number of tags: $tagCount")
                            // Log 標籤資料
                            if (tags.isEmpty()) {
                                Log.d("FirestoreDebug", "Tags are empty")
                            } else {
                                Log.d("FirestoreDebug", "Tags: $tags")
                                tags.forEachIndexed { index, tag ->
                                    Log.d("FirestoreDebug", "Index $index: $tag")
                                }
                            }

                            // 取得圖片 URL
                            val hatImageUrl = document.getString("hatImage")
                            val clothesImageUrl = document.getString("clothesImage")
                            val pantsImageUrl = document.getString("pantsImage")
                            val shoesImageUrl = document.getString("shoesImage")

                            // 更新 UI
                            weatherTextView.text = "天氣：$weather"
                            tagsTextView.text = "標籤：" + tags.joinToString(", ")

                            // 紀錄完整文件資料
                            Log.d("FirestoreDebug", "Document data: ${document.data}")
                        } else {
                            Log.e("Firestore", "Document does not exist")
                        }
                    }
                    .addOnFailureListener { exception ->
                        // 錯誤處理
                        Log.e("Firestore", "Error getting document", exception)
                    }
            }
        } else {
            Log.e("Firestore", "User or email is null")
        }
    }

        // Function to retrieve match data from Firestore

}
