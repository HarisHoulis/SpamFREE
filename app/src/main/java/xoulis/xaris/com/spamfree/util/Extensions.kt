package xoulis.xaris.com.spamfree.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.Group
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.snackbar.Snackbar
import xoulis.xaris.com.spamfree.R
import xoulis.xaris.com.spamfree.data.vo.ClientCode
import xoulis.xaris.com.spamfree.data.vo.CodeStatus
import xoulis.xaris.com.spamfree.data.vo.LocationPoint
import java.util.*

/* Views */
fun View.enableView(enable: Boolean) {
    isEnabled = enable
    isClickable = enable
    alpha = if (enable) 1.0f else 0.5f
}

fun View.showView(show: Boolean) {
    visibility = if (show) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

fun View.showSnackBar(messageResId: Int, shortDuration: Boolean = true) {
    val duration = if (shortDuration) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
    Snackbar.make(this, messageResId, duration).show()
}

fun Group.setAllOnClickListeners(listener: View.OnClickListener) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

/* CustomDialogHelper */
inline fun Context.getDialog(
    title: String,
    text: String = "",
    inputType: Int = InputType.TYPE_CLASS_TEXT,
    filter: InputFilter = InputFilter.LengthFilter(6),
    autoDismiss: Boolean = true,
    f: CustomDialogHelper.() -> Unit
): AlertDialog =
    CustomDialogHelper(this, title, text, inputType, filter, autoDismiss).apply {
        f()
    }.create()


/* Network */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

/* Request's response */
fun String.decodeRequestResponseMessage(): Int {
    return when (this) {
        "0" -> R.string.inexistent_code
        "1" -> R.string.req_code_unusable
        "2" -> R.string.existing_req
        "3" -> R.string.existing_chat
        else -> R.string.req_sent_successfully
    }
}

/* Location */
@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.getLastLocation(f: (lp: LocationPoint) -> Unit) {
    lastLocation.addOnSuccessListener {
        val lp = if (it != null) {
            LocationPoint(it.latitude, it.longitude)
        } else {
            LocationPoint()
        }
        f(lp)
    }
}

fun LocationPoint.computeDistanceTo(destinationPoint: LocationPoint): Float {
    val pointA = Location("pointA")
        .apply {
            latitude = this@computeDistanceTo.latitude
            longitude = this@computeDistanceTo.longitude
        }

    val pointB = Location("pointB").apply {
        latitude = destinationPoint.latitude
        longitude = destinationPoint.longitude
    }
    return pointA.distanceTo(pointB)
}

/* Code */
fun ClientCode.hasExpired() {
    if (status == CodeStatus.EXPIRED) {
        return
    }
    val calendar = Calendar.getInstance()
    val currentDate = calendar.time
    val expirationDate = getExpirationDate()
    if (currentDate.after(expirationDate)) {
        codesDbRef.child("$id/status").setValue(CodeStatus.EXPIRED)
    }
}