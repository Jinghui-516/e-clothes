package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tw.edu.pu.csim.s1120336.e_fit.R
import java.io.File

class RemoveBgActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSelectImage: Button
    private lateinit var btnRemoveBg: Button
    private lateinit var btnSaveImage: Button

    private var imageUri: Uri? = null
    private var resultUri: Uri? = null
    private val apiKey = "6kXFQvqtwRWixcpfVP9Gc4LC" // 妳的 Remove.bg API Key

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            resultUri = null
            Glide.with(this).load(uri).into(ivPreview)
            btnRemoveBg.visibility = View.VISIBLE
            btnSaveImage.visibility = View.GONE
            btnSelectImage.text = "更換圖片"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_bg)

        // 綁定 UI 元件
        ivPreview = findViewById(R.id.iv_preview)
        progressBar = findViewById(R.id.progress_bar)
        btnSelectImage = findViewById(R.id.btn_select_image)
        btnRemoveBg = findViewById(R.id.btn_remove_bg)
        btnSaveImage = findViewById(R.id.btn_save_image)

        // 1. 選擇圖片
        btnSelectImage.setOnClickListener { selectImageLauncher.launch("image/*") }

        // 2. 執行去背
        btnRemoveBg.setOnClickListener {
            imageUri?.let { uri ->
                progressBar.visibility = View.VISIBLE
                btnRemoveBg.isEnabled = false
                btnRemoveBg.text = "AI 處理中..."

                lifecycleScope.launch {
                    val result = performRemoveBackground(uri)
                    progressBar.visibility = View.GONE
                    btnRemoveBg.isEnabled = true
                    btnRemoveBg.text = "開始 AI 去背"

                    if (result != null) {
                        resultUri = result
                        Glide.with(this@RemoveBgActivity).load(result).into(ivPreview)
                        Toast.makeText(this@RemoveBgActivity, "去背成功！", Toast.LENGTH_SHORT).show()
                        btnRemoveBg.visibility = View.GONE
                        btnSaveImage.visibility = View.VISIBLE // 顯示儲存按鈕
                    } else {
                        Toast.makeText(this@RemoveBgActivity, "去背失敗，請檢查網路", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 3. 儲存圖片至 Firebase
        btnSaveImage.setOnClickListener {
            resultUri?.let { uri ->
                saveImageToAppAlbum(uri)
            } ?: run {
                Toast.makeText(this, "找不到去背後的圖片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 核心去背 API 邏輯
    private suspend fun performRemoveBackground(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null) return@withContext null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", "image.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .addFormDataPart("size", "auto")
                .build()

            val request = Request.Builder()
                .url("https://api.remove.bg/v1.0/removebg")
                .addHeader("X-Api-Key", apiKey)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()?.let { responseBytes ->
                    // 🌟 修正重點：加上時間戳記，確保每次去背的暫存檔名不同，打破 Glide 快取魔咒！
                    val file = File(cacheDir, "temp_result_${System.currentTimeMillis()}.png")
                    file.writeBytes(responseBytes)
                    return@withContext Uri.fromFile(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 🌟 全新升級：儲存至 App 專屬的 Firebase 去背相簿
    private fun saveImageToAppAlbum(uri: Uri) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val timestamp = System.currentTimeMillis()

        // 設定 Storage 的儲存路徑
        val storageRef = FirebaseStorage.getInstance().reference.child("remove_bg/$email/$timestamp.png")

        // 更新 UI 狀態
        progressBar.visibility = View.VISIBLE
        btnSaveImage.isEnabled = false
        btnSaveImage.text = "上傳至專屬相簿中..."

        // 1. 先把圖片上傳到 Firebase Storage
        storageRef.putFile(uri).addOnSuccessListener {
            // 2. 上傳成功後，取得圖片的下載網址
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val db = FirebaseFirestore.getInstance()
                val data = hashMapOf(
                    "imageUrl" to downloadUrl.toString(),
                    "timestamp" to timestamp
                )

                // 3. 將圖片網址寫入 Firestore 建立相簿紀錄
                db.collection(email).document("去背相簿").collection("images").document(timestamp.toString())
                    .set(data)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        btnSaveImage.text = "儲存成功！"
                        Toast.makeText(this, "已儲存至專屬去背相簿！", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            btnSaveImage.isEnabled = true
            btnSaveImage.text = "儲存去背照片到相簿"
            Toast.makeText(this, "上傳失敗，請檢查網路連線", Toast.LENGTH_SHORT).show()
        }
    }
}