package xoulis.xaris.com.spamfree.view.chats


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import kotlinx.android.synthetic.main.fragment_chats.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import xoulis.xaris.com.spamfree.view.MainActivity

class ChatsFragment : Fragment() {

    private lateinit var newRequestDialog: AlertDialog

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

        (activity as MainActivity).setRequestResponseListener(object :
            MainActivity.OnRequestResponseListener {
            override fun onRequestResponseReceived() {
                newRequestDialog.dismiss()
            }
        })

        new_chat_fab.setOnClickListener { _ ->
            showNewRequestDialog()
        }
    }

    private fun showNewRequestDialog() {
        val title = "Create new chat room"
        newRequestDialog = context!!.getDialog(
            title = title,
            inputType = InputType.TYPE_CLASS_NUMBER,
            filter = InputFilter.LengthFilter(5),
            autoDismiss = false
        ) {
            setOkButtonClickListener { codeId ->
                newRequestDialog.custom_dialog_ok_button.apply {
                    isEnabled = false
                    alpha = 0.5f
                }
                newRequestDialog.custom_dialog_progressBar.visibility = View.VISIBLE
                sendRequest(codeId)
            }
        }
        newRequestDialog.show()
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
            }
    }
}
