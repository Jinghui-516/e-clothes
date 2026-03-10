package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import org.w3c.dom.Text
import tw.edu.pu.csim.s1102294.e_clothes.Community.Liked_Post
import tw.edu.pu.csim.s1102294.e_clothes.R
import tw.edu.pu.csim.s1102294.e_clothes.Setting
import tw.edu.pu.csim.s1102294.e_clothes.home

class edit_Chosen_Match : AppCompatActivity(), GestureDetector.OnGestureListener {

    lateinit var Home: ImageView
    lateinit var Match: ImageView
    lateinit var gDetector: GestureDetector
    lateinit var pages: TextView
    private var count: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_chosen_match)

        gDetector = GestureDetector(this, this)
        pages = findViewById(R.id.pages)
        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, Match_home::class.java)
            startActivity(intent1)
            finish()
        }

        val menu = findViewById<ImageView>(R.id.menu)
        menu.setOnClickListener {
            val popup = PopupMenu(this, menu)
            popup.menuInflater.inflate(R.menu.menu_share, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add -> {
                        Toast.makeText(this, "編輯個人資料", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, edit_Profile::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.check -> {
                        Toast.makeText(this, "編輯精選穿搭", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, edit_Chosen_Match::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.share -> {
                        Toast.makeText(this, "分享搭配", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, share_Match::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.like -> {
                        Toast.makeText(this, "喜歡的貼文", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, Liked_Post::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    R.id.settings -> {
                        Toast.makeText(this, "設定", Toast.LENGTH_SHORT).show()
                        val intent2 = Intent(this, Setting::class.java)
                        startActivity(intent2)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gDetector.onTouchEvent(event)
        return true
    }
    override fun onDown(p0: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent) {
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return true
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent) {

    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1.x >= e2.x) {
            count++
            if (count > 5) {
                count = 1
            }
        }
        else{
            count --
            if (count < 1){
                count = 5
            }
        }

        if (count == 1) {
            pages.text = "(1/5)"
        } else if(count == 2) {
            pages.text = "(2/5)"
        } else if(count == 3) {
            pages.text = "(3/5)"
        } else if(count == 4) {
            pages.text = "(4/5)"
        } else {
            pages.text = "(5/5)"
        }

        return true
    }
}