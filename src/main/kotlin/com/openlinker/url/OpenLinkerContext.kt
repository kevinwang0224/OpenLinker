package com.openlinker.url

data class OpenLinkerContext(
    val projectName: String = "",
    val moduleName: String = "",
    val fileName: String = "",
    val filePath: String = "",
) {
    fun valueFor(variableName: String): String = when (variableName) {
        "PROJECT_NAME" -> projectName
        "MODULE_NAME" -> moduleName
        "FILE_NAME" -> fileName
        "FILE_PATH" -> filePath
        else -> ""
    }
}
