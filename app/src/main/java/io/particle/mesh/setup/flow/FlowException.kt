package io.particle.mesh.setup.flow


enum class ExceptionType {
    EXPECTED_FLOW,
    ERROR_RECOVERABLE,
    ERROR_FATAL
}


class FlowException(
    msg: String = "",
    val exceptionType: ExceptionType = ExceptionType.ERROR_RECOVERABLE
) : Exception(msg) {
    // FIXME: give this an "error type" which describes the exact nature of the error
    // (e.g.: couldn't connect BT device
}


