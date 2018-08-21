package io.particle.android.sdk.devicesetup.model;

import android.net.wifi.ScanResult;

import java.util.Set;

import io.particle.android.sdk.utils.SSID;

import static io.particle.android.sdk.utils.Py.set;


// FIXME: this naming... is not ideal.
public class ScanResultNetwork implements WifiNetwork {

    private static final Set<String> wifiSecurityTypes = set("WEP", "PSK", "EAP");


    private final ScanResult scanResult;
    private final SSID ssid;

    public ScanResultNetwork(ScanResult scanResult) {
        this.scanResult = scanResult;
        ssid = SSID.from(scanResult.SSID);
    }

    @Override
    public SSID getSsid() {
        return ssid;
    }

    @Override
    public boolean isSecured() {
        // <sad trombone>
        // this seems like a bad joke of an "API", but this is basically what
        // Android does internally (see: http://goo.gl/GCRIKi)
        for (String securityType : wifiSecurityTypes) {
            if (scanResult.capabilities.contains(securityType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScanResultNetwork that = (ScanResultNetwork) o;

        return getSsid() != null ? getSsid().equals(that.getSsid()) : that.getSsid() == null;
    }

    @Override
    public int hashCode() {
        return getSsid() != null ? getSsid().hashCode() : 0;
    }
}
