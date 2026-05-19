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

    data class ChatRoom(val friendEmail: String, val friendName: String, val friendAvatar: String)

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
                    ChatRoom(doc.getString("friendEmail")!!, doc.getString("friendName")!!, doc.getString("friendAvatar")!!)
                } ?: emptyList()
                rvChatList.adapter = ChatListAdapter(chatRooms)
            }
    }

    inner class ChatListAdapter(private val list: List<ChatRoom>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val room = list[position]
            holder.itemView.findViewById<TextView>(R.id.tv_friend_name).text = room.friendName
            Glide.with(holder.itemView.context).load(room.friendAvatar).placeholder(R.drawable.user).into(holder.itemView.findViewById(R.id.iv_friend_avatar))
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