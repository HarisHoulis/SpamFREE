package xoulis.xaris.com.spamfree

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.constraint.Group
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*

/* Views */
fun View.enableView(enable: Boolean) {
    isEnabled = enable
    isClickable = enable
    alpha = if (enable) 1.0f else 0.5f
}

fun View.showSnackBar(messageResId: Int, shortDuration: Boolean = true) {
    val duration = if (shortDuration) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
    Snackbar.make(this, messageResId, duration).show()
}

fun Group.setAllOnClickListeners(listener: View.OnClickListener) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

fun Context.showDialog(
    title: String,
    message: String,
    inputType: Int,
    f: (input: String) -> (Unit)
) {
    val dialogBuilder by lazy {
        AlertDialog.Builder(this)
    }
    dialogBuilder.setView(R.layout.custom_edit_text_dialog)
    val dialog by lazy { dialogBuilder.show() }

    val messageEditText: EditText by lazy { dialog.findViewById<EditText>(R.id.custom_dialog_message)!! }
    val okButton: Button by lazy { dialog.findViewById<Button>(R.id.custom_dialog_ok_button)!! }

    okButton.enableView(false)

    messageEditText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            okButton.enableView(p0.toString() != message)
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    })
    okButton.setOnClickListener {
        val input = messageEditText.text.toString()
        f(input)
        dialog.dismiss()
    }

    dialog.custom_dialog_title.text = title
    messageEditText.inputType = inputType
    messageEditText.setText(message)
}

/* Network */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}