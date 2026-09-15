package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.clothes.Wardrobe
import tw.edu.pu.csim.s1120336.e_fit.home

class Rank : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var rankRecyclerView: RecyclerView

    private val db = FirebaseFirestore.getInstance()

    // 🌟 修正資料結構：加入 postId 來記錄這是哪一篇貼文
    data class RankData(
        val name: String = "",
        val caption: String = "",
        val likesCount: Int = 0,
        val postImageUrl: String = "",
        val userEmail: String = "",
        val postId: String = "" // 👈 新增這個欄位
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rank)

        rankRecyclerView = findViewById(R.id.rank_recycler_view)
        rankRecyclerView.layoutManager = LinearLayoutManager(this)

        fetchRealRankData()

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_rank
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> {
                    startActivity(Intent(this, home::class.java))
                    finish()
                    true
                }
                R.id.nav_wardrobe -> {
                    startActivity(Intent(this, Wardrobe::class.java))
                    finish()
                    true
                }
                R.id.nav_community -> {
                    startActivity(Intent(this, Match_home::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Personal_Page::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }
    }

    private fun fetchRealRankData() {
        db.collection("AllPosts").get().addOnSuccessListener { documents ->
            val realRankList = mutableListOf<RankData>()

            for (document in documents) {
                val postId = document.id // 🌟 抓取這篇貼文在資料庫的專屬 ID
                val name = document.getString("userName") ?: "匿名使用者"
                val caption = document.getString("caption") ?: ""
                val userEmail = document.getString("userEmail") ?: ""

                // 抓取貼文裡的照片陣列，並取第一張圖
                val imageUrls = document.get("imageUrls") as? List<*>
                val postImageUrl = imageUrls?.firstOrNull()?.toString() ?: ""

                val likedBy = document.get("likedBy") as? List<*>
                val likesCount = likedBy?.size ?: 0

                // 🌟 記得把 postId 也放進去
                realRankList.add(RankData(name, caption, likesCount, postImageUrl, userEmail, postId))
            }

            val sortedRankList = realRankList
                .sortedByDescending { it.likesCount }
                .take(10)

            rankRecyclerView.adapter = RankAdapter(sortedRankList)
        }
    }

    inner class RankAdapter(private val list: List<RankData>) : RecyclerView.Adapter<RankAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvRankNumber = v.findViewById<TextView>(R.id.tv_rank_number)
            val ivCrown = v.findViewById<ImageView>(R.id.iv_crown)
            val ivPostImage = v.findViewById<ImageView>(R.id.iv_post_image)
            val tvUsername = v.findViewById<TextView>(R.id.tv_username)
            val tvCaption = v.findViewById<TextView>(R.id.tv_caption)
            val tvLikesCount = v.findViewById<TextView>(R.id.tv_likes_count)
            val btnViewOutfit = v.findViewById<TextView>(R.id.btn_view_outfit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rank, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            val rank = position + 1

            holder.tvRankNumber.text = rank.toString()
            holder.tvUsername.text = data.name
            holder.tvCaption.text = data.caption
            holder.tvLikesCount.text = data.likesCount.toString()

            // 皇冠邏輯：只有前三名顯示皇冠
            if (rank <= 3) {
                holder.ivCrown.visibility = View.VISIBLE
            } else {
                holder.ivCrown.visibility = View.GONE
            }

            // 載入穿搭照片
            Glide.with(holder.itemView.context)
                .load(data.postImageUrl)
                .placeholder(R.drawable.user) // 若無圖片的預設圖
                .into(holder.ivPostImage)

            // 🌟 點擊「查看穿搭」按鈕，精準跳轉到單篇貼文詳細頁
            holder.btnViewOutfit.setOnClickListener {
                val intent = Intent(holder.itemView.context, PostDetailActivity::class.java)
                // 傳遞貼文 ID，讓 PostDetailActivity 讀取該篇貼文資料
                intent.putExtra("POST_ID", data.postId)
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = list.size
    }
}