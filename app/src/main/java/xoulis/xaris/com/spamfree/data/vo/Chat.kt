package xoulis.xaris.com.spamfree.data.vo

import android.location.Location
import android.os.Parcelable
import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Chat(
    val codeId: String = "",
    val ownerId: String = "",
    val ownerImage: String = "",
    val memberId: String = "",
    val memberImage: String = "",
    val ownerName: String = "",
    val memberName: String = "",
    val lastMessage: String = "",
    val timestamp: @RawValue Any = ServerValue.TIMESTAMP,
    val messages: String = "",
    val finished: Boolean = false,
    val location: LocationPoint = LocationPoint()
) : Parcelable {

    @Exclude
    fun getTimestampLong(): Long {
        return timestamp as Long
    }
}