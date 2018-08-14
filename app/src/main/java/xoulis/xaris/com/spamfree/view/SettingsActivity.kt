package xoulis.xaris.com.spamfree.view

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.SwipeDismissBehavior
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.User
import xoulis.xaris.com.spamfree.databinding.ActivitySettingsBinding
import xoulis.xaris.com.spamfree.dbRef
import xoulis.xaris.com.spamfree.enableView
import xoulis.xaris.com.spamfree.viewmodel.SettingsViewModel


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.isLoading = true
        binding.handler = this

        val viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        viewModel.userLiveData.observe(this, Observer {
            it?.run {
                binding.user = getValue(User::class.java)
                binding.isLoading = false
            }
        })
    }

    fun onStatusChangeClick(user: User) {
        val dialogBuilder by lazy { AlertDialog.Builder(this) }
        dialogBuilder.setView(R.layout.custom_edit_text_dialog)
        val dialog by lazy { dialogBuilder.show() }

        val messageEditText: EditText =
            dialog.findViewById(R.id.custom_dialog_message)!!
        val okButton: Button =
            dialog.findViewById(R.id.custom_dialog_ok_button)!!

        okButton.enableView(false)
        messageEditText.setText(user.status)
        dialog.custom_dialog_title.text = getString(R.string.new_status)

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                okButton.enableView(p0.toString() != user.status)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        okButton.setOnClickListener {
            val newStatus = messageEditText.text.toString().trim()
            updateUserStatus(newStatus, user)
            dialog.dismiss()
        }
    }

    private fun updateUserStatus(newStatus: String, user: User) {
        if (newStatus != user.status) {
            dbRef().setValue(user)
        }
    }
}
