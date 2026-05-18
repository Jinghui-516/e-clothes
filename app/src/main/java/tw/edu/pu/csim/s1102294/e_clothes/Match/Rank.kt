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
import com.google.android.material.bottomnavigation.BottomNavigationView
import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.clothes.Wardrobe
import tw.edu.pu.csim.s1120336.e_fit.home

class Rank : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var rankRecyclerView: RecyclerView

    // 定義資料結構
    data class RankData(val name: String, val style: String, val likes: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rank)

        // 1. 模擬前 10 名資料 (未來這裡改從 Firestore 撈資料)
        val dummyRankList = listOf(
            RankData("穿搭達人 小明", "#韓系極簡", 1250),
            RankData("時尚教主 Alice", "#美式復古", 1100),
            RankData("甜美系女孩", "#日系甜美", 980),
            RankData("街頭型男", "#工裝風格", 850),
            RankData("大四學姐", "#正式休閒", 720),
            RankData("健身教練", "#運動機能", 600),
            RankData("文青少女", "#鹽系穿搭", 550),
            RankData("校園之星", "#潮流時尚", 480),
            RankData("極致黑控", "#暗黑系", 300),
            RankData("資管系小胖", "#舒適居家", 150)
        )

        // 2. 設定 RecyclerView
        rankRecyclerView = findViewById(R.id.rank_recycler_view)
        rankRecyclerView.layoutManager = LinearLayoutManager(this)
        rankRecyclerView.adapter = RankAdapter(dummyRankList)

        // 3. 導覽列邏輯 (跟其他頁面一樣)
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

    // 排行榜專用 Adapter
    inner class RankAdapter(private val list: List<RankData>) : RecyclerView.Adapter<RankAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvRank = v.findViewById<TextView>(R.id.tv_rank_number)
            val tvName = v.findViewById<TextView>(R.id.tv_rank_name)
            val tvStyle = v.findViewById<TextView>(R.id.tv_rank_style)
            val tvLikes = v.findViewById<TextView>(R.id.tv_rank_likes)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rank, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            val rank = position + 1
            holder.tvRank.text = rank.toString()
            holder.tvName.text = data.name
            holder.tvStyle.text = data.style
            holder.tvLikes.text = data.likes.toString()

            // 🌟 針對前三名給予特殊顏色
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