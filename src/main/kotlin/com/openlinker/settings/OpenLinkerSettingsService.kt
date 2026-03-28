package com.openlinker.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.normalized

@Service(Service.Level.APP)
@State(
    name = "OpenLinkerSettings",
    storages = [Storage(value = "OpenLinkerSettings.xml", roamingType = RoamingType.DISABLED)],
)
class OpenLinkerSettingsService : PersistentStateComponent<OpenLinkerSettingsService.SettingsState> {
    private var state = SettingsState.default()

    override fun getState(): SettingsState = state

    override fun loadState(state: SettingsState) {
        this.state = SettingsState.fromRules(state.rules.map(RuleState::toRule))
    }

    fun getRules(): List<CustomUrlRule> = state.rules.map(RuleState::toRule).normalized()

    fun getEnabledRules(): List<CustomUrlRule> = getRules().filter { it.enabled }

    fun setRules(rules: List<CustomUrlRule>) {
        state = SettingsState.fromRules(rules)
    }

    class SettingsState {
        var rules: MutableList<RuleState> = mutableListOf()

        companion object {
            fun default(): SettingsState = fromRules(OpenLinkerDefaults.defaultRules())

            fun fromRules(rules: List<CustomUrlRule>): SettingsState = SettingsState().apply {
                this.rules = rules.normalized().map(::RuleState).toMutableList()
            }
        }
    }

    class RuleState() {
        var name: String = ""
        var urlTemplate: String = ""
        var enabled: Boolean = true

        constructor(rule: CustomUrlRule) : this() {
            name = rule.name
            urlTemplate = rule.urlTemplate
            enabled = rule.enabled
        }

        fun toRule(): CustomUrlRule = CustomUrlRule(
            name = name,
            urlTemplate = urlTemplate,
            enabled = enabled,
        )
    }

    companion object {
        fun getInstance(): OpenLinkerSettingsService =
            ApplicationManager.getApplication().getService(OpenLinkerSettingsService::class.java)
    }
}
