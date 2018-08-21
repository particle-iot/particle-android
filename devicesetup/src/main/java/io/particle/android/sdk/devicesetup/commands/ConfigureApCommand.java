package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.annotations.SerializedName;

import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;

import static io.particle.android.sdk.utils.Py.all;
import static io.particle.android.sdk.utils.Py.truthy;


/**
 * Configure the access point details to connect to when connect-ap is called. The AP doesn't have
 * to be in the list from scan-ap, allowing manual entry of hidden networks.
 */
public class ConfigureApCommand extends Command {

    public final Integer idx;

    public final String ssid;

    @SerializedName("pwd")
    public final String encryptedPasswordHex;

    @SerializedName("sec")
    public final Integer wifiSecurityType;

    @SerializedName("ch")
    public final Integer channel;

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String getCommandName() {
        return "configure-ap";
    }

    // private constructor -- use .newBuilder() instead.
    private ConfigureApCommand(int idx, String ssid, String encryptedPasswordHex,
                               WifiSecurity wifiSecurityType, int channel) {
        this.idx = idx;
        this.ssid = ssid;
        this.encryptedPasswordHex = encryptedPasswordHex;
        this.wifiSecurityType = wifiSecurityType.asInt();
        this.channel = channel;
    }


    public static class Response {

        @SerializedName("r")
        public final Integer responseCode;  // 0 == OK, non-zero == problem with index/data

        public Response(Integer responseCode) {
            this.responseCode = responseCode;
        }

        // FIXME: do this for the other ones with just the "responseCode" field
        public boolean isOk() {
            return responseCode == 0;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "responseCode=" + responseCode +
                    '}';
        }
    }


    public static class Builder {
        private Integer idx;
        private String ssid;
        private String encryptedPasswordHex;
        private WifiSecurity securityType;
        private Integer channel;

        public Builder setIdx(int idx) {
            this.idx = idx;
            return this;
        }

        public Builder setSsid(String ssid) {
            this.ssid = ssid;
            return this;
        }

        public Builder setEncryptedPasswordHex(String encryptedPasswordHex) {
            this.encryptedPasswordHex = encryptedPasswordHex;
            return this;
        }

        public Builder setSecurityType(WifiSecurity securityType) {
            this.securityType = securityType;
            return this;
        }

        public Builder setChannel(int channel) {
            this.channel = channel;
            return this;
        }

        public ConfigureApCommand build() {
            if (!all(ssid, securityType)
                    || (truthy(encryptedPasswordHex) && securityType == WifiSecurity.OPEN)) {
                throw new IllegalArgumentException(
                        "One or more required arguments was not set on ConfigureApCommand");
            }
            if (idx == null) {
                idx = 0;
            }
            return new ConfigureApCommand(idx, ssid, encryptedPasswordHex, securityType, channel);
        }
    }

}
