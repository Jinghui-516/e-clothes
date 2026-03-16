package tw.edu.pu.csim.s1102294.e_clothes.clothes

// isUser = true 代表使用者發的，false 代表 AI 發的
data class ChatMessage(
    val message: String,
    val isUser: Boolean
)