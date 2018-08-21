package xoulis.xaris.com.spamfree

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/* Firebase */
private const val CHILD_USERS = "users"
private const val CHILD_CODES = "codes"
private val UID = FirebaseAuth.getInstance().uid

val userDbRef =  FirebaseDatabase.getInstance().reference.child(CHILD_USERS).child(UID!!)
val userCodesDbRef = userDbRef.child(CHILD_CODES)