package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import tw.edu.pu.csim.s1102294.e_clothes.R

// 定義貼文資料結構
data class Post(
    val userName: String = "",
    val userAvatar: String = "",
    val imageUrls: List<String> = listOf(),
    val caption: String = "",
    val likes: Int = 0
)

class PostAdapter(private val postList: List<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName = v.findViewById<TextView>(R.id.tv_post_name)
        val ivAvatar = v.findViewById<ImageView>(R.id.iv_post_avatar)
        val viewPager = v.findViewById<ViewPager2>(R.id.viewPager_post_images)
        val tabLayout = v.findViewById<TabLayout>(R.id.tab_layout_indicator)
        val tvLikes = v.findViewById<TextView>(R.id.tv_post_likes)
        val tvCaption = v.findViewById<TextView>(R.id.tv_post_caption)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = postList[position]

        holder.tvName.text = post.userName
        holder.tvLikes.text = "${post.likes} 個讚"
        holder.tvCaption.text = post.caption

        // 1. 載入頭貼
        Glide.with(holder.itemView.context)
            .load(post.userAvatar).placeholder(R.drawable.user).into(holder.ivAvatar)

        // 2. 🌟 設置輪播圖 Adapter
        val imageAdapter = ImageSliderAdapter(post.imageUrls)
        holder.viewPager.adapter = imageAdapter

        // 3. 🌟 設置小圓點 Indicator 與滑動連動
        TabLayoutMediator(holder.tabLayout, holder.viewPager) { _, _ -> }.attach()
    }

    override fun getItemCount() = postList.size

    // --- 內部類別：處理單一貼文內的多張照片滑動 ---
    inner class ImageSliderAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImgViewHolder>() {
        inner class ImgViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val img = v.findViewById<ImageView>(R.id.iv_single_post_img)
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