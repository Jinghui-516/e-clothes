package tw.edu.pu.csim.s1102294.e_clothes.Community

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.Match.New_Match
import tw.edu.pu.csim.s1102294.e_clothes.Match.share_Match
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class Friends : AppCompatActivity() {

    private lateinit var matchButton: ImageView
    private lateinit var homeButton: ImageView
    private lateinit var friendButton: ImageView
    private lateinit var clothesButton: ImageView
    private lateinit var settingsButton: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var friendAdapter: FriendAdapter
    private lateinit var recyclerView: RecyclerView
    private val friendList = mutableListOf<friend_data>()

    inner class FriendAdapter(private val friends: List<friend_data>) :
        RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_all_friend, parent, false)
            return FriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val friend = friends[position]
            holder.bind(friend)

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, Other_Page::class.java).apply {
                    putExtra("username", friend.username)
                    putExtra("email", friend.email)
                    putExtra("profileImage", friend.profileImage)
                    Log.e("測試圖片",friend.profileImage)
                    Log.e("測試",friend.username)
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = friends.size

        inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val friendImage: ShapeableImageView = itemView.findViewById(R.id.friend_image)
            private val friendName: TextView = itemView.findViewById(R.id.friend_name)
            private val friendEmail: TextView = itemView.findViewById(R.id.friend_email)

            fun bind(friend: friend_data) {
                friendEmail.text = friend.email

                db.collection(friend.email)
                    .document("個人資料")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val friendNameValue = document.getString("使用者名稱") ?: "Unknown"
                            val friendProfileImageUrl = document.getString("頭貼圖片") ?: ""

                            friendName.text = friendNameValue
                            Log.e("圖片",friendProfileImageUrl)

                            if (friendProfileImageUrl.isNotEmpty()) {
                                Picasso.get()
                                    .load(friendProfileImageUrl)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .error(R.drawable.user)
                                    .into(friendImage)
                            } else {
                                friendImage.setImageResource(R.drawable.user)
                            }
                        } else {
                            friendName.text = "Unknown"
                            friendImage.setImageResource(R.drawable.user)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Error fetching friend data: ", e)
                        friendName.text = "Error"
                        friendImage.setImageResource(R.drawable.user)
                    }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerViewFriends)
        recyclerView.layoutManager = LinearLayoutManager(this)

        friendAdapter = FriendAdapter(friendList)
        recyclerView.adapter = friendAdapter

        fetchFriends()

        homeButton = findViewById(R.id.Home)
        homeButton.setOnClickListener {
            startActivity(Intent(this, home::class.java))
            finish()
        }

        matchButton = findViewById(R.id.Match)
        matchButton.setOnClickListener {
            startActivity(Intent(this, Match_home::class.java))
            finish()
        }

        clothesButton = findViewById(R.id.Clothes)
        clothesButton.setOnClickListener {
            startActivity(Intent(this, choose_add::class.java))
            finish()
        }

        friendButton = findViewById(R.id.Friend)
        friendButton.setOnClickListener {
            startActivity(Intent(this, Friends::class.java))
            finish()
        }

        settingsButton = findViewById(R.id.set)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
            finish()
        }

        val menuCatch = findViewById<ImageView>(R.id.menu_friend)
        menuCatch.setOnClickListener {
            val popup = PopupMenu(this, menuCatch)
            popup.menuInflater.inflate(R.menu.menu_friends, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        Toast.makeText(this, "新增好友", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, add_Friends::class.java))
                        finish()
                        true
                    }
                    R.id.edit -> {
                        Toast.makeText(this, "編輯好友", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, edit_Friends::class.java))
                        finish()
                        true
                    }
                    R.id.invit -> {
                        Toast.makeText(this, "查看好友邀請", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Friend_Invite::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun fetchFriends() {
        val currentUserEmail = auth.currentUser?.email

        if (currentUserEmail != null) {
            db.collection("users").document(currentUserEmail)
                .collection("friends")
                .get()
                .addOnSuccessListener { documents ->
                    friendList.clear()
                    for (document in documents) {
                        val friendEmail = document.getString("email") ?: ""
                        val friendName = document.getString("name") ?: "Unknown"
                        val friendProfileImageUrl = document.getString("profileImageUrl") ?: ""

                        val friend = friend_data(
                            email = friendEmail,
                            username = friendName,
                            profileImage = friendProfileImageUrl
                        )

                        friendList.add(friend)
                    }
                    friendAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error fetching friends: ", e)
                    Toast.makeText(this, "Failed to retrieve friend list", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
