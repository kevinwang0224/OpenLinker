package com.openlinker.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.OpenLinkerBrowserPreference
import com.openlinker.model.ProjectUrlOverride
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

class OpenLinkerRuleDialog(
    initialRule: CustomUrlRule? = null,
    currentProjectName: String = "",
) : DialogWrapper(true) {
    private val currentProjectName = currentProjectName.trim()
    private val initialProjectOverrides = initialRule?.projectOverrides.orEmpty()
    private val existingProjectOverride = initialRule
        ?.projectOverrides
        ?.firstOrNull { it.projectName.trim() == currentProjectName }
    private val nameField = JBTextField(initialRule?.name.orEmpty())
    private val enabledCheckBox = JBCheckBox("Enabled", initialRule?.enabled ?: true)
    private val browserSelector = OpenLinkerBrowserSelector("Global default")
    private val urlTemplateArea = JBTextArea(initialRule?.urlTemplate.orEmpty(), 10, 0)
    private val projectOverrideCheckBox = JBCheckBox(
        "Use project override for $currentProjectName",
        existingProjectOverride != null,
    )
    private val projectOverrideArea = JBTextArea(existingProjectOverride?.urlTemplate.orEmpty(), 6, 0)
    private val projectOverridePanel = buildProjectOverridePanel()

    init {
        title = if (initialRule == null) "Add Rule" else "Edit Rule"
        browserSelector.setPreference(initialRule?.browserPreference ?: OpenLinkerBrowserPreference())
        initEditors()
        init()
        initValidation()
    }

    fun getRule(): CustomUrlRule {
        val browserPreference = browserSelector.getPreference()
        val projectOverrides = initialRuleProjectOverridesWithoutCurrentProject().toMutableList()
        if (currentProjectName.isNotBlank() && projectOverrideCheckBox.isSelected) {
            projectOverrides.add(
                ProjectUrlOverride(
                    projectName = currentProjectName,
                    urlTemplate = projectOverrideArea.text.trim(),
                ),
            )
        }

        return CustomUrlRule(
            name = nameField.text.trim(),
            urlTemplate = urlTemplateArea.text.trim(),
            enabled = enabledCheckBox.isSelected,
            browserId = browserPreference.browserId,
            browserName = browserPreference.browserName,
            projectOverrides = projectOverrides,
        )
    }

    override fun createCenterPanel(): JComponent {
        val editorScrollPane = JBScrollPane(urlTemplateArea).apply {
            preferredSize = Dimension(JBUI.scale(720), JBUI.scale(220))
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(borderColor()),
                JBUI.Borders.empty(0),
            )
        }

        val content = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 10, 4, 10)
            add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                add(enabledCheckBox)
            })
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(labeled("Rule name:", nameField))
            add(Box.createVerticalStrut(JBUI.scale(14)))
            add(labeled("Browser:", browserSelector.component))
            add(Box.createVerticalStrut(JBUI.scale(14)))
            add(buildEditorHeader())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(editorScrollPane)
            if (currentProjectName.isNotBlank()) {
                add(Box.createVerticalStrut(JBUI.scale(12)))
                add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                    isOpaque = false
                    add(projectOverrideCheckBox)
                })
                add(Box.createVerticalStrut(JBUI.scale(8)))
                add(projectOverridePanel)
            }
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                add(
                    JBLabel(
                        "<html>Supported variables: <code>\${PROJECT_NAME}</code>, <code>\${MODULE_NAME}</code>, " +
                            "<code>\${FILE_NAME}</code>, <code>\${FILE_PATH}</code><br/>" +
                            "Missing values stay empty until OpenLinker runs in a real project context.</html>",
                    ).apply {
                        foreground = UIManager.getColor("Label.disabledForeground")
                    },
                    BorderLayout.WEST,
                )
            })
        }

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(content, BorderLayout.CENTER)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = urlTemplateArea

    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) {
            return ValidationInfo("Rule name cannot be empty.", nameField)
        }
        if (urlTemplateArea.text.isBlank()) {
            return ValidationInfo("URL template cannot be empty.", urlTemplateArea)
        }
        if (
            currentProjectName.isNotBlank() &&
            projectOverrideCheckBox.isSelected &&
            projectOverrideArea.text.isBlank()
        ) {
            return ValidationInfo("Project override URL template cannot be empty.", projectOverrideArea)
        }
        return null
    }

    private fun buildEditorHeader(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(JBLabel("URL template:"), BorderLayout.WEST)
            add(variableButtonsPanel(urlTemplateArea), BorderLayout.EAST)
        }
    }

    private fun buildProjectOverridePanel(): JComponent {
        val editorScrollPane = JBScrollPane(projectOverrideArea).apply {
            preferredSize = Dimension(JBUI.scale(720), JBUI.scale(140))
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(borderColor()),
                JBUI.Borders.empty(0),
            )
        }

        return JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isVisible = existingProjectOverride != null
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                add(JBLabel("Project URL template:"), BorderLayout.WEST)
                add(variableButtonsPanel(projectOverrideArea), BorderLayout.EAST)
            })
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(editorScrollPane)
        }
    }

    private fun labeled(label: String, component: JComponent): JComponent {
        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(JBLabel(label), BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
        }
    }

    private fun variableButtonsPanel(target: JBTextArea): JComponent {
        return JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            SUPPORTED_VARIABLES.forEach { variable ->
                add(JButton(InsertVariableAction(variable, target)).apply {
                    putClientProperty("JButton.buttonType", "roundRect")
                    isFocusable = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(13))
                    margin = JBUI.insets(4, 10)
                    background = chipBackgroundColor()
                    foreground = chipForegroundColor()
                    border = BorderFactory.createCompoundBorder(
                        JBUI.Borders.customLine(chipBorderColor()),
                        JBUI.Borders.empty(3, 8),
                    )
                })
            }
        }
    }

    private fun initEditors() {
        nameField.emptyText.text = "GitHub"
        urlTemplateArea.lineWrap = false
        urlTemplateArea.wrapStyleWord = false
        urlTemplateArea.font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(15))
        urlTemplateArea.border = JBUI.Borders.empty(10)
        projectOverrideArea.lineWrap = false
        projectOverrideArea.wrapStyleWord = false
        projectOverrideArea.font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(15))
        projectOverrideArea.border = JBUI.Borders.empty(10)
        projectOverrideCheckBox.addActionListener {
            projectOverridePanel.isVisible = projectOverrideCheckBox.isSelected
            projectOverridePanel.parent?.revalidate()
            projectOverridePanel.parent?.repaint()
        }
    }

    private fun initialRuleProjectOverridesWithoutCurrentProject(): List<ProjectUrlOverride> {
        return initialProjectOverrides.filterNot { it.projectName.trim() == currentProjectName }
    }

    private fun borderColor() = UIManager.getColor("Component.borderColor")
        ?: UIManager.getColor("Separator.foreground")

    private fun chipBackgroundColor() = UIManager.getColor("Panel.background")
        ?.brighter()
        ?: borderColor()

    private fun chipBorderColor() = UIManager.getColor("Component.focusColor")
        ?: borderColor()

    private fun chipForegroundColor() = UIManager.getColor("Label.foreground")
        ?: nameField.foreground

    private inner class InsertVariableAction(
        private val variable: String,
        private val target: JBTextArea,
    ) : AbstractAction(variable) {
        override fun actionPerformed(event: ActionEvent) {
            target.requestFocusInWindow()
            target.replaceSelection(variable)
        }
    }

    private companion object {
        val SUPPORTED_VARIABLES = listOf(
            "\${PROJECT_NAME}",
            "\${MODULE_NAME}",
            "\${FILE_NAME}",
            "\${FILE_PATH}",
        )
    }
}
