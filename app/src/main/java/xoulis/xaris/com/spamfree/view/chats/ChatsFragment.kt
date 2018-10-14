package xoulis.xaris.com.spamfree.view.chats


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import kotlinx.android.synthetic.main.fragment_chats.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import xoulis.xaris.com.spamfree.databinding.ListItemChatBinding
import xoulis.xaris.com.spamfree.view.MainActivity

class ChatsFragment : Fragment() {

    private lateinit var chatsAdapter: FirebaseRecyclerAdapter<Chat, ChatsViewHolder>

    private val chatsMap: MutableMap<String, Int> = mutableMapOf()

    private lateinit var newRequestDialog: AlertDialog

    private lateinit var onChatsFetchedListener: OnChatsFetchedListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        onChatsFetchedListener = context as OnChatsFetchedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setListeners()
        fetchChats()

        new_chat_fab.setOnClickListener { _ ->
            showNewRequestDialog()
        }
    }

    private fun setListeners() {
        val mainActivity = activity as MainActivity
        mainActivity.setRequestResponseListener(object :
            MainActivity.OnRequestResponseListener {
            override fun onRequestResponseReceived() {
                newRequestDialog.dismiss()
            }
        })

        mainActivity.setNewMessageNotificationListener(object :
            MainActivity.OnNewMessageNotificationListener {
            override fun onNewMessageNotificationReceived(chatId: String) {
                val chatIndex = chatsMap[chatId]!!
                val chat = chatsAdapter.getItem(chatIndex)
                showChatRoom(chat)
            }

        })
    }

    private fun fetchChats() {
        val chatIndexRef = FirebaseDatabase.getInstance().getReference("/user_chats/${uid()}")
        val chatDataRef = FirebaseDatabase.getInstance().getReference("/chats")

        val options = FirebaseRecyclerOptions.Builder<Chat>()
            .setLifecycleOwner(this)
            .setIndexedQuery(chatIndexRef, chatDataRef, Chat::class.java)
            .build()

        chatsAdapter = object : FirebaseRecyclerAdapter<Chat, ChatsViewHolder>(options) {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ChatsViewHolder {
                val inflater = LayoutInflater.from(p0.context)
                val itemBinding = ListItemChatBinding.inflate(inflater, p0, false)
                return ChatsViewHolder(itemBinding)
            }

            override fun onBindViewHolder(holder: ChatsViewHolder, position: Int, model: Chat) {
                holder.bind(model)
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
                onChatsFetchedListener.onChatsFetched()
            }
        }

        chats_recyclerView.setHasFixedSize(true)
        chats_recyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        chats_recyclerView.adapter = chatsAdapter
    }

    private fun showNewRequestDialog() {
        val title = "Send a new request"
        newRequestDialog = context!!.getDialog(
            title = title,
            inputType = InputType.TYPE_CLASS_NUMBER,
            filter = InputFilter.LengthFilter(5),
            autoDismiss = false
        ) {
            setOkButtonClickListener { codeId -> okButtonClicked(codeId) }
        }
        newRequestDialog.show()
    }

    private fun okButtonClicked(codeId: String) {
        if (context!!.isNetworkAvailable()) {
            newRequestDialog.dismiss()
            fragment_chats_root.showSnackBar(R.string.detailed_error)
        } else {
            newRequestDialog.custom_dialog_ok_button.apply {
                isEnabled = false
                alpha = 0.5f
            }
            newRequestDialog.custom_dialog_progressBar.visibility = View.VISIBLE
            newRequestDialog.setCancelable(false)
            sendRequest(codeId)
        }
    }

    private fun sendRequest(codeId: String) {
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnCompleteListener { task ->
                val token = task.result.token
                val request = ChatRequest(
                    codeId = codeId,
                    senderToken = token
                )
                uncheckedRequestsRef().child(codeId).setValue(request)
                    .addOnCompleteListener { sendReqTask ->
                        if (!sendReqTask.isSuccessful) {
                            this@ChatsFragment.newRequestDialog.dismiss()
                            fragment_chats_root.showSnackBar(R.string.detailed_error)
                        }
                    }
            }
    }

    private fun showChatRoom(chat: Chat) {
        val intent = Intent(context, ChatRoomActivity::class.java).apply {
            putExtra(CHAT_EXTRA, chat)
        }
        startActivity(intent)
    }

    interface OnChatsFetchedListener {
        fun onChatsFetched()
    }

    inner class ChatsViewHolder(private val itemBinding: ListItemChatBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(chat: Chat) {
            itemBinding.chat = chat
            itemBinding.root.setOnClickListener {
                showChatRoom(chat)
            }
            itemBinding.executePendingBindings()
        }

    }
}
