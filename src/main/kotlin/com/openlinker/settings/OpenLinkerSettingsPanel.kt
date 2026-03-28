package com.openlinker.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.AnActionButton
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.openlinker.OpenLinkerConstants
import com.openlinker.OpenLinkerIcons
import com.openlinker.OpenLinkerResolvedUrl
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.normalized
import com.openlinker.url.OpenLinkerContext
import com.openlinker.url.OpenLinkerUrlTemplateResolver
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class OpenLinkerSettingsPanel {
    private val listModel = DefaultListModel<CustomUrlRule>()
    private val ruleList = JBList(listModel)
    private val nameField = JBTextField()
    private val enabledCheckBox = JBCheckBox("Enabled")
    private val urlTemplateArea = JBTextArea(6, 0)
    private val detailCards = CardLayout()
    private val detailPanel = JPanel(detailCards)
    private var isUpdatingDetails = false

    val component: JComponent
    val preferredFocusedComponent: JComponent
        get() = if (ruleList.selectedIndex >= 0) nameField else ruleList

    init {
        configureRuleList()
        configureEditors()
        bindEditorListeners()

        val splitter = Splitter(false, 0.32f).apply {
            firstComponent = buildRuleListPanel()
            secondComponent = buildDetailPanel()
        }

        component = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(8)
            add(
                JBLabel("Rules are shown in the same order as the action chooser."),
                BorderLayout.NORTH,
            )
            add(splitter, BorderLayout.CENTER)
        }

        updateDetailView()
    }

    fun reset(rules: List<CustomUrlRule>) {
        listModel.clear()
        rules.normalized().forEach(listModel::addElement)

        if (listModel.isEmpty) {
            ruleList.clearSelection()
            updateDetailView()
        } else {
            ruleList.selectedIndex = 0
        }
    }

    fun isModified(savedRules: List<CustomUrlRule>): Boolean {
        return currentRules().normalized() != savedRules.normalized()
    }

    @Throws(ConfigurationException::class)
    fun getValidatedRules(): List<CustomUrlRule> {
        val rules = currentRules().normalized()

        rules.forEachIndexed { index, rule ->
            if (rule.name.isBlank()) {
                throw ConfigurationException("Rule ${index + 1} name cannot be empty.", "OpenLinker")
            }
            if (rule.urlTemplate.isBlank()) {
                throw ConfigurationException("Rule ${index + 1} URL template cannot be empty.", "OpenLinker")
            }
        }

        return rules
    }

    private fun configureRuleList() {
        ruleList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        ruleList.emptyText.text = "No rules yet. Click + to add one."
        ruleList.cellRenderer = object : ColoredListCellRenderer<CustomUrlRule>() {
            override fun customizeCellRenderer(
                list: javax.swing.JList<out CustomUrlRule>,
                value: CustomUrlRule?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                if (value == null) {
                    return
                }

                append(value.name.ifBlank { "Untitled rule" })
                if (!value.enabled) {
                    append("  disabled", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                if (value.urlTemplate.isNotBlank()) {
                    append("  ")
                    append(value.urlTemplate, SimpleTextAttributes.GRAY_ATTRIBUTES)
                }
            }
        }
        ruleList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                updateDetailView()
            }
        }
    }

    private fun configureEditors() {
        nameField.emptyText.text = "Google"
        urlTemplateArea.emptyText.text = "https://www.google.com/search?q=\${PROJECT_NAME}"
        urlTemplateArea.lineWrap = false
        urlTemplateArea.wrapStyleWord = false
        urlTemplateArea.border = JBUI.Borders.empty(6)
    }

    private fun bindEditorListeners() {
        nameField.document.addDocumentListener(textChangeListener { text ->
            updateSelectedRule { it.copy(name = text) }
        })
        urlTemplateArea.document.addDocumentListener(textChangeListener { text ->
            updateSelectedRule { it.copy(urlTemplate = text) }
        })
        enabledCheckBox.addActionListener {
            updateSelectedRule { it.copy(enabled = enabledCheckBox.isSelected) }
        }
    }

    private fun buildRuleListPanel(): JComponent {
        val decorator = ToolbarDecorator.createDecorator(ruleList)
            .setAddAction { _ -> addRule() }
            .setRemoveAction { _ -> removeRule() }
            .setMoveUpAction { _ -> moveRule(-1) }
            .setMoveDownAction { _ -> moveRule(1) }
            .addExtraAction(OpenSelectedRuleActionButton())

        return decorator.createPanel().apply {
            preferredSize = Dimension(JBUI.scale(280), JBUI.scale(420))
        }
    }

    private fun buildDetailPanel(): JComponent {
        detailPanel.add(buildEmptyStatePanel(), EMPTY_CARD)
        detailPanel.add(buildEditorPanel(), EDITOR_CARD)
        return detailPanel
    }

    private fun buildEmptyStatePanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(24)
            add(
                JBLabel(
                    "<html><b>No rule selected</b><br/>Choose a rule on the left or click + to add one.</html>",
                ),
                BorderLayout.NORTH,
            )
        }
    }

    private fun buildEditorPanel(): JComponent {
        val urlScrollPane = JBScrollPane(urlTemplateArea).apply {
            preferredSize = Dimension(0, JBUI.scale(150))
        }

        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(16)
            add(labeled("Rule name", nameField))
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(enabledCheckBox)
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(labeled("URL template", urlScrollPane))
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(variableButtonsPanel())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(
                JBLabel(
                    "<html>Supported variables: <code>\${PROJECT_NAME}</code>, <code>\${MODULE_NAME}</code>, " +
                        "<code>\${FILE_NAME}</code>, <code>\${FILE_PATH}</code><br/>" +
                        "Missing values are left empty when the action runs.</html>",
                ),
            )
        }

        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(formPanel), BorderLayout.CENTER)
        }
    }

    private fun labeled(label: String, field: JComponent): JComponent {
        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(JBLabel(label), BorderLayout.NORTH)
            add(field, BorderLayout.CENTER)
        }
    }

    private fun variableButtonsPanel(): JComponent {
        return JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            add(JBLabel("Insert variable:"))
            SUPPORTED_VARIABLES.forEach { variable ->
                add(JButton(variable).apply {
                    addActionListener {
                        if (!urlTemplateArea.isEnabled) {
                            return@addActionListener
                        }

                        urlTemplateArea.requestFocusInWindow()
                        urlTemplateArea.replaceSelection(variable)
                    }
                })
            }
        }
    }

    private fun addRule() {
        val rule = CustomUrlRule(
            name = "New Rule",
            urlTemplate = "https://",
            enabled = true,
        )
        listModel.addElement(rule)
        ruleList.selectedIndex = listModel.size() - 1
        SwingUtilities.invokeLater {
            nameField.requestFocusInWindow()
            nameField.selectAll()
        }
    }

    private fun removeRule() {
        val index = ruleList.selectedIndex
        if (index < 0) {
            return
        }

        listModel.remove(index)

        if (listModel.isEmpty) {
            ruleList.clearSelection()
            updateDetailView()
            return
        }

        ruleList.selectedIndex = index.coerceAtMost(listModel.size() - 1)
    }

    private fun moveRule(offset: Int) {
        val index = ruleList.selectedIndex
        if (index < 0) {
            return
        }

        val targetIndex = index + offset
        if (targetIndex !in 0 until listModel.size()) {
            return
        }

        val rule = listModel.remove(index)
        listModel.add(targetIndex, rule)
        ruleList.selectedIndex = targetIndex
    }

    private fun updateSelectedRule(update: (CustomUrlRule) -> CustomUrlRule) {
        if (isUpdatingDetails) {
            return
        }

        val index = ruleList.selectedIndex
        if (index < 0) {
            return
        }

        val updatedRule = update(listModel.getElementAt(index))
        listModel.set(index, updatedRule)
        ruleList.selectedIndex = index
    }

    private fun updateDetailView() {
        val selectedRule = ruleList.selectedValue
        if (selectedRule == null) {
            isUpdatingDetails = true
            try {
                nameField.text = ""
                urlTemplateArea.text = ""
                enabledCheckBox.isSelected = false
                setEditorEnabled(false)
                detailCards.show(detailPanel, EMPTY_CARD)
            } finally {
                isUpdatingDetails = false
            }
            return
        }

        isUpdatingDetails = true
        try {
            setEditorEnabled(true)
            nameField.text = selectedRule.name
            urlTemplateArea.text = selectedRule.urlTemplate
            enabledCheckBox.isSelected = selectedRule.enabled
            detailCards.show(detailPanel, EDITOR_CARD)
        } finally {
            isUpdatingDetails = false
        }
    }

    private fun setEditorEnabled(enabled: Boolean) {
        nameField.isEnabled = enabled
        enabledCheckBox.isEnabled = enabled
        urlTemplateArea.isEnabled = enabled
    }

    private fun currentRules(): List<CustomUrlRule> = List(listModel.size()) { index ->
        listModel.getElementAt(index)
    }

    private fun openSelectedRule() {
        val resolvedUrl = selectedResolvedUrl()
        if (!OpenLinkerResolvedUrl.canOpen(resolvedUrl)) {
            Messages.showWarningDialog(
                "The selected rule does not currently produce a usable address.",
                OpenLinkerConstants.PLUGIN_NAME,
            )
            return
        }

        try {
            BrowserUtil.browse(resolvedUrl)
        } catch (_: Exception) {
            Messages.showErrorDialog(
                "OpenLinker could not open this address:\n$resolvedUrl",
                OpenLinkerConstants.PLUGIN_NAME,
            )
        }
    }

    private fun selectedResolvedUrl(): String {
        val template = ruleList.selectedValue?.urlTemplate.orEmpty()
        return OpenLinkerUrlTemplateResolver.resolve(template, OpenLinkerContext()).trim()
    }

    private fun textChangeListener(onChange: (String) -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(event: DocumentEvent) = onChange(event)

        override fun removeUpdate(event: DocumentEvent) = onChange(event)

        override fun changedUpdate(event: DocumentEvent) = onChange(event)

        private fun onChange(event: DocumentEvent) {
            onChange(event.document.getText(0, event.document.length))
        }
    }

    private companion object {
        const val EMPTY_CARD = "empty"
        const val EDITOR_CARD = "editor"
        val SUPPORTED_VARIABLES = listOf(
            "\${PROJECT_NAME}",
            "\${MODULE_NAME}",
            "\${FILE_NAME}",
            "\${FILE_PATH}",
        )
    }

    private inner class OpenSelectedRuleActionButton : AnActionButton("Open Selected", OpenLinkerIcons.BROWSER_OPEN) {
        override fun actionPerformed(event: AnActionEvent) {
            openSelectedRule()
        }

        override fun updateButton(event: AnActionEvent) {
            super.updateButton(event)
            event.presentation.isEnabled = OpenLinkerResolvedUrl.canOpen(selectedResolvedUrl())
        }
    }
}
