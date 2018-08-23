package xoulis.xaris.com.spamfree.util

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.w3c.dom.Text
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.enableView

class CustomDialogHelper(context: Context, val title: String, val text: String) {

    private val dialogView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.custom_edit_text_dialog, null)
    }
    private val builder: AlertDialog.Builder = AlertDialog.Builder(context).setView(dialogView)
    private lateinit var dialog: AlertDialog

    private val titleTextView: TextView by lazy {
        dialogView.findViewById<TextView>(R.id.custom_dialog_title)
    }
    private val editText: EditText by lazy {
        dialogView.findViewById<EditText>(R.id.custom_dialog_message)
    }
    private val okButton: Button by lazy {
        dialogView.findViewById<Button>(R.id.custom_dialog_ok_button)
    }

    fun create(): AlertDialog {
        titleTextView.text = title
        editText.setText(text)
        dialog = builder.create()
        return dialog
    }

    fun setOkButtonClickListener(f: (userInput: String) -> (Unit)) {
        okButton.setOnClickListener {
            f(editText.text.toString())
            dialog.dismiss()
        }
    }

    fun setEditTextWatcher() {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                with(p0.toString()) {
                    okButton.enableView(!(this == text || this.isBlank()))
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }
}