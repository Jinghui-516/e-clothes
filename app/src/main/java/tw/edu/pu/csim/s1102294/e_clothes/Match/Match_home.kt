package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
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

    private var postAdapter: PostAdapter? = null

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

                if (postAdapter == null) {
                    postAdapter = PostAdapter(posts)
                    matchRecyclerView.adapter = postAdapter
                } else {
                    val recyclerViewState = matchRecyclerView.layoutManager?.onSaveInstanceState()
                    postAdapter?.updateData(posts)
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
            fetchAndShowCategories()
        }

        btnText.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, share_Match::class.java))
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun fetchAndShowCategories() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection(email).document("我的搭配").collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val allOutfits = snapshot.documents.mapNotNull {
                    val url = it.getString("imageUrl")
                    val category = it.getString("category") ?: "未分類"
                    if (url != null) Pair(url, category) else null
                }

                if (allOutfits.isEmpty()) {
                    Toast.makeText(this, "您的搭配庫目前是空的唷！", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                showCategorySelectionDialog(allOutfits)
            }
    }

    private fun showCategorySelectionDialog(allOutfits: List<Pair<String, String>>) {
        val recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@Match_home, 2)
            setPadding(16, 16, 16, 16)
            clipToPadding = false
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("請選擇穿搭情境 📁")
            .setView(recyclerView)
            .setNegativeButton("取消", null)
            .show() // 先 show 出來才能改大小

        // 🌟 把彈出視窗拉大：寬度 95%，高度 85% (更接近全螢幕)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )

        val categories = allOutfits.map { it.second }.distinct()

        recyclerView.adapter = CategorySelectionAdapter(categories, allOutfits) { selectedCategory ->
            dialog.dismiss()
            showOutfitSelectionDialog(allOutfits, selectedCategory)
        }
    }

    private fun showOutfitSelectionDialog(allOutfits: List<Pair<String, String>>, selectedCategory: String) {
        val filteredOutfits = allOutfits.filter { it.second == selectedCategory }

        val recyclerView = RecyclerView(this).apply {
            // 🌟 恢復一排兩張 (雙欄)
            layoutManager = GridLayoutManager(this@Match_home, 2)
            setPadding(16, 16, 16, 16)
            clipToPadding = false
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("分享搭配：$selectedCategory")
            .setView(recyclerView)
            .setNeutralButton("🔙 返回分類") { _, _ ->
                showCategorySelectionDialog(allOutfits)
            }
            .setNegativeButton("取消", null)
            .show() // 先 show 出來才能改大小

        // 🌟 同樣把選照片的視窗也拉大：寬度 95%，高度 85%
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )

        recyclerView.adapter = OutfitSelectionAdapter(filteredOutfits.map { it.first }) { selectedUrl ->
            dialog.dismiss()
            val uriStringList = ArrayList(listOf(selectedUrl))
            val intent = Intent(this, share_Match::class.java)
            intent.putStringArrayListExtra("selected_images_uris", uriStringList)
            startActivity(intent)
        }
    }

    inner class CategorySelectionAdapter(
        private val categories: List<String>,
        private val allOutfits: List<Pair<String, String>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

        inner class ViewHolder(val layout: LinearLayout, val tvCategory: TextView, val tvCount: TextView) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 16, 16, 16) }
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#F5F0E6"))
                setPadding(0, 60, 0, 60)
            }
            val icon = TextView(context).apply { text = "📁"; textSize = 40f; gravity = android.view.Gravity.CENTER }
            val tvCategory = TextView(context).apply { textSize = 18f; setTextColor(android.graphics.Color.DKGRAY); setTypeface(null, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER }
            val tvCount = TextView(context).apply { textSize = 14f; setTextColor(android.graphics.Color.GRAY); gravity = android.view.Gravity.CENTER; setPadding(0, 10, 0, 0) }

            layout.addView(icon)
            layout.addView(tvCategory)
            layout.addView(tvCount)
            return ViewHolder(layout, tvCategory, tvCount)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.tvCategory.text = category
            val count = allOutfits.count { it.second == category }
            holder.tvCount.text = "$count 套搭配"
            holder.layout.setOnClickListener { onClick(category) }
        }
        override fun getItemCount() = categories.size
    }

    inner class OutfitSelectionAdapter(private val urls: List<String>, private val onOutfitClick: (String) -> Unit) : RecyclerView.Adapter<OutfitSelectionAdapter.ViewHolder>() {
        inner class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val imageView = ImageView(parent.context).apply {
                val layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    // 🌟 恢復原本適合雙欄的 200dp 高度
                    (200 * resources.displayMetrics.density).toInt()
                )
                layoutParams.setMargins(12, 12, 12, 12)
                this.layoutParams = layoutParams
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#F9F9F9"))
            }
            return ViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val url = urls[position]
            Glide.with(holder.imageView.context).load(url).into(holder.imageView)
            holder.imageView.setOnClickListener { onOutfitClick(url) }
        }

        override fun getItemCount() = urls.size
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