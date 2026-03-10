package tw.edu.pu.csim.s1102294.e_clothes.clothes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.net.wifi.EasyConnectStatusCallback
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Community.Personal_Page
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.home
import tw.edu.pu.csim.s1102294.e_clothes.login
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class New_clothes : AppCompatActivity() {

    class LabelAdapter(
        private val labels: MutableList<String>,
        private val onLabelLongPress: (Int) -> Unit // Pass the index of the label to be deleted
    ) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

        class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_label, parent, false)
            return LabelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
            holder.textView.text = labels[position]

            // Set long press listener on each label
            holder.itemView.setOnLongClickListener {
                onLabelLongPress(position) // Trigger long press action
                true
            }
        }

        override fun getItemCount(): Int {
            return labels.size
        }
    }

    private val labelList = mutableListOf<String>()
    private lateinit var adapter: LabelAdapter


    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var addClothes: ImageView
    lateinit var Personal_page: ImageView

    lateinit var finish: ImageView
    lateinit var previous: ImageView
    lateinit var clothes: ImageView
    lateinit var Classification_name: TextView
    lateinit var weather_name: TextView
    lateinit var add_label: ImageView
    lateinit var label: EditText
    var userId: String? = null
    lateinit var firebaseHelper: FirebaseHelper
    var imageUrl: String? = null // 新增用來存儲圖片的URL
    private val labelTexts = mutableListOf<String>()

    lateinit var handsome: Button
    lateinit var cute: Button
    lateinit var daily: Button
    lateinit var easy: Button
    lateinit var formal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_clothes)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "用戶未登入，請先登入", Toast.LENGTH_LONG).show()
            val loginIntent = Intent(this, login::class.java)
            startActivity(loginIntent)
            finish()
            return
        } else {
            userId = currentUser.uid
        }

        val db = FirebaseFirestore.getInstance()

        label = findViewById(R.id.label)
        handsome = findViewById(R.id.handsome)
        handsome.setOnClickListener {
            label.setText(handsome.text)
        }

        cute = findViewById(R.id.cute)
        cute.setOnClickListener {
            label.setText(cute.text)
        }

        daily = findViewById(R.id.daily)
        daily.setOnClickListener {
            label.setText(daily.text)
        }

        easy = findViewById(R.id.easy)
        easy.setOnClickListener {
            label.setText(easy.text)
        }

        formal = findViewById(R.id.formal)
        formal.setOnClickListener {
            label.setText(formal.text)
        }

        clothes = findViewById(R.id.clothes)

        // 获取传递过来的图片URI或Bitmap
        val imageUriString = intent.getStringExtra("selectedImageUri")
        val imageBitmap = intent.getParcelableExtra<Bitmap>("capturedPhoto")

        // 根据传递的内容设置ImageView
        when {
            imageUriString != null -> {
                val imageUri = Uri.parse(imageUriString)
                clothes.setImageURI(imageUri)
            }
            imageBitmap != null -> {
                clothes.setImageBitmap(imageBitmap)
            }
            else -> {
                Toast.makeText(this, "加載失敗", Toast.LENGTH_LONG).show()
            }
        }

        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
            val intent2 = Intent(this, Match_home::class.java)
            startActivity(intent2)
            finish()
        }

        addClothes = findViewById(R.id.addClothes)
        addClothes.setOnClickListener {
            val intent2 = Intent(this, choose_add::class.java)
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }

        Personal_page = findViewById(R.id.Personal_page)
        Personal_page.setOnClickListener {
            val intent1 = Intent(this, Personal_Page::class.java)
            startActivity(intent1)
            finish()
        }

        finish = findViewById(R.id.finish)
        firebaseHelper = FirebaseHelper()

//        finish.setOnClickListener {
//            val bitmap = (clothes.drawable as? BitmapDrawable)?.bitmap
//            if (bitmap != null) {
//                val imageUri = getImageUriFromBitmap(this, bitmap)
//                if (imageUri != null) {
//                    firebaseHelper.uploadImage(this, imageUri, onSuccess = { fullUrl ->
//                        // Extract the relative path from the full URL
//                        val relativePath =
//                            fullUrl.substringAfter("/o/").substringBefore("?alt=media")
//                        imageUrl = relativePath  // Now `imageUrl` stores the relative path
//                        saveDataToFirestore(db)   // Proceed to save the data to Firestore
//                    }, onFailure = { e ->
//                        Toast.makeText(this, "上傳失敗: ${e.message}", Toast.LENGTH_LONG).show()
//                    })
//                } else {
//                    Toast.makeText(this, "無法取得URL", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Toast.makeText(this, "加載失敗", Toast.LENGTH_SHORT).show()
//            }
//        }

        finish.setOnClickListener {
            val bitmap = (clothes.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val imageUri = getImageUriFromBitmap(this, bitmap)
                if (imageUri != null) {
                    firebaseHelper.uploadImage(this, imageUri, onSuccess = { fullUrl ->
                        // Extract the relative path, replace %2F with /, and add a leading /
                        val relativePath =
                            "/" + fullUrl.substringAfter("/o/").substringBefore("?alt=media")
                                .replace("%2F", "/")
                        imageUrl = relativePath  // Now `imageUrl` stores the cleaned relative path with a leading /
                        saveDataToFirestore(db, fullUrl)   // Pass the fullUrl here to save it to Firestore
                    }, onFailure = { e ->
                        Toast.makeText(this, "上傳失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    })
                } else {
                    Toast.makeText(this, "無法取得URL", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "加載失敗", Toast.LENGTH_SHORT).show()
            }
        }

        previous = findViewById(R.id.previous)
        previous.setOnClickListener {
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Classification_name = findViewById(R.id.Classification_name)
        val Classification: ImageView = findViewById(R.id.Classification)
        val popupMenu =
            PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenu), Classification)
        Classification.setOnClickListener { view ->
            showPopupMenu(view)
        }
        weather_name = findViewById(R.id.weather_name)
        val weather: ImageView = findViewById(R.id.weather)
        val popupMenu1 =
            PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenu), weather)
        weather.setOnClickListener { view ->
            showWeatherMenu(view)
        }


        // 初始化 RecyclerView 和 Adapter
        val recyclerView = findViewById<RecyclerView>(R.id.labelRecyclerView)
        adapter = LabelAdapter(labelList) { position ->
            showDeleteDialog(position) // Show confirmation dialog on long press
        }
        recyclerView.adapter = adapter

        // 設定每行顯示 4 個項目
        val gridLayoutManager = GridLayoutManager(this, 4)
        recyclerView.layoutManager = gridLayoutManager

// 添加新標籤的邏輯
        add_label = findViewById(R.id.add_label)
        add_label.setOnClickListener {
            if (label.text.isNotEmpty()) {
                val newLabel = label.text.toString()

                // Add to display list
                labelList.add(newLabel)
                adapter.notifyItemInserted(labelList.size - 1)

                // Add to the list for Firestore
                labelTexts.add(newLabel)

                // Clear the input field
                label.text.clear()
            } else {
                Toast.makeText(this, "標籤不能為空", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 長按刪除標籤的方法
    private fun showDeleteDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("刪除標籤")
        builder.setMessage("確定要刪除此標籤嗎？")
        builder.setPositiveButton("是") { _, _ ->
            // 確認刪除標籤
            labelList.removeAt(position)  // 從顯示列表中刪除
            labelTexts.removeAt(position) // 從要保存到 Firestore 的列表中同步刪除

            // 通知適配器有項目被刪除，刷新 RecyclerView
            adapter.notifyItemRemoved(position)
        }
        builder.setNegativeButton("否", null)
        builder.show()
    }

    private fun saveDataToFirestore(db: FirebaseFirestore, fullUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            val category = Classification_name.text.toString()
            val weatherCategory = weather_name.text.toString()

            if (email != null) {
                db.collection(email)
                    .whereEqualTo("服裝種類", category)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        val newDocumentName = if (documents.isEmpty) {
                            "${category}1"
                        } else {
                            val lastDocumentName = documents.first().id
                            val lastNumber = lastDocumentName.replace(category, "").toIntOrNull() ?: 0
                            "$category${lastNumber + 1}"
                        }

                        val user = hashMapOf(
                            "服裝種類" to category,
                            "天氣種類" to weatherCategory,
                            "圖片網址" to imageUrl,
                            "圖片完整網址" to fullUrl,  // Store the full URL here
                            "標籤" to labelTexts
                        )

                        db.collection(email)
                            .document(newDocumentName)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "新增完成", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, home::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("New_clothes", "新增失敗: ${e.message}")
                                Toast.makeText(this, "新增失敗: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("New_clothes", "獲取最新文件失敗: ${e.message}")
                        Toast.makeText(this, "獲取最新文件失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "用戶未登入", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "用戶未登入", Toast.LENGTH_LONG).show()
        }
    }


    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.classification_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.hat -> Classification_name.text = "頭飾"
                R.id.clothes -> Classification_name.text = "上衣"
                R.id.pants -> Classification_name.text = "褲子"
                R.id.shoes -> Classification_name.text = "鞋子"
                R.id.dress -> Classification_name.text = "洋裝"
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popupMenu.show()
    }

    private fun showWeatherMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.weather_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sunny -> weather_name.text = "晴天"
                R.id.shower -> weather_name.text = "雨天"
                R.id.cloudy -> weather_name.text = "陰天"
                R.id.raining -> weather_name.text = "雷雨"
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popupMenu.show()
    }

    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri? {
        val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}
