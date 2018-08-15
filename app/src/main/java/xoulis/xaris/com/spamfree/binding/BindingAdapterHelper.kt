package xoulis.xaris.com.spamfree.binding

import android.databinding.BindingAdapter
import android.text.TextUtils
import android.widget.ImageView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import xoulis.xaris.com.spamfree.R

@BindingAdapter("userImage")
fun loadUserImage(view: CircleImageView, imageUrl: String?) {
    if (TextUtils.isEmpty(imageUrl) || imageUrl == view.context.getString(R.string.default_user_image_name)) {
        return
    }

    Picasso.get()
        .load(imageUrl)
        .error(R.drawable.ic_default_avatar)
        .into(view)
}
