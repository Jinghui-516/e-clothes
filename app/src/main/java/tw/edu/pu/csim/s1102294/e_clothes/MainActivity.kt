package tw.edu.pu.csim.s1102294.e_clothes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import java.util.*                // 保留
import com.bumptech.glide.Glide    // 保留，因為你正在使用 Glide

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView: ImageView = findViewById(R.id.imageView)

        // 這裡你已經正確使用了 Glide，所以不需要 Coil
        Glide.with(this)
            .asGif()
            .load(R.drawable.animate_logo_gif)
            .into(imageView)

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val intent1 = Intent(this@MainActivity, login::class.java)
                startActivity(intent1)
                finish()
            }
        }, 3000L)
    }
}