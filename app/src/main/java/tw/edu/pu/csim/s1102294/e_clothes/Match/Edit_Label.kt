package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Community.Personal_Page
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.clothes.FirebaseHelper
import tw.edu.pu.csim.s1102294.e_clothes.clothes.New_clothes
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.home
import tw.edu.pu.csim.s1102294.e_clothes.login

class Edit_Label : AppCompatActivity(), GestureDetector.OnGestureListener {

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

    lateinit var clothes: ImageView
    lateinit var weather_name: TextView
    lateinit var add_label: ImageView
    lateinit var label: EditText
    lateinit var match_name: EditText
    var userId: String? = null
    lateinit var firebaseHelper: FirebaseHelper
    var imageUrl: String? = null
    private val labelTexts = mutableListOf<String>()

    lateinit var handsome: Button
    lateinit var cute: Button
    lateinit var daily: Button
    lateinit var easy: Button
    lateinit var formal: Button

    lateinit var finish: ImageView
    lateinit var previous: ImageView
    lateinit var gDetector: GestureDetector
    lateinit var imgDisplay: ImageView

    private var imageUrls: MutableList<String> = mutableListOf()
    private var currentImageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_label)

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
        match_name = findViewById(R.id.match_name)
        handsome = findViewById(R.id.handsome)
        handsome.setOnClickListener { label.setText(handsome.text) }

        cute = findViewById(R.id.cute)
        cute.setOnClickListener { label.setText(cute.text) }

        daily = findViewById(R.id.daily)
        daily.setOnClickListener { label.setText(daily.text) }

        easy = findViewById(R.id.easy)
        easy.setOnClickListener { label.setText(easy.text) }

        formal = findViewById(R.id.formal)
        formal.setOnClickListener { label.setText(formal.text) }

        finish = findViewById(R.id.finish)
        finish.setOnClickListener {
            saveDataToFirestore(db)
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                startActivity(Intent(this@Edit_Label, Match_home::class.java))
                finish()
            }
        }

        previous = findViewById(R.id.previous)
        previous.setOnClickListener {
            startActivity(Intent(this, New_Match::class.java))
            finish()
        }

        imgDisplay = findViewById(R.id.imgDisplay)

        // 接收圖片 URL
        val hatUrl = intent.getStringExtra("hatUrl")
        val clothesUrl = intent.getStringExtra("clothesUrl")
        val pantsUrl = intent.getStringExtra("pantsUrl")
        val shoesUrl = intent.getStringExtra("shoesUrl")
//        val bodyPhotoUrl = intent.getStringExtra("bodyPhotoUrl")

        val imageUriString = intent.getStringExtra("bodyPhotoUrl")
        val imageBitmap = intent.getParcelableExtra<Bitmap>("capturedPhoto")

// 根据传递的内容设置ImageView
        when {
            imageUriString != null -> {
                val imageUri = Uri.parse(imageUriString)
                imageUrls.add(imageUriString)  // Add the new image URL to the list
                currentImageIndex = imageUrls.size - 1  // Update to point to the new image
                Picasso.get().load(imageUri).into(imgDisplay) // Use Picasso to load the image from URI
            }
            imageBitmap != null -> {
                // Convert the Bitmap to a file URI if necessary, and add it to the imageUrls
                val fileUri = Uri.parse(MediaStore.Images.Media.insertImage(contentResolver, imageBitmap, "CapturedImage", ""))
                imageUrls.add(fileUri.toString())  // Add the new image URI to the list
                currentImageIndex = imageUrls.size - 1  // Update to point to the new image
                imgDisplay.setImageBitmap(imageBitmap) // Display the image directly
            }
            else -> {
                Toast.makeText(this, "加載失敗", Toast.LENGTH_LONG).show()
            }
        }

        imageUrls.add(hatUrl ?: "")
        imageUrls.add(clothesUrl ?: "")
        imageUrls.add(pantsUrl ?: "")
        imageUrls.add(shoesUrl ?: "")
        imageUrls.add(imageUriString ?: "")

        updateImage()

        gDetector = GestureDetector(this, this)

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

        addClothes = findViewById(R.id.addClothes)
        addClothes.setOnClickListener {
            startActivity(Intent(this, choose_add::class.java))
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            startActivity(Intent(this, Friends::class.java))
            finish()
        }

        Personal_page = findViewById(R.id.Personal_page)
        Personal_page.setOnClickListener {
            startActivity(Intent(this, Personal_Page::class.java))
            finish()
        }

        firebaseHelper = FirebaseHelper()
        weather_name = findViewById(R.id.weather_name)
        val weather: ImageView = findViewById(R.id.weather)
        weather.setOnClickListener {
            showWeatherMenu(it)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.labelRecyclerView)
        adapter = LabelAdapter(labelList) { position -> showDeleteDialog(position) }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        add_label = findViewById(R.id.add_label)
        add_label.setOnClickListener {
            if (label.text.isNotEmpty()) {
                val newLabel = label.text.toString()
                labelList.add(newLabel)
                adapter.notifyItemInserted(labelList.size - 1)
                labelTexts.add(newLabel)
                label.text.clear()
            } else {
                Toast.makeText(this, "標籤不能為空", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("刪除標籤")
        builder.setMessage("確定要刪除此標籤嗎？")
        builder.setPositiveButton("是") { _, _ ->
            labelList.removeAt(position)
            labelTexts.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        builder.setNegativeButton("否", null)
        builder.show()
    }

    private fun saveDataToFirestore(db: FirebaseFirestore) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.email?.let { email ->
            val weatherCategory = weather_name.text.toString()
            val hatUrl = imageUrls.getOrNull(0) ?: ""
            val clothesUrl = imageUrls.getOrNull(1) ?: ""
            val pantsUrl = imageUrls.getOrNull(2) ?: ""
            val shoesUrl = imageUrls.getOrNull(3) ?: ""
            val bodyPhotoUrl = imageUrls.getOrNull(4) ?: ""

            db.collection(email)
                .get()
                .addOnSuccessListener { documents ->
                    val maxNumber = documents.mapNotNull { doc -> doc.id.replace("搭配", "").toIntOrNull() }.maxOrNull() ?: 0
                    val newDocumentName = "搭配${maxNumber + 1}"
                    val matchName = match_name.text?.toString().orEmpty()
                    val labelsList = labelTexts.toList()

                    val data = hashMapOf(
                        "搭配名稱" to matchName,
                        "天氣種類" to weatherCategory,
                        "標籤" to labelsList,
                        "上衣圖片網址" to clothesUrl,
                        "鞋子圖片網址" to shoesUrl,
                        "褲子圖片網址" to pantsUrl,
                        "帽子圖片網址" to hatUrl,
                        "身體照" to bodyPhotoUrl
                    )

                    db.collection(email)
                        .document(newDocumentName)
                        .set(data)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Document added with ID: $newDocumentName")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error adding document", e)
                        }
                }
        }
    }

    // 顯示天氣選擇菜單的函數
    private fun showWeatherMenu(view: View) {
        val weatherOptions = arrayOf("晴天", "陰天", "雨天", "雪天")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("選擇天氣")
        builder.setItems(weatherOptions) { dialog, which ->
            // 根據選擇的天氣，將其顯示在 weather_name TextView 中
            weather_name.text = weatherOptions[which]
        }
        builder.show()
    }

    private fun updateImage() {
        val imageUrlToLoad = if (imageUrls[currentImageIndex].isNotEmpty()) {
            imageUrls[currentImageIndex]
        } else {
            "https://www.example.com/default_image.jpg"  // 用你自己預設的圖片 URL
        }
        Picasso.get().load(imageUrlToLoad).error(R.drawable.m).into(imgDisplay)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1.x - e2.x > 50) {
            currentImageIndex = (currentImageIndex + 1) % imageUrls.size
        } else if (e2.x - e1.x > 50) {
            currentImageIndex = (currentImageIndex - 1 + imageUrls.size) % imageUrls.size
        }
        updateImage()
        return true
    }

    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onLongPress(e: MotionEvent) {}
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onSingleTapUp(e: MotionEvent): Boolean = true
}
