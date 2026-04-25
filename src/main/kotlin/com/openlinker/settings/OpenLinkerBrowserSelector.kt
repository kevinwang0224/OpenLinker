package com.openlinker.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ComboboxWithBrowseButton
import com.openlinker.browser.OpenLinkerBrowsers
import com.openlinker.model.OpenLinkerBrowserPreference
import com.openlinker.model.normalized
import java.awt.Component
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

internal class OpenLinkerBrowserSelector(private val defaultLabel: String) {
    private val comboBox = ComboBox<BrowserOption>()
    private val comboWithBrowseButton = ComboboxWithBrowseButton(comboBox)

    val component: JComponent
        get() = comboWithBrowseButton

    init {
        comboBox.renderer = BrowserOptionRenderer()
        reloadOptions(OpenLinkerBrowserPreference())
        comboWithBrowseButton.addActionListener {
            val selectedPreference = getPreference()
            if (ApplicationManager.getApplication() != null) {
                ShowSettingsUtil.getInstance().editConfigurable(comboWithBrowseButton, BrowserSettings())
            }
            reloadOptions(selectedPreference)
        }
    }

    fun getPreference(): OpenLinkerBrowserPreference {
        return (comboBox.selectedItem as? BrowserOption)?.preference ?: OpenLinkerBrowserPreference()
    }

    fun setPreference(preference: OpenLinkerBrowserPreference) {
        reloadOptions(preference.normalized())
    }

    private fun reloadOptions(selection: OpenLinkerBrowserPreference) {
        val normalizedSelection = selection.normalized()
        val options = mutableListOf(defaultOption())
        options.addAll(
            OpenLinkerBrowsers.activeBrowsers().map { browser ->
                BrowserOption(
                    preference = OpenLinkerBrowsers.preferenceFor(browser),
                    displayName = browser.name,
                    icon = browser.icon,
                )
            },
        )

        if (!normalizedSelection.isDefault && options.none { it.preference.browserId == normalizedSelection.browserId }) {
            options.add(
                BrowserOption(
                    preference = normalizedSelection,
                    displayName = "Missing: ${normalizedSelection.browserName.ifBlank { normalizedSelection.browserId }}",
                    icon = AllIcons.General.Web,
                ),
            )
        }

        comboBox.model = DefaultComboBoxModel(options.toTypedArray())
        comboBox.selectedItem = options.firstOrNull { it.preference.browserId == normalizedSelection.browserId }
            ?: options.first()
    }

    private fun defaultOption(): BrowserOption = BrowserOption(
        preference = OpenLinkerBrowserPreference(),
        displayName = defaultLabel,
        icon = AllIcons.General.Web,
    )

    private data class BrowserOption(
        val preference: OpenLinkerBrowserPreference,
        val displayName: String,
        val icon: Icon,
    )

    private class BrowserOptionRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            val option = value as? BrowserOption
            label.text = option?.displayName.orEmpty()
            label.icon = option?.icon
            return label
        }
    }
}
