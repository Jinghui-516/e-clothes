package tw.edu.pu.csim.s1120336.e_fit.Match

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import tw.edu.pu.csim.s1120336.e_fit.R

// 🌟 1. 定義每則留言要有什麼資料
data class Comment(
    val userName: String = "",
    val userAvatar: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

class CommentActivity : AppCompatActivity() {

    private lateinit var rvComments: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var postId: String = ""
    private var currentUserName = "匿名使用者"
    private var currentUserAvatar = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        // 接收點擊貼文傳過來的貼文 ID
        postId = intent.getStringExtra("POST_ID") ?: ""

        rvComments = findViewById(R.id.rv_comments)
        etInput = findViewById(R.id.et_comment_input)
        btnSend = findViewById(R.id.btn_send_comment)
        btnBack = findViewById(R.id.btn_back)

        rvComments.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener { finish() }

        // 🌟 抓取發言者（自己）的暱稱和頭貼
        fetchMyProfile()

        // 🌟 即時監聽雲端資料庫裡的留言
        if (postId.isNotEmpty()) {
            listenForComments()
        }

        // 🌟 發送留言
        btnSend.setOnClickListener {
            sendComment()
        }
    }

    private fun fetchMyProfile() {
        val email = auth.currentUser?.email ?: return
        db.collection(email).document("個人資料").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                currentUserName = doc.getString("使用者名稱") ?: "匿名使用者"
                currentUserAvatar = doc.getString("頭貼圖片") ?: ""
            }
        }
    }

    private fun listenForComments() {
        // 在這篇貼文底下，建立一個子集合叫做 "Comments"
        db.collection("AllPosts").document(postId).collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val commentList = mutableListOf<Comment>()
                for (doc in snapshot!!) {
                    val comment = doc.toObject(Comment::class.java)
                    commentList.add(comment)
                }

                rvComments.adapter = CommentAdapter(commentList)
                // 如果有新留言，自動滑動到最底下
                if (commentList.isNotEmpty()) {
                    rvComments.scrollToPosition(commentList.size - 1)
                }
            }
    }

    private fun sendComment() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return

        btnSend.isEnabled = false // 防連點

        val newComment = Comment(
            userName = currentUserName,
            userAvatar = currentUserAvatar,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        db.collection("AllPosts").document(postId).collection("Comments")
            .add(newComment)
            .addOnSuccessListener {
                etInput.text.clear() // 清空輸入框
                btnSend.isEnabled = true

                // 🌟 核心魔法：留言成功後，同步更新主貼文的留言數與預覽清單！
                val postRef = db.collection("AllPosts").document(postId)
                val previewText = "$currentUserName: $text" // 組合預覽文字

                postRef.update(
                    "commentCount", com.google.firebase.firestore.FieldValue.increment(1),
                    "previewComments", com.google.firebase.firestore.FieldValue.arrayUnion(previewText)
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "留言失敗，請重試", Toast.LENGTH_SHORT).show()
                btnSend.isEnabled = true
            }
    }
}

// --- 內部類別：處理畫面上的留言列表 (Adapter) ---
class CommentAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    inner class CommentViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: ImageView = v.findViewById(R.id.iv_comment_avatar)
        val tvName: TextView = v.findViewById(R.id.tv_comment_name)
        val tvText: TextView = v.findViewById(R.id.tv_comment_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvName.text = comment.userName
        holder.tvText.text = comment.text
        Glide.with(holder.itemView.context)
            .load(comment.userAvatar)
            .placeholder(R.drawable.user)
            .into(holder.ivAvatar)
    }

    override fun getItemCount() = comments.size
}