package xoulis.xaris.com.spamfree.view.settings


import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputFilter
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView


import xoulis.xaris.com.spamfree.data.vo.User
import xoulis.xaris.com.spamfree.databinding.FragmentProfileBinding
import xoulis.xaris.com.spamfree.util.*
import android.view.*
import kotlinx.android.synthetic.main.fragment_profile.*
import xoulis.xaris.com.spamfree.R


private const val ARG_UID = "argument_uid"

class ProfileFragment() : Fragment() {
    private var paramUserId: String? = null

    private lateinit var binding: FragmentProfileBinding
    private val chatPhotoStorageRef =
        FirebaseStorage.getInstance().reference.child("profile_photos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            paramUserId = it.getString(ARG_UID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showButtons = paramUserId == uid()
        binding.isLoading = true
        binding.handler = this

        val viewModel = ViewModelProviders.of(this)
            .get(SettingsViewModel::class.java)
            .apply {
                setUid(paramUserId!!)
            }
        viewModel.getUserLiveData().observe(this, Observer {
            it?.run {
                binding.user = getValue(User::class.java)
                binding.isLoading = false
            }
        })

        settings_back_arrow_imageView.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
        }
    }

    fun onImageChangeClick() {
        if (context!!.isNetworkAvailable()) {
            binding.settingsActivityRoot.showSnackBar(xoulis.xaris.com.spamfree.R.string.no_internet_connection)
        } else {
            CropImage.activity()
                .setAspectRatio(1, 1)
                .setMaxCropResultSize(5000, 5000)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(activity!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = CropImage.getActivityResult(data)
            uploadUserPhoto(result.uri)
        }
    }

    private fun uploadUserPhoto(selectedImageUri: Uri) {
        if (context!!.isNetworkAvailable()) {
            binding.settingsActivityRoot.showSnackBar(R.string.no_internet_connection)
        } else {
            selectedImageUri.lastPathSegment?.let {
                val photoRef = chatPhotoStorageRef.child(it)
                photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener {
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
        val dialog =
            context!!.getDialog(title = title, text = text, filter = InputFilter.LengthFilter(30)) {
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

    companion object {
        @JvmStatic
        fun newInstance(uid: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_UID, uid)
                }
            }
    }
}
