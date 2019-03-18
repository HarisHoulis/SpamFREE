package xoulis.xaris.com.spamfree.binding

import android.widget.TextView
import androidx.databinding.BindingAdapter
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.CodeStatus
import xoulis.xaris.com.spamfree.util.CODE_EXPIRATION_TIMESTAMP_FORMAT
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("formatExpirationDate")
fun setFormattedExpirationDate(view: TextView, date: Date) {
    val sdf = SimpleDateFormat(CODE_EXPIRATION_TIMESTAMP_FORMAT, Locale.getDefault())
    view.text = sdf.format(date)
}

@BindingAdapter("codeStatus")
fun setCodeStatusMessage(view: TextView, status: CodeStatus) {
    val context = view.context
    view.text = when (status) {
        CodeStatus.UNUSED -> context.getString(R.string.unused)
        CodeStatus.ACTIVE -> context.getString(R.string.active)
        CodeStatus.USED -> context.getString(R.string.used)
        else -> context.getString(R.string.expired)
    }
}