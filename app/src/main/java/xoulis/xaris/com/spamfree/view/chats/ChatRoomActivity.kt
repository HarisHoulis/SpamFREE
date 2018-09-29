package xoulis.xaris.com.spamfree.view.chats

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.android.synthetic.main.activity_chat_room.*
import kotlinx.android.synthetic.main.notification_template_part_time.*
import xoulis.xaris.com.spamfree.CHAT_EXTRA
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.data.vo.ChatMessage
import xoulis.xaris.com.spamfree.databinding.ListItemReceivedMessageBinding
import xoulis.xaris.com.spamfree.databinding.ListItemSentMessageBinding
import xoulis.xaris.com.spamfree.enableView
import xoulis.xaris.com.spamfree.uid
import java.lang.Exception

class ChatRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val args = intent.extras
        args?.getParcelable<Chat>(CHAT_EXTRA)?.let {
            val chatId = it.codeId
            val receiverName = if (it.ownerId == uid()) it.memberName else it.ownerName
            val senderImage = if (it.ownerId == uid()) it.ownerImage else it.memberImage
            initToolbar(receiverName)
            setupMessagesRecyclerView(chatId)
            setupBottomToolbar(chatId, senderImage)
        }
    }

    private fun initToolbar(title: String) {
        setSupportActionBar(chat_room_toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = title
        }
    }

    private fun setupMessagesRecyclerView(chatId: String) {
        val recyclerView = messages_recyclerView
        val linearLayoutManager =
            LinearLayoutManager(this@ChatRoomActivity, LinearLayoutManager.VERTICAL, false)

        val messagesRef = FirebaseDatabase
            .getInstance()
            .getReference("/messages/$chatId")
            .orderByChild("timestamp")

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

                override fun onCreateViewHolder(
                    p0: ViewGroup,
                    viewType: Int
                ): RecyclerView.ViewHolder {
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

        // Scroll to the bottom every time a new message is added,
        // only if the user is already at the bottom
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                val messagesCount = adapter.itemCount
                val lastVisiblePosition =
                    linearLayoutManager.findLastCompletelyVisibleItemPosition()
                if (lastVisiblePosition == -1 || (positionStart >= (messagesCount - 1) &&
                            lastVisiblePosition == (positionStart - 1))
                ) {
                    recyclerView.scrollToPosition(positionStart)
                }
            }
        })
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter
    }

    private fun setupBottomToolbar(chatId: String, senderImage: String) {
        chat_room_editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                chat_room_send_button.enableView(p0.toString().trim().isNotEmpty())
            }
        })

        val currentText = chat_room_editText.text
        chat_room_send_button.enableView(currentText.toString().trim().isNotEmpty())

        chat_room_send_button.setOnClickListener {
            sendMessage(chatId, senderImage)
        }
    }

    private fun sendMessage(chatId: String, senderImage: String) {
        val db = FirebaseDatabase.getInstance()
        val messageBody = chat_room_editText.text.toString().trim()
        val timestamp = ServerValue.TIMESTAMP
        val message = ChatMessage(
            chatId,
            uid(),
            senderImage,
            messageBody,
            timestamp
        )

        // Update DB
        db.getReference("/messages/$chatId").push().setValue(message)
        db.getReference("/chats/$chatId/lastMessage").setValue(messageBody)
        db.getReference("/chats/$chatId/timestamp").setValue(timestamp)
        chat_room_editText.setText("")
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
