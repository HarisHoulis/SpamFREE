package xoulis.xaris.com.spamfree.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import xoulis.xaris.com.spamfree.dbRef

class SettingsViewModel : ViewModel() {

    val userLiveData = SettingsLiveData(dbRef())
}