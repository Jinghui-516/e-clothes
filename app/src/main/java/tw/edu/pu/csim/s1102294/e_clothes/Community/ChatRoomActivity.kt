package tw.edu.pu.csim.s1120336.e_fit.Match

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import tw.edu.pu.csim.s1120336.e_fit.R

// 🌟 統一在這裡定義，確保沒有匯入衝突
data class ChatMessage(
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

class ChatRoomActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private var friendEmail = ""
    private var friendName = ""
    private var friendAvatar = ""
    private var roomId = ""

    lateinit var rvMessages: RecyclerView
    lateinit var etMessage: EditText
    lateinit var btnSend: ImageView
    lateinit var tvChatTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        friendEmail = intent.getStringExtra("FRIEND_EMAIL") ?: ""
        friendName = intent.getStringExtra("FRIEND_NAME") ?: "聊天室"
        friendAvatar = intent.getStringExtra("FRIEND_AVATAR") ?: ""
        roomId = if (myEmail < friendEmail) "${myEmail}_${friendEmail}" else "${friendEmail}_${myEmail}"

        tvChatTitle = findViewById(R.id.tv_chat_title)
        tvChatTitle.text = friendName
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
        rvMessages = findViewById(R.id.rv_messages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }
        listenForMessages()
    }

    private fun sendMessage(text: String) {
        val timestamp = System.currentTimeMillis()
        val msg = ChatMessage(myEmail, text, timestamp)
        db.collection("Chats").document(roomId).collection("Messages").add(msg)

        val chatDataForMe = hashMapOf("lastMessage" to text, "timestamp" to timestamp, "friendEmail" to friendEmail, "friendName" to friendName, "friendAvatar" to friendAvatar)
        val chatDataForFriend = hashMapOf("lastMessage" to text, "timestamp" to timestamp, "friendEmail" to myEmail, "friendName" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "我"), "friendAvatar" to "")

        db.collection(myEmail).document("RecentChats").collection("Rooms").document(friendEmail).set(chatDataForMe)
        db.collection(friendEmail).document("RecentChats").collection("Rooms").document(myEmail).set(chatDataForFriend)
        etMessage.text.clear()
    }

    private fun listenForMessages() {
        db.collection("Chats").document(roomId).collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                rvMessages.adapter = ChatAdapter(list)
                if (list.isNotEmpty()) rvMessages.scrollToPosition(list.size - 1)
            }
    }

    // 🌟 直接內嵌在同一個檔案裡，確保 Adapter 認得 ChatMessage
    inner class ChatAdapter(private val list: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = list[position]
            val layoutLeft = holder.itemView.findViewById<View>(R.id.layout_left)
            val layoutRight = holder.itemView.findViewById<View>(R.id.layout_right)

            if (msg.sender == myEmail) {
                layoutRight.visibility = View.VISIBLE
                layoutLeft.visibility = View.GONE
                holder.itemView.findViewById<TextView>(R.id.tv_right_message).text = msg.text
            } else {
                layoutRight.visibility = View.GONE
                layoutLeft.visibility = View.VISIBLE
                holder.itemView.findViewById<TextView>(R.id.tv_left_message).text = msg.text
                Glide.with(holder.itemView.context).load(friendAvatar).placeholder(R.drawable.user).into(holder.itemView.findViewById(R.id.iv_friend_avatar))
            }
        }
        override fun getItemCount() = list.size
    }
}