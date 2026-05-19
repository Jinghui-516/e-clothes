package tw.edu.pu.csim.s1120336.e_fit.Match

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1120336.e_fit.Community.Personal_Page
import tw.edu.pu.csim.s1120336.e_fit.R

class Edit_Label : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 🌟 用來存放使用者「目前勾選了哪些風格標籤」的清單
    private val selectedStyles = mutableListOf<String>()

    lateinit var finish: ImageView
    lateinit var previous: ImageView

    // 宣告畫面上的風格按鈕
    lateinit var btnHandsome: Button
    lateinit var btnCute: Button
    lateinit var btnDaily: Button
    lateinit var btnEasy: Button
    lateinit var btnFormal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_label)

        // 確保用戶已登入
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "用戶未登入，請先登入", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 1. 綁定按鈕元件
        btnHandsome = findViewById(R.id.handsome)
        btnCute = findViewById(R.id.cute)
        btnDaily = findViewById(R.id.daily)
        btnEasy = findViewById(R.id.easy)
        btnFormal = findViewById(R.id.formal)
        finish = findViewById(R.id.finish)
        previous = findViewById(R.id.previous)

        // 2. 🌟 先去雲端撈取使用者「原本就選過」的風格，讓按鈕維持選取狀態，體驗更貼心
        loadExistingStyles()

        // 3. 設定按鈕的點擊多選魔法（點第一次加入並變色，再點一次移出並復原）
        setupStyleButton(btnHandsome, "#帥氣")
        setupStyleButton(btnCute, "#可愛")
        setupStyleButton(btnDaily, "#日常")
        setupStyleButton(btnEasy, "#休閒")
        setupStyleButton(btnFormal, "#正式")

        // 4. 返回按鈕
        previous.setOnClickListener {
            finish() // 直接結束，回到個人資料頁面
        }

        // 5. 🌟 完成儲存按鈕：把多選完的清單一次打包存進個人 Profile 裡
        finish.setOnClickListener {
            saveStylesToFirebase()
        }
    }

    /**
     * 🌟 核心多選切換邏輯：控制標籤選取、取消與視覺顏色變換
     */
    private fun setupStyleButton(button: Button, styleName: String) {
        button.setOnClickListener {
            if (selectedStyles.contains(styleName)) {
                // 如果已經選過了 ➔ 移出選取清單
                selectedStyles.remove(styleName)
                button.setBackgroundColor(Color.parseColor("#EFEFEF")) // 換回原本未選取的淡灰色
                button.setTextColor(Color.parseColor("#4A3E3D"))     // 換回深色文字
            } else {
                // 如果還沒選過 ➔ 新增進去
                selectedStyles.add(styleName)
                button.setBackgroundColor(Color.parseColor("#745E4D")) // 🌟 換成妳們 App 的招牌質感咖啡色！
                button.setTextColor(Color.WHITE)                     // 文字變白色
            }
        }
    }

    /**
     * 🌟 讀取現有風格：一開網頁自動幫使用者勾選好以前選過的標籤
     */
    private fun loadExistingStyles() {
        val email = auth.currentUser?.email ?: return
        db.collection(email).document("profile").get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val savedStyles = document.get("myStyles") as? List<*>
                savedStyles?.forEach {
                    val style = it.toString()
                    selectedStyles.add(style)
                    // 同步點亮原本就選好的按鈕顏色
                    matchButtonUI(style)
                }
            }
        }
    }

    /**
     * 根據從雲端抓下來的標籤，點亮對應按鈕
     */
    private fun matchButtonUI(style: String) {
        when (style) {
            "#帥氣" -> setButtonSelectedVisual(btnHandsome)
            "#可愛" -> setButtonSelectedVisual(btnCute)
            "#日常" -> setButtonSelectedVisual(btnDaily)
            "#休閒" -> setButtonSelectedVisual(btnEasy)
            "#正式" -> setButtonSelectedVisual(btnFormal)
        }
    }

    private fun setButtonSelectedVisual(button: Button) {
        button.setBackgroundColor(Color.parseColor("#745E4D"))
        button.setTextColor(Color.WHITE)
    }

    /**
     * 🌟 寫入個人資料庫：精準存入個人 profile 中，完美銜接主頁刷新
     */
    private fun saveStylesToFirebase() {
        val email = auth.currentUser?.email ?: return

        val data = hashMapOf(
            "myStyles" to selectedStyles // 直接用 List 形式覆蓋存檔
        )

        Toast.makeText(this, "正在儲存風格標籤...", Toast.LENGTH_SHORT).show()

        // 🌟 對齊妳在 Personal_Page.kt 寫的讀取路徑：集合(email) -> 文件("profile")
        db.collection(email).document("profile")
            .set(data, com.google.firebase.firestore.SetOptions.merge()) // 用 merge 確保原本的其他資料不被蓋掉
            .addOnSuccessListener {
                Toast.makeText(this, "風格標籤設定成功！", Toast.LENGTH_SHORT).show()
                // 儲存成功，高高興興返回個人主頁，這時 Personal_Page 的 onResume 會自動抓到最新改好的標籤！
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "儲存失敗，請重試", Toast.LENGTH_SHORT).show()
                Log.e("Edit_Label", "儲存風格失敗: ${e.message}")
            }
    }
}