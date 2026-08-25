package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.FileOutputStream

class RemoveBgActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSelectImage: Button
    private lateinit var btnRemoveBg: Button
    private lateinit var btnSaveImage: Button

    private var imageUri: Uri? = null
    private var resultUri: Uri? = null
    private val apiKey = "6kXFQvqtwRWixcpfVP9Gc4LC" // 妳的 Remove.bg API Key

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

        ivPreview = findViewById(R.id.iv_preview)
        progressBar = findViewById(R.id.progress_bar)
        btnSelectImage = findViewById(R.id.btn_select_image)
        btnRemoveBg = findViewById(R.id.btn_remove_bg)
        btnSaveImage = findViewById(R.id.btn_save_image)

        btnSelectImage.setOnClickListener { selectImageLauncher.launch("image/*") }

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
                        Toast.makeText(this@RemoveBgActivity, "去背與裁切成功！", Toast.LENGTH_SHORT)
                            .show()
                        btnRemoveBg.visibility = View.GONE
                        btnSaveImage.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            this@RemoveBgActivity,
                            "去背失敗，請檢查網路",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        btnSaveImage.setOnClickListener {
            resultUri?.let { uri ->
                saveImageToAppAlbum(uri)
            } ?: run {
                Toast.makeText(this, "找不到去背後的圖片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun performRemoveBackground(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null) return@withContext null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image_file",
                    "image.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("size", "auto")
                .addFormDataPart("crop", "true")
                .build()

            val request = Request.Builder()
                .url("https://api.remove.bg/v1.0/removebg")
                .addHeader("X-Api-Key", apiKey)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()?.let { responseBytes ->
                    val originalBitmap =
                        BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
                    val croppedBitmap = cropTransparentEdges(originalBitmap)

                    val file = File(cacheDir, "temp_result_${System.currentTimeMillis()}.png")
                    val outputStream = FileOutputStream(file)
                    croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    return@withContext Uri.fromFile(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun cropTransparentEdges(bitmap: Bitmap): Bitmap {
        var top = bitmap.height
        var bottom = 0
        var left = bitmap.width
        var right = 0
        var isEmpty = true

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel != android.graphics.Color.TRANSPARENT) {
                    isEmpty = false
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (isEmpty) return bitmap

        val newWidth = right - left + 1
        val newHeight = bottom - top + 1
        return Bitmap.createBitmap(bitmap, left, top, newWidth, newHeight)
    }

    // 🌟 全新升級：儲存至 App 專屬的 Firebase 去背相簿 (記憶體直傳版)
    private fun saveImageToAppAlbum(uri: Uri) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val timestamp = System.currentTimeMillis()

// 🌟 校正回歸：讓系統自動去抓 google-services.json 裡的正確地址！
        val storageRef = FirebaseStorage.getInstance().reference.child("remove_bg/$email/$timestamp.png")

        progressBar.visibility = View.VISIBLE
        btnSaveImage.isEnabled = false
        btnSaveImage.text = "上傳至專屬相簿中..."

        // 🌟 終極殺招：避開 Android 檔案權限，直接把圖片轉成位元組 (Bytes) 從記憶體上傳！
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val data = inputStream?.readBytes()
            inputStream?.close()

            if (data == null) {
                Toast.makeText(this, "無法讀取圖片資料", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnSaveImage.isEnabled = true
                return
            }

            // 改用 putBytes(data) 進行上傳
            storageRef.putBytes(data).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val db = FirebaseFirestore.getInstance()
                    val mapData = hashMapOf(
                        "imageUrl" to downloadUrl.toString(),
                        "timestamp" to timestamp
                    )

                    db.collection(email).document("去背相簿").collection("images")
                        .document(timestamp.toString())
                        .set(mapData)
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            btnSaveImage.text = "儲存成功！"
                            Toast.makeText(
                                this@RemoveBgActivity,
                                "已儲存至專屬去背相簿！",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }.addOnFailureListener { exception ->
                    progressBar.visibility = View.GONE
                    btnSaveImage.isEnabled = true
                    Toast.makeText(
                        this@RemoveBgActivity,
                        "取得網址失敗: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                btnSaveImage.isEnabled = true
                btnSaveImage.text = "儲存去背照片到相簿"
                Toast.makeText(
                    this@RemoveBgActivity,
                    "上傳失敗: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            btnSaveImage.isEnabled = true
            Toast.makeText(this, "讀取檔案失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}