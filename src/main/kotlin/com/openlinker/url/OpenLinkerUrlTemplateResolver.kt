package com.openlinker.url

object OpenLinkerUrlTemplateResolver {
    private val variablePattern = Regex("\\$\\{([A-Z_]+)}")

    fun resolve(template: String, context: OpenLinkerContext): String {
        return variablePattern.replace(template) { matchResult ->
            context.valueFor(matchResult.groupValues[1])
        }
    }
}
