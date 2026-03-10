package tw.edu.pu.csim.s1102294.e_clothes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import coil.decode.GifDecoder
import coil.load
import java.util.*
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView: ImageView = findViewById(R.id.imageView)
//        imageView.load("file:///android_asset/animate_logo_gif.gif")
        Glide.with(this)
            .asGif() // 確保告訴 Glide 您要加載的是 GIF
            .load(R.drawable.animate_logo_gif) // 替換為您的 GIF 資源名稱
            .into(imageView)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val intent1 = Intent(this@MainActivity, whether_to_login::class.java)
                startActivity(intent1)
                finish()
            }
        }, 3000L) //3秒後跳轉頁面
    }
}