package com.openlinker.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
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
import com.openlinker.browser.OpenLinkerBrowsers
import com.openlinker.model.CustomUrlRule
import com.openlinker.model.OpenLinkerBrowserPreference
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
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
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

class OpenLinkerSettingsPanel(currentProjectName: String = "") {
    private val currentProjectName = currentProjectName.trim()
    private val tableModel = RuleTableModel(this.currentProjectName)
    private val ruleTable = JBTable(tableModel)
    private val globalBrowserSelector = OpenLinkerBrowserSelector("System default browser")

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
        reset(OpenLinkerBrowserPreference(), rules)
    }

    fun reset(globalBrowserPreference: OpenLinkerBrowserPreference, rules: List<CustomUrlRule>) {
        globalBrowserSelector.setPreference(globalBrowserPreference)
        tableModel.setRules(rules.normalized())
        if (tableModel.rowCount > 0) {
            selectRow(0)
        } else {
            ruleTable.clearSelection()
        }
    }

    fun isModified(savedRules: List<CustomUrlRule>): Boolean {
        return isModified(OpenLinkerBrowserPreference(), savedRules)
    }

    fun isModified(savedGlobalBrowserPreference: OpenLinkerBrowserPreference, savedRules: List<CustomUrlRule>): Boolean {
        return globalBrowserSelector.getPreference() != savedGlobalBrowserPreference.normalized() ||
            tableModel.rules() != savedRules.normalized()
    }

    fun getGlobalBrowserPreference(): OpenLinkerBrowserPreference = globalBrowserSelector.getPreference()

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

    internal fun appendRules(rules: List<CustomUrlRule>): IntRange? {
        val insertedRange = tableModel.addRules(rules)
        if (insertedRange != null) {
            selectRange(insertedRange.first, insertedRange.last)
        }
        return insertedRange
    }

    internal fun selectRowsForTesting(vararg rows: Int) {
        ruleTable.clearSelection()
        rows.sorted().forEach { row ->
            if (row in 0 until tableModel.rowCount) {
                ruleTable.selectionModel.addSelectionInterval(row, row)
            }
        }
    }

    internal fun selectedRulesForTesting(): List<CustomUrlRule> = selectedRules()

    internal fun configurationTextForTesting(row: Int): String = tableModel.configurationText(row)

    internal fun isEnabledColumnEditableForTesting(row: Int): Boolean =
        tableModel.isCellEditable(row, COLUMN_ENABLED)

    internal fun setRuleEnabledForTesting(row: Int, enabled: Boolean) {
        tableModel.setValueAt(enabled, row, COLUMN_ENABLED)
    }

    internal fun toolbarActionState(): ToolbarActionState = ToolbarActionState(
        canRemove = hasSelection(),
        canEdit = hasSingleSelection(),
        canMoveUp = canMoveSelected(-1),
        canMoveDown = canMoveSelected(1),
        canOpen = canOpenSelectedRule(),
        canExportSelected = hasSelection(),
    )

    private fun buildHeaderPanel(): JComponent {
        return JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                isOpaque = false
                add(JBLabel("Global browser:"), BorderLayout.WEST)
                add(globalBrowserSelector.component, BorderLayout.CENTER)
            })
            add(Box.createVerticalStrut(JBUI.scale(8)))
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
        if (ApplicationManager.getApplication() == null) {
            return JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.compound(
                    JBUI.Borders.customLineBottom(borderColor()),
                    JBUI.Borders.empty(4, 8),
                )
            }
        }

        val actionGroup = DefaultActionGroup().apply {
            add(AddRuleAction())
            add(RemoveRuleAction())
            add(EditRuleAction())
            addSeparator()
            add(MoveRuleAction(-1))
            add(MoveRuleAction(1))
            addSeparator()
            add(OpenRuleAction())
            addSeparator()
            add(ImportRulesAction())
            add(ExportSelectedRulesAction())
            add(ExportAllRulesAction())
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
        ruleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
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
        ruleTable.columnModel.getColumn(COLUMN_BROWSER).preferredWidth = JBUI.scale(160)
        ruleTable.columnModel.getColumn(COLUMN_TEMPLATE).preferredWidth = JBUI.scale(500)
        ruleTable.columnModel.getColumn(COLUMN_NAME).cellRenderer = RuleTextRenderer(tableModel, COLUMN_NAME)
        ruleTable.columnModel.getColumn(COLUMN_BROWSER).cellRenderer = RuleTextRenderer(tableModel, COLUMN_BROWSER)
        ruleTable.columnModel.getColumn(COLUMN_TEMPLATE).cellRenderer = RuleTextRenderer(tableModel, COLUMN_TEMPLATE)

        if (ApplicationManager.getApplication() != null) {
            TableSpeedSearch(ruleTable)
        }

        ruleTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (ruleTable.columnAtPoint(event.point) == COLUMN_ENABLED) {
                    return
                }

                if (event.clickCount == 2 && event.button == MouseEvent.BUTTON1 && hasSingleSelection()) {
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
        val dialog = OpenLinkerRuleDialog(currentProjectName = currentProjectName)
        if (!dialog.showAndGet()) {
            return
        }

        val row = tableModel.addRule(dialog.getRule())
        selectRow(row)
    }

    private fun editSelectedRule() {
        val row = selectedSingleRow()
        if (row < 0) {
            return
        }

        val dialog = OpenLinkerRuleDialog(tableModel.getRule(row), currentProjectName)
        if (!dialog.showAndGet()) {
            return
        }

        tableModel.updateRule(row, dialog.getRule())
        selectRow(row)
    }

    private fun removeSelectedRules() {
        val rows = selectedRows()
        if (rows.isEmpty()) {
            return
        }

        val nextRow = rows.first()
        tableModel.removeRules(rows)
        if (tableModel.rowCount > 0) {
            selectRow(nextRow.coerceAtMost(tableModel.rowCount - 1))
        }
    }

    private fun moveSelectedRule(offset: Int) {
        val row = selectedSingleRow()
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

    private fun importRules() {
        val path = chooseImportPath() ?: return

        val result = try {
            OpenLinkerRuleFileTransfer.importRules(path)
        } catch (exception: OpenLinkerRuleFileTransfer.RuleFileException) {
            Messages.showErrorDialog(
                "OpenLinker could not import rules from this file:\n${exception.message}",
                OpenLinkerConstants.PLUGIN_NAME,
            )
            return
        } catch (_: IOException) {
            Messages.showErrorDialog(
                "OpenLinker could not read this file:\n$path",
                OpenLinkerConstants.PLUGIN_NAME,
            )
            return
        }

        val insertedRange = appendRules(result.rules)
        if (insertedRange != null) {
            selectRange(insertedRange.first, insertedRange.last)
        }

        showImportResult(result)
    }

    private fun exportSelectedRules() {
        exportRules(selectedRules(), "selected")
    }

    private fun exportAllRules() {
        exportRules(tableModel.rules(), "all")
    }

    private fun exportRules(rules: List<CustomUrlRule>, scopeLabel: String) {
        if (rules.isEmpty()) {
            Messages.showWarningDialog(
                "There are no $scopeLabel rules to export.",
                OpenLinkerConstants.PLUGIN_NAME,
            )
            return
        }

        val path = chooseExportPath() ?: return
        try {
            OpenLinkerRuleFileTransfer.exportRules(path, rules)
            Messages.showInfoMessage(
                "Exported ${rules.size} rule(s) to:\n$path",
                OpenLinkerConstants.PLUGIN_NAME,
            )
        } catch (_: IOException) {
            Messages.showErrorDialog(
                "OpenLinker could not write this file:\n$path",
                OpenLinkerConstants.PLUGIN_NAME,
            )
        }
    }

    private fun openSelectedRule() {
        val rule = selectedRule() ?: return
        val resolvedUrl = OpenLinkerUrlTemplateResolver.resolve(
            rule.urlTemplateForProject(currentProjectName),
            OpenLinkerContext(projectName = currentProjectName),
        ).trim()

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
            OpenLinkerBrowsers.browse(
                null,
                resolvedUrl,
                OpenLinkerBrowsers.effectivePreference(rule.browserPreference, getGlobalBrowserPreference()),
            )
        } catch (_: Exception) {
            Messages.showErrorDialog(
                "OpenLinker could not open this address:\n$resolvedUrl",
                OpenLinkerConstants.PLUGIN_NAME,
            )
        }
    }

    private fun selectedRule(): CustomUrlRule? {
        val row = selectedSingleRow()
        return if (row >= 0) tableModel.getRule(row) else null
    }

    private fun selectedSingleRow(): Int {
        val rows = selectedRows()
        return if (rows.size == 1) rows[0] else -1
    }

    private fun selectedRows(): IntArray = ruleTable.selectedRows.sortedArray()

    private fun selectedRules(): List<CustomUrlRule> = selectedRows().map(tableModel::getRule)

    private fun hasSelection(): Boolean = selectedRows().isNotEmpty()

    private fun hasSingleSelection(): Boolean = selectedRows().size == 1

    private fun canMoveSelected(offset: Int): Boolean {
        val row = selectedSingleRow()
        return when {
            row < 0 -> false
            offset < 0 -> row > 0
            else -> row < tableModel.rowCount - 1
        }
    }

    private fun canOpenSelectedRule(): Boolean {
        val rule = selectedRule() ?: return false
        val resolvedUrl = OpenLinkerUrlTemplateResolver.resolve(
            rule.urlTemplateForProject(currentProjectName),
            OpenLinkerContext(projectName = currentProjectName),
        ).trim()
        return OpenLinkerResolvedUrl.canOpen(resolvedUrl)
    }

    private fun selectRow(row: Int) {
        if (row !in 0 until tableModel.rowCount) {
            return
        }

        ruleTable.selectionModel.setSelectionInterval(row, row)
        ruleTable.scrollRectToVisible(ruleTable.getCellRect(row, 0, true))
    }

    private fun selectRange(firstRow: Int, lastRow: Int) {
        if (firstRow !in 0 until tableModel.rowCount || lastRow !in 0 until tableModel.rowCount) {
            return
        }

        ruleTable.selectionModel.setSelectionInterval(firstRow, lastRow)
        ruleTable.scrollRectToVisible(ruleTable.getCellRect(lastRow, 0, true))
    }

    private fun chooseImportPath(): Path? {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json").apply {
            title = "Import OpenLinker Rules"
            description = "Choose a JSON file exported from OpenLinker."
        }

        val selectedFile = FileChooser.chooseFile(descriptor, null as Project?, null) ?: return null
        return Path.of(selectedFile.path)
    }

    private fun chooseExportPath(): Path? {
        val descriptor = FileSaverDescriptor(
            "Export OpenLinker Rules",
            "Choose where to save the exported OpenLinker rule file.",
            "json",
        )
        val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null as Project?)
        val selectedFile = saveDialog.save(null as Path?, OpenLinkerRuleFileTransfer.DEFAULT_FILE_NAME) ?: return null
        return selectedFile.file.toPath()
    }

    private fun showImportResult(result: OpenLinkerRuleFileTransfer.ImportResult) {
        val message = buildString {
            append("Imported ${result.rules.size} rule(s).")
            if (result.skippedEntries.isNotEmpty()) {
                append("\n\nSkipped ${result.skippedEntries.size} invalid entr")
                append(if (result.skippedEntries.size == 1) "y" else "ies")
                append(":\n")
                result.skippedEntries.forEach { issue ->
                    append("- Entry ${issue.entryIndex}: ${issue.reason}\n")
                }
            }
        }.trimEnd()

        if (result.rules.isEmpty() && result.skippedEntries.isNotEmpty()) {
            Messages.showWarningDialog(message, OpenLinkerConstants.PLUGIN_NAME)
            return
        }

        Messages.showInfoMessage(message, OpenLinkerConstants.PLUGIN_NAME)
    }

    private fun borderColor(): Color {
        return UIManager.getColor("Component.borderColor")
            ?: UIManager.getColor("Table.gridColor")
            ?: UIManager.getColor("Separator.foreground")
            ?: Color.GRAY
    }

    internal data class ToolbarActionState(
        val canRemove: Boolean,
        val canEdit: Boolean,
        val canMoveUp: Boolean,
        val canMoveDown: Boolean,
        val canOpen: Boolean,
        val canExportSelected: Boolean,
    )

    private class RuleTableModel(private val currentProjectName: String) : AbstractTableModel() {
        private val rules = mutableListOf<CustomUrlRule>()

        override fun getRowCount(): Int = rules.size

        override fun getColumnCount(): Int = 4

        override fun getColumnName(column: Int): String = when (column) {
            COLUMN_ENABLED -> "Enabled"
            COLUMN_NAME -> "Rule Name"
            COLUMN_BROWSER -> "Browser"
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
            COLUMN_BROWSER -> OpenLinkerBrowsers.displayName(rules[rowIndex].browserPreference, "Global default")
            COLUMN_TEMPLATE -> configurationText(rowIndex)
            else -> ""
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == COLUMN_ENABLED

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex != COLUMN_ENABLED || rowIndex !in rules.indices) {
                return
            }

            val enabled = value as? Boolean ?: return
            if (rules[rowIndex].enabled == enabled) {
                return
            }

            rules[rowIndex] = rules[rowIndex].copy(enabled = enabled)
            fireTableRowsUpdated(rowIndex, rowIndex)
        }

        fun setRules(rules: List<CustomUrlRule>) {
            this.rules.clear()
            this.rules.addAll(rules.normalized())
            fireTableDataChanged()
        }

        fun rules(): List<CustomUrlRule> = rules.toList().normalized()

        fun getRule(row: Int): CustomUrlRule = rules[row]

        fun configurationText(row: Int): String {
            val rule = rules[row]
            val projectUrlTemplate = rule.urlTemplateForProject(currentProjectName)
            return if (currentProjectName.isNotBlank() && projectUrlTemplate != rule.urlTemplate) {
                "Project override: $projectUrlTemplate"
            } else {
                rule.urlTemplate
            }
        }

        fun addRule(rule: CustomUrlRule): Int {
            val row = rules.size
            rules.add(rule.normalized())
            fireTableRowsInserted(row, row)
            return row
        }

        fun addRules(rules: List<CustomUrlRule>): IntRange? {
            if (rules.isEmpty()) {
                return null
            }

            val startRow = this.rules.size
            this.rules.addAll(rules.normalized())
            val endRow = this.rules.size - 1
            fireTableRowsInserted(startRow, endRow)
            return startRow..endRow
        }

        fun updateRule(row: Int, rule: CustomUrlRule) {
            rules[row] = rule.normalized()
            fireTableRowsUpdated(row, row)
        }

        fun removeRules(rows: IntArray) {
            rows.sortedDescending().forEach { row ->
                rules.removeAt(row)
            }
            fireTableDataChanged()
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
            removeSelectedRules()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = hasSelection()
        }
    }

    private inner class EditRuleAction : DumbAwareAction("", "Edit selected rule", AllIcons.Actions.Edit) {
        override fun actionPerformed(event: AnActionEvent) {
            editSelectedRule()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = hasSingleSelection()
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
            event.presentation.isEnabled = canMoveSelected(offset)
        }
    }

    private inner class OpenRuleAction : DumbAwareAction("", "Open selected rule", OpenLinkerIcons.BROWSER_OPEN) {
        override fun actionPerformed(event: AnActionEvent) {
            openSelectedRule()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = canOpenSelectedRule()
        }
    }

    private inner class ImportRulesAction : DumbAwareAction(
        "Import",
        "Import rules from a JSON file",
        OpenLinkerIcons.RULE_IMPORT,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            importRules()
        }
    }

    private inner class ExportSelectedRulesAction : DumbAwareAction(
        "Export Selected",
        "Export selected rules to a JSON file",
        OpenLinkerIcons.RULE_EXPORT_SELECTED,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            exportSelectedRules()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = hasSelection()
        }
    }

    private inner class ExportAllRulesAction : DumbAwareAction(
        "Export All",
        "Export all rules to a JSON file",
        OpenLinkerIcons.RULE_EXPORT_ALL,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            exportAllRules()
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = tableModel.rowCount > 0
        }
    }

    private companion object {
        const val COLUMN_ENABLED = 0
        const val COLUMN_NAME = 1
        const val COLUMN_BROWSER = 2
        const val COLUMN_TEMPLATE = 3
    }
}
