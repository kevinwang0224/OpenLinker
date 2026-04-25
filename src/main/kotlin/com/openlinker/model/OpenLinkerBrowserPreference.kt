package com.openlinker.model

data class OpenLinkerBrowserPreference(
    val browserId: String = "",
    val browserName: String = "",
) {
    val isDefault: Boolean
        get() = browserId.isBlank()
}

fun OpenLinkerBrowserPreference.normalized(): OpenLinkerBrowserPreference {
    val normalizedBrowserId = browserId.trim()
    return copy(
        browserId = normalizedBrowserId,
        browserName = if (normalizedBrowserId.isBlank()) "" else browserName.trim(),
    )
}
