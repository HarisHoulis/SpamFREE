package xoulis.xaris.com.spamfree.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_main.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.util.*
import xoulis.xaris.com.spamfree.view.chats.ChatsFragment
import xoulis.xaris.com.spamfree.view.codes.CodesFragment
import xoulis.xaris.com.spamfree.view.requests.RequestsFragment
import xoulis.xaris.com.spamfree.view.settings.ProfileFragment

class MainActivity : AppCompatActivity(), ChatsFragment.OnChatsFetchedListener {

    private val requestResponseReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                p1?.getStringExtra(REQUEST_RESPONSE_EXTRA)?.let {
                    val decodedMessage = it.decodeRequestResponseMessage()
                    requestResponseListener?.onRequestResponseReceived()
                    main_content.showSnackBar(decodedMessage)
                }
            }
        }
    }

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    private var requestResponseListener: OnRequestResponseListener? = null
    private var newMessageNotificationListener: OnNewMessageNotificationListener? = null

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(requestResponseReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        viewPager.adapter = mSectionsPagerAdapter
        viewPager.offscreenPageLimit = 3

        // Implement swipe views
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(viewPager))

        // Register request response receiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                requestResponseReceiver, IntentFilter(
                    REQUEST_RESPONSE_RECEIVED
                )
            )
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.action_settings -> {
                val fragment = ProfileFragment.newInstance(uid())
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right,0,0, android.R.anim.slide_out_right)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logoutUser() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                startActivity(Intent(this, SplashActivity::class.java))
                this.finish()
            }
    }

    override fun onChatsFetched() {
        intent.getStringExtra(CHAT_ID_EXTRA)?.let { chatId ->
            intent.removeExtra(CHAT_ID_EXTRA)
            viewPager.currentItem = 1
            newMessageNotificationListener?.onNewMessageNotificationReceived(chatId)
        }
    }

    fun setRequestResponseListener(listener: OnRequestResponseListener) {
        this.requestResponseListener = listener
    }

    fun setNewMessageNotificationListener(listener: OnNewMessageNotificationListener) {
        this.newMessageNotificationListener = listener
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> RequestsFragment()
                1 -> ChatsFragment()
                else -> CodesFragment()
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }

    interface OnRequestResponseListener {
        fun onRequestResponseReceived()
    }

    interface OnNewMessageNotificationListener {
        fun onNewMessageNotificationReceived(chatId: String)
    }
}
