package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // 🌟 新增 Glide 套件
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import tw.edu.pu.csim.s1120336.e_fit.clothes.choose_add
import tw.edu.pu.csim.s1120336.e_fit.Community.Friends
import tw.edu.pu.csim.s1120336.e_fit.Match.Match_home
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.Setting
import tw.edu.pu.csim.s1120336.e_fit.home

class add_clothes : AppCompatActivity() {

    class ImageAdapter(
        private val context: Context,
        private val imageUrls: MutableList<String>,
        private val documentIds: MutableList<String>
    ) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)

            init {
                itemView.setOnLongClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        showDeleteConfirmationDialog(position)
                    }
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val imageUrl = imageUrls[position]
            // 🌟 神級優化：改用 Glide，完美支援完整網址，速度更快且不會閃退！
            Glide.with(context).load(imageUrl).into(holder.imageView)
        }

        override fun getItemCount() = imageUrls.size

        private fun showDeleteConfirmationDialog(position: Int) {
            val documentId = documentIds[position]

            AlertDialog.Builder(context)
                .setTitle("移除確認")
                .setMessage("確定要從衣櫃移除這件上衣嗎？\n(這不會刪除妳在作品集裡的去背圖片喔！)")
                .setPositiveButton("移除") { _, _ ->
                    removeAt(position)
                    deleteFromFirestore(documentId)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        private fun deleteFromFirestore(documentId: String) {
            // 🌟 修正 Bug：刪除時統一使用 email，跟儲存時的集合名稱對齊
            val email = FirebaseAuth.getInstance().currentUser?.email
            val db = FirebaseFirestore.getInstance()

            if (email != null) {
                db.collection(email).document(documentId).delete()
                    .addOnSuccessListener {
                        Log.d("ImageAdapter", "Document successfully deleted from Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ImageAdapter", "Error deleting document from Firestore: ${e.message}")
                    }
            }
        }

        fun removeAt(position: Int) {
            imageUrls.removeAt(position)
            documentIds.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val firestore = FirebaseFirestore.getInstance()
    lateinit var add: ImageView

    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var Clothes: ImageView
    lateinit var set: ImageView
    lateinit var menu: ImageView

    // 🌟 全新邏輯：宣告一個用來接收「作品集選取結果」的 Launcher
    private val albumPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val imageUrl = result.data?.getStringExtra("SELECTED_IMAGE_URL")
            if (imageUrl != null) {
                saveSelectedImageToFirestore(imageUrl)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_clothes)

        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
            startActivity(Intent(this, home::class.java))
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
            startActivity(Intent(this, Match_home::class.java))
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
            startActivity(Intent(this, choose_add::class.java))
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            startActivity(Intent(this, Friends::class.java))
            finish()
        }

        set = findViewById(R.id.set)
        set.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
            finish()
        }

        // 🌟 核心修改：把加號按鈕改成跳轉到作品集挑選模式
        add = findViewById(R.id.add)
        add.setOnClickListener {
            val intent = Intent(this, RemoveBgAlbumActivity::class.java)
            intent.putExtra("IS_PICKER_MODE", true) // 告訴相簿：我是來挑選照片的！
            albumPickerLauncher.launch(intent)
        }

        menu = findViewById(R.id.menu)
        menu.setOnClickListener {
            val popupMenu = PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenu), menu)
            popupMenu.inflate(R.menu.wardrobe_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.manage_clothes -> {
                        Toast.makeText(this, "管理所有衣服", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Wardrobe::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        loadImagesFromFirestore()
    }

    // 🌟 全新邏輯：將選取到的網址直接寫入 Firestore (瞬間完成)
    private fun saveSelectedImageToFirestore(imageUrl: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val timestamp = System.currentTimeMillis()
        val documentId = "上衣_$timestamp" // 確保檔名不重複

        val data = hashMapOf(
            "圖片網址" to imageUrl,
            "服裝種類" to "上衣",
            "timestamp" to timestamp
        )

        firestore.collection(email).document(documentId).set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "成功從作品集新增上衣！", Toast.LENGTH_SHORT).show()
                loadImagesFromFirestore() // 重新載入列表，讓畫面立刻更新
            }
            .addOnFailureListener {
                Toast.makeText(this, "新增上衣失敗，請檢查網路", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadImagesFromFirestore() {
        val imageUrls = mutableListOf<String>()
        val documentIds = mutableListOf<String>()
        val email = FirebaseAuth.getInstance().currentUser?.email

        if (!email.isNullOrEmpty()) {
            firestore.collection(email)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        if (document.id.contains("上衣")) {
                            val imageUrl = document.getString("圖片網址")
                            if (!imageUrl.isNullOrEmpty()) {
                                imageUrls.add(imageUrl)
                                documentIds.add(document.id)
                            }
                        }
                    }
                    imageAdapter = ImageAdapter(this, imageUrls, documentIds)
                    recyclerView.adapter = imageAdapter
                    imageAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error loading images: ${exception.message}")
                }
        }
    }
}