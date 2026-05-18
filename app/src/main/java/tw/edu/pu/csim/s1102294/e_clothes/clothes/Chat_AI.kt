package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content // 🌟 這個 import 很重要，用來打包圖片和文字
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import tw.edu.pu.csim.s1120336.e_fit.R

class Chat_AI : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var recyclerView: RecyclerView

    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // 🌟 保持使用最新版的 flash 模型
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyCjc1L2VTsxOPjJEi-8MDmXnND5-7O1-d8" // 👈 記得貼回你的鑰匙
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_ai)

        btnBack = findViewById(R.id.btn_chat_back)
        etInput = findViewById(R.id.et_message_input)
        btnSend = findViewById(R.id.btn_send_message)
        recyclerView = findViewById(R.id.recycler_chat)

        chatAdapter = ChatAdapter(chatList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = chatAdapter

        addMessage("哈囉！我是 e-fit-ai 🤖\n你可以問我：「這件衣服適合配什麼？」我會直接看你衣櫃裡的照片給你建議唷！", false)

        btnBack.setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                addMessage(userText, true)
                etInput.setText("")

                addMessage("正在翻找你的衣櫃並思考搭配中...", false)
                val loadingIndex = chatList.size - 1

                // 🌟 啟動全自動通靈模式：先去抓衣服照片，再問 AI
                fetchWardrobeAndAskAI(userText, loadingIndex)

            } else {
                Toast.makeText(this, "請先輸入想問 AI 的問題唷！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🌟 核心功能 1：去 Firestore 找使用者的衣服
    private fun fetchWardrobeAndAskAI(userText: String, loadingIndex: Int) {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email

        if (email == null) {
            sendPureTextToAI(userText, loadingIndex)
            return
        }

        // 為了示範且不拖慢速度，我們設定讓 AI 先讀取衣櫥裡的一件「上衣」
        db.collection(email)
            .whereEqualTo("服裝種類", "上衣")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // 找到上衣了！拿圖片網址去 Storage 下載實體圖片
                    val imageUrl = documents.documents[0].getString("圖片網址") ?: ""
                    downloadImageAndAskAI(imageUrl, userText, loadingIndex)
                } else {
                    // 衣櫃裡還沒有上衣，啟動備用方案：純文字問答
                    sendPureTextToAI(userText, loadingIndex)
                }
            }
            .addOnFailureListener {
                sendPureTextToAI(userText, loadingIndex)
            }
    }

    // 🌟 核心功能 2：下載圖片並連同問題一起打包送給 AI
    private fun downloadImageAndAskAI(imageUrl: String, userText: String, loadingIndex: Int) {
        val storageRef = FirebaseStorage.getInstance().reference.child(imageUrl)

        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            lifecycleScope.launch {
                try {
                    // 【多模態大絕招】把圖片和文字包在一起送給 Gemini！
                    val inputContent = content {
                        image(bitmap)
                        text("你現在是專屬穿搭顧問 e-fit-ai。請用繁體中文、親切像朋友的語氣回答。請根據我提供的這件「上衣」照片，回答我的問題：$userText。規定：回覆必須簡短俐落，控制在 50 個字以內。")
                    }
                    val response = generativeModel.generateContent(inputContent)
                    updateChatBubble(loadingIndex, response.text ?: "抱歉，我有點當機了！")
                } catch (e: Exception) {
                    updateChatBubble(loadingIndex, "連線失敗🥲\n系統說：${e.message}")
                }
            }
        }.addOnFailureListener {
            sendPureTextToAI(userText, loadingIndex) // 圖片下載失敗，切回純文字
        }
    }

    // 🌟 核心功能 3：純文字備用方案 (當衣櫃沒衣服時)
    private fun sendPureTextToAI(userText: String, loadingIndex: Int) {
        lifecycleScope.launch {
            try {
                val prompt = "你現在是專屬穿搭顧問 e-fit-ai。請用繁體中文、親切像朋友的語氣回答。規定：回覆必須簡短俐落，控制在 50 個字以內。使用者的問題是：$userText"
                val response = generativeModel.generateContent(prompt)
                updateChatBubble(loadingIndex, response.text ?: "抱歉，我有點當機了！")
            } catch (e: Exception) {
                updateChatBubble(loadingIndex, "連線失敗🥲\n系統說：${e.message}")
            }
        }
    }

    private fun addMessage(message: String, isUser: Boolean) {
        chatList.add(ChatMessage(message, isUser))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        recyclerView.scrollToPosition(chatList.size - 1)
    }

    private fun updateChatBubble(index: Int, newMessage: String) {
        chatList[index] = ChatMessage(newMessage, false)
        chatAdapter.notifyItemChanged(index)
        recyclerView.scrollToPosition(chatList.size - 1)
    }
}