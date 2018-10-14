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
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import kotlinx.android.synthetic.main.fragment_chats.*
import kotlinx.android.synthetic.main.fragment_codes.*
import kotlinx.android.synthetic.main.list_item_secondary_code.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.Chat

import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.databinding.FragmentCodesBinding
import xoulis.xaris.com.spamfree.databinding.ListItemMostRecentCodeBinding
import xoulis.xaris.com.spamfree.databinding.ListItemSecondaryCodeBinding
import xoulis.xaris.com.spamfree.util.CustomDialogHelper
import java.lang.Exception

class CodesFragment : Fragment() {

    private lateinit var binding: FragmentCodesBinding

    private lateinit var codesAdapter: FirebaseRecyclerAdapter<ClientCode, RecyclerView.ViewHolder>

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

        codesAdapter =
            object : FirebaseRecyclerAdapter<ClientCode, RecyclerView.ViewHolder>(options) {

                override fun getItemViewType(position: Int): Int {
                    return if (position == itemCount - 1) {
                        MOST_RECENT_CODE_VIEW_TYPE
                    } else {
                        SECONDARY_CODE_VIEW_TYPE
                    }
                }

                override fun onCreateViewHolder(
                    p0: ViewGroup,
                    viewType: Int
                ): RecyclerView.ViewHolder {
                    val inflater = LayoutInflater.from(p0.context)
                    return when (viewType) {
                        MOST_RECENT_CODE_VIEW_TYPE -> {
                            val itemBinding =
                                ListItemMostRecentCodeBinding.inflate(inflater, p0, false)
                            MostRecentCodeViewHolder(itemBinding)
                        }
                        SECONDARY_CODE_VIEW_TYPE -> {
                            val itemBinding =
                                ListItemSecondaryCodeBinding.inflate(inflater, p0, false)
                            SecondaryCodesViewHolder(itemBinding)
                        }
                        else -> throw Exception("Wrong code item view type!")
                    }
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    position: Int,
                    model: ClientCode
                ) {
                    when (holder.itemViewType) {
                        MOST_RECENT_CODE_VIEW_TYPE -> (holder as MostRecentCodeViewHolder).bind(
                            model
                        )
                        SECONDARY_CODE_VIEW_TYPE -> (holder as SecondaryCodesViewHolder).bind(
                            model,
                            position
                        )
                    }
                }
            }

        val recyclerView = binding.codesRecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
                    .apply {
                        stackFromEnd = true
                    }
        val divider = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        divider.setDrawable(activity!!.getDrawable(R.color.dividerColor)!!)
        recyclerView.addItemDecoration(divider)
        recyclerView.adapter = codesAdapter
    }

    private fun requestNewCode() {
        showLoading(true)
        val functions = FirebaseFunctions.getInstance()
        functions.getHttpsCallable("requestNewCode")
            .call()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    fragment_codes_root.showSnackBar(R.string.failed_to_add_new_code)
                }
                showLoading(false)
            }
    }

    private fun View.setLongClickListener(codeId: String) {
        setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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

    private fun View.setChangeMessagesLimitListener() {
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
        setOnClickListener(listener)
    }

    private fun updateCodeMessages(codeId: String, messages: String) {
        userCodesDbRef().child("$codeId/messages").setValue(messages)
        FirebaseDatabase.getInstance().getReference("/codes/$codeId/messages")
            .setValue(messages)
    }

    private fun showLoading(enable: Boolean) {
        binding.showLoading = enable
        binding.addNewCodeFab.isEnabled = enable
    }

    inner class MostRecentCodeViewHolder(private val itemBinding: ListItemMostRecentCodeBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(code: ClientCode) {
            itemBinding.code = code
            itemBinding.root.setLongClickListener(code.id)
            itemBinding.mostRecentCodeEditMessagesImageView.setChangeMessagesLimitListener()
            itemBinding.executePendingBindings()
        }
    }

    inner class SecondaryCodesViewHolder(private val itemBinding: ListItemSecondaryCodeBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(code: ClientCode, position: Int) {
            itemBinding.index = codesAdapter .itemCount - position
            itemBinding.code = code
            if (!code.used) {
                itemBinding.root.setLongClickListener(code.id)
                itemBinding.secondaryCodeEditMessagesImageView.setChangeMessagesLimitListener()
            }
            itemBinding.executePendingBindings()
        }
    }

    private companion object {
        const val MOST_RECENT_CODE_VIEW_TYPE = 0
        const val SECONDARY_CODE_VIEW_TYPE = 1
    }
}
