package tw.edu.pu.csim.s1102294.e_clothes.clothes

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.home

class add_shoes : AppCompatActivity() {

    class ImageAdapter(
        private val context: Context,
        private val imageUrls: MutableList<String>,
        private val documentIds: MutableList<String> // Add documentIds parameter
    ) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)

            init {
                // 設置長按監聽器
                itemView.setOnLongClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        // 調用刪除方法
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
            downloadImage(imageUrl, holder.imageView)
        }

        override fun getItemCount() = imageUrls.size

        private fun downloadImage(imageUrl: String, imageView: ImageView) {
            val storageRef = FirebaseStorage.getInstance().reference.child(imageUrl)

            storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageView.setImageBitmap(bitmap)
            }.addOnFailureListener { exception ->
                Log.e("ImageAdapter", "Error loading image: ${exception.message}")
            }
        }

        private fun showDeleteConfirmationDialog(position: Int) {
            val imageUrl = imageUrls[position]
            val documentId = documentIds[position] // Get the document ID

            AlertDialog.Builder(context)
                .setTitle("刪除確認")
                .setMessage("確定要刪除這張圖片嗎？")
                .setPositiveButton("是的") { _, _ ->
                    removeAt(position)
                    deleteImageFromStorage(imageUrl)
                    deleteFromFirestore(documentId) // Pass the document ID to delete from Firestore
                }
                .setNegativeButton("取消", null)
                .show()
        }

        private fun deleteImageFromStorage(imageUrl: String) {
            val storageRef = FirebaseStorage.getInstance().reference.child(imageUrl)

            storageRef.delete()
                .addOnSuccessListener {
                    Log.d("ImageAdapter", "Image successfully deleted from Storage")
                }
                .addOnFailureListener { e ->
                    Log.e("ImageAdapter", "Error deleting image from Storage: ${e.message}")
                }
        }

        private fun deleteFromFirestore(documentId: String) { // Accept documentId as a parameter
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val db = FirebaseFirestore.getInstance()

            if (userId != null) {
                // Use the document ID directly for deletion
                db.collection(userId).document(documentId).delete()
                    .addOnSuccessListener {
                        Log.d("ImageAdapter", "Document successfully deleted from Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "ImageAdapter",
                            "Error deleting document from Firestore: ${e.message}"
                        )
                    }
            }
        }

        fun removeAt(position: Int) {
            imageUrls.removeAt(position)
            documentIds.removeAt(position) // Also remove the document ID
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shoes)


        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
//            Match.text = ""
            val intent2 = Intent(this, Match_home::class.java)
            startActivity(intent2)
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
//            textView9.text = "123"
//            checkPermission()
            val intent = Intent(this, choose_add::class.java)
            startActivity(intent)
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }

        set = findViewById(R.id.set)
        set.setOnClickListener {
            val intent1 = Intent(this, Setting::class.java)
            startActivity(intent1)
            finish()
        }

        add = findViewById(R.id.add)
        add.setOnClickListener {
            val intent1 = Intent(this, choose_add::class.java)
            startActivity(intent1)
            finish()
        }

        // 修改為使用 ContextThemeWrapper 來套用自訂樣式
        menu = findViewById(R.id.menu)
        menu.setOnClickListener {
            // 使用 ContextThemeWrapper 應用自訂樣式
            val popupMenu = PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenu), menu)
            popupMenu.inflate(R.menu.wardrobe_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.manage_clothes -> {
                        Toast.makeText(this, "管理所友衣服", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, Wardrobe::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()  // 顯示 PopupMenu
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Load images from Firestore
        loadImagesFromFirestore()
    }

    private fun loadImagesFromFirestore() {
        val imageUrls = mutableListOf<String>()
        val documentIds = mutableListOf<String>() // List to hold document IDs
        val email = FirebaseAuth.getInstance().currentUser?.email

        if (!email.isNullOrEmpty()) {
            firestore.collection(email) // Use email as the collection name
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        if (document.id.contains("鞋子")) { // Check if document ID contains "上衣"
                            val imageUrl = document.getString("圖片網址")
                            if (!imageUrl.isNullOrEmpty()) {
                                imageUrls.add(imageUrl)
                                documentIds.add(document.id) // Add the document ID to the list
                            } else {
                                Log.d("Firestore", "Empty image URL found in document: ${document.id}")
                            }
                        }
                    }

                    // Create and set the adapter with document IDs
                    imageAdapter = add_shoes.ImageAdapter(this, imageUrls, documentIds)
                    recyclerView.adapter = imageAdapter
                    imageAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error loading images: ${exception.message}")
                }
        } else {
            Log.e("Firestore", "User email is null or empty.")
        }
    }
}