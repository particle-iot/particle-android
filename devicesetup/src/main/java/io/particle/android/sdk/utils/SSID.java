package io.particle.android.sdk.utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Locale;


/**
 * Simple value wrapper for SSID strings.  Eliminates case comparison issues and the quoting
 * nonsense introduced by {@link android.net.wifi.WifiConfiguration#SSID} (and potentially elsewhere)
 */
public class SSID implements Comparable<SSID>, Parcelable {

    public static SSID from(@NonNull String rawSsidString) {
        Preconditions.checkNotNull(rawSsidString);
        return new SSID(deQuotifySsid(rawSsidString));
    }

    public static SSID from(WifiInfo wifiInfo) {
        return from(wifiInfo.getSSID());
    }

    public static SSID from(WifiConfiguration wifiConfiguration) {
        return from(wifiConfiguration.SSID);
    }

    public static SSID from(ScanResult scanResult) {
        return SSID.from(scanResult.SSID);
    }


    private final String ssidString;

    private SSID(String ssidString) {
        this.ssidString = ssidString;
    }

    @Override
    public String toString() {
        return ssidString;
    }

    public String inQuotes() {
        return "\"" + ssidString + "\"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SSID ssid = (SSID) o;

        return ssidString.equalsIgnoreCase(ssid.ssidString);
    }

    @Override
    public int hashCode() {
        return ssidString.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public int compareTo(@NonNull SSID o) {
        return ssidString.compareToIgnoreCase(o.ssidString);
    }

    private static String deQuotifySsid(String SSID) {
        String quoteMark = "\"";
        SSID = ParticleDeviceSetupInternalStringUtils.removeStart(SSID, quoteMark);
        SSID = ParticleDeviceSetupInternalStringUtils.removeEnd(SSID, quoteMark);
        return SSID;
    }


    //region Parcelable noise
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ssidString);
    }

    public static final Creator<SSID> CREATOR = new Creator<SSID>() {
        @Override
        public SSID createFromParcel(Parcel in) {
            return new SSID(in.readString());
        }

        @Override
        public SSID[] newArray(int size) {
            return new SSID[size];
        }
    };
    //endregion
}
