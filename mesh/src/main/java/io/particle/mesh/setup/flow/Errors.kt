package io.particle.mesh.setup.flow

import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.ExceptionType.ERROR_RECOVERABLE


enum class ExceptionType {
    EXPECTED_FLOW,      // this is expected, just use the retry logic to continue
    ERROR_RECOVERABLE,  // can't continue at this point, but retrying might help
    ERROR_FATAL         // can't continue and flow has to be restarted
}


open class MeshSetupFlowException(
    cause: Throwable? = null,
    message: String? = null,
    val severity: ExceptionType = ExceptionType.ERROR_RECOVERABLE,
    val userFacingMessage: String? = null
) : Exception(message ?: userFacingMessage, cause)


class ExpectedFlowException(message: String) :
    MeshSetupFlowException(message = message, severity = ExceptionType.EXPECTED_FLOW)


open class TerminateFlowException(
    message: String
) : MeshSetupFlowException(
    message = message,
    severity = ERROR_FATAL
)


class TerminateFlowAndStartControlPanelException(
    val device: ParticleDevice
) : TerminateFlowException("Ending flow and starting Control Panel!")


// For when a user bails out of setup, like hiting the "x" button before setup is complete
class UserTerminatedFlowException(
    message: String?
) : MeshSetupFlowException(
    message = message,
    severity = ERROR_FATAL
)

class NoBarcodeScannedException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "No data matrix scanned.  Please exit setup and try again.",
    severity = ExceptionType.ERROR_FATAL
)


//trying to perform action at the wrong time
class IllegalOperationException(message: String) :
    MeshSetupFlowException(
        null,
        message,
        userFacingMessage = "Setup encountered an unexpected error. Please try again."
    )

// trying to perform an operation which shouldn't be performed
// (e.g.: showing pricing screen for NODE_JOINER setup)
class InvalidOperationException(message: String, severity: ExceptionType = ERROR_FATAL) :
    MeshSetupFlowException(
        null,
        message,
        severity = severity,
        userFacingMessage = "Setup encountered an unexpected error. Please try again."
    )

//EnsureTargetDeviceCanBeClaimed
class UnableToGenerateClaimCodeException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error attempting to claim this device to your account."
)

//ConnectToTargetDevice && ConnectToCommissionerDevice
class DeviceTooFarException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    severity = ERROR_FATAL,
    userFacingMessage = "Your mesh device is too far away from your phone. Please hold your phone closer and try again."
)

class FailedToScanBecauseOfTimeoutException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Unable to find your mesh device. Make sure the mesh device's LED is blinking blue and that it's not connected to any other devices."
)

class FailedToConnectException(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "Failed to connect to your mesh device. Please try again."
    )

class CannotAddGatewayDeviceAsJoinerException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Support for adding multiple gateways to a single network is coming soon. Argons, Borons, and Xenons with Ethernet FeatherWings, must be set up as a standalone device or as the first gateway in a new mesh network."
)

class UnableToDownloadFirmwareBinaryException(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "Failed to download the firmware update. Please try again later."
    )


class BluetoothDisabledException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Bluetooth appears to be disabled on your phone. Please enable Bluetooth and try again."
)

class BluetoothConnectionDroppedException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "The Bluetooth connection was dropped unexpectedly. Please restart the set up and try again."
)

//Can happen in any step, when result != NONE and special class is not handled by onReply handler
class BluetoothErrorException(cause: Throwable? = null, message: String? = null) : MeshSetupFlowException(
    cause,
    message = message,
    userFacingMessage = "Something went wrong with Bluetooth. Please restart the set up process and try again."
)

class BluetoothTimeoutException(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "Sending Bluetooth messages failed. Please try again."
    )

class CommissionerNetworkDoesNotMatchException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "The assisting device is on a different mesh network than the one you are trying to join. Please make sure the devices are trying to use the same network."
)

class WrongNetworkPasswordException(cause: Throwable? = null) :
    MeshSetupFlowException(cause, userFacingMessage = "The password you entered is incorrect.")

class PasswordTooShortException(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "Your network password must be between 6 and 16 characters."
    )

class SameDeviceScannedTwiceException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "This is the device that is being setup. Please scan the sticker of device that is on the mesh network you are trying to join."
)


//EnsureHasInternetAccess
class FailedToObtainIpBoronException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Your device is taking longer than expected to connect to the Internet. If you are setting up a Boron 2/3G, it may take up to 5 minutes to establish a connection with the cellular tower in your area."
)

class FailedToUpdateDeviceOSException(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "There was an error while performing a Device OS update."
    )


class FailedToGetDeviceInfo(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "Unable to get information about the device from Device Cloud."
    )


class InvalidDeviceStateException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Device is in invalid state, please reset the device and start again."
)


class SimBelongsToOtherAccountException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "The SIM you are trying to interact with is owned by a different user account."
)

class ExternalSimNotSupportedException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "We have detected that you are using external sim card. Use the internal SIM to complete setup. You may use an external SIM after setup is complete."
)

class StickerErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There is a problem with the sticker on your device. Please contact support for a solution."
)


class FailedToActivateSimException(severity: ExceptionType, cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "SIM activation is taking longer than expected. Please retry your SIM activation. If you have retried multiple times, please contact support.",
    severity = severity
)


class FailedToDeactivateSimException : MeshSetupFlowException(
    userFacingMessage = "SIM deactivation is taking longer than expected. Please retry your SIM deactivation. If you have retried multiple times, please contact support.",
    severity = ERROR_FATAL
)


class FailedToChangeSimDataLimitException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error changing data limit for SIM card. If you have retried multiple times, please contact support.",
    severity = ERROR_RECOVERABLE
)


class UnableToGetPricingInformationException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while retrieving pricing information. Please try again."
)

class UnableToPublishDeviceSetupEventException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while notifying the Particle Device Cloud about successful device setup. Please try again."
)

class UnableToGetSimStatusException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while reading internal SIM card status. Please try again."
)

class UnableToJoinNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while adding your device to mesh network on Particle Device Cloud."
)

class UnableToRetrieveNetworksException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while accessing your mesh network information on Particle Device Cloud."
)

class UnableToLeaveNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while removing your device from the mesh network on Particle Device Cloud."
)

class UnableToCreateNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while registering your new network with Particle Device Cloud."
)

class UnableToRenameDeviceException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Unable to rename your device at this time. Please try again later."
)

class NameTooShortException(cause: Throwable? = null) :
    MeshSetupFlowException(cause, userFacingMessage = "Your device name cannot be empty.")

class BoronModemErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "There was an error while accessing modem on your device. Device is now rebooting the modem in attempt to recover. Give it a second and try again. If this error persists try rebooting your device manually and restart the setup."
)

class NetworkNameTooShortException(cause: Throwable? = null) :
    MeshSetupFlowException(cause, userFacingMessage = "Your network name cannot be empty.")

class NameInUseException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "You already own a network with this name. Please use different name."
)

class DeviceIsNotAllowedToJoinNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Your device was unable to join the network (NOT_ALLOWED). Please press try again."
)

class DeviceIsUnableToFindNetworkToJoinException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Your device was unable to join the network (NOT_FOUND). Please press try again."
)

class DeviceTimeoutWhileJoiningNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Your device was unable to join the network (TIMEOUT). Please press try again."
)

class ThisDeviceIsACommissionerException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "This device now acts as commissioner. Please restart the setup if you want to set it up again."
)

//CheckDeviceGotClaimed
class DeviceConnectToCloudTimeoutException(cause: Throwable? = null) : MeshSetupFlowException(
    cause,
    userFacingMessage = "Your device could not connect to Device Cloud. Please try again."
)

class DeviceGettingClaimedTimeoutException(cause: Throwable? = null) :
    MeshSetupFlowException(
        cause,
        userFacingMessage = "Your device failed to be claimed. Please try again."
    )
