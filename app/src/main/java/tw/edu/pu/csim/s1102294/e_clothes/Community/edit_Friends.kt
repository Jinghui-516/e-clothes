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
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.home

class edit_Friends : AppCompatActivity() {

    // RecyclerView Adapter for displaying friends
    class FriendsAdapter(
        private val friendsList: List<delete_friends_data>,
        private val onDeleteClick: (delete_friends_data) -> Unit
    ) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

        class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val friendImage: ShapeableImageView = itemView.findViewById(R.id.friend_image)
            private val friendName: TextView = itemView.findViewById(R.id.friend_name)
            private val friendEmail: TextView = itemView.findViewById(R.id.friend_email)

            fun bind(friend: delete_friends_data) {
                friendEmail.text = friend.email

                // Load friend data from Firestore
                FirebaseFirestore.getInstance()
                    .collection(friend.email)
                    .document("個人資料")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val friendNameValue = document.getString("使用者名稱") ?: "Unknown"
                            val friendProfileImageUrl = document.getString("頭貼圖片") ?: ""

                            friendName.text = friendNameValue
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
                        Log.e("Firestore", "Error fetching friend data: ", e)
                        friendName.text = "Error"
                        friendImage.setImageResource(R.drawable.user)
                    }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_delete_friends, parent, false)
            return FriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val friend = friendsList[position]
            holder.bind(friend)

            // Set click listener to navigate to other page
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, Other_Page::class.java).apply {
                    putExtra("username", friend.username)
                    putExtra("email", friend.email)
                    putExtra("profileImage", friend.profileImage)
                }
                holder.itemView.context.startActivity(intent)
            }

            // Set delete button click event
            holder.itemView.findViewById<View>(R.id.Delete).setOnClickListener {
                onDeleteClick(friend)
            }
        }

        override fun getItemCount(): Int = friendsList.size
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<delete_friends_data>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_friends)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        friendsRecyclerView = findViewById(R.id.recyclerViewFriends)

        // Set up RecyclerView and Adapter
        friendsAdapter = FriendsAdapter(friendsList) { friend -> showDeleteConfirmationDialog(friend) }
        friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.adapter = friendsAdapter

        // Load friends from Firestore
        loadFriendsFromFirestore()

        // Navigation setup
        setupNavigation()
    }

    private fun loadFriendsFromFirestore() {
        val currentUserEmail = auth.currentUser?.email ?: return

        db.collection("users").document(currentUserEmail)
            .collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                friendsList.clear()
                for (document in documents) {
                    val friendEmail = document.getString("email") ?: ""
                    val friendName = document.getString("name") ?: "Unknown"
                    val friendProfileImageUrl = document.getString("profileImageUrl") ?: ""

                    val friend = delete_friends_data(
                        email = friendEmail,
                        username = friendName,
                        profileImage = friendProfileImageUrl
                    )

                    friendsList.add(friend)
                }
                friendsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching friends: ", e)
                Toast.makeText(this, "Failed to retrieve friend list: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(friend: delete_friends_data) {
        AlertDialog.Builder(this)
            .setTitle("刪除好友")
            .setMessage("確定要刪除嗎？")
            .setPositiveButton("刪除") { _, _ -> deleteFriend(friend) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteFriend(friend: delete_friends_data) {
        // Firestore deletion logic
        val currentUserEmail = auth.currentUser?.email ?: return

        db.collection("users").document(currentUserEmail)
            .collection("friends")
            .document(friend.email) // Assuming each friend's email is the document ID
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${friend.username} 已刪除", Toast.LENGTH_SHORT).show()
                loadFriendsFromFirestore() // Reload friend list after deletion
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to remove friend: ${friend.email}", e)
                Toast.makeText(this, "刪除失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.Home).setOnClickListener {
            startActivity(Intent(this, home::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.Match).setOnClickListener {
            startActivity(Intent(this, Match_home::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.Friend).setOnClickListener {
            startActivity(Intent(this, Friends::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.menu_friend).setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.menu_friends, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        startActivity(Intent(this, add_Friends::class.java))
                        finish()
                        true
                    }
                    R.id.edit -> {
                        startActivity(Intent(this, edit_Friends::class.java))
                        finish()
                        true
                    }
                    R.id.invit -> {
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
}
