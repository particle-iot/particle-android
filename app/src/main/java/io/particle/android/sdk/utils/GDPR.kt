package io.particle.android.sdk.utils

import android.content.res.Resources


fun isUserCoveredByGDPR(): Boolean {
    val locale = Resources.getSystem().configuration.locale
    return gdprCountryCodes.contains(locale.country)
}


private val gdprCountryCodes: Set<String> = setOf(
    "AT",
    "BE",
    "BG",
    "CY",
    "CZ",
    "DE",
    "DK",
    "EE",
    "EL",
    "ES",
    "FI",
    "FR",
    "HR",
    "HU",
    "IE",
    "IT",
    "LT",
    "LU",
    "LV",
    "MT",
    "NL",
    "PL",
    "PT",
    "RO",
    "SE",
    "SI",
    "SK",
    "UK",
    // plus 3 non-EU countries using GDPR
    "IS",
    "LI",
    "NO"
)
