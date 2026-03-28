package com.openlinker.url

import com.openlinker.model.CustomUrlRule

sealed interface OpenLinkerRuleDecision {
    data object NoEnabledRules : OpenLinkerRuleDecision
    data class SingleRule(val rule: CustomUrlRule) : OpenLinkerRuleDecision
    data class MultipleRules(val rules: List<CustomUrlRule>) : OpenLinkerRuleDecision
}

object OpenLinkerRuleDecider {
    fun decide(rules: List<CustomUrlRule>): OpenLinkerRuleDecision {
        val enabledRules = rules.filter { it.enabled }

        return when (enabledRules.size) {
            0 -> OpenLinkerRuleDecision.NoEnabledRules
            1 -> OpenLinkerRuleDecision.SingleRule(enabledRules.first())
            else -> OpenLinkerRuleDecision.MultipleRules(enabledRules)
        }
    }
}
