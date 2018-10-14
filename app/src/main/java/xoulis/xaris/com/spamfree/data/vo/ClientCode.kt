package xoulis.xaris.com.spamfree.data.vo

data class ClientCode(
    val id: String = "",
    val hasActiveRequest: Boolean = false,
    var messages: String = "5",
    var used: Boolean = false,
    val assignedUid: String = ""
)