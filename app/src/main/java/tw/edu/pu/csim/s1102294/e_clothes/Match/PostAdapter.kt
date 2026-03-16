package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1102294.e_clothes.R

class PostAdapter(private val postList: List<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    // 取得當前登入者的 Email，用來判斷他有沒有按過讚
    private val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private val db = FirebaseFirestore.getInstance()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_post_name)
        val ivAvatar: ImageView = v.findViewById(R.id.iv_post_avatar)
        val viewPager: ViewPager2 = v.findViewById(R.id.viewPager_post_images)
        val tabLayout: TabLayout = v.findViewById(R.id.tab_layout_indicator)
        val tvCaption: TextView = v.findViewById(R.id.tv_post_caption)

        // 🌟 互動按鈕綁定
        val btnLike: ImageButton = v.findViewById(R.id.btn_post_like)
        val tvLikes: TextView = v.findViewById(R.id.tv_post_likes)
        val btnComment: ImageButton = v.findViewById(R.id.btn_post_comment)
        val tvCommentsCount: TextView = v.findViewById(R.id.tv_post_comments_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = postList[position]

        // 1. 綁定基本文字與頭貼
        holder.tvName.text = post.userName
        holder.tvCaption.text = post.caption
        Glide.with(holder.itemView.context)
            .load(post.userAvatar)
            .placeholder(R.drawable.user) // 預設頭貼
            .into(holder.ivAvatar)

        // 2. 綁定多張照片滑動 (ViewPager2)
        holder.viewPager.adapter = ImageSliderAdapter(post.imageUrls)
        TabLayoutMediator(holder.tabLayout, holder.viewPager) { _, _ -> }.attach()

        // 3. 🌟 判斷按讚狀態 (衣架 or 衣服)
        val isLiked = post.likedBy.contains(currentUserEmail)

        // 動態切換圖示：有按讚給衣服，沒按讚給衣架
        if (isLiked) {
            holder.btnLike.setImageResource(R.drawable.ic_clothes)
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_hanger)
        }

        // 更新讚數文字
        holder.tvLikes.text = post.likedBy.size.toString()

        // 4. 🌟 處理按讚點擊事件
        holder.btnLike.setOnClickListener {
            if (post.postId.isEmpty()) return@setOnClickListener // 防呆機制

            // 讓按鈕暫時不能點，防止狂點造成資料庫異常
            holder.btnLike.isEnabled = false

            val postRef = db.collection("AllPosts").document(post.postId)

            if (isLiked) {
                // 取消按讚 (脫下衣服) -> 從資料庫移除 Email
                postRef.update("likedBy", FieldValue.arrayRemove(currentUserEmail))
                    .addOnCompleteListener { holder.btnLike.isEnabled = true }
            } else {
                // 新增按讚 (穿上衣服) -> 把 Email 存進資料庫
                postRef.update("likedBy", FieldValue.arrayUnion(currentUserEmail))
                    .addOnCompleteListener { holder.btnLike.isEnabled = true }
            }
        }

        // 5. 處理留言點擊事件
        holder.btnComment.setOnClickListener {
            Toast.makeText(holder.itemView.context, "準備開啟留言板...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = postList.size

    // --- 內部類別：處理單一貼文內的多圖滑動 ---
    inner class ImageSliderAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImgViewHolder>() {
        inner class ImgViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.iv_single_post_img)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImgViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_post_image_single, parent, false)
        )
        override fun onBindViewHolder(holder: ImgViewHolder, pos: Int) {
            Glide.with(holder.itemView.context)
                .load(images[pos])
                .into(holder.img)
        }
        override fun getItemCount() = images.size
    }
}