package xoulis.xaris.com.spamfree.data.vo

import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue

data class ChatMessage(
    val chatId: String = "",
    val senderId: String = "",
    val senderImage: String = "",
    val message: String = "",
    val timestamp: Any = ServerValue.TIMESTAMP
) {
    @Exclude
    fun getTimestampLong(): Long {
        return timestamp as Long
    }
}