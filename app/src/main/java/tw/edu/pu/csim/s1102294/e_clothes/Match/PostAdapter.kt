// 🌟 1. 確保是妳正確的專案名字！
package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import tw.edu.pu.csim.s1120336.e_fit.R

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

        val btnLike: ImageView = v.findViewById(R.id.btn_post_like)

        // 🌟 【修正重點 1】這裡把「旁邊的數字」和「下方的文字」分開綁定！
        val tvLikesCount: TextView = v.findViewById(R.id.tv_post_likes_count)
        val tvLikes: TextView = v.findViewById(R.id.tv_post_likes)

        val btnComment: ImageView = v.findViewById(R.id.btn_post_comment)
        val tvCommentsCount: TextView = v.findViewById(R.id.tv_post_comments_count)

        // 新增綁定：留言預覽區塊的元件
        val tvViewAllComments: TextView = v.findViewById(R.id.tv_view_all_comments)
        val tvCommentPreview1: TextView = v.findViewById(R.id.tv_comment_preview_1)
        val tvCommentPreview2: TextView = v.findViewById(R.id.tv_comment_preview_2)
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

        // 3. 判斷按讚狀態 (衣架 or 衣服)
        val isLiked = post.likedBy.contains(currentUserEmail)

        if (isLiked) {
            holder.btnLike.setImageResource(R.drawable.ic_clothes)
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_hanger)
        }

        // 🌟 【修正重點 2】把讚數分別塞給這兩個 TextView
        holder.tvLikesCount.text = post.likedBy.size.toString() // 更新衣服旁邊的純數字
        holder.tvLikes.text = "${post.likedBy.size} 個讚"        // 更新下方的文字

        // 4. 更新留言圖示旁邊的數量
        holder.tvCommentsCount.text = post.commentCount.toString()

        // 判斷是否要顯示「查看全部留言」
        if (post.commentCount > 0) {
            holder.tvViewAllComments.visibility = View.VISIBLE
            holder.tvViewAllComments.text = "查看全部 ${post.commentCount} 則留言"
        } else {
            holder.tvViewAllComments.visibility = View.GONE
        }

        // 抓取最新的兩則留言來當預覽
        val latestPreviews = post.previewComments.takeLast(2)

        if (latestPreviews.isNotEmpty()) {
            holder.tvCommentPreview1.visibility = View.VISIBLE
            holder.tvCommentPreview1.text = latestPreviews[0]
        } else {
            holder.tvCommentPreview1.visibility = View.GONE
        }

        if (latestPreviews.size > 1) {
            holder.tvCommentPreview2.visibility = View.VISIBLE
            holder.tvCommentPreview2.text = latestPreviews[1]
        } else {
            holder.tvCommentPreview2.visibility = View.GONE
        }

        // 5. 處理按讚點擊事件
        holder.btnLike.setOnClickListener {
            if (post.postId.isEmpty()) return@setOnClickListener // 防呆機制
            holder.btnLike.isEnabled = false
            val postRef = db.collection("AllPosts").document(post.postId)
            if (isLiked) {
                postRef.update("likedBy", FieldValue.arrayRemove(currentUserEmail))
                    .addOnCompleteListener { holder.btnLike.isEnabled = true }
            } else {
                postRef.update("likedBy", FieldValue.arrayUnion(currentUserEmail))
                    .addOnCompleteListener { holder.btnLike.isEnabled = true }
            }
        }

        // 6. 處理留言圖示點擊事件 (跳轉留言板)
        holder.btnComment.setOnClickListener {
            if (post.postId.isEmpty()) return@setOnClickListener
            val context = holder.itemView.context
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            context.startActivity(intent)
        }

        // 點擊「查看全部留言」一樣跳轉到留言板
        holder.tvViewAllComments.setOnClickListener {
            if (post.postId.isEmpty()) return@setOnClickListener
            val context = holder.itemView.context
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            context.startActivity(intent)
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