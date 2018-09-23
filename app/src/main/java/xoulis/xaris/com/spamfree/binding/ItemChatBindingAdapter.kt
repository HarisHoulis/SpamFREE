package xoulis.xaris.com.spamfree.binding

import android.databinding.BindingAdapter
import android.widget.TextView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import xoulis.xaris.com.spamfree.CHAT_TIMESTAMP_FORMAT
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.uid
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("chatImage")
fun setChatImage(view: CircleImageView, chat: Chat) {
    val imageUrl = if (chat.ownerId === uid()) {
        chat.ownerImage
    } else {
        chat.memberImage
    }

    Picasso.get()
        .load(imageUrl)
        .error(R.drawable.ic_default_avatar)
        .into(view)
}

@BindingAdapter("chatUsername")
fun setChatUsername(view: TextView, chat: Chat) {
    val username = if (chat.ownerId === uid()) {
        chat.ownerName
    } else {
        chat.memberName
    }

    view.text = username
}

@BindingAdapter("chatTimestamp")
fun setChatLastMessageTimestamp(view: TextView, timestamp: Long) {
    val date = Date(timestamp)
    val sdf by lazy {
        SimpleDateFormat(CHAT_TIMESTAMP_FORMAT, Locale.getDefault())
    }
    val dateString = sdf.format(date)
    view.text = dateString
}

