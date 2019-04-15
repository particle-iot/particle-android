package io.particle.android.sdk.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.PointF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleEvent
import io.particle.android.sdk.cloud.ParticleEventHandler
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.ui.Ui
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_events.*
import kotlinx.android.synthetic.main.fragment_events.view.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Objects.requireNonNull

/**
 * Created by Julius.
 */
class EventsFragment : Fragment() {

    companion object {

        const val ARG_DEVICE = "ARG_DEVICE"  // The device that this fragment represents

        @JvmStatic
        fun newInstance(device: ParticleDevice): EventsFragment {
            return EventsFragment().apply {
                arguments = bundleOf(EventsFragment.ARG_DEVICE to device)
            }
        }
    }


    private var device: ParticleDevice? = null
    private var subscriptionId: Long? = null
    private var subscribed: Boolean = false

    private var eventsLayoutManager: LinearLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val top = inflater.inflate(R.layout.fragment_events, container, false)

        device = requireNonNull<Bundle>(arguments).getParcelable(ARG_DEVICE)
        top.events_empty.visibility = View.VISIBLE

        top.events_list.setHasFixedSize(true)  // perf. optimization
        eventsLayoutManager = SpeedyLinearLayoutManager(inflater.context)
        top.events_list.layoutManager = eventsLayoutManager
        val adapter = EventListAdapter()
        top.events_list.adapter = adapter
        top.events_list.addItemDecoration(
            DividerItemDecoration(
                requireNonNull<Context>(context),
                LinearLayout.VERTICAL
            )
        )

        setupClearListener(top, adapter)
        initEventSubscription(top, adapter)
        initFiltering(top, adapter)
        return top
    }

    override fun onResume() {
        super.onResume()
        if (!subscribed) {
            startEventSubscription(events_list.adapter as EventListAdapter)
        }
    }

    override fun onPause() {
        super.onPause()
        stopEventSubscription()
    }

    private fun setupClearListener(rootView: View, adapter: EventListAdapter) {
        rootView.findViewById<View>(R.id.events_clear).setOnClickListener { _ ->
            AlertDialog.Builder(requireNonNull<FragmentActivity>(activity))
                .setTitle(R.string.clear_events_title)
                .setMessage(R.string.clear_events_message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    events_empty.visibility = View.VISIBLE
                    adapter.clear()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun initFiltering(top: View, adapter: EventListAdapter) {
        val filter = Ui.findView<EditText>(top, R.id.events_search)
        filter.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                adapter.filter(v.text.toString())
                val view = activity!!.currentFocus
                if (view != null) {
                    val imm: InputMethodManager? = activity!!.getSystemService()
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                }
                return@setOnEditorActionListener true
            }
            false
        }

        filter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                adapter.filter(filter.text.toString())
            }
        })
    }

    private fun initEventSubscription(top: View, adapter: EventListAdapter) {
        val eventButton = top.findViewById<ImageView>(R.id.events_pause)
        eventButton.setOnClickListener { _ ->
            if (subscribed) {
                eventButton.setImageResource(R.drawable.ic_play)
                stopEventSubscription()
            } else {
                eventButton.setImageResource(R.drawable.ic_pause)
                startEventSubscription(adapter)
            }
        }
        startEventSubscription(adapter)
    }

    private fun startEventSubscription(adapter: EventListAdapter) {
        subscribed = true
        try {
            Async.executeAsync(device!!, object : Async.ApiProcedure<ParticleDevice>() {
                @Throws(IOException::class)
                override fun callApi(particleDevice: ParticleDevice): Void? {
                    try {
                        subscriptionId =
                            device!!.subscribeToEvents(null, object : ParticleEventHandler {
                                override fun onEventError(e: Exception) {
                                    e.printStackTrace()
                                }

                                override fun onEvent(
                                    eventName: String,
                                    particleEvent: ParticleEvent
                                ) {
                                    adapter.add(Event(eventName, particleEvent))
                                    if (eventsLayoutManager!!.findFirstVisibleItemPosition() < 1) {
                                        events_list.smoothScrollToPosition(0)
                                    }
                                    events_empty.post { events_empty.visibility = View.GONE }
                                }
                            })
                    } catch (ex: NullPointerException) {
                        //set not subscribed
                        subscribed = false
                    }

                    return null
                }

                override fun onFailure(exception: ParticleCloudException) {
                    exception.printStackTrace()
                }
            })
        } catch (e: ParticleCloudException) {
            //set not subscribed
            subscribed = false
        }

    }

    private fun stopEventSubscription() {
        subscribed = false
        try {
            Async.executeAsync(device!!, object : Async.ApiProcedure<ParticleDevice>() {
                @Throws(ParticleCloudException::class)
                override fun callApi(particleDevice: ParticleDevice): Void? {
                    try {
                        device!!.unsubscribeFromEvents(subscriptionId!!)
                    } catch (ignore: NullPointerException) {
                        //set to still subscribed
                        subscribed = true
                    }

                    return null
                }

                override fun onFailure(exception: ParticleCloudException) {
                    exception.printStackTrace()
                }
            })
        } catch (e: ParticleCloudException) {
            //set to still subscribed
            subscribed = true
        }

    }

    private class Event internal constructor(
        internal var name: String,
        internal var particleEvent: ParticleEvent
    )

    private class EventListAdapter : RecyclerView.Adapter<EventListAdapter.ViewHolder>() {

        private val data = list<Event>()
        private val filteredData = list<Event>()
        private var filter = ""

        internal class ViewHolder(val topLevel: View) : RecyclerView.ViewHolder(topLevel) {
            val eventName: TextView
            val eventData: TextView
            val eventTime: TextView
            val copyButton: View

            init {
                eventName = Ui.findView(topLevel, R.id.event_name)
                eventData = Ui.findView(topLevel, R.id.event_data)
                eventTime = Ui.findView(topLevel, R.id.event_time)
                copyButton = Ui.findView(topLevel, R.id.event_copy)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v =
                LayoutInflater.from(parent.context).inflate(R.layout.row_events_list, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val event = filteredData[position]
            holder.eventName.text = event.name
            holder.eventData.text = event.particleEvent.dataPayload
            holder.eventTime.text = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                .format(event.particleEvent.publishedAt)

            holder.copyButton.setOnClickListener { _ ->
                val context = holder.itemView.context
                val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Event data", buildEventClipboardCopy(event))
                clipboard.primaryClip = clip
                Toast.makeText(context, R.string.clipboard_copy_event_msg, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        private fun buildEventClipboardCopy(event: Event): String {
            val jsonObject = JSONObject()
            try {
                jsonObject.put("Event", event.name)
                jsonObject.put("DeviceID", event.particleEvent.deviceId)
                jsonObject.put("Data", event.particleEvent.dataPayload)
                val dateTime = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm::ssZ",
                    Locale.getDefault()
                ).format(event.particleEvent.publishedAt)
                jsonObject.put("Time", dateTime)
                jsonObject.put("TTL", event.particleEvent.timeToLive)
            } catch (ignore: JSONException) {
            }

            return jsonObject.toString()
        }

        internal fun clear() {
            filteredData.clear()
            data.clear()
            notifyDataSetChanged()
        }

        fun add(event: Event) {
            data.add(0, event)
            if (event.name.contains(filter)) {
                filteredData.add(0, event)
                notifyItemInserted(0)
            }
        }

        internal fun filter(filterText: String) {
            this.filter = filterText
            filteredData.clear()
            notifyDataSetChanged()

            for (event in data) {
                if (event.name.contains(filter) || event.particleEvent.dataPayload.contains(filter)) {
                    filteredData.add(event)
                    notifyItemInserted(data.indexOf(event))
                }
            }
        }

        override fun getItemCount(): Int {
            return filteredData.size
        }
    }


    private class SpeedyLinearLayoutManager : LinearLayoutManager {

        constructor(context: Context) : super(context)

        constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
            context,
            orientation,
            reverseLayout
        )

        constructor(
            context: Context,
            attrs: AttributeSet,
            defStyleAttr: Int,
            defStyleRes: Int
        ) : super(context, attrs, defStyleAttr, defStyleRes)

        override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State?,
            position: Int
        ) {
            val linearSmoothScroller = object : LinearSmoothScroller(recyclerView.context) {

                override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                    return this@SpeedyLinearLayoutManager.computeScrollVectorForPosition(
                        targetPosition
                    )
                }

                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
                }
            }

            linearSmoothScroller.targetPosition = position
            startSmoothScroll(linearSmoothScroller)
        }

        companion object {

            private const val MILLISECONDS_PER_INCH = 250f
        }
    }

}
