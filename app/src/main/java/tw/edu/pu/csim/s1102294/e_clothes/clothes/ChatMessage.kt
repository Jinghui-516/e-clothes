package tw.edu.pu.csim.s1120336.e_fit.clothes

// isUser = true 代表使用者發的，false 代表 AI 發的
data class ChatMessage(
    val message: String,
    val isUser: Boolean
)