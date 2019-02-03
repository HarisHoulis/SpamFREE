package xoulis.xaris.com.spamfree.view.settings

import android.arch.lifecycle.ViewModel
import xoulis.xaris.com.spamfree.util.userDbRef
import xoulis.xaris.com.spamfree.util.usersDbRef

class SettingsViewModel : ViewModel() {

    private var uid: String? = null


    fun setUid(uid: String) {
        this.uid = uid
    }

    fun getUserLiveData(): SettingsLiveData =
        SettingsLiveData(usersDbRef.child(uid!!))

}