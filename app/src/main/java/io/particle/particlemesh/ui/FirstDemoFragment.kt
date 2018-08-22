package io.particle.particlemesh.ui


import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.particlemesh.bluetooth.connecting.BTDeviceAddress
import io.particle.particlemesh.common.android.livedata.distinct
import io.particle.particlemesh.common.truthy
import io.particle.particlemesh.meshsetup.connection.BT_SETUP_SERVICE_ID
import io.particle.particlemesh.meshsetup.connection.ProtocolTranceiver
import io.particle.particlemesh.meshsetup.connection.buildMeshDeviceScanner
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_first_demo.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


private val deviceConnections = mutableMapOf<BTDeviceAddress, ProtocolTranceiver>()


private const val ARGON_ADDY = "F8:6C:27:52:46:4B"
private const val XENON_ADDY = "D2:CF:8B:9B:5C:82"


data class Params(
        val networkName: String,
        val commissionerCredential: String,
        val networkInfo: Mesh.NetworkInfo?,
        val joinerEui64: String?,
        val joinerPassword: String?
)


class FirstDemoFragment : Fragment() {

    @Volatile  // create blank params to start
    var params = Params(
            networkName = "BeefNet",
            commissionerCredential = "IAmAPassword",
            networkInfo = null,
            joinerEui64 = null,
            joinerPassword = null
    )

    private val log = KotlinLogging.logger {}

    lateinit var scannerLD: LiveData<List<ScanResult>?>

    private var selectedDeviceAddress: BTDeviceAddress = ARGON_ADDY

    private lateinit var applicationContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        applicationContext = context.applicationContext
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        initUi()

//        launch {
//            val demo = SSLEngineSimpleDemo()
//            demo.runDemo()
//        }
        launch {
//            val kotlinExample = JPAKEExample()
//            val origExample = JPAKEExampleOrig()

            try {
//                kotlinExample.gogogo()
                log.info { "Kotlin example completed successfully!" }
//                origExample.gogogo()
//                log.info { "Kotlin example completed successfully!" }

            } catch (ex: Exception) {
                log.error(ex) { "Example failed: " }
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first_demo, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (device in deviceConnections.values) {
            device.disconnect()
        }
    }


    private fun initUi() {
        action_ArgonConnectionStateToggle.setOnClickListener { toggleConnection(ARGON_ADDY) }

        action_XenonConnectionStateToggle.setOnClickListener { toggleConnection(XENON_ADDY) }

        action_send_GetNetworkInfo.setOnClickListener {
            launch(UI) { selectedSender()?.sendGetNetworkInfo() }
        }

        action_send_CreateNetwork.setOnClickListener {
            launch(UI) {
                selectedSender()?.sendCreateNetwork(
                        params.networkName, params.commissionerCredential
                )
            }
        }

        action_send_Auth.setOnClickListener {
            launch(UI) { selectedSender()?.sendAuth(params.commissionerCredential) }
        }

        action_send_StartCommissioner.setOnClickListener {
            launch(UI) { selectedSender()?.sendStartCommissioner() }
        }

        action_send_PrepareJoiner.setOnClickListener {
            launch(UI) { selectedSender()?.sendPrepareJoiner(params.networkInfo!!) }
        }

        action_send_AddJoiner.setOnClickListener {
            launch(UI) {
                selectedSender()?.sendAddJoiner(
                        params.joinerEui64!!, params.joinerPassword!!
                )
            }
        }

        action_send_JoinNetwork.setOnClickListener {
            launch(UI) { selectedSender()?.sendJoinNetwork() }
        }

        action_send_StopCommissioner.setOnClickListener {
            launch(UI) { selectedSender()?.sendStopCommissioner() }
        }

        action_send_LeaveNetwork.setOnClickListener {
            launch(UI) { selectedSender()?.sendLeaveNetwork() }
        }

        argonLabel.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchActiveDevice(ARGON_ADDY)
            }
        }

        xenonLabel.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchActiveDevice(XENON_ADDY)
            }
        }
    }

    private fun switchActiveDevice(btAddress: BTDeviceAddress) {
        selectedDeviceAddress = btAddress
        if (btAddress == ARGON_ADDY) {
            xenonLabel.isChecked = false
        } else if (btAddress == XENON_ADDY) {
            argonLabel.isChecked = false
        }
    }

    private fun selectedSender(): ProtocolTranceiver? {
        return deviceConnections[selectedDeviceAddress]
    }

    private fun toggleConnection(btAddress: BTDeviceAddress) {
        if (deviceConnections[btAddress]?.isConnected.truthy()) {
            disconnect(btAddress)
        } else {
            connect(btAddress)
        }
    }

    private fun connect(btAddress: BTDeviceAddress) {
//        launch(UI) {
//            val rsFactory = RequestSenderFactory(
//                    BluetoothConnectionManager(applicationContext)
//            )
//
//            log.info { "building sender for " }
//            val requestSender = rsFactory.buildRequestSender(btAddress, btAddress)
//            log.info { "sender built" }
//            if (requestSender == null) {
//                applicationContext.safeToast("Could not connect to $btAddress!")
//            } else {
////                deviceConnections[btAddress] = UIRequestSender(this@FirstDemoFragment, requestSender)
//            }
//            updateDeviceStatusIcons()
//        }
    }

    private fun disconnect(btAddress: BTDeviceAddress) {
        deviceConnections[btAddress]?.disconnect()
        deviceConnections.remove(btAddress)
        launch(UI) {
            delay(500)
            updateDeviceStatusIcons()
        }
    }

    private fun updateDeviceStatusIcons() {
        val addressesToButtons = mapOf(
                ARGON_ADDY to action_ArgonConnectionStateToggle,
                XENON_ADDY to action_XenonConnectionStateToggle
        )
        for ((addy, button) in addressesToButtons) {
            doUpdateDeviceStatusIcon(addy, button)
        }
    }

    private fun doUpdateDeviceStatusIcon(address: BTDeviceAddress, imageButton: ImageButton) {
        val sender = deviceConnections[address]
        @DrawableRes val statusIcon = if (sender?.isConnected.truthy()) {
            R.drawable.ic_bluetooth_connected_black_24dp
        } else {
            R.drawable.ic_bluetooth_black_24dp
        }
        imageButton.setImageResource(statusIcon)
    }

    private fun initScan() {
        val scannerAndSwitch = buildMeshDeviceScanner(
                applicationContext,
                { sr -> sr.device.name.truthy() },
                ScanFilter.Builder().setServiceUuid(ParcelUuid(BT_SETUP_SERVICE_ID)).build()
        )
        scannerAndSwitch.toggleSwitch.value = true
        scannerLD = scannerAndSwitch.scannerLD.distinct()

        scannerLD.observe(
                this,
                Observer { results: List<ScanResult>? -> log.info { "Look ma, results: $results" } }
        )
    }

    private fun echoTest() {
//        launch(UI) {
//            log.info { "Building request sender factory" }
//            val rsFactory = RequestSenderFactory(
//                    BluetoothConnectionManager(applicationContext)
//            )
//            log.info { "building sender" }
//            val requestSender = rsFactory.buildRequestSender(ARGON_ADDY, "Argon")
//            log.info { "sender built" }
//            val beefBytes = byteArrayOf(
//                    0xDE.toByte(),
//                    0xAD.toByte(),
//                    0xBE.toByte(),
//                    0xEF.toByte()
//            )
//            val response = requestSender!!.sendEchoRequest(beefBytes)
//            val result = if (response.value?.contentEquals(beefBytes).truthy()) "WINNING" else "try again bro."
//            log.info { "Expected response='${beefBytes.toHex()}', actual response=${response.value.toHex()}, result: $result" }
//        }
    }

}


//class UIRequestSender(
//        private val fragment: FirstDemoFragment,
//        private val sender: ProtocolTranceiver
//) : AbstractRequestSender {
//    override suspend fun sendGetDeviceId(): Result<GetDeviceIdReply, ResultCode> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun sendSetClaimCode(claimCode: String): Result<SetClaimCodeReply, ResultCode> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    private val log = KotlinLogging.logger {}
//
//
//    val isConnected: Boolean
//        get() = sender.isConnected
//
//    fun disconnect() {
//        sender.disconnect()
//    }
//
//    override suspend fun sendGetNetworkInfo(): Result<Mesh.GetNetworkInfoReply, Common.ResultCode> {
//        val result = sender.sendGetNetworkInfo()
//        onResult(result)
//        return result
//    }
//
//    override suspend fun sendCreateNetwork(
//            name: String,
//            password: String
//    ): Result<Mesh.CreateNetworkReply, Common.ResultCode> {
//        val result = sender.sendCreateNetwork(name, password)
//        onResult(result)
//        if (result.value != null) {
//            fragment.params = fragment.params.copy(networkInfo = result.value!!.network)
//        }
//        return result
//    }
//
//    override suspend fun sendAuth(
//            commissionerCredential: String
//    ): Result<Mesh.AuthReply, Common.ResultCode> {
//        val result = sender.sendAuth(commissionerCredential)
//        onResult(result)
//        return result
//    }
//
//    override suspend fun sendStartCommissioner(): Result<Mesh.StartCommissionerReply, Common.ResultCode> {
//        val result = sender.sendStartCommissioner()
//        onResult(result)
//        return result
//    }
//
//    override suspend fun sendPrepareJoiner(
//            network: Mesh.NetworkInfo
//    ): Result<Mesh.PrepareJoinerReply, Common.ResultCode> {
//        val result = sender.sendPrepareJoiner(network)
//        onResult(result)
//        if (result.value != null) {
//            val joinerReply = result.value!!
//            fragment.params = fragment.params.copy(
//                    joinerEui64 = joinerReply.eui64,
//                    joinerPassword = joinerReply.password
//            )
//        }
//        return result
//    }
//
//    override suspend fun sendAddJoiner(eui64: String, joiningCredential: String
//    ): Result<Mesh.AddJoinerReply, Common.ResultCode> {
//        val result = sender.sendAddJoiner(eui64, joiningCredential)
//        onResult(result)
//        return result
//    }
//
//    override suspend fun sendJoinNetwork(): Result<Mesh.JoinNetworkReply, Common.ResultCode> {
//        val result = sender.sendJoinNetwork()
//        onResult(result)
//        return result
//    }
//
//    override suspend fun sendStopCommissioner(): Result<Mesh.StopCommissionerReply, Common.ResultCode> {
//        val result = sender.sendStopCommissioner()
//        onResult(result)
//        return result
//    }
//
//    override suspend fun sendLeaveNetwork(): Result<Mesh.LeaveNetworkReply, Common.ResultCode> {
//        val result = sender.sendLeaveNetwork()
//        onResult(result)
//        return result
//    }
//
//    private fun <T : GeneratedMessageV3> onResult(result: Result<T, Common.ResultCode>) {
//        when (result) {
//            is Result.Present -> output("Success: $result")
//            is Result.Error -> output("Error: ${result.error}")
//            is Result.Absent -> output("No result received!")
//        }
//    }
//
//    private fun output(msg: String) {
//        log.info { "message result: $msg" }
//        fragment.activity?.toast(msg)
//    }
//
//}