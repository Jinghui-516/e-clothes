package tw.edu.pu.csim.s1120336.e_fit.clothes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tw.edu.pu.csim.s1120336.e_fit.R

class Chat_AI : AppCompatActivity() {

    // 基本 UI 元件
    private lateinit var btnBack: ImageButton
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var recyclerView: RecyclerView

    // 對話列表資料
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_ai)

        // 1. 初始化基本元件綁定
        btnBack = findViewById(R.id.btn_chat_back)
        etInput = findViewById(R.id.et_message_input)
        btnSend = findViewById(R.id.btn_send)
        recyclerView = findViewById(R.id.recycler_chat)

        // 2. 設定 RecyclerView
        chatAdapter = ChatAdapter(chatList)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = chatAdapter

        // 3. 🌟 進入畫面，接收首頁傳來的真實氣溫，並主動打招呼
        val initialGreeting = generateWeatherGreeting()
        addMessage(initialGreeting, false)

        // 4. 返回按鈕
        btnBack.setOnClickListener { finish() }

        // 5. 綁定 9 大風格按鈕的點擊劇本
        setupStyleButtons()

        // 6. 送出按鈕的高智商通靈邏輯
        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                // 先顯示使用者輸入的訊息
                addMessage(userText, true)
                etInput.setText("")

                // 模擬 AI 思考 0.5 秒，感覺更真實
                Handler(Looper.getMainLooper()).postDelayed({
                    val botReply = when {
                        // 如果使用者主動問到天氣
                        userText.contains("天氣") || userText.contains("溫度") || userText.contains("氣溫") ->
                            "【天氣穿搭】最近天氣變化比較大，出門最好採用『洋蔥式穿搭』，以短袖為主，再隨身帶一件輕薄的外套就萬無一失囉！"

                        // 類別一：基本打招呼與禮貌用語
                        userText.contains("你好") || userText.contains("嗨") || userText.contains("哈囉") ->
                            "哈囉！今天想找什麼靈感呢？可以直接跟我說妳要去哪裡，或是直接點選下方的風格按鈕唷！"
                        userText.contains("謝謝") || userText.contains("好") || userText.contains("讚") ->
                            "不客氣！希望能幫妳穿出自信每一天 🥰 如果還想嘗試新風格，隨時呼叫我！"

                        // 類別二：各種「場合」穿搭
                        userText.contains("約會") || userText.contains("男朋友") || userText.contains("男友") ->
                            "【約會穿搭】如果另一半比較高挑，不妨大膽穿上有跟的短靴或厚底鞋拉長比例，配上微收腰的連身洋裝，絕對讓他眼睛一亮！"
                        userText.contains("出國") || userText.contains("旅行") || userText.contains("玩") ->
                            "【旅遊穿搭】如果是去比較冷的地方，建議採用『洋蔥式穿搭』！內搭發熱衣，外面套一件好穿脫的防風大衣，拍照好看又保暖！"
                        userText.contains("開車") || userText.contains("兜風") || userText.contains("出去") ->
                            "【開車出遊】去兜風穿搭當然要以舒適為主！推薦寬鬆的工裝褲配上平底休閒鞋，下車拍照也超有型！"
                        userText.contains("面試") || userText.contains("報告") || userText.contains("正式") ->
                            "【正式場合】建議選擇微寬鬆的西裝外套，內搭素色雪紡衫，下半身配上九分西裝褲與樂福鞋。專業俐落又不死板！"
                        userText.contains("運動") || userText.contains("健身") ->
                            "【運動機能】直接穿上高腰瑜珈褲配上運動內衣，外面套一件短版防風薄外套，時髦的運動女孩就是妳！"

                        // 類別三：根據「天氣」穿搭
                        userText.contains("熱") || userText.contains("夏天") ->
                            "【炎熱天氣】建議穿著棉麻材質的無袖上衣或短T，配上寬鬆的落地涼感褲，透氣舒服又能防曬！"
                        userText.contains("冷") || userText.contains("寒流") || userText.contains("冬天") ->
                            "【禦寒穿搭】保暖最重要！推薦高領針織毛衣配上毛呢長裙，外面套一件長版大衣，腳踩長靴，保暖又顯瘦！"
                        userText.contains("雨") || userText.contains("下雨") ->
                            "【雨天穿搭】雨天推薦穿深色短褲或防潑水材質的九分褲，鞋子選防水的厚底靴，就算踩到水窪也不怕。"

                        // 類別四：身形修飾與困擾
                        userText.contains("胖") || userText.contains("顯瘦") || userText.contains("修飾") ->
                            "【顯瘦秘訣】記住『上寬下窄』或『上窄下寬』的黃金法則！如果是大腿肉肉，可以選 A 字裙或深色寬褲喔！"
                        userText.contains("腿") || userText.contains("比例") || userText.contains("矮") ->
                            "【拉長比例】『高腰』是妳最好的朋友！把上衣紮進高腰褲或高腰裙裡，瞬間營造胸部以下都是腿的視覺效果！"

                        // 類別五：原本的風格關鍵字
                        userText.contains("帥") || userText.contains("酷") ->
                            "【帥氣建議】短版黑色皮衣 + 內搭白T + 高腰直筒牛仔褲 + 馬丁靴。線條俐落，帥氣度直接爆表！"
                        userText.contains("甜") || userText.contains("裙") || userText.contains("可愛") ->
                            "【甜美建議】法式小碎花連身裙 + 米色針織薄外套 + 帆布鞋。再戴上一頂燕麥色貝雷帽，少女初戀感滿滿！"

                        // 類別六：追問、不滿意、想要其他的
                        userText.contains("其他") || userText.contains("更多") || userText.contains("還有") ->
                            "【更多靈感】想看點不一樣的嗎？可以試試看今年的『波西米亞風』或『千禧 Y2K』，直接點擊下方按鈕看看專屬公式吧！"
                        userText.contains("不喜歡") || userText.contains("換") || userText.contains("醜") ->
                            "【風格切換】沒問題，穿搭就是要多方嘗試！不如試試看『極簡主義』？簡單乾淨的黑白灰色系通常最百搭不出錯唷！"

                        // 萬用防呆回覆
                        else ->
                            "【顧問分析】這個想法很有趣！不過我最拿手的是搭配經典風格，建議妳點選下方的【快捷按鈕】，裡面有我精心準備的 9 大最流行風格公式唷！"
                    }
                    // 把 AI 的回覆顯示出來
                    addMessage(botReply, false)
                }, 500)
            } else {
                Toast.makeText(this@Chat_AI, "請先輸入想問的問題，或是直接點選下方風格按鈕唷！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🌟 核心修改：接收首頁傳來的真實氣溫，並根據氣溫給建議
    private fun generateWeatherGreeting(): String {
        // 從 Intent 接收首頁傳過來的真實氣溫 (預設值 25 是為了防止首頁沒抓到資料時出錯)
        val currentTemp = intent.getIntExtra("REAL_TEMP", 25)

        // 根據真實溫度自動改變台詞
        val weatherAdvice = when {
            currentTemp >= 28 -> "目前當地氣溫是 ${currentTemp}°C，滿熱的 ☀️\n建議穿著透氣的短袖或棉麻材質，出門記得防曬喔！"
            currentTemp in 24..27 -> "目前當地氣溫是 ${currentTemp}°C，非常舒適 ⛅\n穿件簡單的短袖搭配薄外套就非常完美！"
            else -> "目前當地氣溫大約 ${currentTemp}°C，稍微偏涼 ❄️\n出門記得多加一件長袖或針織衫，別著涼囉！"
        }

        return "哈囉！我是妳的 e-fit 智慧顧問 🤖🎀\n$weatherAdvice\n\n今天想嘗試什麼穿搭風格呢？點擊下方的【圓角快捷鍵】我立刻給妳建議唷！"
    }

    // 設定 9 大風格按鈕的專屬劇本
    private fun setupStyleButtons() {
        val styles = mapOf(
            R.id.btn_handsome to "【😎 帥氣個性】\n建議：黑色短版皮衣 + 白T + 高腰直筒褲 + 馬丁靴。線條俐落又帥氣！",
            R.id.btn_cute to "【🎀 甜美可愛】\n建議：碎花連身裙 + 米色針織外套 + 帆布鞋。初戀感爆表！",
            R.id.btn_daily to "【☕ 韓系休閒】\n建議：寬鬆衛衣 + 白襯衫疊搭 + 灰色縮口褲。隨性時髦！",
            R.id.btn_retro to "【🎞️ 復古摩登】\n建議：格紋襯衫 + 高腰喇叭褲 + 樂福鞋。摩登感十足！",
            R.id.btn_sporty to "【👟 運動機能】\n建議：機能上衣 + 瑜珈褲 + 防風外套。動感又有型！",
            R.id.btn_minimalist to "【⚖️ 極簡主義】\n建議：質感白襯衫 + 灰色西裝褲。簡單就是高級！",
            R.id.btn_boho to "【🌵 波西米亞】\n建議：蕾絲長裙 + 皮革騎士靴。隨性浪漫感！",
            R.id.btn_street to "【🛹 美式街頭】\n建議：帽T + 工裝褲 + 滑板鞋。潮流感滿分！",
            R.id.btn_y2k to "【💿 千禧 Y2K】\n建議：短版亮色上衣 + 低腰牛仔褲 + 厚底鞋。復古又閃耀！"
        )

        for ((id, advice) in styles) {
            findViewById<Button>(id).setOnClickListener {
                val styleName = (it as Button).text.toString()
                // 顯示按鈕名稱
                addMessage(styleName, true)
                // 模擬思考 0.3 秒後給出建議
                Handler(Looper.getMainLooper()).postDelayed({ addMessage(advice, false) }, 300)
            }
        }
    }

    // 處理訊息對話框的共用方法
    private fun addMessage(message: String, isUser: Boolean) {
        chatList.add(ChatMessage(message, isUser))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        recyclerView.scrollToPosition(chatList.size - 1)
    }
}