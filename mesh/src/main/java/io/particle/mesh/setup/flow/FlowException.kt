package io.particle.mesh.setup.flow

import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.ExceptionType.ERROR_RECOVERABLE
import io.particle.mesh.setup.flow.ExceptionType.EXPECTED_FLOW


@Deprecated("Use the new one in Errors.kt")
open class FlowException(
    msg: String = "",
    val exceptionType: ExceptionType = ExceptionType.ERROR_RECOVERABLE,
    // FIXME: implement showing dialog...
    val showErrorAsDialog: Boolean = false
) : Exception(msg) {
    // FIXME: give this an "error type" which describes the exact nature of the error
    // (e.g.: couldn't connect BT device
}


class RecoverableFlowErrorException(
    msg: String,
    showErrorAsDialog: Boolean = false
) : FlowException(msg, ERROR_RECOVERABLE, showErrorAsDialog)


//class ExepectedFlowException(
//    msg: String,
//    showErrorAsDialog: Boolean = false
//) : FlowException(msg, EXPECTED_FLOW, showErrorAsDialog)


class FatalFlowException(
    msg: String,
    showErrorAsDialog: Boolean = false
) : FlowException(msg, ERROR_FATAL, showErrorAsDialog)
