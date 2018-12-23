package xoulis.xaris.com.spamfree.data.services

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import xoulis.xaris.com.spamfree.util.REQUEST_RESPONSE_EXTRA
import xoulis.xaris.com.spamfree.util.REQUEST_RESPONSE_RECEIVED
import xoulis.xaris.com.spamfree.util.userDbRef

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        token?.let {
            userDbRef().child("deviceTokens/$it").setValue(true)
        }
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        message?.data?.let {
            if (it.isNotEmpty()) {
                if (it.containsKey(FCM_REQUEST_RESPONSE_KEY)) {
                    handleRequestResponse(it[FCM_REQUEST_RESPONSE_KEY]!!)
                }
            }
        }
    }

    private fun handleRequestResponse(responseCode: String) {
        val intent = Intent(REQUEST_RESPONSE_RECEIVED)
        intent.putExtra(REQUEST_RESPONSE_EXTRA, responseCode)
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(intent)
    }

    private companion object {
        const val FCM_REQUEST_RESPONSE_KEY = "reqResp"
    }
}