package io.particle.android.sdk.ui

import android.app.AlertDialog
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.VariableType
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode.FUNCTIONS
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode.VARIABLES
import io.particle.android.sdk.utils.AnimationUtil
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Py.list
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.data_header_list.view.*
import kotlinx.android.synthetic.main.fragment_data.*
import kotlinx.android.synthetic.main.row_function_list.view.*
import kotlinx.android.synthetic.main.row_variable_list.view.*
import java.io.IOException
import java.util.*


private data class Variable(val name: String, val variableType: VariableType)
private data class Function(val name: String)


class FunctionsAndVariablesFragment : Fragment() {

    enum class DisplayMode {
        VARIABLES,
        FUNCTIONS
    }


    companion object {

        private const val ARG_DEVICE = "ARG_DEVICE"
        private const val ARG_DISPLAY_MODE = "ARG_DISPLAY_MODE"

        fun newInstance(device: ParticleDevice, displayMode: DisplayMode): FunctionsAndVariablesFragment {
            return FunctionsAndVariablesFragment().apply {
                arguments = bundleOf(
                    ARG_DEVICE to device,
                    ARG_DISPLAY_MODE to displayMode
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val top = inflater.inflate(R.layout.fragment_data, container, false)
        val device: ParticleDevice = arguments!!.getParcelable(ARG_DEVICE)!!
        val displayMode = arguments!!.getSerializable(ARG_DISPLAY_MODE) as DisplayMode

        data_list.setHasFixedSize(true)  // perf. optimization
        data_list.layoutManager = LinearLayoutManager(inflater.context)
        data_list.adapter = DataListAdapter(device, displayMode)
        data_list.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )
        return top
    }
}


private class DataListAdapter(
    private val device: ParticleDevice,
    private val mode: DisplayMode
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

    private val data = list<Any>()
    private var defaultBackground: Drawable? = null

    init {
        when (mode) {
            VARIABLES -> {
                data.add("Particle.variable()")
                for ((key, value) in device.variables) {
                    data.add(Variable(key, value))
                }
            }
            FUNCTIONS -> {
                data.add("Particle.function()")
                for (function in device.functions) {
                    data.add(Function(function))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (viewType) {
            FUNCTION -> {
                val v = LayoutInflater.from(parent.context).inflate(
                    R.layout.row_function_list, parent, false
                )
                return FunctionViewHolder(v)
            }
            VARIABLE -> {
                val v = LayoutInflater.from(parent.context).inflate(
                    R.layout.row_variable_list, parent, false
                )
                return VariableViewHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(
                    R.layout.data_header_list, parent, false
                )
                return HeaderViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (defaultBackground == null) {
            defaultBackground = holder.topLevel.background
        }

        holder.topLevel.background = defaultBackground

        when (getItemViewType(position)) {
            HEADER -> {
                val header = data[position] as String
                val headerViewHolder = holder as HeaderViewHolder
                headerViewHolder.headerText.text = header
                //check if there's any data
                if (device.variables.isEmpty() && position != 0) {
                    headerViewHolder.emptyText.setText(R.string.no_exposed_variable_msg)
                    headerViewHolder.emptyText.visibility = View.VISIBLE
                } else if (device.functions.isEmpty() && position == 0) {
                    headerViewHolder.emptyText.setText(R.string.no_exposed_function_msg)
                    headerViewHolder.emptyText.visibility = View.VISIBLE
                } else {
                    headerViewHolder.emptyText.visibility = View.GONE
                }
            }

            VARIABLE -> {
                val variable = data[position] as Variable
                val varHolder = holder as VariableViewHolder
                varHolder.name.text = variable.name
                varHolder.topLevel.setBackgroundResource(R.color.device_item_bg)
                setupVariableType(varHolder, variable)
                setupVariableValue(varHolder, variable)
                varHolder.name.setOnClickListener { setupVariableValue(varHolder, variable) }
                varHolder.type.setOnClickListener { setupVariableValue(varHolder, variable) }
            }

            FUNCTION -> {
                val function = data[position] as Function
                val funcHolder = holder as FunctionViewHolder
                funcHolder.name.text = function.name
                funcHolder.topLevel.setBackgroundResource(R.color.device_item_bg)
                setupArgumentSend(funcHolder, function)
                setupArgumentExpandAndCollapse(funcHolder)
            }
        }
    }

    private fun createValuePopup(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.action_clipboard) { dialog, _ ->
                val clipboard: ClipboardManager? = context.getSystemService()
                val clip = ClipData.newPlainText(title, message)
                if (clipboard != null) {
                    clipboard.primaryClip = clip
                }
                Toast.makeText(context, R.string.clipboard_copy_variable, Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.close) { dialogInterface, _ -> dialogInterface.dismiss() }
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

    private fun setupArgumentSend(holder: FunctionViewHolder, function: Function) {
        val context = holder.itemView.context
        holder.argument.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEND) {
                holder.progressBar.visibility = View.VISIBLE
                holder.value.visibility = View.GONE

                try {
                    Async.executeAsync(device, object : Async.ApiWork<ParticleDevice, Int>() {
                        @Throws(ParticleCloudException::class, IOException::class)
                        override fun callApi(particleDevice: ParticleDevice): Int? {
                            try {
                                val imm = context.getSystemService<InputMethodManager>()

                                imm?.hideSoftInputFromWindow(holder.argument.windowToken, 0)
                                return particleDevice.callFunction(
                                    function.name,
                                    ArrayList(listOf(holder.argument.text.toString()))
                                )
                            } catch (e: ParticleDevice.FunctionDoesNotExistException) {
                                e.printStackTrace()
                            } catch (e: IllegalArgumentException) {
                                e.printStackTrace()
                            }

                            return -1
                        }

                        override fun onSuccess(value: Int) {
                            holder.value.text = value.toString()
                            holder.progressBar.visibility = View.GONE
                            holder.value.visibility = View.VISIBLE
                        }

                        override fun onFailure(exception: ParticleCloudException) {
                            holder.value.text = ""
                            Toast.makeText(
                                context,
                                R.string.sending_argument_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                            holder.progressBar.visibility = View.GONE
                            holder.value.visibility = View.VISIBLE
                        }
                    })
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

    private fun setupVariableValue(holder: VariableViewHolder, variable: Variable) {
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

    private fun setupVariableType(holder: VariableViewHolder, variable: Variable) {
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
            is Variable -> VARIABLE
            is Function -> FUNCTION
            else -> HEADER
        }
    }

    companion object {
        internal val HEADER = 0
        internal val FUNCTION = 1
        internal val VARIABLE = 2
    }
}