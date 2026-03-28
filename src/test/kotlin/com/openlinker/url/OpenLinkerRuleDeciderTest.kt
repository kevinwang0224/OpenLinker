package com.openlinker.url

import com.openlinker.model.CustomUrlRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OpenLinkerRuleDeciderTest {
    @Test
    fun `returns no enabled rules when list is empty`() {
        val decision = OpenLinkerRuleDecider.decide(emptyList())

        assertEquals(OpenLinkerRuleDecision.NoEnabledRules, decision)
    }

    @Test
    fun `returns single rule when exactly one rule is enabled`() {
        val decision = OpenLinkerRuleDecider.decide(
            listOf(
                CustomUrlRule(name = "Disabled", urlTemplate = "https://disabled", enabled = false),
                CustomUrlRule(name = "Google", urlTemplate = "https://google", enabled = true),
            ),
        )

        assertIs<OpenLinkerRuleDecision.SingleRule>(decision)
        assertEquals("Google", decision.rule.name)
    }

    @Test
    fun `returns multiple rules when several rules are enabled`() {
        val decision = OpenLinkerRuleDecider.decide(
            listOf(
                CustomUrlRule(name = "Google", urlTemplate = "https://google", enabled = true),
                CustomUrlRule(name = "Docs", urlTemplate = "https://docs", enabled = true),
            ),
        )

        assertIs<OpenLinkerRuleDecision.MultipleRules>(decision)
        assertEquals(listOf("Google", "Docs"), decision.rules.map { it.name })
    }
}
