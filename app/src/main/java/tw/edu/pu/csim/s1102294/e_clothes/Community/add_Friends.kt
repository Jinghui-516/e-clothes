package tw.edu.pu.csim.s1102294.e_clothes.Community

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class add_Friends : AppCompatActivity() {

    private lateinit var matchButton: ImageView
    private lateinit var homeButton: ImageView
    private lateinit var friendButton: ImageView
    private lateinit var clothesButton: ImageView
    private lateinit var settingsButton: ImageView

    private lateinit var searchFriendEditText: EditText
    private lateinit var searchFriendButton: Button
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friends)

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

        // Initialize views
        searchFriendEditText = findViewById(R.id.searchFriendEditText)
        searchFriendButton = findViewById(R.id.searchFriendButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)

        // Setup RecyclerView
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsAdapter = FriendsAdapter(listOf())
        searchResultsRecyclerView.adapter = friendsAdapter

        auth = FirebaseAuth.getInstance()

        // Setup search button click event
        searchFriendButton.setOnClickListener {
            val email = searchFriendEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                searchFriendByEmail(email)
            } else {
                Toast.makeText(this, "請輸入Email", Toast.LENGTH_SHORT).show()
            }
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

    private fun searchFriendByEmail(email: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("email", email)  // Search based on 'email' field
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val userList = querySnapshot.documents.mapNotNull { documentSnapshot ->
                        // Fetch fields correctly
                        val username = documentSnapshot.getString("使用者名稱") ?: ""
                        val profileImage = documentSnapshot.getString("頭貼圖片") ?: ""
                        val email = documentSnapshot.getString("email") ?: ""

                        // Create User object
                        User(email, username, profileImage)
                    }

                    // Update adapter with user list
                    friendsAdapter = FriendsAdapter(userList)
                    searchResultsRecyclerView.adapter = friendsAdapter
                } else {
                    Toast.makeText(this, "找不到該使用者", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "搜尋失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    class FriendsAdapter(private val friendList: List<User>) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
            return FriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val user = friendList[position]
            holder.bind(user)

            // Set click listener for navigating to friend's profile
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, Other_Page::class.java).apply {
                    putExtra("username", user.username)
                    putExtra("email", user.email)
                    putExtra("profileImage", user.profileImage)
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return friendList.size
        }

        class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val friendImage: ImageView = itemView.findViewById(R.id.friend_image)
            private val friendName: TextView = itemView.findViewById(R.id.friend_name)
            private val friendEmail: TextView = itemView.findViewById(R.id.friend_email)

            fun bind(user: User) {
                friendName.text = user.username
                friendEmail.text = user.email
                loadImageFromUrl(friendImage, user.profileImage)
            }

            private fun loadImageFromUrl(imageView: ImageView, imageUrl: String) {
                if (imageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.user)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
        }
    }
}
