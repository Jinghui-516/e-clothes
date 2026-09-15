package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil // 🌟 新增匯入差異計算工具
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1120336.e_fit.R

// 🌟 修改 1：將括號裡的 val 改成 var，讓清單資料可以被抽換
class PostAdapter(private var postList: MutableList<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    private val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private val db = FirebaseFirestore.getInstance()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_post_name)
        val ivAvatar: ImageView = v.findViewById(R.id.iv_post_avatar)
        val viewPager: ViewPager2 = v.findViewById(R.id.viewPager_post_images)
        val tabLayout: TabLayout = v.findViewById(R.id.tab_layout_indicator)
        val tvCaption: TextView = v.findViewById(R.id.tv_post_caption)
        val btnLike: ImageView = v.findViewById(R.id.btn_post_like)
        val tvLikesCount: TextView = v.findViewById(R.id.tv_post_likes_count)
        val tvLikes: TextView = v.findViewById(R.id.tv_post_likes)
        val btnComment: ImageView = v.findViewById(R.id.btn_post_comment)
        val tvCommentsCount: TextView = v.findViewById(R.id.tv_post_comments_count)
        val tvViewAllComments: TextView = v.findViewById(R.id.tv_view_all_comments)
        val tvCommentPreview1: TextView = v.findViewById(R.id.tv_comment_preview_1)
        val tvCommentPreview2: TextView = v.findViewById(R.id.tv_comment_preview_2)
        val btnBookmark: ImageView = v.findViewById(R.id.btn_post_bookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = postList[position]

        holder.tvName.text = post.userName
        holder.tvCaption.text = post.caption
        Glide.with(holder.itemView.context).load(post.userAvatar).placeholder(R.drawable.user).into(holder.ivAvatar)

        holder.viewPager.adapter = ImageSliderAdapter(post.imageUrls)
        TabLayoutMediator(holder.tabLayout, holder.viewPager) { _, _ -> }.attach()

        val isLiked = post.likedBy.contains(currentUserEmail)
        holder.btnLike.setImageResource(if (isLiked) R.drawable.ic_clothes else R.drawable.ic_hanger)
        holder.tvLikesCount.text = post.likedBy.size.toString()
        holder.tvLikes.text = "${post.likedBy.size} 個讚"

        holder.btnLike.setOnClickListener {
            if (post.postId.isEmpty()) return@setOnClickListener
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

        val postImageUrl = post.imageUrls.firstOrNull()
        if (postImageUrl != null && currentUserEmail.isNotEmpty()) {
            val collectionRef = db.collection(currentUserEmail).document("我的珍藏").collection("items")

            collectionRef.whereEqualTo("imageUrl", postImageUrl).get().addOnSuccessListener { snapshot ->
                var isBookmarked = !snapshot.isEmpty
                var savedDocId = if (isBookmarked) snapshot.documents.first().id else null

                holder.btnBookmark.setImageResource(if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)

                holder.btnBookmark.setOnClickListener {
                    holder.btnBookmark.isEnabled = false

                    if (isBookmarked && savedDocId != null) {
                        collectionRef.document(savedDocId!!).delete().addOnSuccessListener {
                            isBookmarked = false
                            savedDocId = null
                            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark)
                            holder.btnBookmark.isEnabled = true
                            Toast.makeText(holder.itemView.context, "已取消珍藏", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val savedData = hashMapOf(
                            "imageUrl" to postImageUrl,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        collectionRef.add(savedData).addOnSuccessListener { docRef ->
                            isBookmarked = true
                            savedDocId = docRef.id
                            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                            holder.btnBookmark.isEnabled = true
                            Toast.makeText(holder.itemView.context, "已成功加入珍藏！✨", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        holder.tvCommentsCount.text = post.commentCount.toString()
        holder.tvViewAllComments.visibility = if (post.commentCount > 0) View.VISIBLE else View.GONE
        holder.tvViewAllComments.text = "查看全部 ${post.commentCount} 則留言"

        val latestPreviews = post.previewComments.takeLast(2)
        holder.tvCommentPreview1.visibility = if (latestPreviews.isNotEmpty()) View.VISIBLE else View.GONE
        if (latestPreviews.isNotEmpty()) holder.tvCommentPreview1.text = latestPreviews[0]

        holder.tvCommentPreview2.visibility = if (latestPreviews.size > 1) View.VISIBLE else View.GONE
        if (latestPreviews.size > 1) holder.tvCommentPreview2.text = latestPreviews[1]

        val commentListener = View.OnClickListener {
            val intent = Intent(holder.itemView.context, CommentActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            holder.itemView.context.startActivity(intent)
        }
        holder.btnComment.setOnClickListener(commentListener)
        holder.tvViewAllComments.setOnClickListener(commentListener)
    }

    override fun getItemCount() = postList.size

    // 🌟 修改 2：新增資料局部更新的魔法函數
    fun updateData(newPosts: List<Post>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = postList.size
            override fun getNewListSize() = newPosts.size

            // 判斷是不是同一篇貼文
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return postList[oldItemPosition].postId == newPosts[newItemPosition].postId
            }

            // 判斷貼文內容是否有變動（例如按讚數變了）
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = postList[oldItemPosition]
                val new = newPosts[newItemPosition]
                return old.likedBy.size == new.likedBy.size &&
                        old.commentCount == new.commentCount
            }
        })

        // 把新資料蓋過去，並請系統執行「局部平滑更新」
        this.postList = newPosts.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }

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