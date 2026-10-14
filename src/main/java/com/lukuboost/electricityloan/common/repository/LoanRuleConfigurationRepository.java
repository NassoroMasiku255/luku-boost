package com.lukuboost.electricityloan.common.repository;

import com.lukuboost.electricityloan.common.entity.LoanRuleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRuleConfigurationRepository extends JpaRepository<LoanRuleConfiguration, Long> {

    Optional<LoanRuleConfiguration> findByRuleName(String ruleName);

    List<LoanRuleConfiguration> findByRuleTypeAndEnabledTrueOrderByPriorityAsc(String ruleType);

    @Query("SELECT r FROM LoanRuleConfiguration r WHERE r.enabled = true " +
            "AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= CURRENT_TIMESTAMP) " +
            "AND (r.effectiveTo IS NULL OR r.effectiveTo >= CURRENT_TIMESTAMP) " +
            "ORDER BY r.priority ASC")
    List<LoanRuleConfiguration> findActiveRules();
}
