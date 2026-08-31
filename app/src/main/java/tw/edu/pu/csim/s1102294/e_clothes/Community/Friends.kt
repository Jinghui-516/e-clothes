package tw.edu.pu.csim.s1120336.e_fit.Community

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private var searchedEmail: String = "" // 雖然用名稱搜，但背後還是記住 Email 才能加好友

    lateinit var etSearch: EditText
    lateinit var btnSearch: Button
    lateinit var cardResult: MaterialCardView
    lateinit var tvResultName: TextView
    lateinit var ivResultAvatar: ImageView
    lateinit var btnAddFriend: Button
    lateinit var rvFriendsList: RecyclerView

    data class FriendData(val email: String, val name: String, val avatar: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        etSearch = findViewById(R.id.et_search_username)
        etSearch.hint = "請輸入對方的使用者名稱..." // 🌟 提示改回使用者名稱

        btnSearch = findViewById(R.id.btn_search)
        cardResult = findViewById(R.id.card_search_result)
        tvResultName = findViewById(R.id.tv_result_name)
        ivResultAvatar = findViewById(R.id.iv_result_avatar)
        btnAddFriend = findViewById(R.id.btn_add_friend)

        rvFriendsList = findViewById(R.id.rv_friends_list)
        rvFriendsList.layoutManager = LinearLayoutManager(this)

        // 1. 搜尋按鈕邏輯
        btnSearch.setOnClickListener {
            val nameToSearch = etSearch.text.toString().trim()
            if (nameToSearch.isNotEmpty()) {
                searchUserInFirestore(nameToSearch) // 🌟 傳入使用者名稱
            } else {
                Toast.makeText(this, "請輸入要搜尋的使用者名稱", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 加好友按鈕邏輯
        btnAddFriend.setOnClickListener {
            addFriendToDatabase(searchedEmail)
        }

        // 3. 一開畫面就去撈取現有的好友列表
        loadMyFriendsList()
    }

    // 🌟 全新升級：用「使用者名稱」跨資料夾搜尋
    private fun searchUserInFirestore(searchName: String) {
        val myEmail = auth.currentUser?.email ?: return

        // 使用 collectionGroup 掃描全站所有的「個人資料」文件
        db.collectionGroup("個人資料")
            .whereEqualTo("使用者名稱", searchName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // 搜到了！(假設名稱不重複，我們取第一個結果)
                    val doc = querySnapshot.documents[0]

                    // 🌟 魔法步驟：從找到的文件路徑中，反向推導出這個人的 Email (資料夾名稱)
                    val targetEmail = doc.reference.parent.id

                    if (targetEmail == myEmail) {
                        Toast.makeText(this, "不能加自己為好友啦！", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 記錄對方的 Email 準備加好友，並顯示卡片
                    searchedEmail = targetEmail
                    cardResult.visibility = View.VISIBLE
                    tvResultName.text = "@${doc.getString("使用者名稱") ?: "未設定"}"

                    val avatarUrl = doc.getString("頭貼圖片")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this).load(avatarUrl).into(ivResultAvatar)
                    }
                } else {
                    cardResult.visibility = View.GONE
                    Toast.makeText(this, "找不到這個使用者名稱喔！", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "搜尋失敗，請稍後再試", Toast.LENGTH_SHORT).show()
                // 🌟 把真正的錯誤原因印在下方的 Logcat 裡面，裡面藏著建立索引的網址！
                Log.e("SearchError", "Firebase 搜尋失敗原因 (請點擊網址建立索引): ", e)
            }
    }

    // 雙向加好友 (保持原樣，因為底層還是用 Email 溝通)
    private fun addFriendToDatabase(friendEmail: String) {
        val myEmail = auth.currentUser?.email ?: return

        db.collection(myEmail).document("friends")
            .set(hashMapOf("list" to FieldValue.arrayUnion(friendEmail)), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                db.collection(friendEmail).document("friends")
                    .set(hashMapOf("list" to FieldValue.arrayUnion(myEmail)), com.google.firebase.firestore.SetOptions.merge())

                Toast.makeText(this, "加好友成功！", Toast.LENGTH_SHORT).show()
                cardResult.visibility = View.GONE
                etSearch.text.clear()
                loadMyFriendsList()
            }
    }

    // 讀取好友清單
    private fun loadMyFriendsList() {
        val myEmail = auth.currentUser?.email ?: return
        db.collection(myEmail).document("friends").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val friendsEmails = doc.get("list") as? List<String> ?: return@addOnSuccessListener
                    val friendsDataList = mutableListOf<FriendData>()

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

    // 好友列表 Adapter
    inner class FriendsAdapter(private val list: List<FriendData>) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivAvatar = v.findViewById<ImageView>(R.id.iv_friend_avatar)
            val tvName = v.findViewById<TextView>(R.id.tv_friend_name)
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

            holder.itemView.setOnClickListener {
                val intent = Intent(this@Friends, tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page::class.java)
                intent.putExtra("TARGET_EMAIL", friend.email)
                startActivity(intent)
            }

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