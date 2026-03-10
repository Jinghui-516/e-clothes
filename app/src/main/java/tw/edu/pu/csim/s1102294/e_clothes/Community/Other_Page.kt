package tw.edu.pu.csim.s1102294.e_clothes.Community

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class Other_Page : AppCompatActivity() {

    lateinit var add_friend: Button
    lateinit var block_account: Button
    lateinit var Match: ImageView
    lateinit var Clothes: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView

    lateinit var profile_image: ShapeableImageView
    lateinit var nameTextView: TextView
    lateinit var birthdayTextView: TextView
    lateinit var signatureTextView: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_page)

        profile_image = findViewById(R.id.profile_image)
        nameTextView = findViewById(R.id.nameTextView)
        birthdayTextView = findViewById(R.id.birthdayTextView)
        signatureTextView = findViewById(R.id.signatureTextView)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val friend_email = intent.getStringExtra("email") ?: ""    //friend mail
        val profileImageUrl = intent.getStringExtra("profileImage") ?: ""
        Log.e("頭貼", profileImageUrl)

        if (profileImageUrl.isNotEmpty()) {
            // Load image using Picasso
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)  // Placeholder if URL is loading
                .error(R.drawable.user)  // Error image if URL fails
                .into(profile_image)
        } else {
            // Default image if URL is empty or null
            profile_image.setImageResource(R.drawable.user)
        }
        if (userId != null) {
            // Access the "個人資料" document inside the user's Firestore collection
            val documentRef = db.collection(friend_email).document("個人資料")

            // Fetch the document and handle the response
            documentRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Retrieve the user's details from the document
                        val name = document.getString("使用者名稱")
                        val birthday = document.getString("生日")
                        val signature = document.getString("個性簽名")
                        val gender = document.getString("性別")
                        val personalImage = document.getString("頭貼圖片")

                        // Display the retrieved details in the TextViews
                        nameTextView.text = "@${name ?: "Name not found"}"
                        birthdayTextView.text = "生日：${birthday ?: "Birthday not found"} _ ${gender ?: "Gender not found"}"
                        signatureTextView.text = "個性簽名：${signature ?: "Signature not found"}"
                        if (!personalImage.isNullOrEmpty()) {
                            Picasso.get()
                                .load(personalImage)
                                .placeholder(R.drawable.ic_launcher_foreground)  // Show this while loading
                                .error(R.drawable.user)  // Show this if the image URL is invalid
                                .into(profile_image)
                        } else {
                            // Set a default image if the URL is empty or null
                            profile_image.setImageResource(R.drawable.user)
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

        add_friend = findViewById(R.id.add_friend)
        val currentUserEmail = auth.currentUser?.email

        // Check if already friends
        if (currentUserEmail != null) {
            checkIfFriends(currentUserEmail, friend_email) { areFriends ->
                if (areFriends) {
                    // Already friends
                    add_friend.isEnabled = false
                    add_friend.text = "已成為好友"
                } else {
                    // Check if a friend request already exists
                    checkFriendRequestExists(currentUserEmail, friend_email) { exists ->
                        if (exists) {
                            // Friend request already sent
                            add_friend.isEnabled = false
                            add_friend.text = "已發送好友邀請"
                        } else {
                            // No friend request sent, enable the button
                            add_friend.isEnabled = true
                            add_friend.text = "發送好友邀請"
                        }
                    }
                }
            }
        }

        add_friend.setOnClickListener {
            val receiver = friend_email  // 對方的 email
            if (currentUserEmail != null) {
                checkFriendRequestExists(currentUserEmail, receiver) { exists ->
                    if (exists) {
                        Toast.makeText(this, "已發送好友邀請，無法重複發送", Toast.LENGTH_SHORT).show()
                    } else {
                        sendFriendRequest(currentUserEmail, receiver)  // 發送好友請求
                        add_friend.isEnabled = false  // 停用按鈕，避免重複點擊
                        add_friend.text = "已發送好友邀請"
                    }
                }
            } else {
                Toast.makeText(this, "發送邀請失敗，請重新嘗試", Toast.LENGTH_SHORT).show()
            }
        }

        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
            val intent2 = Intent(this, Match_home::class.java)
            startActivity(intent2)
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
            val intent3 = Intent(this, choose_add::class.java)
            startActivity(intent3)
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }
    }

    private fun checkFriendRequestExists(sender: String, receiver: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        // 檢查是否已存在好友請求
        db.collection("friend_requests")
            .whereEqualTo("sender", sender)
            .whereEqualTo("receiver", receiver)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.size() > 0)  // 如果查詢結果的大小大於0，則表示已發送過好友邀請
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error checking friend request: ", e)
                callback(false)  // 如果出現錯誤，默認返回 false
            }
    }

    private fun checkIfFriends(userEmail: String, friendEmail: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val currentUserEmail = auth.currentUser?.email

        // 檢查兩個用戶是否已成為好友
        if (currentUserEmail != null) {
            db.collection("users").document(currentUserEmail)
                .collection("friends")
                .get()
                .addOnSuccessListener { documents ->
                    callback(documents.size() > 0)  // 如果查詢結果的大小大於0，表示已成為好友
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error checking friends: ", e)
                    callback(false)  // 如果出現錯誤，默認返回 false
                }
        }
    }

    private fun sendFriendRequest(sender: String, receiver: String) {
        val db = FirebaseFirestore.getInstance()

        // 準備好友請求的資料
        val friendRequest = hashMapOf(
            "sender" to sender,         // 發送者的 email
            "receiver" to receiver,     // 接收者的 email
            "status" to "pending",      // 狀態：等待回應
            "timestamp" to System.currentTimeMillis()  // 發送的時間戳
        )

        // 將好友請求資料添加到 Firestore 的 'friend_requests' 集合
        db.collection("friend_requests")
            .add(friendRequest)
            .addOnSuccessListener {
                // 顯示成功訊息
                Toast.makeText(this, "好友邀請已發送", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // 若發送失敗，顯示錯誤訊息並重新啟用按鈕
                Toast.makeText(this, "發送好友邀請失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                add_friend.isEnabled = true  // 發送失敗時，重新啟用按鈕
                add_friend.text = "發送好友邀請"
            }
    }
}
