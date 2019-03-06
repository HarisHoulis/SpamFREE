package xoulis.xaris.com.spamfree.view.codes


import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.fragment_codes.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.databinding.FragmentCodesBinding
import xoulis.xaris.com.spamfree.databinding.ListItemMostRecentCodeBinding
import xoulis.xaris.com.spamfree.databinding.ListItemSecondaryCodeBinding
import xoulis.xaris.com.spamfree.util.getDialog
import xoulis.xaris.com.spamfree.util.showSnackBar
import xoulis.xaris.com.spamfree.util.userCodesDbRef

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
        val query = userCodesDbRef().orderByChild("timestamp")
        val options =
            FirebaseRecyclerOptions.Builder<ClientCode>()
                .setLifecycleOwner(this)
                .setQuery(query, ClientCode::class.java)
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
        // TODO fix indexing problem
        showLoading(true)
        val functions = FirebaseFunctions.getInstance("europe-west1")
        functions.getHttpsCallable("requestNewCode")
            .call()
            .addOnCompleteListener { task ->
                Log.i("sds", "sdsd")
                if (!task.isSuccessful) {
                    fragment_codes_root.showSnackBar(R.string.failed_to_add_new_code)
                }
                showLoading(false)
            }
    }

    private fun updateCodeMessages(codeId: String, messages: String) {
        userCodesDbRef().child("$codeId/messages").setValue(messages)
        FirebaseDatabase.getInstance().getReference("/codes/$codeId/messages")
            .setValue(messages)
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
        val dialog = context!!.getDialog(title, text, InputType.TYPE_CLASS_NUMBER) {
            setEditTextWatcher()
            setOkButtonClickListener { userInput ->
                updateCodeMessages(code.id, userInput)
            }
        }
        dialog.show()
    }

    override fun onCodeChangeDateClick(code: ClientCode) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
