package tw.edu.pu.csim.s1120336.e_fit.Match

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1120336.e_fit.R

class PostDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail) // 確保對應妳的 xml 畫面

        // 🌟 1. 綁定返回按鈕，點擊時關閉目前頁面回到上一頁
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // 2. 接收從排行榜傳過來的貼文 ID
        val postId = intent.getStringExtra("POST_ID") ?: return

        // 3. 綁定 xml 裡面的元件
        val tvName = findViewById<TextView>(R.id.tv_post_name)
        val ivAvatar = findViewById<ImageView>(R.id.iv_post_avatar)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager_post_images)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout_indicator)
        val tvCaption = findViewById<TextView>(R.id.tv_post_caption)
        val tvLikesCount = findViewById<TextView>(R.id.tv_post_likes_count)

        // 4. 根據 postId 去 Firebase 抓取該篇貼文的詳細資料
        db.collection("AllPosts").document(postId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                tvName.text = doc.getString("userName") ?: ""
                tvCaption.text = doc.getString("caption") ?: ""

                val avatarUrl = doc.getString("userAvatar") ?: ""
                Glide.with(this).load(avatarUrl).placeholder(R.drawable.user).into(ivAvatar)

                val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                if (imageUrls.isNotEmpty()) {
                    // 使用內部宣告的圖片輪播 Adapter
                    viewPager.adapter = ImageSliderAdapter(imageUrls)
                    TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
                }

                val likedBy = doc.get("likedBy") as? List<*>
                tvLikesCount.text = "${likedBy?.size ?: 0} 個讚"
            }
        }
    }

    // 內部專用的圖片滑動適配器
    inner class ImageSliderAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImgViewHolder>() {
        inner class ImgViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.iv_single_post_img)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImgViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_post_image_single, parent, false)
        )
        override fun onBindViewHolder(holder: ImgViewHolder, pos: Int) {
            Glide.with(holder.itemView.context).load(images[pos]).into(holder.img)
        }
        override fun getItemCount() = images.size
    }
}