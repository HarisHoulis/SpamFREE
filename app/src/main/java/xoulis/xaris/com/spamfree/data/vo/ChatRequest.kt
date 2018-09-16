package xoulis.xaris.com.spamfree.data.vo

import com.google.firebase.database.ServerValue
import xoulis.xaris.com.spamfree.uid
import xoulis.xaris.com.spamfree.userDisplayName
import java.text.SimpleDateFormat
import java.util.*

data class ChatRequest(
    val codeId: String = "",
    val senderId: String = uid(),
    val receiverId: String = "",
    val senderName: String = userDisplayName(),
    val receiverName: String = "",
    val senderToken: String = "",
    val timestamp: Any = ServerValue.TIMESTAMP,
    val incoming: Boolean = false,
    val status: RequestStatus = RequestStatus.PENDING
) {
    private val sdf by lazy {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }

//    val formattedTimeStamp: String
//        get() {
//            val date = Date(timestamp as Long)
//            return sdf.format(date)
//        }
}