package io.particle.mesh.common

private const val ALL_HEX_CHARS = "0123456789ABCDEF"
@Suppress("PrivatePropertyName")
private val HEX_CHARARRAY = ALL_HEX_CHARS.toCharArray()


fun ByteArray?.toHex(): String {
    if (this == null) {
        return "[null]"
    }

    val hexChars = CharArray(this.size * 2)

    for (i in this.indices) {
        val asInt = this[i].toInt()
        val v = asInt and 0xFF
        hexChars[i * 2] = HEX_CHARARRAY[v ushr 4]
        hexChars[i * 2 + 1] = HEX_CHARARRAY[v and 0x0F]
    }

    return String(hexChars)
}


fun Byte.toHex(): String {
    return byteArrayOf(this).toHex()
}
