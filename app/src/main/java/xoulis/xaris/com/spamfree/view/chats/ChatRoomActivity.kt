package xoulis.xaris.com.spamfree.view.chats

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_chat_room.*
import xoulis.xaris.com.spamfree.R

class ChatRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val args = intent.extras
        var receiverName = "Anonymous"
        args?.let {
            receiverName = args.getString("receiverName") ?: "Anonymous"
        }
        initToolbar(receiverName)
    }

    private fun initToolbar(title: String) {
        setSupportActionBar(chat_room_toolbar)
        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = title
        }
    }
}
