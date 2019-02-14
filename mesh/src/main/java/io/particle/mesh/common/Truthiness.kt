package io.particle.mesh.common


private const val falseyByte = 0.toByte()
private const val falseyShort = 0.toShort()


fun String?.truthy(): Boolean = this != null && this.isNotEmpty()

fun Boolean?.truthy(): Boolean = this ?: false

fun Byte?.truthy(): Boolean = this != null && this != falseyByte

fun Char?.truthy(): Boolean = this != null

fun Short?.truthy(): Boolean = this != null && this != falseyShort

fun Int?.truthy(): Boolean = this != null && this != 0

fun Float?.truthy(): Boolean = this != null && this != 0.0f

fun Long?.truthy(): Boolean = this != null && this != 0L

fun Double?.truthy(): Boolean = this != null && this != 0.0


fun ByteArray?.truthy(): Boolean = this?.isNotEmpty() ?: false

fun FloatArray?.truthy(): Boolean = this?.isNotEmpty() ?: false

fun DoubleArray?.truthy(): Boolean = this?.isNotEmpty() ?: false

fun ShortArray?.truthy(): Boolean = this?.isNotEmpty() ?: false

fun IntArray?.truthy(): Boolean = this?.isNotEmpty() ?: false

fun LongArray?.truthy(): Boolean = this?.isNotEmpty() ?: false

fun Array<*>?.truthy(): Boolean = this?.isNotEmpty() ?: false


fun Collection<*>?.truthy(): Boolean = this != null && this.isNotEmpty()


// third party types:

fun okio.ByteString?.truthy(): Boolean = this != null && this.size() > 0

fun com.google.protobuf.ByteString?.truthy(): Boolean = this != null && !this.isEmpty