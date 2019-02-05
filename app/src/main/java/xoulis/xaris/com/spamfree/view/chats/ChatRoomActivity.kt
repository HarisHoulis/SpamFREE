package xoulis.xaris.com.spamfree.view.chats

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.activity_chat_room.*
import kotlinx.android.synthetic.main.fragment_profile.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.binding.setChatImage
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.data.vo.ChatMessage
import xoulis.xaris.com.spamfree.databinding.ListItemReceivedMessageBinding
import xoulis.xaris.com.spamfree.databinding.ListItemSentMessageBinding
import xoulis.xaris.com.spamfree.util.*
import xoulis.xaris.com.spamfree.view.settings.ProfileFragment
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomActivity : AppCompatActivity() {

    private var sentMessagesCount: Int = 0
    private var chatMessagesLimit: Int = 0

    private val sdf by lazy {
        SimpleDateFormat(CHAT_TIMESTAMP_FORMAT, Locale.getDefault())
    }

    private var receiverId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val args = intent.extras
        args?.getParcelable<Chat>(CHAT_EXTRA)?.let {
            chatMessagesLimit = it.messages.toInt()
            val chatId = it.codeId
            val isOwner = it.ownerId == uid()
            val receiverName = if (isOwner) it.memberName else it.ownerName
            val senderImage = if (isOwner) it.ownerImage else it.memberImage
            val senderName = if (isOwner) it.ownerName else it.memberName
            receiverId = if (isOwner) it.memberId else it.ownerId

            initToolbar(receiverName, it)
            setupMessagesRecyclerView(chatId)
            setupBottomToolbar(chatId, senderName, senderImage)
            setChatImageListener()
        }
    }

    private fun initToolbar(title: String, chat: Chat) {
        setSupportActionBar(chat_room_toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = title
            it.subtitle = "${chat.messages} messages left"
        }
        setChatImage(chat_room_profile_image, chat)
    }

    private fun setChatImageListener() {
        chat_room_profile_image.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                val fragment = ProfileFragment.newInstance(receiverId)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        0,
                        0,
                        android.R.anim.slide_out_right
                    )
                    .replace(R.id.chat_room_activity_root, fragment)
                    .addToBackStack(null)
                    .commit()
            }
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
                        else -> throw Exception("Wrong message item view type!")
                    }
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    position: Int,
                    model: ChatMessage
                ) {
                    when (holder.itemViewType) {
                        VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessagesViewHolder).bind(
                            model,
                            position
                        )
                        VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessagesViewHolder).bind(
                            model,
                            position
                        )
                    }
                }

                override fun onChildChanged(
                    type: ChangeEventType,
                    snapshot: DataSnapshot,
                    newIndex: Int,
                    oldIndex: Int
                ) {
                    super.onChildChanged(type, snapshot, newIndex, oldIndex)
                    if (type == ChangeEventType.ADDED) {
                        val message = snapshot.getValue(ChatMessage::class.java)!!
                        if (message.senderId == uid()) {
                            sentMessagesCount++
                        }
                    }
                }

                override fun onDataChanged() {
                    super.onDataChanged()
                    chat_message_day_textView.showView(itemCount != 0)
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

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = linearLayoutManager.findFirstVisibleItemPosition()
                val firstVisibleMessageTimestamp = adapter.getItem(pos).getTimestampLong()
                chat_message_day_textView.text = sdf.format(Date(firstVisibleMessageTimestamp))
            }
        })
    }

    private fun setupBottomToolbar(chatId: String, senderName: String, senderImage: String) {
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
            sendMessage(chatId, senderName, senderImage)
        }
    }

    private fun sendMessage(chatId: String, senderName: String, senderImage: String) {
        val db = FirebaseDatabase.getInstance()
        val messageBody = chat_room_editText.text.toString().trim()
        val timestamp = ServerValue.TIMESTAMP
        val message = ChatMessage(
            chatId,
            uid(),
            senderName,
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

        fun bind(chatMessage: ChatMessage, pos: Int) {
            // Add margin to 1st message, so as not to interfere with the date
            if (pos == 0) {
                val view = itemBinding.root
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(p.leftMargin, 26, p.rightMargin, p.bottomMargin)
                view.layoutParams = p
            }

            val leftMessages = chatMessagesLimit - sentMessagesCount
            if (leftMessages == 0) {
                chat_room_editText.isEnabled = false
            }
            supportActionBar?.subtitle = "$leftMessages messages left"
            itemBinding.chatMessage = chatMessage
            itemBinding.executePendingBindings()
        }

    }

    private inner class ReceivedMessagesViewHolder(private val itemBinding: ListItemReceivedMessageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(chatMessage: ChatMessage, pos: Int) {
            // Add margin to 1st message, so as not to interfere with the date
            if (pos == 0) {
                val view = itemBinding.root
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(p.leftMargin, 26, p.rightMargin, p.bottomMargin)
                view.layoutParams = p
            }

            itemBinding.chatMessage = chatMessage
            itemBinding.executePendingBindings()
        }
    }

    private fun getMarginParams(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) =
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(left, top, right, bottom)
        }

    private companion object {
        const val VIEW_TYPE_MESSAGE_SENT = 1
        const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }
}
