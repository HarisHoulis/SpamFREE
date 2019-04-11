package xoulis.xaris.com.spamfree.view.chats

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import kotlinx.android.synthetic.main.fragment_chats.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import xoulis.xaris.com.spamfree.util.*
import xoulis.xaris.com.spamfree.view.MainActivity

class ChatsFragment : Fragment(), ChatsListener {

    private lateinit var chatAdapter: ChatsAdapter

    private lateinit var newRequestDialog: AlertDialog

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

        new_chat_fab.setOnClickListener {
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
    }

    private fun fetchChats() {
        val chatIndexRef = FirebaseDatabase.getInstance().getReference("/user_chats/${uid()}")
        val chatDataRef = FirebaseDatabase.getInstance().getReference("/chats")

        val options = FirebaseRecyclerOptions.Builder<Chat>()
            .setLifecycleOwner(this)
            .setIndexedQuery(chatIndexRef, chatDataRef, Chat::class.java)
            .build()

        chatAdapter = ChatsAdapter(options, this)

        chats_recyclerView.setHasFixedSize(true)
        chats_recyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
        chats_recyclerView.itemAnimator =
            null // Set itemAnimator = null, in order to keep the alpha of items
        chats_recyclerView.adapter = chatAdapter
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
        if (!context!!.isNetworkAvailable()) {
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
                val token = task.result!!.token
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

    override fun onChatsFetched() {
        val intent = activity!!.intent
        intent.getStringExtra(CHAT_ID_EXTRA)?.let { chatId ->
            intent.removeExtra(CHAT_ID_EXTRA)
            val mainViewPager = activity!!.findViewById<ViewPager>(R.id.viewPager)
            mainViewPager.currentItem = 1
            val chatIndex = chatAdapter.chatsMap[chatId]!!
            val chat = chatAdapter.getItem(chatIndex)
            showChatRoom(chat)
        }
    }

    override fun onChatRoomClicked(chat: Chat) {
        showChatRoom(chat)
    }
}
