package com.anomapro.finndot.domain.usecase

import com.anomapro.finndot.domain.repository.RuleRepository
import com.anomapro.finndot.domain.service.RuleTemplateService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class InitializeRuleTemplatesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val ruleTemplateService: RuleTemplateService
) {
    suspend operator fun invoke(forceReset: Boolean = false) {
        val existingRules = ruleRepository.getAllRules().first()

        if (existingRules.isEmpty() || forceReset) {
            val templates = ruleTemplateService.getDefaultRuleTemplates()
            templates.forEach { template ->
                // Only insert if not already exists (by name) or if force reset
                if (forceReset || existingRules.none { it.name == template.name }) {
                    ruleRepository.insertRule(template)
                }
            }
        }
    }
}