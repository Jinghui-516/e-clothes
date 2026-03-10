package tw.edu.pu.csim.s1102294.e_clothes.Match

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Match(
    @PropertyName("搭配名稱") val 搭配名稱: String = "",
    @PropertyName("天氣種類") val 天氣種類: String = "",
    @PropertyName("帽子圖片網址") val 帽子圖片網址: String = "",
    @PropertyName("上衣圖片網址") val 上衣圖片網址: String = "",
    @PropertyName("褲子圖片網址") val 褲子圖片網址: String = "",
    @PropertyName("圖片完整網址") val 圖片完整網址: String? = null,  // Add this if needed
    @PropertyName("鞋子圖片網址") val 鞋子圖片網址: String = "",
    @PropertyName("標籤") val 標籤: List<String>? = null,
    @PropertyName("服裝種類") val 服裝種類: String? = null,  // Add this if needed
    @PropertyName("圖片網址") val 圖片網址: String? = null   // Add this if needed
) : Serializable {
    // 判斷是否為有效的搭配
    fun isValid(): Boolean {
        return 搭配名稱.isNotEmpty() &&
                天氣種類.isNotEmpty() &&
                帽子圖片網址.isNotEmpty() &&
                上衣圖片網址.isNotEmpty() &&
                褲子圖片網址.isNotEmpty() &&
                鞋子圖片網址.isNotEmpty()
    }
}
