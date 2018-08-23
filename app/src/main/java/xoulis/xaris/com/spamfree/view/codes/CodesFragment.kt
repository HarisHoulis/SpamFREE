package xoulis.xaris.com.spamfree.view.codes


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import kotlinx.android.synthetic.main.fragment_codes.*
import kotlinx.android.synthetic.main.list_item_code.*
import xoulis.xaris.com.spamfree.*

import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.databinding.FragmentCodesBinding
import xoulis.xaris.com.spamfree.databinding.ListItemCodeBinding
import xoulis.xaris.com.spamfree.util.CustomDialogHelper

class CodesFragment : Fragment() {

    lateinit var binding: FragmentCodesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_codes, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUsedCodesRecyclerView()
        fetchUnusedCode()

        setCodeLongClickListener()
        setUnusedCodeMessagesClickListener()
    }

    private fun setCodeLongClickListener() {
        binding.unusedCodeTextVew.setOnLongClickListener {
            binding.unusedCodeTextVew.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val codeId = binding.code?.id
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_code_subject))
                putExtra(Intent.EXTRA_TEXT, codeId)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code_title)))
            false
        }
    }

    private fun setUnusedCodeMessagesClickListener() {
        val listener = View.OnClickListener {
            val code = binding.code!!
            val title = "Number of messages"
            val text = code.messages
            val dialog = context!!.getDialog(title, text) {
                setEditTextWatcher()
                setOkButtonClickListener { userInput ->
                    updateCodeMessages(code.id, userInput)
                }
            }
            dialog.show()
        }
        binding.unusedCodeMessagesGroup.setAllOnClickListeners(listener)
    }

    private fun updateCodeMessages(codeId: String, messages: String) {
        val query = userCodesDbRef.orderByChild("id").equalTo(codeId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val childKey = child.key!!
                    snapshot.ref.child(childKey).child("messages").setValue(messages)
                }
            }
        })
    }

    private fun fetchUnusedCode() {
        val query = userCodesDbRef.orderByChild("used").equalTo(false).limitToFirst(1)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    return
                }

                for (child in snapshot.children) {
                    val childKey = child.key
                    childKey?.let {
                        val clientCode = snapshot.child(it).getValue(ClientCode::class.java)
                        binding.code = clientCode
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun setupUsedCodesRecyclerView() {
        binding.showShimmer = true
        binding.shimmerViewContainer.startShimmer()

        val query = userCodesDbRef.orderByChild("used").equalTo(true).limitToFirst(10)
        val options =
            FirebaseRecyclerOptions.Builder<ClientCode>()
                .setLifecycleOwner(this)
                .setQuery(query, ClientCode::class.java)
                .build()

        val adapter = object : FirebaseRecyclerAdapter<ClientCode, CodesViewHolder>(options) {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): CodesViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val itemBinding = ListItemCodeBinding.inflate(inflater, parent, false)
                return CodesViewHolder(itemBinding)
            }

            override fun onBindViewHolder(
                holder: CodesViewHolder,
                position: Int,
                model: ClientCode
            ) {
                holder.bind(model, position)
            }

            override fun onDataChanged() {
                super.onDataChanged()
                hideLoadingAndEmptyViews(itemCount)
            }

            override fun onError(error: DatabaseError) {
                super.onError(error)
                hideLoadingAndEmptyViews(itemCount)
            }
        }

        val recyclerView = binding.usedCodesRecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val divider = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        divider.setDrawable(activity!!.getDrawable(R.color.dividerColor)!!)
        recyclerView.addItemDecoration(divider)
        recyclerView.adapter = adapter

    }

    private fun hideLoadingAndEmptyViews(itemCount: Int) {
        binding.showShimmer = false
        binding.shimmerViewContainer.stopShimmer()
        binding.showEmptyView = itemCount == 0
        binding.executePendingBindings()
    }

    inner class CodesViewHolder(private val itemBinding: ListItemCodeBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(code: ClientCode, position: Int) {
            itemBinding.number = position + 1
            itemBinding.code = code
            itemBinding.executePendingBindings()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CodesFragment()
    }
}
