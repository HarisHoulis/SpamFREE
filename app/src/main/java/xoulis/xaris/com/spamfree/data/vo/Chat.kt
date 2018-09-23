package xoulis.xaris.com.spamfree.data.vo

import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue

data class Chat(
    val codeId: String = "",
    val ownerId: String = "",
    val ownerImage: String = "",
    val memberImage: String = "",
    val ownerName: String = "",
    val memberName: String = "",
    val lastMessage: String = "",
    val lastMessageDate: HashMap<String, Any> = hashMapOf("timestamp" to ServerValue.TIMESTAMP),
    val messages: String = ""
) {
    @Exclude
    fun getTimestampLong() = lastMessageDate["timestamp"] as Long
}