package xoulis.xaris.com.spamfree.binding

import android.databinding.BindingAdapter
import android.text.TextUtils
import android.widget.TextView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import xoulis.xaris.com.spamfree.util.CHAT_TIMESTAMP_FORMAT
import xoulis.xaris.com.spamfree.util.MESSAGE_TIMESTAMP_FORMAT
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("loadUserImage")
fun loadUserImage(view: CircleImageView, imageUrl: String) {
    if (TextUtils.isEmpty(imageUrl) || imageUrl == view.context.getString(R.string.default_user_image_name)) {
        return
    }

    Picasso.get()
        .load(imageUrl)
        .error(R.drawable.ic_default_avatar)
        .into(view)
}

@BindingAdapter("setRequestDisplayName")
fun setRequestDisplayName(view: TextView, request: ChatRequest) {
    val name = if (request.incoming) {
        "From: ${request.senderName}"
    } else {
        "To: ${request.receiverName}"
    }
    view.text = name
}

@BindingAdapter("requestTimestamp")
fun setRequestTimestamp(view: TextView, timestamp: Long) {
    val sdf by lazy {
        SimpleDateFormat(CHAT_TIMESTAMP_FORMAT, Locale.getDefault())
    }
    val date = Date(timestamp)
    val formattedTime = sdf.format(date)
    val text = "${view.context.getText(R.string.at)} $formattedTime"
    view.text = text
}

@BindingAdapter("messageTimestamp")
fun setMessageTimestamp(view: TextView, timestamp: Long) {
    val sdf by lazy {
        SimpleDateFormat(MESSAGE_TIMESTAMP_FORMAT, Locale.getDefault())
    }

    val date = Date(timestamp)
    val formattedTime = sdf.format(date)
    view.text = formattedTime
}
