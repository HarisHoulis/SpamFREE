package xoulis.xaris.com.spamfree

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/* Firebase */
private const val CHILD_USERS = "users"
private const val CHILD_CODES = "client_codes"
const val CHILD_INCOMING_REQUESTS = "incoming_requests"
const val CHILD_OUTGOING_REQUESTS = "outgoing_requests"
const val CHILD_UNCHECKED_OUTGOING_REQUESTS = "unchecked_outgoing_requests"

val uid = { FirebaseAuth.getInstance().uid!! }
val userDisplayName = { FirebaseAuth.getInstance().currentUser!!.displayName!! }

val userDbRef = { FirebaseDatabase.getInstance().reference.child(CHILD_USERS).child(uid()) }
val userCodesDbRef = { FirebaseDatabase.getInstance().reference.child(CHILD_CODES).child(uid()) }

val incomingRequestsRef =
    { FirebaseDatabase.getInstance().reference.child(CHILD_INCOMING_REQUESTS).child(uid()) }
val outgoingRequestsRef =
    { FirebaseDatabase.getInstance().reference.child(CHILD_OUTGOING_REQUESTS).child(uid()) }
val uncheckedRequestsRef =
    {
        FirebaseDatabase.getInstance().reference.child(CHILD_UNCHECKED_OUTGOING_REQUESTS)
            .child(uid())
    }