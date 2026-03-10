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
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home

class Friend_Invite : AppCompatActivity() {

    lateinit var Home: ImageView
    lateinit var Match: ImageView
    lateinit var Clothes: ImageView
    lateinit var Friend: ImageView
    lateinit var set: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var friendRequestAdapter: FriendRequestAdapter
    private lateinit var recyclerView: RecyclerView
    private val friendRequests = mutableListOf<FriendRequest>()

    class FriendRequestAdapter(
        private val friendRequests: List<FriendRequest>,
        private val onAcceptClick: (FriendRequest) -> Unit,
        private val onDeclineClick: (FriendRequest) -> Unit
    ) : RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_friend_request, parent, false)
            return FriendRequestViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
            val friendRequest = friendRequests[position]
            holder.bind(friendRequest, onAcceptClick, onDeclineClick)
        }

        override fun getItemCount(): Int {
            return friendRequests.size
        }

        class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val friendImage: ShapeableImageView = itemView.findViewById(R.id.friend_image)
            private val friendName: TextView = itemView.findViewById(R.id.friend_name)
            private val friendEmail: TextView = itemView.findViewById(R.id.friend_email)
            private val acceptButton: ShapeableImageView = itemView.findViewById(R.id.accept)
            private val declineButton: ShapeableImageView = itemView.findViewById(R.id.decline)
            private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

            fun bind(
                friendRequest: FriendRequest,
                onAcceptClick: (FriendRequest) -> Unit,
                onDeclineClick: (FriendRequest) -> Unit
            ) {
                // 設定電子郵件
                friendEmail.text = friendRequest.sender

                // 查詢 Firestore 中的用戶詳細信息 (從 friendRequest.sender 集合)
                if (!friendRequest.sender.isNullOrEmpty()) {
                    db.collection(friendRequest.sender)
                        .document("個人資料")
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val senderName = document.getString("使用者名稱") ?: "Unknown"
                                val senderProfileImageUrl = document.getString("頭貼圖片") ?: ""

                                // 設定發送者的名稱
                                friendName.text = senderName

                                // 加載頭像圖片 (檢查 URL 是否為空)
                                if (!senderProfileImageUrl.isNullOrEmpty()) {
                                    Picasso.get()
                                        .load(senderProfileImageUrl)
                                        .placeholder(R.drawable.ic_launcher_foreground) // 可選的占位符
                                        .error(R.drawable.user) // 頭像加載錯誤時顯示的預設圖片
                                        .into(friendImage)
                                } else {
                                    // 如果 URL 為空，顯示預設圖片
                                    friendImage.setImageResource(R.drawable.user)
                                }
                            } else {
                                // 如果文件不存在，顯示未知名稱和預設圖片
                                friendName.text = "Unknown"
                                friendImage.setImageResource(R.drawable.user)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error fetching sender data: ", e)
                            friendName.text = "Error"
                            friendImage.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                } else {
                    friendName.text = "Unknown"
                    friendImage.setImageResource(R.drawable.user)
                }

                // 接受按鈕的點擊事件
                acceptButton.setOnClickListener {
                    onAcceptClick(friendRequest)
                }

                // 拒絕按鈕的點擊事件
                declineButton.setOnClickListener {
                    onDeclineClick(friendRequest)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_invite)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        friendRequestAdapter = FriendRequestAdapter(friendRequests,
            onAcceptClick = { request ->
                acceptFriendRequest(request)
            },
            onDeclineClick = { request ->
                declineFriendRequest(request)
            }
        )

        recyclerView.adapter = friendRequestAdapter

        // 取得並顯示好友邀請
        fetchFriendRequests()

        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, Match_home::class.java)
            startActivity(intent1)
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
            startActivity(Intent(this, choose_add::class.java))
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }
        set = findViewById(R.id.set)
        set.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
            finish()
        }

        val menu_catch = findViewById<ImageView>(R.id.menu_friend)
        menu_catch.setOnClickListener {
            val popup = PopupMenu(this, menu_catch)
            popup.menuInflater.inflate(R.menu.menu_friends, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        Toast.makeText(this, "新增好友", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, add_Friends::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.edit -> {
                        Toast.makeText(this, "編輯好友", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, edit_Friends::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.invit -> {
                        Toast.makeText(this, "查看好友邀請", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, Friend_Invite::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    private fun fetchFriendRequests() {
        val currentUserEmail = auth.currentUser?.email

        if (currentUserEmail != null) {
            db.collection("friend_requests")
                .whereEqualTo("receiver", currentUserEmail)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { documents ->
                    friendRequests.clear()
                    for (document in documents) {
                        val senderEmail = document.getString("sender") ?: ""
                        val receiverEmail = document.getString("receiver") ?: ""
                        val status = document.getString("status") ?: ""
                        val timestamp = document.getLong("timestamp") ?: 0L

                        val request = FriendRequest(senderEmail, receiverEmail, status, timestamp)
                        friendRequests.add(request)
                    }
                    friendRequestAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error getting friend requests: ", e)
                    Toast.makeText(this, "獲取好友邀請失敗", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun acceptFriendRequest(request: FriendRequest) {
        db.collection("friend_requests")
            .whereEqualTo("sender", request.sender)
            .whereEqualTo("receiver", request.receiver)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // 更新好友邀請的狀態
                    document.reference.update("status", "accepted")
                }
                // 添加好友到雙方的好友列表
                addFriend(request.sender, request.receiver)
                Toast.makeText(this, "已接受好友請求", Toast.LENGTH_SHORT).show()
                // 移除已接受的請求
                friendRequests.remove(request)
                friendRequestAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error accepting friend request: ", e)
                Toast.makeText(this, "接受好友請求失敗", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineFriendRequest(request: FriendRequest) {
        db.collection("friend_requests")
            .whereEqualTo("sender", request.sender)
            .whereEqualTo("receiver", request.receiver)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // 更新好友邀請的狀態
                    document.reference.update("status", "declined")
                }
                Toast.makeText(this, "已拒絕好友請求", Toast.LENGTH_SHORT).show()
                // 移除已拒絕的請求
                friendRequests.remove(request)
                friendRequestAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error declining friend request: ", e)
                Toast.makeText(this, "拒絕好友請求失敗", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addFriend(user1: String, user2: String) {
        // 將 user2 添加到 user1 的好友列表
        db.collection("users").document(user1)
            .collection("friends")
            .document(user2)
            .set(mapOf("email" to user2))
            .addOnSuccessListener {
                // 將 user1 添加到 user2 的好友列表
                db.collection("users").document(user2)
                    .collection("friends")
                    .document(user1)
                    .set(mapOf("email" to user1))
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding friend: ", e)
            }
    }
}
