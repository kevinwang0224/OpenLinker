package com.openlinker.model

data class ProjectUrlOverride(
    val projectName: String = "",
    val urlTemplate: String = "",
)

data class CustomUrlRule(
    val name: String = "",
    val urlTemplate: String = "",
    val enabled: Boolean = true,
    val browserId: String = "",
    val browserName: String = "",
    val projectOverrides: List<ProjectUrlOverride> = emptyList(),
) {
    val browserPreference: OpenLinkerBrowserPreference
        get() = OpenLinkerBrowserPreference(browserId = browserId, browserName = browserName).normalized()

    fun urlTemplateForProject(projectName: String): String {
        val normalizedProjectName = projectName.trim()
        if (normalizedProjectName.isEmpty()) {
            return urlTemplate
        }

        return projectOverrides.normalizedProjectOverrides()
            .firstOrNull { it.projectName == normalizedProjectName }
            ?.urlTemplate
            ?: urlTemplate
    }

    override fun toString(): String = name.ifBlank { urlTemplate }
}

fun CustomUrlRule.normalized(): CustomUrlRule {
    val normalizedBrowserPreference = browserPreference
    return copy(
        name = name.trim(),
        urlTemplate = urlTemplate.trim(),
        browserId = normalizedBrowserPreference.browserId,
        browserName = normalizedBrowserPreference.browserName,
        projectOverrides = projectOverrides.normalizedProjectOverrides(),
    )
}

fun List<CustomUrlRule>.normalized(): List<CustomUrlRule> = map(CustomUrlRule::normalized)

fun ProjectUrlOverride.normalized(): ProjectUrlOverride = copy(
    projectName = projectName.trim(),
    urlTemplate = urlTemplate.trim(),
)

fun List<ProjectUrlOverride>.normalizedProjectOverrides(): List<ProjectUrlOverride> {
    val overridesByProject = linkedMapOf<String, ProjectUrlOverride>()
    forEach { override ->
        val normalizedOverride = override.normalized()
        if (normalizedOverride.projectName.isBlank() || normalizedOverride.urlTemplate.isBlank()) {
            return@forEach
        }

        overridesByProject.remove(normalizedOverride.projectName)
        overridesByProject[normalizedOverride.projectName] = normalizedOverride
    }
    return overridesByProject.values.toList()
}
