package xoulis.xaris.com.spamfree.data.vo

import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue
import xoulis.xaris.com.spamfree.util.uid
import xoulis.xaris.com.spamfree.util.userDisplayName

data class ChatRequest(
        val codeId: String = "",
        val senderId: String = uid(),
        val receiverId: String = "",
        val senderName: String = userDisplayName(),
        val receiverName: String = "",
        val senderImage: String = "",
        val receiverImage: String = "",
        val senderToken: String = "",
        val timestamp: Any = ServerValue.TIMESTAMP,
        val incoming: Boolean = false,
        val status: RequestStatus = RequestStatus.PENDING,
        val messages: String = ""
) {
    @Exclude
    fun getTimestampLong() = timestamp as Long
}