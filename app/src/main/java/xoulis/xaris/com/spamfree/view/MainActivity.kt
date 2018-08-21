package xoulis.xaris.com.spamfree.view

import android.content.Intent
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import xoulis.xaris.com.spamfree.R
import kotlinx.android.synthetic.main.activity_main.*
import xoulis.xaris.com.spamfree.view.chats.ChatsFragment
import xoulis.xaris.com.spamfree.view.codes.CodesFragment
import xoulis.xaris.com.spamfree.view.requests.RequestsFragment
import xoulis.xaris.com.spamfree.view.settings.SettingsActivity

class MainActivity : AppCompatActivity() {

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

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
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                true
            }
            R.id.action_logout -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> RequestsFragment.newInstance("fragment1", "requests")
                1 -> ChatsFragment.newInstance("fragment2", "chats")
                else -> CodesFragment.newInstance()
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }
}
