package xoulis.xaris.com.spamfree.view.requests


import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_requests.*
import xoulis.xaris.com.spamfree.*

import xoulis.xaris.com.spamfree.data.vo.ChatRequest
import xoulis.xaris.com.spamfree.data.vo.RequestStatus
import xoulis.xaris.com.spamfree.databinding.ListItemRequestBinding

class RequestsFragment : Fragment() {

    private lateinit var incomingReqAdapter: FirebaseRecyclerAdapter<ChatRequest, RequestsViewHolder>

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

    fun onAcceptRequestClick(view: View) {
        val itemPos = requests_received_recyclerView.getChildAdapterPosition(view)
        val itemRef = incomingReqAdapter.getRef(itemPos)
        itemRef.child("status").setValue(1)
            .addOnSuccessListener {
                itemRef.removeValue()
            }

        Log.i("req122", "req accepted")
    }

    fun onRejectRequestClick(view: View) {
        val itemPos = requests_received_recyclerView.getChildAdapterPosition(view)
        val itemRef = incomingReqAdapter.getRef(itemPos)
        itemRef.child("status").setValue(2)
            .addOnSuccessListener {
                itemRef.removeValue()
            }

        Log.i("req122", "req rejected")
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

                    override fun onChildChanged(
                        type: ChangeEventType,
                        snapshot: DataSnapshot,
                        newIndex: Int,
                        oldIndex: Int
                    ) {
                        val s = 4
                        super.onChildChanged(type, snapshot, newIndex, oldIndex)
                    }
                }
        requests_received_recyclerView.setHasFixedSize(true)
        requests_received_recyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
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
        }
        requests_sent_recyclerView.setHasFixedSize(true)
        requests_sent_recyclerView.layoutManager = LinearLayoutManager(context)
        requests_sent_recyclerView.adapter = adapter
    }

    inner class RequestsViewHolder(private val itemBinding: ListItemRequestBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(request: ChatRequest) {
            itemBinding.request = request
            if (request.incoming) {
                itemBinding.handler = this@RequestsFragment
            }
            itemBinding.executePendingBindings()
        }
    }
}
