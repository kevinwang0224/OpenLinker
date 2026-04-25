package com.openlinker.model

data class CustomUrlRule(
    val name: String = "",
    val urlTemplate: String = "",
    val enabled: Boolean = true,
    val browserId: String = "",
    val browserName: String = "",
) {
    val browserPreference: OpenLinkerBrowserPreference
        get() = OpenLinkerBrowserPreference(browserId = browserId, browserName = browserName).normalized()

    override fun toString(): String = name.ifBlank { urlTemplate }
}

fun CustomUrlRule.normalized(): CustomUrlRule {
    val normalizedBrowserPreference = browserPreference
    return copy(
        name = name.trim(),
        urlTemplate = urlTemplate.trim(),
        browserId = normalizedBrowserPreference.browserId,
        browserName = normalizedBrowserPreference.browserName,
    )
}

fun List<CustomUrlRule>.normalized(): List<CustomUrlRule> = map(CustomUrlRule::normalized)
