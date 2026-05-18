package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tw.edu.pu.csim.s1120336.e_fit.R

class ChatAdapter(private val chatList: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 定義兩種視圖類型
    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_AI = 2

    // 判斷這句話是誰說的
    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = chatList[position].message
        if (holder is UserViewHolder) {
            holder.tvMessage.text = message
        } else if (holder is AiViewHolder) {
            holder.tvMessage.text = message
        }
    }

    override fun getItemCount(): Int = chatList.size

    // 使用者的 ViewHolder
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tv_user_message)
    }

    // AI 的 ViewHolder
    class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tv_ai_message)
    }
}