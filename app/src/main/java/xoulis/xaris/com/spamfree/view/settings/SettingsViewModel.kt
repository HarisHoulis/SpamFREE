package xoulis.xaris.com.spamfree.view.settings

import android.arch.lifecycle.ViewModel
import xoulis.xaris.com.spamfree.userDbRef

class SettingsViewModel : ViewModel() {

    val userLiveData = SettingsLiveData(userDbRef())
}