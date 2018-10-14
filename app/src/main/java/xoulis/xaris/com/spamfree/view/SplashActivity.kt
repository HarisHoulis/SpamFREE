package xoulis.xaris.com.spamfree.view

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.data.vo.User
import java.util.*
import kotlin.collections.HashMap

class SplashActivity : AppCompatActivity() {

    private companion object {
        const val RC_SIGN_IN = 123
    }

    private val signInProviders = Arrays.asList(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        createNotificationChannel()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            showMainActivity()
        } else {
            signInUser()
        }
    }

    private fun signInUser() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(signInProviders)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK && response != null) {
                if (response.isNewUser) {
                    addUserToDb()
                }
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
                showMainActivity()
            } else {
                val errorMessage = when (response) {
                    null -> getString(R.string.sign_in_cancelled);
                    { response.error?.errorCode ?: ErrorCodes.NO_NETWORK == ErrorCodes.NO_NETWORK } -> getString(
                        R.string.no_internet_connection
                    )
                    else -> getString(R.string.unknown_error)
                }
                errorMessage.showSnackBar()
            }
        }
    }

    private fun addUserToDb() {
        val name = FirebaseAuth.getInstance().currentUser?.displayName ?: "You"
        val user = User(
            name,
            getString(R.string.user_default_status),
            getString(R.string.default_user_image_name)
        )
        userDbRef().setValue(user)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun showMainActivity() {
        val mainActivityIntent = Intent(this@SplashActivity, MainActivity::class.java)
        intent.extras?.getString(FCM_CHAT_ID_KEY)?.let {
            mainActivityIntent.putExtra(CHAT_ID_EXTRA, it)
        }
        startActivity(mainActivityIntent)
        finish()
    }

    private fun String.showSnackBar() {
        val rootView: View by lazy { findViewById<View>(R.id.main_activity_root) }
        Snackbar.make(rootView, this, Snackbar.LENGTH_SHORT).show()
    }
}
