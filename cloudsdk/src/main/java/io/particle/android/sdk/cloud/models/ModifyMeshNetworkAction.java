package io.particle.android.sdk.cloud.models;

import com.google.gson.annotations.SerializedName;


public enum ModifyMeshNetworkAction {
    @SerializedName("add-device")
    ADD_DEVICE,
    @SerializedName("remove-device")
    REMOVE_DEVICE,
    @SerializedName("gateway-enable")
    GATEWAY_ENABLE,
    @SerializedName("gateway-disable")
    GATEWAY_DISABLE
}
