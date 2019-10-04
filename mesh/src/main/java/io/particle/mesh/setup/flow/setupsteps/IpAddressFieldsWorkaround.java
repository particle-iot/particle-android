package io.particle.mesh.setup.flow.setupsteps;

import com.google.protobuf.ByteString;

import io.particle.firmwareprotos.ctrl.Common;
import io.particle.firmwareprotos.ctrl.Network;


// Workaround for build issue on Windows & Mac (but somehow not on Linux) where accessing
// the .v4 and .v6 properties of an IpAddress instance made the compiler complain
// about missing classes(!)
public class IpAddressFieldsWorkaround {

    public static boolean addressHasValue(Network.InterfaceAddress ifAddress) {
        Common.IpAddress address = ifAddress.getAddress();
        return isTruthy(address.getV4().getAddress()) || isTruthy(address.getV6().getAddress());
    }

    private static boolean isTruthy(int addressAsInt) {
        return addressAsInt != 0;
    }

    private static boolean isTruthy(ByteString byteString) {
        return byteString != null && !byteString.isEmpty();
    }

}
