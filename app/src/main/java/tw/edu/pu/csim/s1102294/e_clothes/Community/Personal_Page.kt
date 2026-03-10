package tw.edu.pu.csim.s1102294.e_clothes.Community

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class Personal_Page : AppCompatActivity() {

    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var Clothes: ImageView
    lateinit var set: ImageView

    // Declare the UI elements
    lateinit var nameTextView: TextView
    lateinit var birthdayTextView: TextView
    lateinit var signatureTextView: TextView
    lateinit var circularImageView: ShapeableImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_page)

        // Initialize Firestore and get the current user's ID
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val email = FirebaseAuth.getInstance().currentUser?.email

        // Bind UI elements to their respective views in the layout
        nameTextView = findViewById(R.id.nameTextView)
        birthdayTextView = findViewById(R.id.birthdayTextView)
        signatureTextView = findViewById(R.id.signatureTextView)
        circularImageView = findViewById(R.id.circularImageView)

        // Ensure that userId is not null
        if (email != null) {
            // Access the "個人資料" document inside the user's Firestore collection
            val documentRef = db.collection(email).document("個人資料")

            // Fetch the document and handle the response
            documentRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Retrieve the user's details from the document
                        val name = document.getString("使用者名稱")
                        val birthday = document.getString("生日")
                        val signature = document.getString("個性簽名")
                        val gender = document.getString("性別")
                        val profileImageUrl = document.getString("頭貼圖片")

                        // Display the retrieved details in the TextViews
                        nameTextView.text = "@${name ?: "Name not found"}"
                        birthdayTextView.text = "生日：${birthday ?: "Birthday not found"} _ ${gender ?: "Gender not found"}"
                        signatureTextView.text = "個性簽名：${signature ?: "Signature not found"}"

                        // Load the profile image into the circular ImageView using Picasso
                        if (profileImageUrl != null) {
                            Picasso.get().load(profileImageUrl).into(circularImageView)
                        }
                    } else {
                        Log.d("Firestore", "Document does not exist")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting document: ", exception)
                }
        } else {
            Log.w("Firestore", "User ID is null")
        }

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
            val intent1 = Intent(this, Personal_Page::class.java)
            startActivity(intent1)
            finish()
        }

        set = findViewById(R.id.set)
        set.setOnClickListener {
            val intent1 = Intent(this, Setting::class.java)
            startActivity(intent1)
            finish()
        }
    }
}
