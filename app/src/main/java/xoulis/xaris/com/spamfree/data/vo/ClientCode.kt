package xoulis.xaris.com.spamfree.data.vo

import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue
import java.util.*

data class ClientCode(
    val id: String = "",
    val hasActiveRequest: Boolean = false,
    var messages: String = "5",
    val assignedUid: String = "",
    val timestamp: Any = ServerValue.TIMESTAMP,
    val months: Int = 3,
    val status: CodeStatus = CodeStatus.UNUSED
) {
    private val calendar by lazy {
        Calendar.getInstance()
    }

    @Exclude
    private fun getTimestampLong() = timestamp as Long

    @Exclude
    fun getExpirationDate(): Date {
        return calendar.apply {
            time = Date(getTimestampLong())
            add(Calendar.DATE, months * 30)
        }.time
    }
}