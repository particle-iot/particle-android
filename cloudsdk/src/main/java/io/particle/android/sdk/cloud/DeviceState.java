package io.particle.android.sdk.cloud;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ParticleDevice.VariableType;
import io.particle.android.sdk.utils.Parcelables;


// FIXME: I'm about ready to give up on trying to make this actually immutable.  Bah.
// Instead, make an IDeviceState or something, which is an immutable interface, and then have a
// MutableDeviceState class which will also have setters, and only expose the mutable concrete
// class to whatever class ends up doing the device state management; *everything* else only ever
// gets to see IDeviceState objects.  (This might interfere with using Parcelable though.)
@ParametersAreNonnullByDefault
class DeviceState implements Parcelable {

    final String deviceId;
    @Nullable final Integer platformId;
    @Nullable final Integer productId;
    @Nullable final String ipAddress;
    @Nullable final String lastAppName;
    @Nullable final String status;
    @Nullable final String name;
    @Nullable final Boolean isConnected;
    @Nullable final Boolean cellular;
    @Nullable final String imei;
    @Nullable final String lastIccid;
    @Nullable final String currentBuild;
    @Nullable final String defaultBuild;
    final Set<String> functions;
    final Map<String, VariableType> variables;
    @Nullable final ParticleDevice.ParticleDeviceType deviceType;
    @Nullable final Date lastHeard;
    @Nullable final String serialNumber;
    @Nullable final String mobileSecret;
    @Nullable final String iccid;
    @Nullable final String systemFirmwareVersion;
    @Nullable final String notes;

    DeviceState(DeviceStateBuilder deviceStateBuilder) {
        this.deviceId = deviceStateBuilder.deviceId;
        this.name = deviceStateBuilder.name;
        this.isConnected = deviceStateBuilder.isConnected;
        this.cellular = deviceStateBuilder.cellular;
        this.imei = deviceStateBuilder.imei;
        this.lastIccid = deviceStateBuilder.lastIccid;
        this.currentBuild = deviceStateBuilder.currentBuild;
        this.defaultBuild = deviceStateBuilder.defaultBuild;
        this.functions = deviceStateBuilder.functions;
        this.variables = deviceStateBuilder.variables;
        this.deviceType = deviceStateBuilder.deviceType;
        this.platformId = deviceStateBuilder.platformId;
        this.productId = deviceStateBuilder.productId;
        this.ipAddress = deviceStateBuilder.ipAddress;
        this.lastAppName = deviceStateBuilder.lastAppName;
        this.status = deviceStateBuilder.status;
        this.lastHeard = deviceStateBuilder.lastHeard;
        this.serialNumber = deviceStateBuilder.serialNumber;
        this.mobileSecret = deviceStateBuilder.mobileSecret;
        this.iccid = deviceStateBuilder.iccid;
        this.systemFirmwareVersion = deviceStateBuilder.systemFirmwareVersion;
        this.notes = deviceStateBuilder.notes;
    }

    //region ImmutabilityPhun
    // The following static builder methods are awkward and a little absurd, but they still seem
    // better than the alternatives.  If we have to add another couple mutable fields though, it
    // might be time to reconsider this...
    static DeviceState withNewName(DeviceState other, String newName) {
        return new DeviceStateBuilder(other.deviceId, other.functions, other.variables)
                .name(newName)
                .cellular(other.cellular)
                .connected(other.isConnected)
                .deviceType(other.deviceType)
                .platformId(other.platformId)
                .productId(other.productId)
                .imei(other.imei)
                .lastIccid(other.lastIccid)
                .currentBuild(other.currentBuild)
                .defaultBuild(other.defaultBuild)
                .ipAddress(other.ipAddress)
                .lastAppName(other.lastAppName)
                .status(other.status)
                .lastHeard(other.lastHeard)
                .serialNumber(other.serialNumber)
                .mobileSecret(other.mobileSecret)
                .iccid(other.iccid)
                .systemFirmwareVersion(other.systemFirmwareVersion)
                .notes(other.notes)
                .build();
    }


    static DeviceState withNewConnectedState(DeviceState other, boolean newConnectedState) {
        return new DeviceStateBuilder(other.deviceId, other.functions, other.variables)
                .name(other.name)
                .cellular(other.cellular)
                .connected(newConnectedState)
                .deviceType(other.deviceType)
                .platformId(other.platformId)
                .productId(other.productId)
                .imei(other.imei)
                .lastIccid(other.lastIccid)
                .currentBuild(other.currentBuild)
                .defaultBuild(other.defaultBuild)
                .ipAddress(other.ipAddress)
                .lastAppName(other.lastAppName)
                .status(other.status)
                .lastHeard(other.lastHeard)
                .serialNumber(other.serialNumber)
                .mobileSecret(other.mobileSecret)
                .iccid(other.iccid)
                .systemFirmwareVersion(other.systemFirmwareVersion)
                .notes(other.notes)
                .build();
    }
    //endregion

    //region Parcelable
    private DeviceState(Parcel in) {
        deviceId = in.readString();
        name = (String) in.readValue(String.class.getClassLoader());
        isConnected = (Boolean) in.readValue(Boolean.class.getClassLoader());
        functions = new HashSet<>(Parcelables.readStringList(in));
        variables = Parcelables.readSerializableMap(in);
        deviceType = ParticleDevice.ParticleDeviceType.valueOf((String) in.readValue(String.class.getClassLoader()));
        platformId = (Integer) in.readValue(Integer.class.getClassLoader());
        productId = (Integer) in.readValue(Integer.class.getClassLoader());
        cellular = (Boolean) in.readValue(Boolean.class.getClassLoader());
        imei = (String) in.readValue(String.class.getClassLoader());
        lastIccid = (String) in.readValue(String.class.getClassLoader());
        currentBuild = (String) in.readValue(String.class.getClassLoader());
        defaultBuild = (String) in.readValue(String.class.getClassLoader());
        ipAddress = (String) in.readValue(String.class.getClassLoader());
        lastAppName = (String) in.readValue(String.class.getClassLoader());
        status = (String) in.readValue(String.class.getClassLoader());
        lastHeard = new Date((Long) in.readValue(Long.class.getClassLoader()));
        serialNumber = (String) in.readValue(String.class.getClassLoader());
        mobileSecret = (String) in.readValue(String.class.getClassLoader());
        iccid = (String) in.readValue(String.class.getClassLoader());
        systemFirmwareVersion = (String) in.readValue(String.class.getClassLoader());
        notes = (String) in.readValue(String.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceId);
        dest.writeValue(name);
        dest.writeValue(isConnected);
        dest.writeStringList(new ArrayList<>(functions));
        Parcelables.writeSerializableMap(dest, variables);
        dest.writeValue(deviceType != null ? deviceType.name() : null);
        dest.writeValue(platformId);
        dest.writeValue(productId);
        dest.writeValue(cellular);
        dest.writeValue(imei);
        dest.writeValue(lastIccid);
        dest.writeValue(currentBuild);
        dest.writeValue(defaultBuild);
        dest.writeValue(ipAddress);
        dest.writeValue(lastAppName);
        dest.writeValue(status);
        dest.writeValue(lastHeard != null ? lastHeard.getTime() : 0);
        dest.writeValue(serialNumber);
        dest.writeValue(mobileSecret);
        dest.writeValue(iccid);
        dest.writeValue(systemFirmwareVersion);
        dest.writeValue(notes);
    }

    public static final Creator<DeviceState> CREATOR = new Creator<DeviceState>() {
        @Override
        public DeviceState createFromParcel(Parcel in) {
            return new DeviceState(in);
        }

        @Override
        public DeviceState[] newArray(int size) {
            return new DeviceState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceState that = (DeviceState) o;

        if (!deviceId.equals(that.deviceId)) return false;
        if (platformId != null ? !platformId.equals(that.platformId) : that.platformId != null)
            return false;
        if (productId != null ? !productId.equals(that.productId) : that.productId != null)
            return false;
        if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null)
            return false;
        if (lastAppName != null ? !lastAppName.equals(that.lastAppName) : that.lastAppName != null)
            return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (isConnected != null ? !isConnected.equals(that.isConnected) : that.isConnected != null)
            return false;
        if (cellular != null ? !cellular.equals(that.cellular) : that.cellular != null)
            return false;
        if (imei != null ? !imei.equals(that.imei) : that.imei != null) return false;
        if (lastIccid != null ? !lastIccid.equals(that.lastIccid) : that.lastIccid != null)
            return false;
        if (currentBuild != null ? !currentBuild.equals(that.currentBuild) : that.currentBuild != null)
            return false;
        if (defaultBuild != null ? !defaultBuild.equals(that.defaultBuild) : that.defaultBuild != null)
            return false;
        if (!functions.equals(that.functions)) return false;
        if (!variables.equals(that.variables)) return false;
        if (deviceType != that.deviceType) return false;
        if (lastHeard != null ? !lastHeard.equals(that.lastHeard) : that.lastHeard != null)
            return false;
        if (serialNumber != null ? !serialNumber.equals(that.serialNumber) : that.serialNumber != null)
            return false;
        if (mobileSecret != null ? !mobileSecret.equals(that.mobileSecret) : that.mobileSecret != null)
            return false;
        if (iccid != null ? !iccid.equals(that.iccid) : that.iccid != null) return false;
        if (systemFirmwareVersion != null ? !systemFirmwareVersion.equals(that.systemFirmwareVersion) : that.systemFirmwareVersion != null)
            return false;
        return notes != null ? notes.equals(that.notes) : that.notes == null;
    }

    @Override
    public int hashCode() {
        int result = deviceId.hashCode();
        result = 31 * result + (platformId != null ? platformId.hashCode() : 0);
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (lastAppName != null ? lastAppName.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isConnected != null ? isConnected.hashCode() : 0);
        result = 31 * result + (cellular != null ? cellular.hashCode() : 0);
        result = 31 * result + (imei != null ? imei.hashCode() : 0);
        result = 31 * result + (lastIccid != null ? lastIccid.hashCode() : 0);
        result = 31 * result + (currentBuild != null ? currentBuild.hashCode() : 0);
        result = 31 * result + (defaultBuild != null ? defaultBuild.hashCode() : 0);
        result = 31 * result + functions.hashCode();
        result = 31 * result + variables.hashCode();
        result = 31 * result + (deviceType != null ? deviceType.hashCode() : 0);
        result = 31 * result + (lastHeard != null ? lastHeard.hashCode() : 0);
        result = 31 * result + (serialNumber != null ? serialNumber.hashCode() : 0);
        result = 31 * result + (mobileSecret != null ? mobileSecret.hashCode() : 0);
        result = 31 * result + (iccid != null ? iccid.hashCode() : 0);
        result = 31 * result + (systemFirmwareVersion != null ? systemFirmwareVersion.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }
    //endregion

    public static class DeviceStateBuilder {
        private final String deviceId;
        @Nullable private Integer platformId;
        @Nullable private Integer productId;
        @Nullable private String ipAddress;
        @Nullable private String lastAppName;
        @Nullable private String status;
        @Nullable private String name;
        @Nullable private Boolean isConnected;
        @Nullable private Boolean cellular;
        @Nullable private String imei;
        @Nullable private String lastIccid;
        @Nullable private String currentBuild;
        @Nullable private String defaultBuild;
        private final Set<String> functions;
        private final Map<String, ParticleDevice.VariableType> variables;
        @Nullable ParticleDevice.ParticleDeviceType deviceType;
        @Nullable Date lastHeard;
        @Nullable String serialNumber;
        @Nullable String mobileSecret;
        @Nullable String iccid;
        @Nullable String systemFirmwareVersion;
        @Nullable String notes;

        DeviceStateBuilder(String deviceId, Set<String> functions,
                           Map<String, ParticleDevice.VariableType> variables) {
            this.deviceId = deviceId;
            this.functions = functions;
            this.variables = variables;
        }

        public DeviceStateBuilder platformId(@Nullable Integer platformId) {
            this.platformId = platformId;
            return this;
        }

        public DeviceStateBuilder productId(@Nullable Integer productId) {
            this.productId = productId;
            return this;
        }

        public DeviceStateBuilder ipAddress(@Nullable String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public DeviceStateBuilder lastAppName(@Nullable String lastAppName) {
            this.lastAppName = lastAppName;
            return this;
        }

        public DeviceStateBuilder status(@Nullable String status) {
            this.status = status;
            return this;
        }

        public DeviceStateBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public DeviceStateBuilder connected(@Nullable Boolean connected) {
            isConnected = connected;
            return this;
        }

        public DeviceStateBuilder cellular(@Nullable Boolean cellular) {
            this.cellular = cellular;
            return this;
        }

        public DeviceStateBuilder imei(@Nullable String imei) {
            this.imei = imei;
            return this;
        }

        public DeviceStateBuilder lastIccid(@Nullable String lastIccid) {
            this.lastIccid = lastIccid;
            return this;
        }

        public DeviceStateBuilder currentBuild(@Nullable String currentBuild) {
            this.currentBuild = currentBuild;
            return this;
        }

        public DeviceStateBuilder defaultBuild(@Nullable String defaultBuild) {
            this.defaultBuild = defaultBuild;
            return this;
        }

        public DeviceStateBuilder deviceType(@Nullable ParticleDevice.ParticleDeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public DeviceStateBuilder lastHeard(@Nullable Date lastHeard) {
            this.lastHeard = lastHeard;
            return this;
        }

        public DeviceStateBuilder serialNumber(@Nullable String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public DeviceStateBuilder mobileSecret(@Nullable String mobileSecret) {
            this.mobileSecret = mobileSecret;
            return this;
        }

        public DeviceStateBuilder iccid(@Nullable String iccid) {
            this.iccid = iccid;
            return this;
        }

        public DeviceStateBuilder systemFirmwareVersion(@Nullable String systemFirmwareVersion) {
            this.systemFirmwareVersion = systemFirmwareVersion;
            return this;
        }

        public DeviceStateBuilder notes(@Nullable String notes) {
            this.notes = notes;
            return this;
        }

        public DeviceState build() {
            return new DeviceState(this);
        }
    }

}
