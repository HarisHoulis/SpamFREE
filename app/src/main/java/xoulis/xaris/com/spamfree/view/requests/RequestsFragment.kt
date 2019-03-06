package xoulis.xaris.com.spamfree.view.requests

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_requests.*
import xoulis.xaris.com.spamfree.*
import xoulis.xaris.com.spamfree.data.vo.Chat
import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import xoulis.xaris.com.spamfree.data.vo.LocationPoint
import xoulis.xaris.com.spamfree.data.vo.RequestStatus
import xoulis.xaris.com.spamfree.databinding.ListItemRequestBinding
import xoulis.xaris.com.spamfree.util.getLastLocation
import xoulis.xaris.com.spamfree.util.incomingRequestsRef
import xoulis.xaris.com.spamfree.util.outgoingRequestsRef
import xoulis.xaris.com.spamfree.util.showView

class RequestsFragment : Fragment() {

    private lateinit var incomingReqAdapter: FirebaseRecyclerAdapter<ChatRequest, RequestsViewHolder>

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var currentAcceptedRequest: ChatRequest

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_requests, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupIncomingRequestsRecyclerView()
        setupSentRequestsRecyclerView()
    }

    fun onAcceptRequestClick(pos: Int, request: ChatRequest) {
        val itemRef = incomingReqAdapter.getRef(pos)
        itemRef.child("status")
            .setValue(RequestStatus.ACCEPTED)
            .addOnSuccessListener {
                itemRef.removeValue()
            }
        currentAcceptedRequest = request
        getLocationAndCreateChat(request)
    }

    fun onRejectRequestClick(pos: Int) {
        val itemRef = incomingReqAdapter.getRef(pos)
        itemRef.child("status")
            .setValue(RequestStatus.REJECTED)
            .addOnSuccessListener {
                itemRef.removeValue()
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndCreateChat(request: ChatRequest) {
        askPermission(Manifest.permission.ACCESS_FINE_LOCATION) {
            fusedLocationClient.getLastLocation { lp ->
                createNewChat(request, lp)
            }
        }.onDeclined {
            createNewChat(request, LocationPoint())
        }
    }

    private fun createNewChat(request: ChatRequest, locationPoint: LocationPoint) {
        val codeId = request.codeId
        val ownerId = request.receiverId
        val memberId = request.senderId
        val ownerImage = request.receiverImage
        val ownerName = request.receiverName
        val memberImage = request.senderImage
        val memberName = request.senderName
        val messagesLimit = request.messages
        val db = FirebaseDatabase.getInstance()

        val membersMap = mapOf(ownerId to true, memberId to true)
        // Add chat's members to /chat_members
        db.getReference("/chat_members/$codeId/").setValue(membersMap)

        // Add chat's ID to /user_chats for each user
        db.getReference("/user_chats/$ownerId/$codeId").setValue(true)
        db.getReference("/user_chats/$memberId/$codeId").setValue(true)

        // Create new chat
        db.getReference("/chats/$codeId").setValue(
            Chat(
                codeId,
                ownerId,
                ownerImage,
                memberId,
                memberImage,
                ownerName,
                memberName,
                messages = messagesLimit,
                location = locationPoint
            )
        )
    }

    private fun setupIncomingRequestsRecyclerView() {
        val options = FirebaseRecyclerOptions.Builder<ChatRequest>()
            .setLifecycleOwner(this)
            .setQuery(incomingRequestsRef(), ChatRequest::class.java)
            .build()
        incomingReqAdapter =
            object : FirebaseRecyclerAdapter<ChatRequest, RequestsViewHolder>(options) {
                override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RequestsViewHolder {
                    val inflater = LayoutInflater.from(p0.context)
                    val itemBinding =
                        ListItemRequestBinding.inflate(inflater, p0, false)
                    return RequestsViewHolder(itemBinding)
                }

                override fun onBindViewHolder(
                    holder: RequestsViewHolder,
                    position: Int,
                    model: ChatRequest
                ) {
                    holder.bind(model)
                }

                override fun onDataChanged() {
                    super.onDataChanged()
                    empty_incoming_requests_textView.showView(itemCount == 0)
                }
            }
        requests_received_recyclerView.setHasFixedSize(true)
        requests_received_recyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
        requests_received_recyclerView.adapter = incomingReqAdapter
    }

    private fun setupSentRequestsRecyclerView() {
        val options = FirebaseRecyclerOptions.Builder<ChatRequest>()
            .setLifecycleOwner(this)
            .setQuery(outgoingRequestsRef(), ChatRequest::class.java)
            .build()
        val adapter = object : FirebaseRecyclerAdapter<ChatRequest, RequestsViewHolder>(options) {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RequestsViewHolder {
                val inflater = LayoutInflater.from(p0.context)
                val itemBinding =
                    ListItemRequestBinding.inflate(inflater, p0, false)
                return RequestsViewHolder(itemBinding)
            }

            override fun onBindViewHolder(
                holder: RequestsViewHolder,
                position: Int,
                model: ChatRequest
            ) {
                holder.bind(model)
            }

            override fun onDataChanged() {
                super.onDataChanged()
                empty_outgoing_requests_textView.showView(itemCount == 0)
            }
        }
        requests_sent_recyclerView.setHasFixedSize(true)
        requests_sent_recyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
        requests_sent_recyclerView.adapter = adapter
    }

    inner class RequestsViewHolder(private val itemBinding: ListItemRequestBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(request: ChatRequest) {
            itemBinding.request = request
            if (request.incoming) {
                setListeners(request)
            }
            itemBinding.executePendingBindings()
        }

        private fun setListeners(request: ChatRequest) {
            itemBinding.acceptRequestButton.setOnClickListener {
                onAcceptRequestClick(adapterPosition, request)
            }

            itemBinding.declineRequestButton.setOnClickListener {
                onRejectRequestClick(adapterPosition)
            }
        }
    }

    companion object {
        const val PERMISSIONS_REQUEST_FINE_LOCATION = 199
    }
}
