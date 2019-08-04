package io.particle.android.sdk.ui.devicelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.particle.sdk.app.R


class DeviceFilterFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceFilterFragment()
    }

//    private lateinit var viewModel: DeviceFilterViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProviders.of(this).get(DeviceFilterViewModel::class.java)
    }

}
