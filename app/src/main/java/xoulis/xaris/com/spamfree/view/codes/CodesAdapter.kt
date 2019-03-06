package xoulis.xaris.com.spamfree.view.codes

import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.databinding.ListItemMostRecentCodeBinding
import xoulis.xaris.com.spamfree.databinding.ListItemSecondaryCodeBinding
import xoulis.xaris.com.spamfree.view.BaseCodeViewHolder

class CodesAdapter(
    options: FirebaseRecyclerOptions<ClientCode>,
    private val listener: CodeListener
) :
    FirebaseRecyclerAdapter<ClientCode, RecyclerView.ViewHolder>(options) {

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
    ): androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
        holder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
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

    inner class MostRecentCodeViewHolder(private val itemBinding: ListItemMostRecentCodeBinding) :
        BaseCodeViewHolder(itemBinding, listener) {

        fun bind(code: ClientCode) {
            this.currentCode = code
            itemBinding.code = code
            itemBinding.mostRecentCodeTextVew.setOnLongClickListener {
                listener.onCodeLongClick(it, code.id)
                false
            }
            itemBinding.mostRecentCodeEditMessagesImageView.setOnClickListener {
                (itemBinding.root.context as AppCompatActivity).startSupportActionMode(modeCallback)
            }
            itemBinding.executePendingBindings()
        }
    }

    inner class SecondaryCodesViewHolder(private val itemBinding: ListItemSecondaryCodeBinding) :
        BaseCodeViewHolder(itemBinding, listener) {

        fun bind(code: ClientCode, position: Int) {
            this.currentCode = code
            itemBinding.index = itemCount - position
            itemBinding.code = code
            if (!code.used) {
                itemBinding.root.setOnLongClickListener {
                    listener.onCodeLongClick(it, code.id)
                    false
                }
                itemBinding.secondaryCodeEditMessagesImageView.setOnClickListener {
                    (itemBinding.root.context as AppCompatActivity).startSupportActionMode(
                        modeCallback
                    )
                }
            }
            itemBinding.executePendingBindings()
        }
    }

    private companion object {
        const val MOST_RECENT_CODE_VIEW_TYPE = 0
        const val SECONDARY_CODE_VIEW_TYPE = 1
    }
}