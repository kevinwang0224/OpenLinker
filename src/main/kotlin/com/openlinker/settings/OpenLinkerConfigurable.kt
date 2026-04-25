package com.openlinker.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.openlinker.OpenLinkerConstants
import javax.swing.JComponent

class OpenLinkerConfigurable : SearchableConfigurable, Configurable.NoScroll {
    private var settingsPanel: OpenLinkerSettingsPanel? = null

    override fun getId(): String = OpenLinkerConstants.SETTINGS_ID

    override fun getDisplayName(): String = OpenLinkerConstants.PLUGIN_NAME

    override fun createComponent(): JComponent {
        val panel = settingsPanel ?: OpenLinkerSettingsPanel().also {
            val service = OpenLinkerSettingsService.getInstance()
            it.reset(service.getGlobalBrowserPreference(), service.getRules())
            settingsPanel = it
        }
        return panel.component
    }

    override fun getPreferredFocusedComponent(): JComponent? = settingsPanel?.preferredFocusedComponent

    override fun isModified(): Boolean {
        val panel = settingsPanel ?: return false
        val service = OpenLinkerSettingsService.getInstance()
        return panel.isModified(service.getGlobalBrowserPreference(), service.getRules())
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val panel = settingsPanel ?: return
        OpenLinkerSettingsService.getInstance().setSettings(
            panel.getGlobalBrowserPreference(),
            panel.getValidatedRules(),
        )
        ApplicationManager.getApplication().saveSettings()
    }

    override fun reset() {
        val service = OpenLinkerSettingsService.getInstance()
        settingsPanel?.reset(service.getGlobalBrowserPreference(), service.getRules())
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
