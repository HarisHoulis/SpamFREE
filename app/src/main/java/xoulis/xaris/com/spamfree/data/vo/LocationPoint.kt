package xoulis.xaris.com.spamfree.data.vo

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LocationPoint(
    val latitude: Double = 37.983810,
    val longitude: Double = 23.727539
) : Parcelable