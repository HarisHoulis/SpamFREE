package xoulis.xaris.com.spamfree.view.chats


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_chats.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ChatRequest

import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.data.vo.RequestStatus

class ChatsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        new_chat_fab.setOnClickListener { _ ->
            showNewRequestDialog()
        }
    }

    private fun showNewRequestDialog() {
        val title = "Create new chat room"
        val dialog = context!!.getDialog(
            title = title,
            inputType = InputType.TYPE_CLASS_NUMBER,
            filter = InputFilter.LengthFilter(5)
        ) {
            setOkButtonClickListener { codeId ->
                sendRequest(codeId)
            }
        }
        dialog.show()
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
                outgoingRequestsRef().child(codeId).setValue(request)
                Log.i("req111", "reqSent")
            }

    }
}
