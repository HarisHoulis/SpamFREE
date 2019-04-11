package xoulis.xaris.com.spamfree.view.settings

import androidx.lifecycle.ViewModel
import xoulis.xaris.com.spamfree.util.usersDbRef

class SettingsViewModel : ViewModel() {

    private var uid: String? = null


    fun setUid(uid: String) {
        this.uid = uid
    }

    fun getUserLiveData(): SettingsLiveData =
        SettingsLiveData(usersDbRef.child(uid!!))

}