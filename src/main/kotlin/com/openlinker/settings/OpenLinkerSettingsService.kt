package com.openlinker.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.OpenLinkerBrowserPreference
import com.openlinker.model.ProjectUrlOverride
import com.openlinker.model.normalized
import com.openlinker.model.normalizedProjectOverrides

@Service(Service.Level.APP)
@State(
    name = "OpenLinkerSettings",
    storages = [Storage(value = "OpenLinkerSettings.xml", roamingType = RoamingType.DISABLED)],
)
class OpenLinkerSettingsService : PersistentStateComponent<OpenLinkerSettingsService.SettingsState> {
    private var state = SettingsState.default()

    override fun getState(): SettingsState = state

    override fun loadState(state: SettingsState) {
        this.state = SettingsState.fromSettings(
            OpenLinkerBrowserPreference(
                browserId = state.globalBrowserId,
                browserName = state.globalBrowserName,
            ),
            state.rules.map(RuleState::toRule),
        )
    }

    fun getGlobalBrowserPreference(): OpenLinkerBrowserPreference = OpenLinkerBrowserPreference(
        browserId = state.globalBrowserId,
        browserName = state.globalBrowserName,
    ).normalized()

    fun getRules(): List<CustomUrlRule> = state.rules.map(RuleState::toRule).normalized()

    fun getEnabledRules(): List<CustomUrlRule> = getRules().filter { it.enabled }

    fun setRules(rules: List<CustomUrlRule>) {
        state = SettingsState.fromSettings(getGlobalBrowserPreference(), rules)
    }

    fun setSettings(globalBrowserPreference: OpenLinkerBrowserPreference, rules: List<CustomUrlRule>) {
        state = SettingsState.fromSettings(globalBrowserPreference, rules)
    }

    class SettingsState {
        var globalBrowserId: String = ""
        var globalBrowserName: String = ""
        var rules: MutableList<RuleState> = mutableListOf()

        companion object {
            fun default(): SettingsState = fromRules(OpenLinkerDefaults.defaultRules())

            fun fromRules(rules: List<CustomUrlRule>): SettingsState = fromSettings(
                OpenLinkerBrowserPreference(),
                rules,
            )

            fun fromSettings(
                globalBrowserPreference: OpenLinkerBrowserPreference,
                rules: List<CustomUrlRule>,
            ): SettingsState = SettingsState().apply {
                val normalizedGlobalBrowserPreference = globalBrowserPreference.normalized()
                globalBrowserId = normalizedGlobalBrowserPreference.browserId
                globalBrowserName = normalizedGlobalBrowserPreference.browserName
                this.rules = rules.normalized().map(::RuleState).toMutableList()
            }
        }
    }

    class RuleState() {
        var name: String = ""
        var urlTemplate: String = ""
        var enabled: Boolean = true
        var browserId: String = ""
        var browserName: String = ""
        var projectOverrides: MutableList<ProjectOverrideState> = mutableListOf()

        constructor(rule: CustomUrlRule) : this() {
            name = rule.name
            urlTemplate = rule.urlTemplate
            enabled = rule.enabled
            browserId = rule.browserId
            browserName = rule.browserName
            projectOverrides = rule.projectOverrides.normalizedProjectOverrides().map(::ProjectOverrideState).toMutableList()
        }

        fun toRule(): CustomUrlRule = CustomUrlRule(
            name = name,
            urlTemplate = urlTemplate,
            enabled = enabled,
            browserId = browserId,
            browserName = browserName,
            projectOverrides = projectOverrides.map(ProjectOverrideState::toProjectOverride),
        )
    }

    class ProjectOverrideState() {
        var projectName: String = ""
        var urlTemplate: String = ""

        constructor(projectOverride: ProjectUrlOverride) : this() {
            projectName = projectOverride.projectName
            urlTemplate = projectOverride.urlTemplate
        }

        fun toProjectOverride(): ProjectUrlOverride = ProjectUrlOverride(
            projectName = projectName,
            urlTemplate = urlTemplate,
        )
    }

    companion object {
        fun getInstance(): OpenLinkerSettingsService =
            ApplicationManager.getApplication().getService(OpenLinkerSettingsService::class.java)
    }
}
