package com.openlinker.settings

import com.openlinker.model.CustomUrlRule

object OpenLinkerDefaults {
    fun defaultRules(): List<CustomUrlRule> = listOf(
        CustomUrlRule(
            name = "GitHub(Example)",
            urlTemplate = "https://github.com/your_username/${'$'}{PROJECT_NAME}",
            enabled = false,
        ),
        CustomUrlRule(
            name = "File",
            urlTemplate = "file://${'$'}{FILE_PATH}",
            enabled = true,
        ),
    )
}
