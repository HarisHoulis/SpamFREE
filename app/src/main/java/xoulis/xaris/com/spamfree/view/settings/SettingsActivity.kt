package xoulis.xaris.com.spamfree.view.settings

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.data.vo.User
import xoulis.xaris.com.spamfree.databinding.ActivitySettingsBinding


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val chatPhotoStorageRef =
        FirebaseStorage.getInstance().reference.child("profile_photos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.isLoading = true
        binding.handler = this

        val viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        viewModel.userLiveData.observe(this, Observer {
            it?.run {
                val s = getValue(User::class.java)
                binding.user = getValue(User::class.java)
                binding.isLoading = false
            }
        })
    }

    fun onImageChangeClick() {
        if (!this.isNetworkAvailable()) {
            binding.settingsActivityRoot.showSnackBar(R.string.no_internet_connection)
        } else {
            CropImage.activity()
                .setAspectRatio(1, 1)
                .setMaxCropResultSize(5000, 5000)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = CropImage.getActivityResult(data)
            uploadUserPhoto(result.uri)
        }
    }

    private fun uploadUserPhoto(selectedImageUri: Uri) {
        if (!this.isNetworkAvailable()) {
            binding.settingsActivityRoot.showSnackBar(R.string.no_internet_connection)
        } else {
            selectedImageUri.lastPathSegment?.let {
                val photoRef = chatPhotoStorageRef.child(it)
                photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener { _ ->
                        photoRef.downloadUrl
                            .addOnCompleteListener { task ->
                                check(task.isSuccessful) { "Failed to retrieve url for user photo" }
                                updateUserPhotoPath(task.result.toString())
                            }
                    }
            }
        }
    }

    private fun updateUserPhotoPath(imageUrl: String) {
        // Update locally
        binding.user?.apply { image = imageUrl }

        // Update DB
        userDbRef().child("image").setValue(imageUrl)

        // Call server function
        val data = hashMapOf("imageUrl" to imageUrl)
        FirebaseFunctions
            .getInstance("europe-west1")
            .getHttpsCallable("updateUserImageInChats")
            .call(data)
    }

    fun onStatusChangeClick(user: User) {
        val title = getString(R.string.new_status)
        val text = user.status
        val dialog = this.getDialog(title, text) {
            setEditTextWatcher()
            setOkButtonClickListener { newStatus ->
                updateUserStatus(newStatus, user)
            }
        }
        dialog.show()
    }

    private fun updateUserStatus(newStatus: String, user: User) {
        if (newStatus != user.status) {
            userDbRef().child("status").setValue(newStatus)
        }
    }
}
