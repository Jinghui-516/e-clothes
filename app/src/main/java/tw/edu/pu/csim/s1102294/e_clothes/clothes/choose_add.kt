package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import tw.edu.pu.csim.s1120336.e_fit.Community.Friends
import tw.edu.pu.csim.s1120336.e_fit.Match.Match_home
import tw.edu.pu.csim.s1120336.e_fit.Match.Rank
import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.home
import tw.edu.pu.csim.s1120336.e_fit.clothes.New_clothes


class choose_add : AppCompatActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerColor: Spinner
    lateinit var bottomNavigationView: BottomNavigationView

    // 相機拍照結果處理
    private val takePictureResult =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val intent = Intent(this, New_clothes::class.java).apply {
                    putExtra("capturedPhoto", bitmap)
                    putExtra("category", spinnerCategory.selectedItem.toString())
                    putExtra("color", spinnerColor.selectedItem.toString())
                }
                startActivity(intent)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_add)

        // 綁定選單
        spinnerCategory = findViewById(R.id.spinner_category)
        spinnerColor = findViewById(R.id.spinner_color)

        // 🌟 設定底部導覽列 (讓它跟衣櫃行為一致)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        // 因為是從衣櫃點過來的，我們不強制標註任何按鈕，或標註 nav_wardrobe
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> {
                    startActivity(Intent(this, home::class.java))
                    finish()
                    true
                }
                R.id.nav_wardrobe -> {
                    startActivity(Intent(this, Wardrobe::class.java))
                    finish()
                    true
                }
                R.id.nav_community -> {
                    startActivity(Intent(this, Match_home::class.java))
                    finish()
                    true
                }
                R.id.nav_rank -> {
                    startActivity(Intent(this, Rank::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Personal_Page::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // 按鈕點擊邏輯
        findViewById<Button>(R.id.camera_btn).setOnClickListener {
            if (isSelectionValid()) checkPermission()
        }

        findViewById<Button>(R.id.photo_btn).setOnClickListener {
            if (isSelectionValid()) openPhotoAlbum()
        }
    }

    private fun isSelectionValid(): Boolean {
        val category = spinnerCategory.selectedItem.toString()
        val color = spinnerColor.selectedItem.toString()
        if (category == "請選擇種類" || color == "請選擇顏色") {
            Toast.makeText(this, "請先選擇服裝種類與顏色唷！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) takePictureResult.launch(null)
            }.launch(android.Manifest.permission.CAMERA)
        } else {
            takePictureResult.launch(null)
        }
    }

    private fun openPhotoAlbum() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, New_clothes::class.java).apply {
                putExtra("selectedImageUri", data.data.toString())
                putExtra("category", spinnerCategory.selectedItem.toString())
                putExtra("color", spinnerColor.selectedItem.toString())
            }
            startActivity(intent)
            finish()
        }
    }
}