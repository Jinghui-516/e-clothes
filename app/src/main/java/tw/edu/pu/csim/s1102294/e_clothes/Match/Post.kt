package tw.edu.pu.csim.s1102294.e_clothes.Match

// 這是一則貼文的「藍圖」，規定了每一則貼文必須包含哪些資料
data class Post(
    var postId: String = "",                // 🌟 貼文在資料庫裡的專屬 ID
    val userName: String = "匿名使用者",      // 發文者名稱
    val userAvatar: String = "",            // 發文者頭貼網址
    val imageUrls: List<String> = listOf(), // 照片網址清單 (支援多圖)
    val caption: String = "",               // 貼文文字內容
    val likedBy: List<String> = listOf(),   // 🌟 記錄有誰(Email)按過讚
    val timestamp: Long = 0                 // 發文時間戳記
)