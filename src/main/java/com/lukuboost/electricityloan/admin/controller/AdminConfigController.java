package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.entity.LoanRuleConfiguration;
import com.lukuboost.electricityloan.common.entity.SystemConfiguration;
import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.exception.ResourceNotFoundException;
import com.lukuboost.electricityloan.common.repository.LoanRuleConfigurationRepository;
import com.lukuboost.electricityloan.common.repository.SystemConfigurationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/configurations")
@RequiredArgsConstructor
@Tag(name = "Admin Configuration", description = "System and loan rule configuration management")
public class AdminConfigController {

    private final SystemConfigurationRepository systemConfigRepository;
    private final LoanRuleConfigurationRepository loanRuleRepository;
    private final AuditService auditService;

    // ========================================================================
    // System Configurations
    // ========================================================================

    @GetMapping("/system")
    @Operation(summary = "List all system configurations")
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    public ResponseEntity<ApiResponse<List<SystemConfiguration>>> listSystemConfigs() {
        List<SystemConfiguration> configs = systemConfigRepository.findAll();
        configs.stream()
                .filter(SystemConfiguration::getIsSensitive)
                .forEach(c -> c.setConfigValue("********"));
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/system/category/{category}")
    @Operation(summary = "Get configurations by category")
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    public ResponseEntity<ApiResponse<List<SystemConfiguration>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(systemConfigRepository.findByCategory(category)));
    }

    @PostMapping("/system")
    @Operation(summary = "Create a new system configuration")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<SystemConfiguration>> createSystemConfig(
            @RequestBody Map<String, Object> body) {

        String configKey = (String) body.get("configKey");
        if (configKey == null || configKey.isBlank()) {
            throw new BusinessException("INVALID_CONFIG_KEY", "Config key is required.", HttpStatus.BAD_REQUEST);
        }

        if (systemConfigRepository.findByConfigKey(configKey).isPresent()) {
            throw new BusinessException("CONFIG_KEY_EXISTS",
                    "Configuration with key '" + configKey + "' already exists.", HttpStatus.CONFLICT);
        }

        SystemConfiguration config = SystemConfiguration.builder()
                .configKey(configKey)
                .configValue((String) body.getOrDefault("configValue", ""))
                .category((String) body.getOrDefault("category", "GENERAL"))
                .description((String) body.get("description"))
                .valueType((String) body.getOrDefault("valueType", "STRING"))
                .isSensitive(Boolean.TRUE.equals(body.get("isSensitive")))
                .build();

        config = systemConfigRepository.save(config);

        auditService.logAction("CONFIGURATION_CREATE", "SYSTEM_CONFIGURATION", config.getId().toString(),
                null, config.getConfigValue(), "Created config: " + configKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(config));
    }

    @PutMapping("/system/{id}")
    @Operation(summary = "Update system configuration")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<SystemConfiguration>> updateSystemConfig(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        SystemConfiguration config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfiguration", id.toString()));

        String oldValue = config.getConfigValue();

        if (body.containsKey("configValue")) config.setConfigValue((String) body.get("configValue"));
        if (body.containsKey("value")) config.setConfigValue((String) body.get("value"));
        if (body.containsKey("description")) config.setDescription((String) body.get("description"));
        if (body.containsKey("category")) config.setCategory((String) body.get("category"));
        if (body.containsKey("valueType")) config.setValueType((String) body.get("valueType"));
        if (body.containsKey("isSensitive")) config.setIsSensitive((Boolean) body.get("isSensitive"));

        config = systemConfigRepository.save(config);

        auditService.logAction("CONFIGURATION_CHANGE", "SYSTEM_CONFIGURATION", id.toString(),
                "{\"value\":\"" + (config.getIsSensitive() ? "****" : oldValue) + "\"}",
                "{\"value\":\"" + (config.getIsSensitive() ? "****" : config.getConfigValue()) + "\"}",
                "Updated config: " + config.getConfigKey());

        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @DeleteMapping("/system/{id}")
    @Operation(summary = "Delete system configuration")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemConfig(@PathVariable UUID id) {
        SystemConfiguration config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfiguration", id.toString()));

        systemConfigRepository.delete(config);

        auditService.logAction("CONFIGURATION_DELETE", "SYSTEM_CONFIGURATION", id.toString(),
                config.getConfigKey(), null, "Deleted config: " + config.getConfigKey());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ========================================================================
    // Loan Rule Configurations
    // ========================================================================

    @GetMapping("/loan-rules")
    @Operation(summary = "List all loan rule configurations")
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    public ResponseEntity<ApiResponse<List<LoanRuleConfiguration>>> listLoanRules() {
        return ResponseEntity.ok(ApiResponse.success(loanRuleRepository.findAll()));
    }

    @PostMapping("/loan-rules")
    @Operation(summary = "Create a new loan rule configuration")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<LoanRuleConfiguration>> createLoanRule(
            @RequestBody Map<String, Object> body) {

        String ruleName = (String) body.get("ruleName");
        if (ruleName == null || ruleName.isBlank()) {
            throw new BusinessException("INVALID_RULE_NAME", "Rule name is required.", HttpStatus.BAD_REQUEST);
        }

        if (loanRuleRepository.findByRuleName(ruleName).isPresent()) {
            throw new BusinessException("RULE_NAME_EXISTS",
                    "Loan rule with name '" + ruleName + "' already exists.", HttpStatus.CONFLICT);
        }

        LoanRuleConfiguration rule = LoanRuleConfiguration.builder()
                .ruleName(ruleName)
                .ruleType((String) body.getOrDefault("ruleType", "ELIGIBILITY"))
                .ruleValue((String) body.getOrDefault("ruleValue", "{}"))
                .priority(body.containsKey("priority") ? ((Number) body.get("priority")).intValue() : 0)
                .enabled(body.containsKey("enabled") ? (Boolean) body.get("enabled") : true)
                .description((String) body.get("description"))
                .effectiveFrom(body.containsKey("effectiveFrom") ? Instant.parse((String) body.get("effectiveFrom")) : null)
                .effectiveTo(body.containsKey("effectiveTo") ? Instant.parse((String) body.get("effectiveTo")) : null)
                .build();

        rule = loanRuleRepository.save(rule);

        auditService.logAction("CREATE_LOAN_RULE", "LOAN_RULE_CONFIGURATION", rule.getId().toString(),
                null, rule.getRuleValue(), "Created loan rule: " + ruleName);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(rule));
    }

    @PutMapping("/loan-rules/{id}")
    @Operation(summary = "Update loan rule configuration")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<LoanRuleConfiguration>> updateLoanRule(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        LoanRuleConfiguration rule = loanRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanRuleConfiguration", id.toString()));

        String oldValue = rule.getRuleValue();
        if (body.containsKey("ruleValue")) rule.setRuleValue(body.get("ruleValue").toString());
        if (body.containsKey("enabled")) rule.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("description")) rule.setDescription((String) body.get("description"));
        if (body.containsKey("priority")) rule.setPriority(((Number) body.get("priority")).intValue());
        if (body.containsKey("ruleType")) rule.setRuleType((String) body.get("ruleType"));

        rule = loanRuleRepository.save(rule);

        auditService.logAction("CHANGE_LOAN_RULE", "LOAN_RULE_CONFIGURATION", id.toString(),
                oldValue, rule.getRuleValue(), "Updated loan rule: " + rule.getRuleName());

        return ResponseEntity.ok(ApiResponse.success(rule));
    }

    @DeleteMapping("/loan-rules/{id}")
    @Operation(summary = "Delete loan rule configuration")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteLoanRule(@PathVariable UUID id) {
        LoanRuleConfiguration rule = loanRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanRuleConfiguration", id.toString()));

        loanRuleRepository.delete(rule);

        auditService.logAction("DELETE_LOAN_RULE", "LOAN_RULE_CONFIGURATION", id.toString(),
                rule.getRuleName(), null, "Deleted loan rule: " + rule.getRuleName());

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
