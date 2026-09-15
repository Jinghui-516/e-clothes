package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import tw.edu.pu.csim.s1120336.e_fit.Community.Friends
import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.clothes.Wardrobe
import tw.edu.pu.csim.s1120336.e_fit.home

class Match_home : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddPost: FloatingActionButton
    private lateinit var matchRecyclerView: RecyclerView
    private lateinit var btnChatRoom: ImageView
    private lateinit var btnSearchFriendsBar: View
    private val db = FirebaseFirestore.getInstance()

    // 🌟 1. 宣告一個全域變數來記住 Adapter，不要每次都重新產生
    private var postAdapter: PostAdapter? = null

    // 多選照片啟動器
    private val pickMultipleImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val uriStringList = ArrayList(uris.map { it.toString() })
            val intent = Intent(this, share_Match::class.java)
            intent.putStringArrayListExtra("selected_images_uris", uriStringList)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_home)

        matchRecyclerView = findViewById(R.id.match_recycler_view)
        fabAddPost = findViewById(R.id.fab_add_post)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        btnChatRoom = findViewById(R.id.btn_chat_room)
        btnChatRoom.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
        }

        btnSearchFriendsBar = findViewById(R.id.btn_search_friends_bar)
        btnSearchFriendsBar.setOnClickListener {
            val intent = Intent(this, Friends::class.java)
            startActivity(intent)
        }

        matchRecyclerView.layoutManager = LinearLayoutManager(this)

        fabAddPost.setOnClickListener {
            showPostOptions()
        }

        fetchPosts()
        setupNavigation()
    }

    private fun fetchPosts() {
        db.collection("AllPosts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val posts = mutableListOf<Post>()
                for (doc in value!!) {
                    val post = doc.toObject(Post::class.java)
                    post.postId = doc.id
                    posts.add(post)
                }

                // 🌟 2. 核心防護機制：如果還沒有 Adapter 就建立，如果有了就只更新資料
                if (postAdapter == null) {
                    // 第一次載入，給予新的 Adapter
                    postAdapter = PostAdapter(posts)
                    matchRecyclerView.adapter = postAdapter
                } else {
                    // 記住現在滑動到的位置
                    val recyclerViewState = matchRecyclerView.layoutManager?.onSaveInstanceState()

                    // 只替換 Adapter 裡面的資料，不重新創造整個列表
                    postAdapter?.updateData(posts)

                    // 恢復剛才的滑動位置
                    matchRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
                }
            }
    }

    private fun showPostOptions() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_post_options, null)

        val btnPhoto = view.findViewById<LinearLayout>(R.id.option_photo)
        val btnText = view.findViewById<LinearLayout>(R.id.option_text)

        btnPhoto.setOnClickListener {
            dialog.dismiss()
            pickMultipleImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnText.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, share_Match::class.java))
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun setupNavigation() {
        bottomNavigationView.selectedItemId = R.id.nav_community
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> {
                    startActivity(Intent(this, home::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_wardrobe -> {
                    startActivity(Intent(this, Wardrobe::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_rank -> {
                    startActivity(Intent(this, Rank::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Personal_Page::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> true
            }
        }
    }
}