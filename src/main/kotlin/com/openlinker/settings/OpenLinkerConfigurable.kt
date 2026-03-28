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
            it.reset(OpenLinkerSettingsService.getInstance().getRules())
            settingsPanel = it
        }
        return panel.component
    }

    override fun getPreferredFocusedComponent(): JComponent? = settingsPanel?.preferredFocusedComponent

    override fun isModified(): Boolean {
        val panel = settingsPanel ?: return false
        return panel.isModified(OpenLinkerSettingsService.getInstance().getRules())
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val panel = settingsPanel ?: return
        OpenLinkerSettingsService.getInstance().setRules(panel.getValidatedRules())
        ApplicationManager.getApplication().saveSettings()
    }

    override fun reset() {
        settingsPanel?.reset(OpenLinkerSettingsService.getInstance().getRules())
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
