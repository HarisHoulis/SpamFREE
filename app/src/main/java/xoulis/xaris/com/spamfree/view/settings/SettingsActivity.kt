package xoulis.xaris.com.spamfree.view.settings

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.custom_edit_text_dialog.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.data.vo.User
import xoulis.xaris.com.spamfree.databinding.ActivitySettingsBinding


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val chatPhotoStorageRef =
        FirebaseStorage.getInstance().reference.child("profile_photos")

    companion object {
        private const val RC_PHOTO_PICKER = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.isLoading = true
        binding.handler = this

        val viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        viewModel.userLiveData.observe(this, Observer {
            it?.run {
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
        binding.user?.let {
            val tempUser = it.apply {
                image = imageUrl
            }
            userDbRef.setValue(tempUser)
        }
    }

    fun onStatusChangeClick(user: User) {
        val dialogBuilder by lazy { AlertDialog.Builder(this) }
        dialogBuilder.setView(R.layout.custom_edit_text_dialog)
        val dialog by lazy { dialogBuilder.show() }

        val messageEditText: EditText =
            dialog.findViewById(R.id.custom_dialog_message)!!
        val okButton: Button =
            dialog.findViewById(R.id.custom_dialog_ok_button)!!

        okButton.enableView(false)
        messageEditText.setText(user.status)
        dialog.custom_dialog_title.text = getString(R.string.new_status)

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                okButton.enableView(p0.toString() != user.status)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        okButton.setOnClickListener {
            val newStatus = messageEditText.text.toString().trim()
            updateUserStatus(newStatus, user)
            dialog.dismiss()
        }
    }

    private fun updateUserStatus(newStatus: String, user: User) {
        if (newStatus != user.status) {
            userDbRef.setValue(user)
        }
    }
}