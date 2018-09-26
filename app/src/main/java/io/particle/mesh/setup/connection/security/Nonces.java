package io.particle.mesh.setup.connection.security;

import androidx.annotation.NonNull;

import okio.Buffer;


public class Nonces {

    @NonNull
    public static Buffer writeRequestNonce(@NonNull Buffer buffer,
                                           int currentCount,
                                           @NonNull byte[] fixedSegment) {
        buffer.writeByte(currentCount & 0xff);
        buffer.writeByte((currentCount >> 8) & 0xff);
        buffer.writeByte((currentCount >> 16) & 0xff);
        buffer.writeByte((currentCount >> 24) & 0xff);

        buffer.write(fixedSegment);

        return buffer;
    }

    @NonNull
    public static Buffer writeReplyNonce(@NonNull Buffer buffer,
                                         int currentCount,
                                         @NonNull byte[] fixedSegment) {
        buffer.writeByte(currentCount & 0xff);
        buffer.writeByte((currentCount >> 8) & 0xff);
        buffer.writeByte((currentCount >> 16) & 0xff);
        buffer.writeByte(((currentCount >> 24) & 0xff) | 0x80); // Set most significant bit, per the docs

        buffer.write(fixedSegment);

        return buffer;
    }
}

