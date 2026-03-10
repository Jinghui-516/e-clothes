package tw.edu.pu.csim.s1102294.e_clothes.Match

import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import tw.edu.pu.csim.s1102294.e_clothes.R

class new_match_dress : AppCompatActivity(), GestureDetector.OnGestureListener {

    lateinit var gDetector: GestureDetector
    lateinit var imghat: ImageView
    lateinit var imgdress: ImageView
    lateinit var imgshoes: ImageView
    lateinit var btnNoDress: Button
    lateinit var next: ImageView
    lateinit var previous: ImageView

    val hat = arrayOf(R.drawable.c, R.drawable.k, R.drawable.l)
    val dress = arrayOf(R.drawable.a, R.drawable.i, R.drawable.j)
    val shoes = arrayOf(R.drawable.f, R.drawable.d, R.drawable.e)
    var currentImageIndex1 = 0
    var currentImageIndex2 = 0
    var currentImageIndex3 = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_match_dress)

        imghat = findViewById(R.id.imghat)
        imgdress = findViewById(R.id.imgdress)
        imgshoes = findViewById(R.id.imgshoes)
        btnNoDress = findViewById(R.id.btnNoDress)
        next = findViewById(R.id.next)
        previous = findViewById(R.id.previous)

        gDetector = GestureDetector(this, this)

        previous.setOnClickListener {
            val intent1 = Intent(this, Match_home::class.java)
            startActivity(intent1)
            finish()
        }

        btnNoDress.setOnClickListener {
            val intent1 = Intent(this, New_Match::class.java)
            startActivity(intent1)
            finish()
        }

        next.setOnClickListener {
            val intent1 = Intent(this, Edit_Label::class.java)
            startActivity(intent1)
            finish()
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
        val density = Resources.getSystem().displayMetrics.density
        val dpValue = 150 // 要转换为像素的 dp 值
        val pixels = (dpValue * density).toInt()

        if (Math.abs(velocityX) > Math.abs(velocityY) &&  e1.y >= (pixels+imghat.top) && e1.y <= (pixels+imghat.bottom)) {
            //Math.abs(velocityX) > Math.abs(velocityY)判斷是水平滑動還是垂直
            if (e1.x >= e2.x) {
                currentImageIndex1++
                if (currentImageIndex1 >= hat.size) {
                    currentImageIndex1 = 0
                }
            } else {
                currentImageIndex1--
                if (currentImageIndex1 < 0) {
                    currentImageIndex1 = hat.size - 1
                }
            }
        }else if(Math.abs(velocityX) > Math.abs(velocityY) &&  e1.y >= (pixels+imgdress.top) && e1.y <= (pixels+imgdress.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex2++
                if (currentImageIndex2 >= dress.size) {
                    currentImageIndex2 = 0
                }
            } else {
                currentImageIndex2--
                if (currentImageIndex2 < 0) {
                    currentImageIndex2 = dress.size - 1
                }
            }

        }else if(Math.abs(velocityX) > Math.abs(velocityY) &&  e1.y >= (pixels+imgshoes.top) && e1.y <= (pixels+imgshoes.bottom)) {
            if (e1.x >= e2.x) {
                currentImageIndex3++
                if (currentImageIndex3 >= shoes.size) {
                    currentImageIndex3 = 0
                }
            } else {
                currentImageIndex3--
                if (currentImageIndex3 < 0) {
                    currentImageIndex3 = shoes.size - 1
                }
            }
        }
        imghat.setImageResource(hat[currentImageIndex1])
        imgdress.setImageResource(dress[currentImageIndex2])
        imgshoes.setImageResource(shoes[currentImageIndex3])
        return true
    }

}