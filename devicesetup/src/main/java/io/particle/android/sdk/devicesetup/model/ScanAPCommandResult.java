package io.particle.android.sdk.devicesetup.model;

import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.utils.SSID;


// FIXME: this naming is not ideal.
public class ScanAPCommandResult implements WifiNetwork {

    public final ScanApCommand.Scan scan;
    public final SSID ssid;

    public ScanAPCommandResult(ScanApCommand.Scan scan) {
        this.scan = scan;
        ssid = SSID.from(scan.ssid);
    }

    @Override
    public SSID getSsid() {
        return ssid;
    }

    @Override
    public boolean isSecured() {
        return scan.wifiSecurityType != WifiSecurity.OPEN.asInt();
    }

    @Override
    public String toString() {
        return "ScanAPCommandResult{" +
                "scan=" + scan +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScanAPCommandResult that = (ScanAPCommandResult) o;

        return getSsid() != null ? getSsid().equals(that.getSsid()) : that.getSsid() == null;
    }

    @Override
    public int hashCode() {
        return getSsid() != null ? getSsid().hashCode() : 0;
    }

}
