package xoulis.xaris.com.spamfree.view

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.view.codes.CodeListener

open class BaseCodeViewHolder(itemBinding: ViewDataBinding, private val listener: CodeListener) :
    RecyclerView.ViewHolder(itemBinding.root) {

    lateinit var currentCode: ClientCode

    val modeCallback by lazy {
        object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.menu_edit_message -> {
                        listener.onCodeChangeMessageLimitClick(currentCode)
                        true
                    }
                    R.id.menu_edit_date -> {
                        listener.onCodeChangeDateClick(currentCode)
                        true
                    }
                    else -> false
                }
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.apply {
                    menuInflater.inflate(R.menu.popup_menu, menu)
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                //currentActionMode = null
            }

        }
    }
}