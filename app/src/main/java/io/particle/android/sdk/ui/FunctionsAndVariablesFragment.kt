package io.particle.android.sdk.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme.LIGHT
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.VariableType
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode.FUNCTIONS
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode.VARIABLES
import io.particle.android.sdk.ui.RowItem.FunctionRow
import io.particle.android.sdk.ui.RowItem.HeaderRow
import io.particle.android.sdk.ui.RowItem.VariableRow
import io.particle.android.sdk.utils.AnimationUtil
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Py.list
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.ui.inflateFragment
import io.particle.mesh.ui.inflateRow
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.data_header_list.view.*
import kotlinx.android.synthetic.main.fragment_data.*
import kotlinx.android.synthetic.main.row_function_list.view.*
import kotlinx.android.synthetic.main.row_variable_list.view.*
import java.io.IOException


sealed class RowItem {

    data class HeaderRow(val title: String) : RowItem()

    data class VariableRow(val name: String, val variableType: VariableType) : RowItem()

    data class FunctionRow(val name: String) : RowItem()


    companion object {

        const val HEADER_ROW = R.layout.data_header_list
        const val VARIABLE_ROW = R.layout.row_variable_list
        const val FUNCTION_ROW = R.layout.row_function_list
    }
}


class FunctionsAndVariablesFragment : Fragment() {

    enum class DisplayMode {
        VARIABLES,
        FUNCTIONS
    }


    companion object {

        private const val ARG_DEVICE = "ARG_DEVICE"
        private const val ARG_DISPLAY_MODE = "ARG_DISPLAY_MODE"

        fun newInstance(
            device: ParticleDevice,
            displayMode: DisplayMode
        ): FunctionsAndVariablesFragment {
            return FunctionsAndVariablesFragment().apply {
                arguments = bundleOf(
                    ARG_DEVICE to device,
                    ARG_DISPLAY_MODE to displayMode
                )
            }
        }
    }

    private var viewLifecycleScopes = Scopes()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewLifecycleOwner.lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Event) {
                    if (event == Event.ON_CREATE) {
                        viewLifecycleScopes = Scopes()
                    } else if (event == Event.ON_DESTROY) {
                        viewLifecycleScopes.cancelAll()
                    }
                }

            }
        )
        return container?.inflateFragment(R.layout.fragment_data)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val device: ParticleDevice = arguments!!.getParcelable(ARG_DEVICE)!!
        val displayMode = arguments!!.getSerializable(ARG_DISPLAY_MODE) as DisplayMode

        data_list.setHasFixedSize(true)  // perf. optimization
        data_list.layoutManager = LinearLayoutManager(context)
        data_list.adapter = DataListAdapter(device, displayMode, viewLifecycleScopes)
        data_list.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
    }

}


private class DataListAdapter(
    private val device: ParticleDevice,
    private val mode: DisplayMode,
    private val scopes: Scopes
) : RecyclerView.Adapter<DataListAdapter.BaseViewHolder>() {

    internal open class BaseViewHolder(val topLevel: View) : RecyclerView.ViewHolder(topLevel)

    internal class HeaderViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val headerText: TextView = itemView.header_text
        val emptyText: TextView = itemView.header_empty
    }

    internal class FunctionViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val name: TextView = itemView.function_name
        val value: TextView = itemView.function_value
        val argument: EditText = itemView.function_argument
        val toggle: ImageView = itemView.function_toggle
        val argumentIcon: ImageView = itemView.function_argument_icon
        val progressBar: ProgressBar = itemView.function_progress
    }

    internal class VariableViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val name: TextView = itemView.variable_name
        val type: TextView = itemView.variable_type
        val value: TextView = itemView.variable_value
        val progressBar: ProgressBar = itemView.variable_progress
    }

    private val data = list<RowItem>()
    private var defaultBackground: Drawable? = null

    init {
        when (mode) {
            DisplayMode.VARIABLES -> {
                if (device.variables.isEmpty()) {
                    data.add(HeaderRow(""))
                }
                for ((key, value) in device.variables) {
                    data.add(VariableRow(key, value))
                }
            }
            DisplayMode.FUNCTIONS -> {
                if (device.functions.isEmpty()) {
                    data.add(HeaderRow(""))
                }
                for (function in device.functions) {
                    data.add(FunctionRow(function))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        // the pinLabelView type here is a layout res ID
        val v = parent.inflateRow(viewType)

        return when (viewType) {
            RowItem.FUNCTION_ROW -> FunctionViewHolder(v)
            RowItem.VARIABLE_ROW -> VariableViewHolder(v)
            else -> HeaderViewHolder(v)
        }
    }

    override fun onBindViewHolder(baseHolder: BaseViewHolder, position: Int) {
        if (defaultBackground == null) {
            defaultBackground = baseHolder.topLevel.background
        }

        baseHolder.topLevel.background = defaultBackground

        when (val item = data[position]) {
            is HeaderRow -> {
                val holder = baseHolder as HeaderViewHolder
                holder.headerText.text = item.title

                when (mode) {
                    VARIABLES -> {
                        if (device.variables.isEmpty()) {
                            holder.emptyText.setText(R.string.no_exposed_variable_msg)
                            holder.emptyText.isVisible = true
                        } else {
                            holder.emptyText.isVisible = false
                        }
                    }
                    FUNCTIONS -> {
                        if (device.functions.isEmpty()) {
                            holder.emptyText.setText(R.string.no_exposed_function_msg)
                            holder.emptyText.isVisible = true
                        } else {
                            holder.emptyText.isVisible = false
                        }
                    }
                }
            }
            is VariableRow -> {
                val holder = baseHolder as VariableViewHolder
                holder.name.text = item.name
                holder.topLevel.setBackgroundResource(R.color.device_item_bg)
                setupVariableType(holder, item)
                setupVariableValue(holder, item)
                holder.name.setOnClickListener { setupVariableValue(holder, item) }
                holder.type.setOnClickListener { setupVariableValue(holder, item) }
            }
            is FunctionRow -> {
                val funcHolder = baseHolder as FunctionViewHolder
                funcHolder.name.text = item.name
                funcHolder.topLevel.setBackgroundResource(R.color.device_item_bg)
                setupArgumentSend(funcHolder, item)
                setupArgumentExpandAndCollapse(funcHolder)
            }
        }
    }

    fun createValuePopup(context: Context, title: String, message: String) {
        MaterialDialog.Builder(context)
            .theme(LIGHT)
            .title(title)
            .content(message)
            .positiveText(R.string.action_clipboard)
            .onPositive { dialog, which ->
                val clipboard: ClipboardManager? = context.getSystemService()
                val clip = ClipData.newPlainText(title, message)
                if (clipboard != null) {
                    clipboard.primaryClip = clip
                }
                Toast.makeText(context, R.string.clipboard_copy_variable, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .negativeText(R.string.cancel)
            .autoDismiss(true)
            .canceledOnTouchOutside(true)
            .show()
    }

    private fun setupArgumentExpandAndCollapse(functionViewHolder: FunctionViewHolder) {
        functionViewHolder.toggle.setOnClickListener {
            if (functionViewHolder.argument.visibility == View.VISIBLE) {
                AnimationUtil.collapse(functionViewHolder.argument)
                AnimationUtil.collapse(functionViewHolder.argumentIcon)
                functionViewHolder.toggle.setImageResource(R.drawable.ic_expand)
            } else {
                AnimationUtil.expand(functionViewHolder.argument)
                AnimationUtil.expand(functionViewHolder.argumentIcon)
                functionViewHolder.toggle.setImageResource(R.drawable.ic_collapse)
            }
        }
    }

    private fun setupArgumentSend(holder: FunctionViewHolder, function: FunctionRow) {
        val context = holder.itemView.context
        holder.argument.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEND) {
                holder.progressBar.visibility = View.VISIBLE
                holder.value.visibility = View.GONE

                try {
                    scopes.onWorker {
                        val result: Int = try {
                            val imm = context.getSystemService<InputMethodManager>()

                            imm?.hideSoftInputFromWindow(holder.argument.windowToken, 0)
                            device.callFunction(
                                function.name,
                                ArrayList(listOf(holder.argument.text.toString()))
                            )

                        } catch (e: ParticleDevice.FunctionDoesNotExistException) {
                            e.printStackTrace()
                            -1
                        } catch (e: IllegalArgumentException) {
                            e.printStackTrace()
                            -1
                        }

                        scopes.onMain {
                            if (result == -1) {
                                holder.value.text = ""
                                Toast.makeText(
                                    context,
                                    R.string.sending_argument_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                                holder.progressBar.visibility = View.GONE
                                holder.value.visibility = View.VISIBLE
                            } else {
                                holder.value.text = result.toString()
                                holder.progressBar.visibility = View.GONE
                                holder.value.visibility = View.VISIBLE
                            }
                        }
                    }

                } catch (e: ParticleCloudException) {
                    holder.value.text = ""
                    Toast.makeText(
                        context,
                        R.string.sending_argument_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    holder.progressBar.visibility = View.GONE
                    holder.value.visibility = View.VISIBLE
                }

                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun setupVariableValue(holder: VariableViewHolder, variable: VariableRow) {
        holder.progressBar.visibility = View.VISIBLE
        holder.value.visibility = View.GONE
        try {
            Async.executeAsync(device, object : Async.ApiWork<ParticleDevice, String>() {
                @Throws(ParticleCloudException::class, IOException::class)
                override fun callApi(particleDevice: ParticleDevice): String {
                    return try {
                        if (variable.variableType === VariableType.INT) {
                            val value = device.getVariable(variable.name).toString()
                            val dotIndex = value.indexOf(".")
                            value.substring(
                                0,
                                if (dotIndex > 0) dotIndex else value.length
                            )
                        } else {
                            device.getVariable(variable.name).toString()
                        }
                    } catch (e: ParticleDevice.VariableDoesNotExistException) {
                        throw ParticleCloudException(e)
                    }

                }

                override fun onSuccess(value: String) {
                    holder.value.text = value
                    holder.progressBar.visibility = View.GONE
                    holder.value.visibility = View.VISIBLE
                    holder.value.setOnClickListener { view ->
                        createValuePopup(
                            view.context,
                            variable.name,
                            value
                        )
                    }
                }

                override fun onFailure(exception: ParticleCloudException) {
                    holder.value.text = ""
                    holder.progressBar.visibility = View.GONE
                    holder.value.visibility = View.VISIBLE
                }
            })
        } catch (e: ParticleCloudException) {
            holder.value.text = ""
            holder.progressBar.visibility = View.GONE
            holder.value.visibility = View.VISIBLE
        }

    }

    private fun setupVariableType(holder: VariableViewHolder, variable: VariableRow) {
        val type = when (variable.variableType) {
            VariableType.INT -> "(Integer)"
            VariableType.DOUBLE -> "(Double)"
            VariableType.STRING -> "(String)"
        }
        holder.type.text = type
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is HeaderRow -> RowItem.HEADER_ROW
            is VariableRow -> RowItem.VARIABLE_ROW
            is FunctionRow -> RowItem.FUNCTION_ROW
        }
    }
}