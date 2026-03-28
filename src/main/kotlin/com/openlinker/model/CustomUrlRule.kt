package com.openlinker.model

data class CustomUrlRule(
    val name: String = "",
    val urlTemplate: String = "",
    val enabled: Boolean = true,
) {
    override fun toString(): String = name.ifBlank { urlTemplate }
}

fun CustomUrlRule.normalized(): CustomUrlRule = copy(
    name = name.trim(),
    urlTemplate = urlTemplate.trim(),
)

fun List<CustomUrlRule>.normalized(): List<CustomUrlRule> = map(CustomUrlRule::normalized)
