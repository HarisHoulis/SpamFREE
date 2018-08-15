package xoulis.xaris.com.spamfree.view

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import xoulis.xaris.com.spamfree.R
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

        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
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
        val (uid, name) = FirebaseAuth.getInstance().currentUser.getUserInfo()
        val ref = FirebaseDatabase.getInstance().reference.child("Users").child(uid)
        val user = User(
            name,
            getString(R.string.user_default_status),
            getString(R.string.default_user_image_name)
        )
        ref.setValue(user)
    }

    private fun FirebaseUser?.getUserInfo(): Pair<String, String> =
        Pair(this!!.uid, displayName ?: "You")


    private fun showMainActivity() {
        intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun String.showSnackBar() {
        val rootView: View by lazy { findViewById<View>(R.id.main_activity_root) }
        Snackbar.make(rootView, this, Snackbar.LENGTH_SHORT).show()
    }
}
