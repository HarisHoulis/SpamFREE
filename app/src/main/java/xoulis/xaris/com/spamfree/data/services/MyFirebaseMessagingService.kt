package xoulis.xaris.com.spamfree.data.services

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import xoulis.xaris.com.spamfree.REQUEST_RESPONSE_EXTRA
import xoulis.xaris.com.spamfree.REQUEST_RESPONSE_RECEIVED
import xoulis.xaris.com.spamfree.userDbRef

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {

    }

    override fun onMessageReceived(message: RemoteMessage?) {
        message?.data?.let {
            if (it.isNotEmpty()) {
                val intent = Intent(REQUEST_RESPONSE_RECEIVED)
                Log.i("req111", "req_res_received")
                intent.putExtra(REQUEST_RESPONSE_EXTRA, it["reqResp"])
                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(intent)
            }
        }
    }
}