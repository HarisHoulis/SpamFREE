package xoulis.xaris.com.spamfree.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.view.MainActivity

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

    private fun handleNewMessage(messageBody: String, chatId: String) {

    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String, chatId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ChannelName",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private companion object {
        const val FCM_REQUEST_RESPONSE_KEY = "reqResp"
        const val NOTIFICATION_ID = 999
    }
}