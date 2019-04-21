package io.particle.mesh.setup.flow

import io.particle.mesh.setup.flow.ExceptionType.ERROR_RECOVERABLE


enum class ExceptionType {
    EXPECTED_FLOW,      // this is expected, just use the retry logic to continue
    ERROR_RECOVERABLE,  // can't continue at this point, but retrying might help
    ERROR_FATAL         // can't continue and flow has to be restarted
}


open class MeshSetupFlowException(
    message: String,
    cause: Throwable? = null,
    val severity: ExceptionType = ExceptionType.ERROR_RECOVERABLE
) : Exception(message, cause)


class ExpectedFlowException(
    message: String
) : MeshSetupFlowException(message, severity = ExceptionType.EXPECTED_FLOW)


class NoBarcodeScannedException(cause: Throwable? = null) : MeshSetupFlowException(
    "No data matrix scanned.  Please exit setup and try again.",
    cause,
    severity = ExceptionType.ERROR_FATAL
)

//trying to perform action at the wrong time
class IllegalOperationException(cause: Throwable? = null) :
    MeshSetupFlowException("Setup encountered an unexpected error.  Please try again.", cause)

//EnsureTargetDeviceCanBeClaimed
class UnableToGenerateClaimCodeException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error attempting to claim this device to your account.",
    cause
)

//ConnectToTargetDevice && ConnectToCommissionerDevice
class DeviceTooFarException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your mesh device is too far away from your phone. Please hold your phone closer and try again.",
    cause
)

class FailedToStartScanException(cause: Throwable? = null) : MeshSetupFlowException(
    "Bluetooth appears to be disabled on your phone. Please enable Bluetooth and try again.",
    cause
)

class FailedToFlashBecauseOfTimeoutException(cause: Throwable? = null) : MeshSetupFlowException(
    "It seems that your device has exited listening mode. Please put your device in listening mode (blinking blue) and retry.",
    cause
)

class FailedToScanBecauseOfTimeoutException(cause: Throwable? = null) : MeshSetupFlowException(
    "Unable to find your mesh device. Make sure the mesh device's LED is blinking blue and that it's not connected to any other devices.",
    cause
)

class FailedToConnectException(cause: Throwable? = null) :
    MeshSetupFlowException("Failed to connect to your mesh device. Please try again.", cause)

class CannotAddGatewayDeviceAsJoinerException(cause: Throwable? = null) : MeshSetupFlowException(
    "Support for adding multiple gateways to a single network is coming soon. Argons, Borons, and Xenons with Ethernet FeatherWings, must be set up as a standalone device or as the first gateway in a new mesh network.",
    cause
)

class UnableToDownloadFirmwareBinaryException(cause: Throwable? = null) :
    MeshSetupFlowException("Failed to download the firmware update. Please try again later.", cause)

//Can happen in any step, inform user about it and repeat the step
class BluetoothDisabledException(cause: Throwable? = null) : MeshSetupFlowException(
    "Bluetooth appears to be disabled on your phone. Please enable Bluetooth and try again.",
    cause
)

class BluetoothConnectionDroppedException(cause: Throwable? = null) : MeshSetupFlowException(
    "The Bluetooth connection was dropped unexpectedly. Please restart the set up and try again.",
    cause
)

//Can happen in any step, when result != NONE and special class is not handled by onReply handler
class BluetoothErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    "Something went wrong with Bluetooth. Please restart the set up process and try again.",
    cause
)

class BluetoothTimeoutException(cause: Throwable? = null) :
    MeshSetupFlowException("Sending Bluetooth messages failed. Please try again.", cause)

//EnsureCommissionerNetworkMatchesException(cause: Throwable? = null): MeshSetupFlowException()
class CommissionerNetworkDoesNotMatchException(cause: Throwable? = null) : MeshSetupFlowException(
    "The assisting device is on a different mesh network than the one you are trying to join. Please make sure the devices are trying to use the same network.",
    cause
)

class WrongNetworkPasswordException(cause: Throwable? = null) :
    MeshSetupFlowException("The password you entered is incorrect.", cause)

class PasswordTooShortException(cause: Throwable? = null) :
    MeshSetupFlowException("Your network password must be between 6 and 16 characters.", cause)

class WifiPasswordTooShortException(cause: Throwable? = null) :
    MeshSetupFlowException("The password you entered is too short.", cause)

class SameDeviceScannedTwiceException(cause: Throwable? = null) : MeshSetupFlowException(
    "This is the device that is being setup. Please scan the sticker of device that is on the mesh network you are trying to join.",
    cause
)

class WrongTargetDeviceTypeException(cause: Throwable? = null) : MeshSetupFlowException(
    "This is not valid device sticker. Please scan 3rd generation device sticker.",
    cause
)

class WrongCommissionerDeviceTypeException(cause: Throwable? = null) : MeshSetupFlowException(
    "This is not valid device sticker. Please scan 3rd generation device sticker.",
    cause
)

//EnsureHasInternetAccess
class FailedToObtainIpException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your device failed to obtain an IP address. Please make sure your device has internet access.",
    cause
)

class FailedToObtainIpBoronException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your device is taking longer than expected to connect to the Internet. If you are setting up a Boron 2/3G, it may take up to 5 minutes to establish a connection with the cellular tower in your area.",
    cause
)

class FailedToUpdateDeviceOSException(cause: Throwable? = null) :
    MeshSetupFlowException("There was an error while performing a Device OS update.", cause)

class InvalidDeviceStateException(cause: Throwable? = null) : MeshSetupFlowException(
    "Device is in invalid state, please reset the device and start again.",
    cause
)

//GetNewDeviceName
class SimBelongsToOtherAccountException(cause: Throwable? = null) : MeshSetupFlowException(
    "The SIM you are trying to interact with is owned by a different user account.",
    cause
)

class CriticalFlowErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was a problem with the setup. Please contact support with the latest device log to help us fix it as soon as possible.",
    cause
)

class ExternalSimNotSupportedException(cause: Throwable? = null) : MeshSetupFlowException(
    "We have detected that you are using external sim card. Use the internal SIM to complete setup. You may use an external SIM after setup is complete.",
    cause
)

class StickerErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    "There is a problem with the sticker on your device. Please contact support for a solution.",
    cause
)

class NetworkErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was a network error communicating to Particle Device Cloud.",
    cause
)

class FailedToActivateSimException(
    severity: ExceptionType = ERROR_RECOVERABLE
) : MeshSetupFlowException(
    "SIM activation is taking longer than expected. Please retry your SIM activation. If you have retried multiple times, please contact support.",
    null,
    severity
)

class CCMissingException(cause: Throwable? = null) : MeshSetupFlowException(
    "You need to add a credit card to your account to continue. Please visit https://console.particle.io/billing/edit-card to add a card and return here when you're done.",
    cause
)

class UnableToGetPricingInformationException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while retrieving pricing information. Please try again.",
    cause
)

class UnableToPublishDeviceSetupEventException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while notifying the Particle Device Cloud about successful device setup. Please try again.",
    cause
)

class UnableToGetSimStatusException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while reading internal SIM card status. Please try again.",
    cause
)

class UnableToJoinNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while adding your device to mesh network on Particle Device Cloud.",
    cause
)

class UnableToJoinOldNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    "The network you are trying to join was created locally with test version of the app. Please create new network.",
    cause
)

class UnableToRetrieveNetworksException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while accessing your mesh network information on Particle Device Cloud.",
    cause
)

class UnableToLeaveNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while removing your device from the mesh network on Particle Device Cloud.",
    cause
)

class UnableToCreateNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while registering your new network with Particle Device Cloud.",
    cause
)

class UnableToRenameDeviceException(cause: Throwable? = null) : MeshSetupFlowException(
    "Unable to rename your device at this time. Please try again later.",
    cause
)

class NameTooShortException(cause: Throwable? = null) :
    MeshSetupFlowException("Your device name cannot be empty.", cause)

class BoronModemErrorException(cause: Throwable? = null) : MeshSetupFlowException(
    "There was an error while accessing modem on your device. Device is now rebooting the modem "
            + "in attempt to recover. Give it a second and try again. If this error persists try "
            + "rebooting your device manually and restart the setup.",
    cause
)

class NameInUseException(cause: Throwable? = null) : MeshSetupFlowException(
    "You already own a network with this name. Please use different name.",
    cause
)

class DeviceIsNotAllowedToJoinNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your device was unable to join the network (NOT_ALLOWED). Please press try again.",
    cause
)

class DeviceIsUnableToFindNetworkToJoinException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your device was unable to join the network (NOT_FOUND). Please press try again.",
    cause
)

class DeviceTimeoutWhileJoiningNetworkException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your device was unable to join the network (TIMEOUT). Please press try again.",
    cause
)

class ThisDeviceIsACommissionerException(cause: Throwable? = null) : MeshSetupFlowException(
    "This device now acts as commissioner. Please restart the setup if you want to set it up again.",
    cause
)

//CheckDeviceGotClaimed
class DeviceConnectToCloudTimeoutException(cause: Throwable? = null) : MeshSetupFlowException(
    "Your device could not connect to Device Cloud. Please try again.",
    cause
)

class DeviceGettingClaimedTimeoutException(cause: Throwable? = null) :
    MeshSetupFlowException("Your device failed to be claimed. Please try again.", cause)
