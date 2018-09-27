package xoulis.xaris.com.spamfree.view.chats

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_chat_room.*
import xoulis.xaris.com.spamfree.CHAT_EXTRA
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.data.vo.ChatMessage
import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import xoulis.xaris.com.spamfree.databinding.ListItemReceivedMessageBinding
import xoulis.xaris.com.spamfree.databinding.ListItemSentMessageBinding
import xoulis.xaris.com.spamfree.uid
import xoulis.xaris.com.spamfree.view.requests.RequestsFragment
import java.lang.Exception

class ChatRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val args = intent.extras
        args?.getParcelable<Chat>(CHAT_EXTRA)?.let {
            val receiverName = if (it.ownerId == uid()) it.memberName else it.ownerName
            initToolbar(receiverName)
            setupMessagesRecyclerView(it.codeId)
        }
    }

    private fun initToolbar(title: String) {
        setSupportActionBar(chat_room_toolbar)
        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = title
        }
    }

    private fun setupMessagesRecyclerView(chatId: String) {
        val messagesRef = FirebaseDatabase
            .getInstance()
            .getReference("/messages/$chatId")
            .orderByChild("timestamp")
            .limitToFirst(10)

        val options = FirebaseRecyclerOptions
            .Builder<ChatMessage>()
            .setLifecycleOwner(this)
            .setQuery(messagesRef, ChatMessage::class.java)
            .build()

        val adapter =
            object : FirebaseRecyclerAdapter<ChatMessage, RecyclerView.ViewHolder?>(options) {

                override fun getItemViewType(position: Int): Int {
                    val chatMessage = getItem(position)
                    return if (chatMessage.senderId == uid()) {
                        VIEW_TYPE_MESSAGE_SENT
                    } else {
                        VIEW_TYPE_MESSAGE_RECEIVED
                    }
                }

                override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
                    val viewType = getItemViewType(p1)
                    val inflater = LayoutInflater.from(p0.context)
                    return when (viewType) {
                        VIEW_TYPE_MESSAGE_SENT -> {
                            val itemBinding =
                                ListItemSentMessageBinding.inflate(inflater, p0, false)
                            SentMessagesViewHolder(itemBinding)
                        }
                        VIEW_TYPE_MESSAGE_RECEIVED -> {
                            val itemBinding =
                                ListItemReceivedMessageBinding.inflate(inflater, p0, false)
                            ReceivedMessagesViewHolder(itemBinding)
                        }
                        else -> throw Exception("Wrong item view type!")
                    }
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    position: Int,
                    model: ChatMessage
                ) {
                    when (holder.itemViewType) {
                        VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessagesViewHolder).bind(model)
                        VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessagesViewHolder).bind(
                            model
                        )
                    }
                }
            }
        val recyclerView = messages_recyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =
                LinearLayoutManager(this@ChatRoomActivity, LinearLayoutManager.VERTICAL, true)
        recyclerView.adapter = adapter
    }

    private inner class SentMessagesViewHolder(private val itemBinding: ListItemSentMessageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(chatMessage: ChatMessage) {
            itemBinding.chatMessage = chatMessage
            itemBinding.executePendingBindings()
        }

    }

    private inner class ReceivedMessagesViewHolder(private val itemBinding: ListItemReceivedMessageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(chatMessage: ChatMessage) {
            itemBinding.chatMessage = chatMessage
            itemBinding.executePendingBindings()
        }

    }

    private companion object {
        const val VIEW_TYPE_MESSAGE_SENT = 1
        const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }
}
