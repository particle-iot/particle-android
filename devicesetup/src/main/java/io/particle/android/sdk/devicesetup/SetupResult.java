package io.particle.android.sdk.devicesetup;

import android.os.Parcel;
import android.os.Parcelable;

public class SetupResult implements Parcelable {
    private final boolean wasSuccessful;
    private final String configuredDeviceId;

    public SetupResult(boolean wasSuccessful, String configuredDeviceId) {
        this.wasSuccessful = wasSuccessful;
        this.configuredDeviceId = configuredDeviceId;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public String getConfiguredDeviceId() {
        return configuredDeviceId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(wasSuccessful ? 1 : 0);
        dest.writeString(configuredDeviceId);
    }

    public static final Parcelable.Creator<SetupResult> CREATOR = new Parcelable.Creator<SetupResult>() {
        @Override
        public SetupResult createFromParcel(Parcel source) {
            return new SetupResult(source);
        }

        @Override
        public SetupResult[] newArray(int size) {
            return new SetupResult[size];
        }
    };

    private SetupResult(Parcel source) {
        wasSuccessful = source.readInt() == 1;
        configuredDeviceId = source.readString();
    }
}
