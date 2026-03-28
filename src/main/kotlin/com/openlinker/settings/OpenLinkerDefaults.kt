package com.openlinker.settings

import com.openlinker.model.CustomUrlRule

object OpenLinkerDefaults {
    fun defaultRules(): List<CustomUrlRule> = listOf(
        CustomUrlRule(
            name = "Google",
            urlTemplate = "https://www.google.com/search?q=${'$'}{PROJECT_NAME}",
            enabled = true,
        ),
        CustomUrlRule(
            name = "File",
            urlTemplate = "file://${'$'}{FILE_PATH}",
            enabled = true,
        ),
    )
}
