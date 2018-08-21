package xoulis.xaris.com.spamfree.view.settings

import android.arch.lifecycle.LiveData
import android.util.Log
import com.google.firebase.database.*

class SettingsLiveData(val query: Query) : LiveData<DataSnapshot>() {

    constructor(ref: DatabaseReference) : this(ref as Query)

    private val settingsListener = SettingsValueEventListener()

    override fun onActive() {
        query.addValueEventListener(settingsListener)
    }

    override fun onInactive() {
        query.removeEventListener(settingsListener)
    }

    private inner class SettingsValueEventListener : ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
            value = p0
        }

        override fun onCancelled(p0: DatabaseError) {
            Log.e("SettingsLiveData", "Can't listen to query $query", p0.toException())
        }
    }
}