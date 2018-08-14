package xoulis.xaris.com.spamfree

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

private const val USERS_CHILD = "Users"
private val UID = FirebaseAuth.getInstance().uid

val dbRef = { FirebaseDatabase.getInstance().reference.child(USERS_CHILD).child(UID!!) }



