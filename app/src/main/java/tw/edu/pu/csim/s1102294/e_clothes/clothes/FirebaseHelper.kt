package tw.edu.pu.csim.s1102294.e_clothes.clothes

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.util.*
import com.google.firebase.storage.FirebaseStorage

class FirebaseHelper {
    private val storageReference = FirebaseStorage.getInstance().reference

    // 增加回调来返回上传结果
    fun uploadImage(context: Context, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = storageReference.child("images/${UUID.randomUUID()}")  // 随机生成文件名
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                Log.d("FirebaseHelper", "Image uploaded successfully: $imageUrl")  // 输出到 Logcat
                onSuccess(imageUrl)  // 上传成功，返回 imageUrl
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to get download URL", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseHelper", "Failed to get download URL", it)  // 输出错误信息到 Logcat
                onFailure(it)
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            Log.e("FirebaseHelper", "Failed to upload image", it)
            onFailure(it)
        }
    }
}
