package com.openlinker.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.openlinker.OpenLinkerConstants
import com.openlinker.OpenLinkerIcons
import com.openlinker.OpenLinkerResolvedUrl
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.normalized
import com.openlinker.url.OpenLinkerContext
import com.openlinker.url.OpenLinkerUrlTemplateResolver
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Files
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

class OpenLinkerSettingsPanel {
    private val tableModel = RuleTableModel()
    private val ruleTable = JBTable(tableModel)

    val component: JComponent
    val preferredFocusedComponent: JComponent
        get() = ruleTable

    init {
        configureRuleTable()

        component = JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(10))).apply {
            border = JBUI.Borders.empty(10)
            add(buildHeaderPanel(), BorderLayout.NORTH)
            add(buildTablePanel(), BorderLayout.CENTER)
        }
    }

    fun reset(rules: List<CustomUrlRule>) {
        tableModel.setRules(rules.normalized())
        if (tableModel.rowCount > 0) {
            selectRow(0)
        }
    }

    fun isModified(savedRules: List<CustomUrlRule>): Boolean {
        return tableModel.rules() != savedRules.normalized()
    }

    @Throws(ConfigurationException::class)
    fun getValidatedRules(): List<CustomUrlRule> {
        val rules = tableModel.rules()

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

    private fun buildHeaderPanel(): JComponent {
        val noteColor = UIManager.getColor("Label.disabledForeground")
            ?: SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
            ?: Color.GRAY

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(
                JBLabel("Rules are shown in the same order as the action chooser.").apply {
                    foreground = noteColor
                },
                BorderLayout.NORTH,
            )
        }
    }

    private fun buildTablePanel(): JComponent {
        val toolbar = buildToolbar()
        val scrollPane = JBScrollPane(ruleTable).apply {
            border = BorderFactory.createEmptyBorder()
        }

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(borderColor()),
                JBUI.Borders.empty(0),
            )
            add(toolbar, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun buildToolbar(): JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(AddRuleAction())
            add(RemoveRuleAction())
            add(EditRuleAction())
            addSeparator()
            add(MoveRuleAction(-1))
            add(MoveRuleAction(1))
            addSeparator()
            add(OpenRuleAction())
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("OpenLinkerSettingsToolbar", actionGroup, true)
        toolbar.targetComponent = ruleTable

        return toolbar.component.apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLineBottom(borderColor()),
                JBUI.Borders.empty(4, 8),
            )
        }
    }

    private fun configureRuleTable() {
        ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        ruleTable.setShowGrid(false)
        ruleTable.intercellSpacing = Dimension(0, 0)
        ruleTable.rowHeight = JBUI.scale(38)
        ruleTable.fillsViewportHeight = true
        ruleTable.emptyText.text = "No rules yet. Click + to add one."
        ruleTable.tableHeader.reorderingAllowed = false
        ruleTable.tableHeader.resizingAllowed = true
        ruleTable.font = Font(Font.MONOSPACED, Font.PLAIN, ruleTable.font.size)

        ruleTable.setDefaultRenderer(Boolean::class.java, EnabledCellRenderer())
        ruleTable.columnModel.getColumn(COLUMN_ENABLED).preferredWidth = JBUI.scale(88)
        ruleTable.columnModel.getColumn(COLUMN_ENABLED).maxWidth = JBUI.scale(88)
        ruleTable.columnModel.getColumn(COLUMN_NAME).preferredWidth = JBUI.scale(220)
        ruleTable.columnModel.getColumn(COLUMN_TEMPLATE).preferredWidth = JBUI.scale(540)
        ruleTable.columnModel.getColumn(COLUMN_NAME).cellRenderer = RuleTextRenderer(tableModel, COLUMN_NAME)
        ruleTable.columnModel.getColumn(COLUMN_TEMPLATE).cellRenderer = RuleTextRenderer(tableModel, COLUMN_TEMPLATE)

        TableSpeedSearch(ruleTable)

        ruleTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2 && event.button == MouseEvent.BUTTON1 && selectedRow() >= 0) {
                    editSelectedRule()
                }
            }
        })

        ruleTable.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editRule")
        ruleTable.actionMap.put("editRule", object : AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                editSelectedRule()
            }
        })
    }

    private fun addRule() {
        val dialog = OpenLinkerRuleDialog()
        if (!dialog.showAndGet()) {
            return
        }

        val row = tableModel.addRule(dialog.getRule())
        selectRow(row)
    }

    private fun editSelectedRule() {
        val row = selectedRow()
        if (row < 0) {
            return
        }

        val dialog = OpenLinkerRuleDialog(tableModel.getRule(row))
        if (!dialog.showAndGet()) {
            return
        }

        tableModel.updateRule(row, dialog.getRule())
        selectRow(row)
    }

    private fun removeSelectedRule() {
        val row = selectedRow()
        if (row < 0) {
            return
        }

        tableModel.removeRule(row)
        if (tableModel.rowCount > 0) {
            selectRow(row.coerceAtMost(tableModel.rowCount - 1))
        }
    }

    private fun moveSelectedRule(offset: Int) {
        val row = selectedRow()
        if (row < 0) {
            return
        }

        val targetRow = row + offset
        if (targetRow !in 0 until tableModel.rowCount) {
            return
        }

        tableModel.swap(row, targetRow)
        selectRow(targetRow)
    }

    private fun openSelectedRule() {
        val rule = selectedRule() ?: return
        val resolvedUrl = OpenLinkerUrlTemplateResolver.resolve(rule.urlTemplate, OpenLinkerContext()).trim()

        if (!OpenLinkerResolvedUrl.canOpen(resolvedUrl)) {
            Messages.showWarningDialog(
                "The selected rule does not currently produce a usable address.",
                OpenLinkerConstants.PLUGIN_NAME,
            )
            return
        }

        val localPath = OpenLinkerResolvedUrl.toLocalPath(resolvedUrl)
        if (localPath != null) {
            if (!Files.exists(localPath)) {
                Messages.showWarningDialog(
                    "OpenLinker could not find this file or folder:\n$localPath",
                    OpenLinkerConstants.PLUGIN_NAME,
                )
                return
            }

            if (Files.isDirectory(localPath)) {
                RevealFileAction.openDirectory(localPath)
            } else {
                RevealFileAction.openFile(localPath)
            }
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

    private fun selectedRow(): Int = ruleTable.selectedRow

    private fun selectedRule(): CustomUrlRule? {
        val row = selectedRow()
        return if (row >= 0) tableModel.getRule(row) else null
    }

    private fun selectRow(row: Int) {
        if (row !in 0 until tableModel.rowCount) {
            return
        }

        ruleTable.selectionModel.setSelectionInterval(row, row)
        ruleTable.scrollRectToVisible(ruleTable.getCellRect(row, 0, true))
    }

    private fun borderColor(): Color {
        return UIManager.getColor("Component.borderColor")
            ?: UIManager.getColor("Table.gridColor")
            ?: UIManager.getColor("Separator.foreground")
            ?: Color.GRAY
    }

    private class RuleTableModel : AbstractTableModel() {
        private val rules = mutableListOf<CustomUrlRule>()

        override fun getRowCount(): Int = rules.size

        override fun getColumnCount(): Int = 3

        override fun getColumnName(column: Int): String = when (column) {
            COLUMN_ENABLED -> "Enabled"
            COLUMN_NAME -> "Rule Name"
            COLUMN_TEMPLATE -> "Configuration"
            else -> ""
        }

        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            COLUMN_ENABLED -> java.lang.Boolean::class.java
            else -> String::class.java
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = when (columnIndex) {
            COLUMN_ENABLED -> rules[rowIndex].enabled
            COLUMN_NAME -> rules[rowIndex].name
            COLUMN_TEMPLATE -> rules[rowIndex].urlTemplate
            else -> ""
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

        fun setRules(rules: List<CustomUrlRule>) {
            this.rules.clear()
            this.rules.addAll(rules.normalized())
            fireTableDataChanged()
        }

        fun rules(): List<CustomUrlRule> = rules.toList().normalized()

        fun getRule(row: Int): CustomUrlRule = rules[row]

        fun addRule(rule: CustomUrlRule): Int {
            val row = rules.size
            rules.add(rule.normalized())
            fireTableRowsInserted(row, row)
            return row
        }

        fun updateRule(row: Int, rule: CustomUrlRule) {
            rules[row] = rule.normalized()
            fireTableRowsUpdated(row, row)
        }

        fun removeRule(row: Int) {
            rules.removeAt(row)
            fireTableRowsDeleted(row, row)
        }

        fun swap(first: Int, second: Int) {
            val firstRule = rules[first]
            rules[first] = rules[second]
            rules[second] = firstRule
            fireTableRowsUpdated(minOf(first, second), maxOf(first, second))
        }
    }

    private class EnabledCellRenderer : JPanel(BorderLayout()), TableCellRenderer {
        private val checkBox = JCheckBox()

        init {
            isOpaque = true
            border = JBUI.Borders.empty(0, 12)
            checkBox.isOpaque = false
            checkBox.horizontalAlignment = SwingConstants.CENTER
            add(checkBox, BorderLayout.CENTER)
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            checkBox.isSelected = value as? Boolean ?: false
            background = if (isSelected) table.selectionBackground else table.background
            foreground = if (isSelected) table.selectionForeground else table.foreground
            return this
        }
    }

    private class RuleTextRenderer(
        private val tableModel: RuleTableModel,
        private val column: Int,
    ) : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            val rule = tableModel.getRule(row)
            text = value?.toString().orEmpty().ifBlank {
                if (this.column == COLUMN_NAME) "Untitled rule" else ""
            }
            font = if (this.column == COLUMN_NAME) {
                table.font.deriveFont(Font.BOLD, table.font.size2D)
            } else {
                table.font
            }
            border = JBUI.Borders.empty(0, 12)
            horizontalAlignment = SwingConstants.LEFT

            if (!isSelected && !rule.enabled) {
                foreground = UIManager.getColor("Label.disabledForeground")
                    ?: SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
                    ?: foreground
            }

            return this
        }
    }

    private inner class AddRuleAction : DumbAwareAction("", "Add rule", AllIcons.General.Add) {
        override fun actionPerformed(event: AnActionEvent) {
            addRule()
        }
    }

    private inner class RemoveRuleAction : DumbAwareAction("", "Remove selected rule", AllIcons.General.Remove) {
        override fun actionPerformed(event: AnActionEvent) {
            removeSelectedRule()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = selectedRow() >= 0
        }
    }

    private inner class EditRuleAction : DumbAwareAction("", "Edit selected rule", AllIcons.Actions.Edit) {
        override fun actionPerformed(event: AnActionEvent) {
            editSelectedRule()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = selectedRow() >= 0
        }
    }

    private inner class MoveRuleAction(private val offset: Int) : DumbAwareAction(
        "",
        if (offset < 0) "Move selected rule up" else "Move selected rule down",
        if (offset < 0) AllIcons.Actions.MoveUp else AllIcons.Actions.MoveDown,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            moveSelectedRule(offset)
        }

        override fun update(event: AnActionEvent) {
            val row = selectedRow()
            event.presentation.isEnabled = when {
                row < 0 -> false
                offset < 0 -> row > 0
                else -> row < tableModel.rowCount - 1
            }
        }
    }

    private inner class OpenRuleAction : DumbAwareAction("", "Open selected rule", OpenLinkerIcons.BROWSER_OPEN) {
        override fun actionPerformed(event: AnActionEvent) {
            openSelectedRule()
        }

        override fun update(event: AnActionEvent) {
            val rule = selectedRule()
            val resolvedUrl = rule?.let {
                OpenLinkerUrlTemplateResolver.resolve(it.urlTemplate, OpenLinkerContext()).trim()
            }.orEmpty()
            event.presentation.isEnabled = OpenLinkerResolvedUrl.canOpen(resolvedUrl)
        }
    }

    private companion object {
        const val COLUMN_ENABLED = 0
        const val COLUMN_NAME = 1
        const val COLUMN_TEMPLATE = 2
    }
}
