package io.particle.android.sdk.utils

import io.particle.android.sdk.cloud.models.UserServiceAgreementsData
import io.particle.android.sdk.cloud.models.UserServiceAgreementsResponse



val UserServiceAgreementsResponse.developerAgreement: UserServiceAgreementsData?
    get() {
        return this.data.firstOrNull {
            it.type == "service_agreement" && it.attributes?.agreementType == "developer"
        }
    }


fun UserServiceAgreementsResponse.hasReachedDeviceLimit(): Boolean {
    return this.developerAgreement?.attributes?.currentUsageSummary?.deviceLimitReached == true
}


val UserServiceAgreementsResponse.maxDevices: Int?
    get() = this.developerAgreement?.attributes?.pricingTerms?.deviceTerms?.maxDevices
