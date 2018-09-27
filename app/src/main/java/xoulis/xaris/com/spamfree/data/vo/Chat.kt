package xoulis.xaris.com.spamfree.data.vo

import android.os.Parcel
import android.os.Parcelable
import androidx.versionedparcelable.VersionedParcelize
import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Chat(
    val codeId: String = "",
    val ownerId: String = "",
    val ownerImage: String = "",
    val memberImage: String = "",
    val ownerName: String = "",
    val memberName: String = "",
    val lastMessage: String = "",
    val lastMessageDate: @RawValue HashMap<String, Any> = hashMapOf("timestamp" to ServerValue.TIMESTAMP),
    val messages: String = ""
) : Parcelable {

    @Exclude
    fun getTimestampLong() = lastMessageDate["timestamp"] as Long
}