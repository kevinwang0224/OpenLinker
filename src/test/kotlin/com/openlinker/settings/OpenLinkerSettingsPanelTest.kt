package com.openlinker.settings

import com.openlinker.model.CustomUrlRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenLinkerSettingsPanelTest {
    @Test
    fun `reset discards imported but unapplied rules`() {
        val savedRules = listOf(
            CustomUrlRule(name = "Google", urlTemplate = "https://www.google.com/search?q=${'$'}{PROJECT_NAME}", enabled = true),
        )
        val panel = OpenLinkerSettingsPanel()
        panel.reset(savedRules)

        panel.appendRules(
            listOf(CustomUrlRule(name = "Team Docs", urlTemplate = "https://example.com/docs", enabled = true)),
        )

        assertEquals(
            listOf(
                CustomUrlRule(name = "Google", urlTemplate = "https://www.google.com/search?q=${'$'}{PROJECT_NAME}", enabled = true),
                CustomUrlRule(name = "Team Docs", urlTemplate = "https://example.com/docs", enabled = true),
            ),
            panel.getValidatedRules(),
        )

        panel.reset(savedRules)

        assertEquals(savedRules, panel.getValidatedRules())
    }

    @Test
    fun `append keeps duplicate rule names and preserves import order`() {
        val panel = OpenLinkerSettingsPanel()
        panel.reset(
            listOf(
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/original", enabled = true),
            ),
        )

        panel.appendRules(
            listOf(
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/team-a", enabled = true),
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/team-b", enabled = false),
            ),
        )

        assertEquals(
            listOf(
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/original", enabled = true),
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/team-a", enabled = true),
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/team-b", enabled = false),
            ),
            panel.getValidatedRules(),
        )
    }

    @Test
    fun `selected export follows table order instead of click order`() {
        val panel = OpenLinkerSettingsPanel()
        panel.reset(
            listOf(
                CustomUrlRule(name = "First", urlTemplate = "https://example.com/1", enabled = true),
                CustomUrlRule(name = "Second", urlTemplate = "https://example.com/2", enabled = true),
                CustomUrlRule(name = "Third", urlTemplate = "https://example.com/3", enabled = true),
            ),
        )

        panel.selectRowsForTesting(2, 0)

        assertEquals(
            listOf(
                CustomUrlRule(name = "First", urlTemplate = "https://example.com/1", enabled = true),
                CustomUrlRule(name = "Third", urlTemplate = "https://example.com/3", enabled = true),
            ),
            panel.selectedRulesForTesting(),
        )
    }

    @Test
    fun `enabled column can update rule state directly`() {
        val panel = OpenLinkerSettingsPanel()
        panel.reset(
            listOf(
                CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/docs", enabled = true),
            ),
        )

        assertTrue(panel.isEnabledColumnEditableForTesting(0))

        panel.setRuleEnabledForTesting(0, false)

        assertEquals(
            listOf(CustomUrlRule(name = "Docs", urlTemplate = "https://example.com/docs", enabled = false)),
            panel.getValidatedRules(),
        )
    }

    @Test
    fun `multi selection disables single rule actions and enables bulk actions`() {
        val panel = OpenLinkerSettingsPanel()
        panel.reset(
            listOf(
                CustomUrlRule(name = "First", urlTemplate = "https://example.com/1", enabled = true),
                CustomUrlRule(name = "Second", urlTemplate = "https://example.com/2", enabled = true),
                CustomUrlRule(name = "Third", urlTemplate = "https://example.com/3", enabled = true),
            ),
        )

        panel.selectRowsForTesting(0, 1)

        val state = panel.toolbarActionState()
        assertTrue(state.canRemove)
        assertTrue(state.canExportSelected)
        assertFalse(state.canEdit)
        assertFalse(state.canMoveUp)
        assertFalse(state.canMoveDown)
        assertFalse(state.canOpen)
    }

    @Test
    fun `single selection enables single rule actions when movement is possible`() {
        val panel = OpenLinkerSettingsPanel()
        panel.reset(
            listOf(
                CustomUrlRule(name = "First", urlTemplate = "https://example.com/1", enabled = true),
                CustomUrlRule(name = "Second", urlTemplate = "https://example.com/2", enabled = true),
                CustomUrlRule(name = "Third", urlTemplate = "https://example.com/3", enabled = true),
            ),
        )

        panel.selectRowsForTesting(1)

        val state = panel.toolbarActionState()
        assertTrue(state.canRemove)
        assertTrue(state.canEdit)
        assertTrue(state.canMoveUp)
        assertTrue(state.canMoveDown)
        assertTrue(state.canOpen)
        assertTrue(state.canExportSelected)
    }
}
