package xoulis.xaris.com.spamfree.view.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.databinding.ListItemChatBinding

class ChatsAdapter(
    options: FirebaseRecyclerOptions<Chat>,
    private val listener: ChatsListener
) :
    FirebaseRecyclerAdapter<Chat, ChatsAdapter.ChatViewHolder>(options) {

    val chatsMap: MutableMap<String, Int> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = ListItemChatBinding.inflate(inflater, parent, false)
        return ChatViewHolder(itemBinding)
    }

    override fun onBindViewHolder(p0: ChatViewHolder, p1: Int, p2: Chat) {
        p0.bind(p2)
    }

    override fun onChildChanged(
        type: ChangeEventType,
        snapshot: DataSnapshot,
        newIndex: Int,
        oldIndex: Int
    ) {
        super.onChildChanged(type, snapshot, newIndex, oldIndex)
        val chatId = snapshot.key!!
        if (type == ChangeEventType.ADDED) {
            chatsMap[chatId] = newIndex
        }
    }

    override fun onDataChanged() {
        super.onDataChanged()
        listener.onChatsFetched()
    }

    inner class ChatViewHolder(private val itemBinding: ListItemChatBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(chat: Chat) {
            itemBinding.chat = chat
            itemBinding.root.setOnClickListener {
                listener.onChatRoomClicked(chat)
            }
            itemBinding.executePendingBindings()
        }
    }
}