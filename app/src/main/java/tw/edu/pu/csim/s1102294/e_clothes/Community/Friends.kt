package tw.edu.pu.csim.s1120336.e_fit.Community

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1120336.e_fit.R

class Friends : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var searchedEmail: String = "" // 記住搜到的 Email

    lateinit var etSearch: EditText
    lateinit var btnSearch: Button
    lateinit var cardResult: MaterialCardView
    lateinit var tvResultName: TextView
    lateinit var ivResultAvatar: ImageView
    lateinit var btnAddFriend: Button
    lateinit var rvFriendsList: RecyclerView

    // 資料結構：存放好友 Email、名字、頭貼
    data class FriendData(val email: String, val name: String, val avatar: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        etSearch = findViewById(R.id.et_search_email)
        btnSearch = findViewById(R.id.btn_search)
        cardResult = findViewById(R.id.card_search_result)
        tvResultName = findViewById(R.id.tv_result_name)
        ivResultAvatar = findViewById(R.id.iv_result_avatar)
        btnAddFriend = findViewById(R.id.btn_add_friend)

        rvFriendsList = findViewById(R.id.rv_friends_list)
        rvFriendsList.layoutManager = LinearLayoutManager(this)

        // 1. 搜尋按鈕邏輯
        btnSearch.setOnClickListener {
            val emailToSearch = etSearch.text.toString().trim()
            if (emailToSearch.isNotEmpty()) {
                searchUserInFirestore(emailToSearch)
            } else {
                Toast.makeText(this, "請輸入 Email", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 加好友按鈕邏輯
        btnAddFriend.setOnClickListener {
            addFriendToDatabase(searchedEmail)
        }

        // 3. 一開畫面就去撈取現有的好友列表
        loadMyFriendsList()
    }

    // 去雲端找這個 Email 的人存不存在
    private fun searchUserInFirestore(email: String) {
        val myEmail = auth.currentUser?.email ?: return
        if (email == myEmail) {
            Toast.makeText(this, "不能加自己為好友啦！", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection(email).document("個人資料").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 搜到了！顯示卡片
                    searchedEmail = email
                    cardResult.visibility = View.VISIBLE
                    tvResultName.text = "@${doc.getString("使用者名稱") ?: "未設定"}"
                    val avatarUrl = doc.getString("頭貼圖片")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this).load(avatarUrl).into(ivResultAvatar)
                    }
                } else {
                    cardResult.visibility = View.GONE
                    Toast.makeText(this, "找不到這個用戶喔！", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 把對方加入自己的好友名單，也把自己加入對方的好友名單 (雙向加好友)
    private fun addFriendToDatabase(friendEmail: String) {
        val myEmail = auth.currentUser?.email ?: return

        // 用 Firebase 的 arrayUnion 把好友 Email 塞進 myFriends 陣列中
        db.collection(myEmail).document("friends")
            .set(hashMapOf("list" to FieldValue.arrayUnion(friendEmail)), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                // 順便把自己的 Email 也塞進對方的名單裡
                db.collection(friendEmail).document("friends")
                    .set(hashMapOf("list" to FieldValue.arrayUnion(myEmail)), com.google.firebase.firestore.SetOptions.merge())

                Toast.makeText(this, "加好友成功！", Toast.LENGTH_SHORT).show()
                cardResult.visibility = View.GONE
                etSearch.text.clear()
                loadMyFriendsList() // 刷新列表
            }
    }

    // 讀取好友清單並顯示
    private fun loadMyFriendsList() {
        val myEmail = auth.currentUser?.email ?: return
        db.collection(myEmail).document("friends").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val friendsEmails = doc.get("list") as? List<String> ?: return@addOnSuccessListener
                    val friendsDataList = mutableListOf<FriendData>()

                    // 針對每個 Email 去抓他們的名字跟頭貼
                    var fetchCount = 0
                    for (friendEmail in friendsEmails) {
                        db.collection(friendEmail).document("個人資料").get()
                            .addOnSuccessListener { fDoc ->
                                val name = fDoc.getString("使用者名稱") ?: friendEmail
                                val avatar = fDoc.getString("頭貼圖片") ?: ""
                                friendsDataList.add(FriendData(friendEmail, name, avatar))

                                fetchCount++
                                if (fetchCount == friendsEmails.size) {
                                    rvFriendsList.adapter = FriendsAdapter(friendsDataList)
                                }
                            }
                    }
                }
            }
    }

    // 好友列表專用的 Adapter
    inner class FriendsAdapter(private val list: List<FriendData>) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivAvatar = v.findViewById<ImageView>(R.id.iv_friend_avatar)
            val tvName = v.findViewById<TextView>(R.id.tv_friend_name)
            // 🌟 記得補上這個 ID，對應 item_friend.xml 裡的聊天圖示
            val btnChat = v.findViewById<ImageView>(R.id.btn_chat_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val friend = list[position]
            holder.tvName.text = friend.name
            Glide.with(holder.itemView.context).load(friend.avatar).placeholder(R.drawable.user).into(holder.ivAvatar)

            // 1. 點擊整列：跳轉到個人檔案
            holder.itemView.setOnClickListener {
                val intent = Intent(this@Friends, tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page::class.java)
                intent.putExtra("TARGET_EMAIL", friend.email) // 傳遞對方 Email
                startActivity(intent)
            }

            // 2. 點擊聊天圖示：跳轉到聊天室
            holder.btnChat.setOnClickListener {
                val intent = Intent(this@Friends, tw.edu.pu.csim.s1120336.e_fit.Match.ChatRoomActivity::class.java)
                intent.putExtra("FRIEND_EMAIL", friend.email)
                intent.putExtra("FRIEND_NAME", friend.name)
                intent.putExtra("FRIEND_AVATAR", friend.avatar)
                startActivity(intent)
            }
        }
        override fun getItemCount() = list.size
    }
}