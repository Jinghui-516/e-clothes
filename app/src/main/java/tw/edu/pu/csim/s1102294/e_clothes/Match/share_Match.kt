package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1120336.e_fit.R
import java.util.*

class share_Match : AppCompatActivity() {

    private val imageUris = mutableListOf<Uri>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var progressBar: ProgressBar
    private lateinit var btnShare: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_match)

        val rvPreviews: RecyclerView = findViewById(R.id.rv_image_previews)
        val etCaption: EditText = findViewById(R.id.et_caption)
        btnShare = findViewById(R.id.btn_share)
        val btnCancel: TextView = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.upload_progress)

        // 1. 接收多張圖片的 Uri 列表
        val uriStrings = intent.getStringArrayListExtra("selected_images_uris")
        uriStrings?.forEach { imageUris.add(Uri.parse(it)) }

        // 2. 設定 RecyclerView 展示圖片
        rvPreviews.adapter = ImagePreviewAdapter(imageUris)

        btnCancel.setOnClickListener { finish() }

        // 3. 分享按鈕
        btnShare.setOnClickListener {
            val caption = etCaption.text.toString().trim()
            if (imageUris.isEmpty()) {
                Toast.makeText(this, "請至少選擇一張照片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnShare.isEnabled = false
            startMultiUpload(caption)
        }
    }

    // 處理多張圖片上傳
    private fun startMultiUpload(caption: String) {
        progressBar.visibility = View.VISIBLE
        val uploadedUrls = mutableListOf<String>()
        var uploadCount = 0

        for (uri in imageUris) {
            val ref = FirebaseStorage.getInstance().reference.child("posts/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    uploadedUrls.add(downloadUrl.toString())
                    uploadCount++

                    // 當所有照片都上傳成功後，才寫入資料庫
                    if (uploadCount == imageUris.size) {
                        saveToFirestore(caption, uploadedUrls)
                    }
                }
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                btnShare.isEnabled = true
                Toast.makeText(this, "上傳失敗，請重試", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🌟 修改重點：發文時先抓取個人真實姓名與頭貼，再寫入 AllPosts
    private fun saveToFirestore(caption: String, imageUrls: List<String>) {
        val email = auth.currentUser?.email
        if (email == null) {
            progressBar.visibility = View.GONE
            btnShare.isEnabled = true
            Toast.makeText(this, "登入逾期，請重新登入", Toast.LENGTH_SHORT).show()
            return
        }

        // 先去抓取妳的真實姓名跟頭貼
        db.collection(email).document("個人資料").get().addOnSuccessListener { doc ->
            var realName = "匿名使用者"
            var realAvatar = ""

            if (doc.exists()) {
                realName = doc.getString("使用者名稱") ?: "匿名使用者"
                realAvatar = doc.getString("頭貼圖片") ?: ""
            }

            // 組裝完整的貼文真實資料
            val postData = hashMapOf(
                "userName" to realName,           // 🌟 寫入真實姓名
                "userAvatar" to realAvatar,       // 🌟 寫入真實頭貼
                "userEmail" to email,
                "imageUrls" to imageUrls,
                "caption" to caption,
                "timestamp" to System.currentTimeMillis(),
                "likedBy" to listOf<String>(),
                "commentCount" to 0,              // 🌟 補上新功能需要的留言數
                "previewComments" to listOf<String>() // 🌟 補上新功能需要的預覽清單
            )

            // 正式寫入全域貼文資料庫
            db.collection("AllPosts").add(postData).addOnSuccessListener {
                Toast.makeText(this, "發布成功！", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                btnShare.isEnabled = true
                Toast.makeText(this, "發布失敗，請重試", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            // 如果抓資料失敗的防呆
            progressBar.visibility = View.GONE
            btnShare.isEnabled = true
            Toast.makeText(this, "讀取個人資料失敗", Toast.LENGTH_SHORT).show()
        }
    }

    // 內部 Adapter 用於顯示預覽圖
    inner class ImagePreviewAdapter(private val uris: List<Uri>) :
        RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.iv_item_preview)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context).load(uris[position]).into(holder.img)
        }

        override fun getItemCount() = uris.size
    }
}