package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import tw.edu.pu.csim.s1120336.e_fit.R

class ChatListActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private lateinit var rvChatList: RecyclerView

    data class ChatRoom(val friendEmail: String, var friendName: String, var friendAvatar: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        findViewById<ImageView>(R.id.btn_back_to_match).setOnClickListener { finish() }
        rvChatList = findViewById(R.id.rv_chat_list)
        rvChatList.layoutManager = LinearLayoutManager(this)

        db.collection(myEmail).document("RecentChats").collection("Rooms")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val chatRooms = snapshot?.map { doc ->
                    ChatRoom(
                        friendEmail = doc.getString("friendEmail") ?: "",
                        friendName = doc.getString("friendName") ?: "使用者",
                        friendAvatar = doc.getString("friendAvatar") ?: ""
                    )
                } ?: emptyList()

                if (chatRooms.isEmpty()) {
                    rvChatList.adapter = ChatListAdapter(emptyList())
                    return@addSnapshotListener
                }

                // 🌟 動態去抓取每個好友最新的真實暱稱與頭貼，覆蓋掉錯誤的「我」
                var count = 0
                for (room in chatRooms) {
                    db.collection(room.friendEmail).document("個人資料").get()
                        .addOnSuccessListener { fDoc ->
                            if (fDoc.exists()) {
                                room.friendName = fDoc.getString("使用者名稱") ?: room.friendName
                                room.friendAvatar = fDoc.getString("頭貼圖片") ?: room.friendAvatar
                            }
                            count++
                            if (count == chatRooms.size) {
                                rvChatList.adapter = ChatListAdapter(chatRooms)
                            }
                        }
                        .addOnFailureListener {
                            count++
                            if (count == chatRooms.size) {
                                rvChatList.adapter = ChatListAdapter(chatRooms)
                            }
                        }
                }
            }
    }

    inner class ChatListAdapter(private val list: List<ChatRoom>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val room = list[position]
            holder.itemView.findViewById<TextView>(R.id.tv_friend_name).text = room.friendName

            // 載入真實頭貼，若無則顯示預設圖
            Glide.with(holder.itemView.context)
                .load(room.friendAvatar)
                .placeholder(R.drawable.user)
                .into(holder.itemView.findViewById(R.id.iv_friend_avatar))

            holder.itemView.setOnClickListener {
                startActivity(Intent(this@ChatListActivity, ChatRoomActivity::class.java).apply {
                    putExtra("FRIEND_EMAIL", room.friendEmail)
                    putExtra("FRIEND_NAME", room.friendName)
                    putExtra("FRIEND_AVATAR", room.friendAvatar)
                })
            }
        }
        override fun getItemCount() = list.size
    }
}