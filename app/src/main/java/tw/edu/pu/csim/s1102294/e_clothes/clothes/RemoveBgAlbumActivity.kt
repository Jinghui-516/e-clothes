package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1120336.e_fit.R

class RemoveBgAlbumActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private lateinit var rvAlbum: RecyclerView

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnEditDelete: TextView

    // 狀態管理
    private var isEditMode = false
    private val selectedItems = mutableSetOf<String>() // 存放選取的 docId
    private var albumList = listOf<AlbumItem>()

    // 🌟 新增的資料類別：用來同時記錄 ID 跟 網址
    data class AlbumItem(val id: String, val url: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_bg_album)

        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)
        btnEditDelete = findViewById(R.id.btn_edit_delete)
        rvAlbum = findViewById(R.id.rv_album)

        rvAlbum.layoutManager = GridLayoutManager(this, 3)

        // 左上角按鈕：返回 或 取消編輯
        btnBack.setOnClickListener {
            if (isEditMode) {
                toggleEditMode(false)
            } else {
                finish()
            }
        }

        // 右上角按鈕：編輯 或 刪除
        btnEditDelete.setOnClickListener {
            if (!isEditMode) {
                toggleEditMode(true)
            } else {
                if (selectedItems.isEmpty()) {
                    Toast.makeText(this, "請先選擇要刪除的照片", Toast.LENGTH_SHORT).show()
                } else {
                    showDeleteConfirmDialog()
                }
            }
        }

        loadAlbumImages()
    }

    private fun toggleEditMode(enabled: Boolean) {
        isEditMode = enabled
        selectedItems.clear()

        if (isEditMode) {
            tvTitle.text = "選擇要刪除的照片"
            btnEditDelete.text = "刪除"
            btnEditDelete.setTextColor(Color.RED)
            btnBack.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // 換成 X 圖示
        } else {
            tvTitle.text = "我的去背作品集"
            btnEditDelete.text = "編輯"
            btnEditDelete.setTextColor(Color.parseColor("#8D6E63")) // 妳的品牌色
            btnBack.setImageResource(R.drawable.cross) // 換回原本的返回
        }
        rvAlbum.adapter?.notifyDataSetChanged() // 刷新列表顯示選取狀態
    }

    private fun loadAlbumImages() {
        if (myEmail.isEmpty()) {
            Toast.makeText(this, "讀取失敗：找不到帳號！", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection(myEmail).document("去背相簿").collection("images")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val newList = mutableListOf<AlbumItem>()
                if (snapshot != null) {
                    for (doc in snapshot) {
                        doc.getString("imageUrl")?.let { url ->
                            newList.add(AlbumItem(doc.id, url))
                        }
                    }
                    albumList = newList
                    rvAlbum.adapter = AlbumAdapter(albumList)
                }
            }
    }

    // 🌟 刪除雲端資料邏輯
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("確認刪除")
            .setMessage("確定要刪除這 ${selectedItems.size} 張去背照片嗎？")
            .setPositiveButton("刪除") { _, _ ->
                val storageRef = FirebaseStorage.getInstance().reference

                selectedItems.forEach { docId ->
                    // 1. 刪除 Firestore 紀錄
                    db.collection(myEmail).document("去背相簿").collection("images").document(docId).delete()
                    // 2. 刪除 Storage 裡的實體照片
                    storageRef.child("remove_bg/$myEmail/$docId.png").delete()
                }
                Toast.makeText(this, "刪除成功！", Toast.LENGTH_SHORT).show()
                toggleEditMode(false) // 刪除完退出編輯模式
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 🌟 黑色背景全螢幕展開邏輯
    private fun showFullScreenImage(imageUrl: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK) // 🌟 設定絕對黑色背景
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { dialog.dismiss() } // 點擊畫面關閉
        }

        Glide.with(this).load(imageUrl).into(imageView)
        dialog.setContentView(imageView)
        dialog.show()
    }

    // 相簿 Adapter
    inner class AlbumAdapter(private val list: List<AlbumItem>) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivImage: ImageView = v.findViewById(R.id.iv_grid_image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_personal_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            Glide.with(holder.itemView.context).load(item.url).into(holder.ivImage)

            // 處理選取時的視覺效果 (變暗)
            if (selectedItems.contains(item.id)) {
                holder.ivImage.setColorFilter(Color.argb(120, 0, 0, 0)) // 選取時加上半透明黑底
                holder.ivImage.setPadding(10, 10, 10, 10) // 內縮一點產生選取感
            } else {
                holder.ivImage.clearColorFilter()
                holder.ivImage.setPadding(0, 0, 0, 0)
            }

            // 處理點擊事件
            holder.itemView.setOnClickListener {
                if (isEditMode) {
                    // 編輯模式下：切換選取狀態
                    if (selectedItems.contains(item.id)) {
                        selectedItems.remove(item.id)
                    } else {
                        selectedItems.add(item.id)
                    }
                    notifyItemChanged(position) // 更新這張圖的狀態
                } else {
                    // 一般模式下：展開黑色背景預覽
                    showFullScreenImage(item.url)
                }
            }
        }
        override fun getItemCount() = list.size
    }
}