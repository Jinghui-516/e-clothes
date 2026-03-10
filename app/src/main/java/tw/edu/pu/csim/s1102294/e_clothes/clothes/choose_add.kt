package tw.edu.pu.csim.s1102294.e_clothes.clothes

import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.home

class choose_add : AppCompatActivity() {

    // Step 1: 新增变量用于处理相机结果
    private val takePictureResult =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val intent = Intent(this, New_clothes::class.java)
                intent.putExtra("capturedPhoto", bitmap) // 传递 Bitmap 对象
                startActivity(intent)
                finish()
            } else {
                // Step 2: 处理取消操作
                Toast.makeText(this, "取消拍照", Toast.LENGTH_SHORT).show()
            }
        }

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        when (it) {
            true -> takePictureResult.launch(null)
            false -> Toast.makeText(this, "相機權限未授權", Toast.LENGTH_SHORT).show()
        }
    }

    private val PICK_IMAGE_REQUEST = 1
    private var img: ImageView? = null

    lateinit var camera: ImageView
    lateinit var photo: ImageView
    lateinit var cabinet: ImageView
    lateinit var camera_btn: Button
    lateinit var photo_btn:Button
    lateinit var cabinet_btn: Button

    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var Clothes: ImageView
    lateinit var set: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_add)

        camera = findViewById(R.id.camera)
        camera.setOnClickListener {
            checkPermission() // Step 3: 检查权限并启动相机
        }
        camera_btn = findViewById(R.id.camera_btn)
        camera_btn.setOnClickListener {
            checkPermission()
        }
        photo = findViewById(R.id.photo)
        photo.setOnClickListener {
            openPhotoAlbum()
        }
        photo_btn = findViewById(R.id.photo_btn)
        photo_btn.setOnClickListener {
            openPhotoAlbum()
        }
        cabinet = findViewById(R.id.cabinet)
        cabinet.setOnClickListener {
            val intent = Intent(this, Wardrobe::class.java)
            startActivity(intent)
            finish()
        }
        cabinet_btn = findViewById(R.id.cabinet_btn)
        cabinet_btn.setOnClickListener {
            val intent = Intent(this, Wardrobe::class.java)
            startActivity(intent)
            finish()
        }
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
    }

    private fun checkPermission() {
        val permission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(android.Manifest.permission.CAMERA)
        } else {
            takePictureResult.launch(null)
        }
    }

    private fun openPhotoAlbum() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            val intent = Intent(this, New_clothes::class.java)
            intent.putExtra("selectedImageUri", selectedImageUri.toString()) // 修改为 String 类型
            startActivity(intent)
            finish()
        }
    }
}
