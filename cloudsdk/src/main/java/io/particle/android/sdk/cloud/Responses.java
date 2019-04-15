package io.particle.android.sdk.cloud;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import io.particle.android.sdk.cloud.Responses.Models.CoreInfo;
import io.particle.android.sdk.cloud.models.ParticleNetworkData;

/**
 * All API responses, collected together in one outer class for simplicity's sake.
 */
public class Responses {

    /**
     * ...and to go with the responses, a series of model objects only
     * used internally when dealing with the REST API, never returned
     * outside of the cloudapi package.
     */
    public static class Models {


        public static class CoreInfo {

            @SerializedName("last_app")
            public final String lastApp;

            @SerializedName("last_heard")
            public final Date lastHeard;

            public final boolean connected;

            public final String deviceId;

            public CoreInfo(String lastApp, Date lastHeard, boolean connected, String deviceId) {
                this.lastApp = lastApp;
                this.lastHeard = lastHeard;
                this.connected = connected;
                this.deviceId = deviceId;
            }
        }


        /**
         * Represents a single Particle device in the list returned
         * by a call to "GET /v1/devices"
         */
        public static class SimpleDevice {

            public final String id;

            public final String name;

            public final boolean cellular;

            public final String imei;

            @SerializedName("last_iccid")
            public final String lastIccid;

            @SerializedName("current_build_target")
            public final String currentBuild;

            @SerializedName("default_build_target")
            public final String defaultBuild;

            @SerializedName("connected")
            public final boolean isConnected;

            @SerializedName("product_id")
            public final int productId;

            @SerializedName("platform_id")
            public final int platformId;

            @SerializedName("last_ip_address")
            public final String ipAddress;

            @SerializedName("status")
            public final String status;

            @SerializedName("last_heard")
            public final Date lastHeard;

            public SimpleDevice(String id, String name, boolean isConnected, boolean cellular,
                                String imei, String lastIccid, String currentBuild, String defaultBuild, int platformId,
                                int productId, String ipAddress, String status, Date lastHeard) {
                this.id = id;
                this.name = name;
                this.isConnected = isConnected;
                this.cellular = cellular;
                this.imei = imei;
                this.lastIccid = lastIccid;
                this.currentBuild = currentBuild;
                this.defaultBuild = defaultBuild;
                this.platformId = platformId;
                this.productId = productId;
                this.ipAddress = ipAddress;
                this.status = status;
                this.lastHeard = lastHeard;
            }
        }

        /**
         * Represents a single Particle device as returned from the
         * call to "GET /v1/devices/{device id}"
         */
        public class CompleteDevice {
            @SerializedName("id")
            public final String deviceId;

            public final String name;

            public final boolean cellular;

            public final String imei;

            @SerializedName("last_iccid")
            public final String lastIccid;

            @SerializedName("current_build_target")
            public final String currentBuild;

            @SerializedName("default_build_target")
            public final String defaultBuild;

            @SerializedName("connected")
            public final boolean isConnected;

            public final Map<String, String> variables;

            public final List<String> functions;

            @SerializedName("cc3000_patch_version")
            public final String version;

            @SerializedName("product_id")
            public final int productId;

            @SerializedName("platform_id")
            public final int platformId;

            @SerializedName("last_ip_address")
            public final String ipAddress;

            @SerializedName("last_app")
            public final String lastAppName;

            @SerializedName("status")
            public final String status;

            @SerializedName("device_needs_update")
            public final boolean requiresUpdate;

            @SerializedName("last_heard")
            public final Date lastHeard;

            @SerializedName("serial_number")
            public final String serialNumber;

            @SerializedName("mobile_secret")
            public final String mobileSecret;

            CompleteDevice(String deviceId, String name, boolean isConnected, boolean cellular,
                           String imei, String lastIccid, String currentBuild, String defaultBuild,
                           Map<String, String> variables, List<String> functions, String version,
                           int productId, int platformId, String ipAddress, String lastAppName,
                           String status, boolean requiresUpdate, Date lastHeard,
                           String serialNumber, String mobileSecret) {
                this.deviceId = deviceId;
                this.name = name;
                this.isConnected = isConnected;
                this.cellular = cellular;
                this.imei = imei;
                this.lastIccid = lastIccid;
                this.currentBuild = currentBuild;
                this.defaultBuild = defaultBuild;
                this.variables = variables;
                this.functions = functions;
                this.version = version;
                this.productId = productId;
                this.platformId = platformId;
                this.ipAddress = ipAddress;
                this.lastAppName = lastAppName;
                this.status = status;
                this.requiresUpdate = requiresUpdate;
                this.lastHeard = lastHeard;
                this.serialNumber = serialNumber;
                this.mobileSecret = mobileSecret;
            }
        }

    }


    public static class TokenResponse {

        public final String token;

        public TokenResponse(String token) {
            this.token = token;
        }
    }


    public static class CallFunctionResponse {

        @SerializedName("id")
        public final String deviceId;

        @SerializedName("name")
        public final String deviceName;

        public final boolean connected;

        @SerializedName("return_value")
        public final int returnValue;

        public CallFunctionResponse(String deviceId, String deviceName, boolean connected,
                                    int returnValue) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.connected = connected;
            this.returnValue = returnValue;
        }
    }


    public static class LogInResponse {

        @SerializedName("expires_in")
        public final long expiresInSeconds;

        @SerializedName("access_token")
        public final String accessToken;

        @SerializedName("refresh_token")
        public final String refreshToken;

        @SerializedName("token_type")
        public final String tokenType;

        public LogInResponse(long expiresInSeconds, String accessToken, String refreshToken, String tokenType) {
            this.expiresInSeconds = expiresInSeconds;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
        }
    }


    public static class SimpleResponse {

        public final boolean ok;
        public final String error;

        public SimpleResponse(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        @Override
        public String toString() {
            return "SimpleResponse [ok=" + ok + ", error=" + error + "]";
        }
    }


    public static class ClaimCodeResponse {

        @SerializedName("claim_code")
        public final String claimCode;

        @SerializedName("device_ids")
        public final String[] deviceIds;

        public ClaimCodeResponse(String claimCode, String[] deviceIds) {
            this.claimCode = claimCode;
            this.deviceIds = deviceIds;
        }
    }

    public abstract static class ReadVariableResponse<T> {

        @SerializedName("cmd")
        public final String commandName;

        @SerializedName("name")
        public final String variableName;

        public final T result;

        public final Models.CoreInfo coreInfo;

        public ReadVariableResponse(String commandName, String variableName,
                                    Models.CoreInfo coreInfo, T result) {
            this.commandName = commandName;
            this.variableName = variableName;
            this.result = result;
            this.coreInfo = coreInfo;
        }
    }


    public static class FirmwareUpdateInfoResponse {

        @SerializedName("binary_url")
        public final String nextFileUrl;

        public FirmwareUpdateInfoResponse(String nextFileUrl) {
            this.nextFileUrl = nextFileUrl;
        }
    }


    public static class ReadIntVariableResponse extends ReadVariableResponse<Integer> {

        public ReadIntVariableResponse(String commandName, String variableName, CoreInfo coreInfo,
                                       Integer result) {
            super(commandName, variableName, coreInfo, result);
        }
    }


    public static class ReadDoubleVariableResponse extends ReadVariableResponse<Double> {

        public ReadDoubleVariableResponse(String commandName, String variableName, CoreInfo coreInfo,
                                          Double result) {
            super(commandName, variableName, coreInfo, result);
        }
    }


    public static class ReadStringVariableResponse extends ReadVariableResponse<String> {

        public ReadStringVariableResponse(String commandName, String variableName, CoreInfo coreInfo,
                                          String result) {
            super(commandName, variableName, coreInfo, result);
        }
    }


    public static class ReadObjectVariableResponse extends ReadVariableResponse<Object> {

        public ReadObjectVariableResponse(String commandName, String variableName, CoreInfo coreInfo,
                                          Object result) {
            super(commandName, variableName, coreInfo, result);
        }
    }


    public abstract static class PagedDataResponse {

        public static class PagedMetadata {
            public final int totalRecords;
            public final int totalPages;

            public PagedMetadata(int totalRecords, int totalPages) {
                this.totalRecords = totalRecords;
                this.totalPages = totalPages;
            }
        }


        @NonNull
        public final PagedMetadata meta;

        protected PagedDataResponse(@NonNull PagedMetadata meta) {
            this.meta = meta;
        }

    }


    public static class MeshNetworksResponse extends PagedDataResponse {

        @NonNull
        public final List<ParticleNetworkData> networks;

        protected MeshNetworksResponse(
                @NonNull PagedMetadata meta,
                @NonNull List<ParticleNetworkData> networks) {
            super(meta);
            this.networks = networks;
        }
    }


    public static class MeshNetworkMembershipsResponse extends PagedDataResponse {

        public static class MeshMembership {

            @NonNull
            public final String id;
            public final boolean gateway;

            public MeshMembership(String id, boolean gateway) {
                this.id = id;
                this.gateway = gateway;
            }
        }


        public static class MeshNetworkDeviceMemberships {
            @NonNull
            public final String id;
            @NonNull
            @SerializedName("network")
            public final MeshMembership membership;

            public MeshNetworkDeviceMemberships(String id, MeshMembership membership) {
                this.id = id;
                this.membership = membership;
            }
        }

        @NonNull
        @SerializedName("devices")
        public final List<MeshNetworkDeviceMemberships> memberships;

        public MeshNetworkMembershipsResponse(
                @NonNull PagedMetadata meta,
                @NonNull List<MeshNetworkDeviceMemberships> memberships
        ) {
            super(meta);
            this.memberships = memberships;
        }
    }


    public static class MeshNetworkRegistrationResponse {

        public static class RegisteredNetwork {
            @NonNull
            public final String id;
            @NonNull
            public final String name;
            @NonNull
            public final ParticleNetworkState state;
            @NonNull
            public final ParticleNetworkType type;

            public RegisteredNetwork(String id, String name, ParticleNetworkState state,
                                     ParticleNetworkType type) {
                this.id = id;
                this.name = name;
                this.state = state;
                this.type = type;
            }
        }

        @NonNull
        public final RegisteredNetwork network;

        public MeshNetworkRegistrationResponse(@NonNull RegisteredNetwork network) {
            this.network = network;
        }

    }


}
