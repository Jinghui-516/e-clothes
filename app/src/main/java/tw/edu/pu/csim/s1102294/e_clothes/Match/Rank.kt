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
import com.bumptech.glide.Glide // 🌟 補上 Glide 匯入
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

    // 🌟 修正資料結構：加入 userAvatar 用來記錄頭貼網址
    data class RankData(
        val name: String = "",
        val caption: String = "",
        val likesCount: Int = 0,
        val userAvatar: String = "" // 🌟 新增頭貼網址欄位
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rank)

        rankRecyclerView = findViewById(R.id.rank_recycler_view)
        rankRecyclerView.layoutManager = LinearLayoutManager(this)

        // 核心功能：從 Firebase 撈取真實數據並進行排行排序
        fetchRealRankData()

        // 3. 導覽列邏輯
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

    // 從 Firestore 即時動態撈取並統計排行
    private fun fetchRealRankData() {
        db.collection("AllPosts").get().addOnSuccessListener { documents ->
            val realRankList = mutableListOf<RankData>()

            for (document in documents) {
                val name = document.getString("userName") ?: "匿名使用者"
                val caption = document.getString("caption") ?: ""
                val avatar = document.getString("userAvatar") ?: "" // 🌟 抓取雲端貼文裡的頭貼網址

                // 抓取 likedBy 陣列的大小當作真實讚數
                val likedBy = document.get("likedBy") as? List<*>
                val likesCount = likedBy?.size ?: 0

                realRankList.add(RankData(name, caption, likesCount, avatar))
            }

            // 根據「讚數」由多到少排序，最高只留下前 10 名
            val sortedRankList = realRankList
                .sortedByDescending { it.likesCount }
                .take(10)

            // 將真實數據餵給畫面
            rankRecyclerView.adapter = RankAdapter(sortedRankList)
        }
    }

    // 排行榜專用 Adapter
    inner class RankAdapter(private val list: List<RankData>) : RecyclerView.Adapter<RankAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvRank = v.findViewById<TextView>(R.id.tv_rank_number)
            val tvName = v.findViewById<TextView>(R.id.tv_rank_name)
            val tvStyle = v.findViewById<TextView>(R.id.tv_rank_style)
            val tvLikes = v.findViewById<TextView>(R.id.tv_rank_likes)
            val ivAvatar = v.findViewById<ImageView>(R.id.iv_rank_avatar) // 🌟 修正重點 1：宣告並綁定畫面上的頭貼 ImageView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // 💡 確保妳的 item_rank.xml 裡面存放頭貼的 ImageView ID 真的叫做 iv_rank_avatar
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rank, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            val rank = position + 1
            holder.tvRank.text = rank.toString()
            holder.tvName.text = data.name

            // 將原本顯示風格標籤的地方，動態換成使用者的「貼文內文」
            holder.tvStyle.text = if (data.caption.length > 10) data.caption.take(10) + "..." else data.caption
            holder.tvLikes.text = data.likesCount.toString()

            // 🌟 修正重點 2：動態使用 Glide 將使用者的頭貼網址下載下來並塞進排行榜畫面
            Glide.with(holder.itemView.context)
                .load(data.userAvatar)
                .placeholder(R.drawable.user) // 如果網路太慢或沒頭貼，預設顯示原本的人頭圖
                .into(holder.ivAvatar)

            // 針對前三名給予特殊顏色
            when (rank) {
                1 -> holder.tvRank.setTextColor(Color.parseColor("#FFD700")) // 金色
                2 -> holder.tvRank.setTextColor(Color.parseColor("#C0C0C0")) // 銀色
                3 -> holder.tvRank.setTextColor(Color.parseColor("#CD7F32")) // 銅色
                else -> holder.tvRank.setTextColor(Color.parseColor("#8E8E8E")) // 普通灰色
            }
        }

        override fun getItemCount() = list.size
    }
}