package xoulis.xaris.com.spamfree.view.codes

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.fragment_codes.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.databinding.FragmentCodesBinding
import xoulis.xaris.com.spamfree.util.*

class CodesFragment : Fragment(), CodeListener {

    private lateinit var binding: FragmentCodesBinding

    private lateinit var codesAdapter: CodesAdapter

    private var currentActionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_codes, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchUserCodes()

        add_new_code_fab.setOnClickListener {
            requestNewCode()
        }
    }

    private fun fetchUserCodes() {
        val codeIndexRef = userCodesDbRef()
        val codeDataRef = codesDbRef.orderByChild("timestamp").ref

//        val chatIndexRef = FirebaseDatabase.getInstance().getReference("/user_codes/${uid()}")
//        val chatDataRef = FirebaseDatabase.getInstance().getReference("/codes")

        val options =
            FirebaseRecyclerOptions.Builder<ClientCode>()
                .setLifecycleOwner(this)
                .setIndexedQuery(codeIndexRef, codeDataRef) {snapshot ->
                    val code = snapshot.getValue(ClientCode::class.java)!!
                    code.hasExpired()
                    code
                }
                .build()

        codesAdapter = CodesAdapter(options, this)

        val divider = DividerItemDecoration(context, RecyclerView.VERTICAL).apply {
            setDrawable(activity!!.getDrawable(R.color.dividerColor)!!)
        }

        val recyclerView = binding.codesRecyclerView
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true).apply {
                stackFromEnd = true
            }
            addItemDecoration(divider)
            // Set itemAnimator = null, in order to keep the alpha of items
            itemAnimator = null
            adapter = codesAdapter
        }
    }

    private fun requestNewCode() {
        showLoading(true)
        val functions = FirebaseFunctions.getInstance("europe-west1")
        functions.getHttpsCallable("requestNewCode")
            .call()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    fragment_codes_root.showSnackBar(R.string.failed_to_add_new_code)
                } else {
                    codesAdapter.notifyItemInserted(0)
                }
                showLoading(false)
            }
    }

    private fun updateCodeMessages(codeId: String, messages: String) {
        userCodesDbRef().child("$codeId/messages").setValue(messages)
        FirebaseDatabase.getInstance().getReference("/codes/$codeId/messages")
            .setValue(messages)
    }

    private fun updateCodeExpiration(codeId: String, months: Int) {
        codesDbRef.child("$codeId/months").setValue(months)
    }

    private fun showLoading(show: Boolean) {
        binding.showLoading = show
        binding.addNewCodeFab.isEnabled = !show
    }

    override fun onCodeLongClick(view: View, codeId: String) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_code_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_code_text, codeId))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code_title)))
    }

    override fun onCodeChangeMessageLimitClick(code: ClientCode) {
        val title = "Number of messages"
        val text = code.messages
        val dialog = context!!.getDialog(
            title = title,
            text = text,
            inputType = InputType.TYPE_CLASS_NUMBER,
            filter = InputFilter.LengthFilter(2)
        ) {
            setEditTextWatcher {
                enableOkButton(!(it == text || it.isBlank()))
            }
            setOkButtonClickListener { userInput ->
                updateCodeMessages(code.id, userInput)
            }
        }
        dialog.show()
    }

    override fun onCodeChangeDateClick(code: ClientCode) {
        val currentMonths = code.months
        val title = "Number of months"
        val text = code.months.toString()
        val dialog = context!!.getDialog(title = title,
            text = text,
            inputType = InputType.TYPE_CLASS_NUMBER,
            filter = InputFilter.LengthFilter(2)
        ) {
            setEditTextWatcher {
                val monthsInput = if (it.isBlank()) 0 else it.toInt()
                var enableOkButton = false
                val errorMessage = if (monthsInput == currentMonths || monthsInput == 0) {
                    getString(R.string.generic_empty_text_error)
                } else if (monthsInput > 12) {
                    getString(R.string.months_greater_error)
                } else if (monthsInput < currentMonths) {
                    getString(R.string.months_fewer_error)
                } else {
                    enableOkButton = true
                    null
                }
                enableOkButton(enableOkButton)
                setTextInputLayoutError(errorMessage)
            }
            setOkButtonClickListener { userInput ->
                updateCodeExpiration(code.id, userInput.toInt())
            }
        }
        dialog.show()
    }
}
