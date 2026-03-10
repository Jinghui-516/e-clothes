package tw.edu.pu.csim.s1102294.e_clothes.Community

import com.google.firebase.firestore.PropertyName

data class delete_friends_data(
    val email: String = "",
    @PropertyName("使用者名稱") val username: String = "",
    @PropertyName("頭貼圖片") val profileImage: String = "",
    @PropertyName("生日") val birthday: String = "",
    @PropertyName("性別") val gender: String = "",
    @PropertyName("個性簽名") val signature: String = ""
)
