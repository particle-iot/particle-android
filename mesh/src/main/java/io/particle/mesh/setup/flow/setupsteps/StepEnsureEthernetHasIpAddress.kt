package io.particle.mesh.setup.flow.setupsteps

import io.particle.firmwareprotos.ctrl.Network
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.R
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.DialogSpec.ResDialogSpec
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import kotlinx.coroutines.delay
import mu.KotlinLogging


class StepEnsureEthernetHasIpAddress(private val flowUi: FlowUiDelegate) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        // delay for a moment here because otherwise this always ends up failing the first time
        delay(2000)

        val targetXceiver = ctxs.requireTargetXceiver()

        suspend fun findEthernetInterface(): Network.InterfaceEntry? {
            val ifaceListReply = targetXceiver.sendGetInterfaceList().throwOnErrorOrAbsent()
            return ifaceListReply.interfacesList.firstOrNull { it.type == InterfaceType.ETHERNET }
        }

        val ethernet = findEthernetInterface()
        requireNotNull(ethernet)

        val reply = targetXceiver.sendGetInterface(ethernet.index).throwOnErrorOrAbsent()
        val iface = reply.`interface`
        for (addyList in listOf(iface.ipv4Config.addressesList, iface.ipv6Config.addressesList)) {

            val address = addyList.firstOrNull { it.hasAddressValue() }
            if (address != null) {
                log.debug { "IP address on ethernet (interface ${ethernet.index}) found: $address" }
                return
            }
        }

        val result = flowUi.dialogTool.dialogResultLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.dialogTool.newDialogRequest(
                ResDialogSpec(
                    R.string.p_connecttocloud_xenon_gateway_needs_ethernet,
                    android.R.string.ok
                )
            )
        }

        log.info { "result from awaiting on 'ethernet must be plugged in dialog: $result" }

        flowUi.dialogTool.clearDialogResult()
        throw ExpectedFlowException("Ethernet connection not plugged in; user prompted.")
    }

}


private fun Network.InterfaceAddress.hasAddressValue(): Boolean {
    return this.address.v4.address.truthy() || this.address.v6.address.truthy()
}