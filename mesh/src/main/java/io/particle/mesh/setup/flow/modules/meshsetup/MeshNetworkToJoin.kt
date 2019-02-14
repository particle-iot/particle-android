package io.particle.mesh.setup.flow.modules.meshsetup

import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo


// FIXME: figure out a way to make this "internal"
sealed class MeshNetworkToJoin {

    data class SelectedNetwork(
            val networkToJoin: NetworkInfo
    ) : MeshNetworkToJoin()

    class CreateNewNetwork : MeshNetworkToJoin() {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

}
