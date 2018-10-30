package io.particle.android.sdk.cloud

import com.google.gson.annotations.SerializedName


data class CardOnFileResponse(
    val card: CardOnFile?
) {

    data class CardOnFile(

        val last4: String?,

        val brand: String?,

        @SerializedName("exp_month")
        val expiryMonthString: String?,

        @SerializedName("exp_year")
        val expiryYearString: String?,

        @SerializedName("id")
        val token: String?
    )

}
