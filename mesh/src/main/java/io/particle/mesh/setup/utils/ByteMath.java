package io.particle.mesh.setup.utils;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import okio.Buffer;

public class ByteMath {

    public static short readUint8LE(@NonNull Buffer buffer) {
        return ((short) (buffer.readByte() & 0xff));
    }

    @NonNull
    public static Buffer writeUint8LE(@NonNull Buffer buffer, int value) {
        return buffer.writeByte((byte) (value & 0xff));
    }

    public static int readUint16LE(@NonNull Buffer source) {
        return bytesAsInt(
                source.readByte(),  // "low byte"/LSB
                source.readByte(),  // "high byte"/MSB
                (byte) 0, (byte) 0);
    }

    @NonNull
    public static Buffer writeUint16LE(@NonNull Buffer buffer, int value) {
        return buffer.writeShortLe((short) (value & 0xffff));
    }

    public static int readUint16LE(@NonNull ByteBuffer source) {
        return bytesAsInt(
                source.get(),  // "low byte"/LSB
                source.get(),  // "high byte"/MSB
                (byte) 0, (byte) 0);
    }

    @NonNull
    public static ByteBuffer writeUint16LE(@NonNull ByteBuffer buffer, int value) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.putShort((short) (value & 0xffff));
    }


    /**
     * Return 4 bytes, given in little-endian order (the LSB is the *first* arg), as an int
     */
    private static int bytesAsInt(byte b4, byte b3, byte b2, byte b1) {
        return b1 << 24
                | (b2 & 0xFF) << 16
                | (b3 & 0xFF) << 8
                | (b4 & 0xFF);
    }

}
