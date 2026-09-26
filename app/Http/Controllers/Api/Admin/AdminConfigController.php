<?php

namespace App\Http\Controllers\Api\Admin;

use App\Exceptions\BusinessException;
use App\Http\Controllers\Controller;
use App\Models\LoanRuleConfiguration;
use App\Models\SystemConfiguration;
use App\Services\AuditService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminConfigController extends Controller
{
    protected AuditService $auditService;

    public function __construct(AuditService $auditService)
    {
        $this->auditService = $auditService;
    }

    // System Configurations
    public function listSystemConfigs(): JsonResponse
    {
        $configs = SystemConfiguration::all();
        $configs->each(function ($c) {
            if ($c->is_sensitive) $c->config_value = '********';
        });
        return response()->json(['success' => true, 'error' => null, 'data' => $configs]);
    }

    public function getByCategory(string $category): JsonResponse
    {
        $configs = SystemConfiguration::where('category', $category)->get();
        return response()->json(['success' => true, 'error' => null, 'data' => $configs]);
    }

    public function createSystemConfig(Request $request): JsonResponse
    {
        $configKey = $request->input('configKey');
        if (!$configKey) {
            throw new BusinessException('INVALID_CONFIG_KEY', 'Config key is required.', 400);
        }
        if (SystemConfiguration::where('config_key', $configKey)->exists()) {
            throw new BusinessException('CONFIG_KEY_EXISTS', "Configuration with key '{$configKey}' already exists.", 409);
        }

        $config = SystemConfiguration::create([
            'config_key' => $configKey,
            'config_value' => $request->input('configValue', ''),
            'category' => $request->input('category', 'GENERAL'),
            'description' => $request->input('description'),
            'value_type' => $request->input('valueType', 'STRING'),
            'is_sensitive' => (bool) $request->input('isSensitive', false),
        ]);

        $this->auditService->logAction('CONFIGURATION_CREATE', 'SYSTEM_CONFIGURATION', $config->id,
            null, $config->config_value, "Created config: {$configKey}");

        return response()->json(['success' => true, 'error' => null, 'data' => $config], 201);
    }

    public function updateSystemConfig(Request $request, string $id): JsonResponse
    {
        $config = SystemConfiguration::findOrFail($id);
        $oldValue = $config->config_value;

        if ($request->has('configValue')) $config->config_value = $request->input('configValue');
        if ($request->has('value')) $config->config_value = $request->input('value');
        if ($request->has('description')) $config->description = $request->input('description');
        if ($request->has('category')) $config->category = $request->input('category');
        if ($request->has('valueType')) $config->value_type = $request->input('valueType');
        if ($request->has('isSensitive')) $config->is_sensitive = (bool) $request->input('isSensitive');

        $config->save();

        $this->auditService->logAction('CONFIGURATION_CHANGE', 'SYSTEM_CONFIGURATION', $id,
            json_encode(['value' => $config->is_sensitive ? '****' : $oldValue]),
            json_encode(['value' => $config->is_sensitive ? '****' : $config->config_value]),
            "Updated config: {$config->config_key}");

        return response()->json(['success' => true, 'error' => null, 'data' => $config]);
    }

    public function deleteSystemConfig(string $id): JsonResponse
    {
        $config = SystemConfiguration::findOrFail($id);
        $config->delete();

        $this->auditService->logAction('CONFIGURATION_DELETE', 'SYSTEM_CONFIGURATION', $id,
            $config->config_key, null, "Deleted config: {$config->config_key}");

        return response()->json(['success' => true, 'error' => null, 'data' => null]);
    }

    // Loan Rule Configurations
    public function listLoanRules(): JsonResponse
    {
        return response()->json(['success' => true, 'error' => null, 'data' => LoanRuleConfiguration::all()]);
    }

    public function createLoanRule(Request $request): JsonResponse
    {
        $ruleName = $request->input('ruleName');
        if (!$ruleName) {
            throw new BusinessException('INVALID_RULE_NAME', 'Rule name is required.', 400);
        }
        if (LoanRuleConfiguration::where('rule_name', $ruleName)->exists()) {
            throw new BusinessException('RULE_NAME_EXISTS', "Loan rule with name '{$ruleName}' already exists.", 409);
        }

        $rule = LoanRuleConfiguration::create([
            'rule_name' => $ruleName,
            'rule_type' => $request->input('ruleType', 'ELIGIBILITY'),
            'rule_value' => $request->input('ruleValue', '{}'),
            'priority' => (int) $request->input('priority', 0),
            'enabled' => $request->input('enabled', true),
            'description' => $request->input('description'),
            'effective_from' => $request->input('effectiveFrom'),
            'effective_to' => $request->input('effectiveTo'),
        ]);

        $this->auditService->logAction('CREATE_LOAN_RULE', 'LOAN_RULE_CONFIGURATION', $rule->id,
            null, $rule->rule_value, "Created loan rule: {$ruleName}");

        return response()->json(['success' => true, 'error' => null, 'data' => $rule], 201);
    }

    public function updateLoanRule(Request $request, string $id): JsonResponse
    {
        $rule = LoanRuleConfiguration::findOrFail($id);
        $oldValue = $rule->rule_value;

        if ($request->has('ruleValue')) $rule->rule_value = $request->input('ruleValue');
        if ($request->has('enabled')) $rule->enabled = (bool) $request->input('enabled');
        if ($request->has('description')) $rule->description = $request->input('description');
        if ($request->has('priority')) $rule->priority = (int) $request->input('priority');
        if ($request->has('ruleType')) $rule->rule_type = $request->input('ruleType');

        $rule->save();

        $this->auditService->logAction('CHANGE_LOAN_RULE', 'LOAN_RULE_CONFIGURATION', $id,
            $oldValue, $rule->rule_value, "Updated loan rule: {$rule->rule_name}");

        return response()->json(['success' => true, 'error' => null, 'data' => $rule]);
    }

    public function deleteLoanRule(string $id): JsonResponse
    {
        $rule = LoanRuleConfiguration::findOrFail($id);
        $rule->delete();

        $this->auditService->logAction('DELETE_LOAN_RULE', 'LOAN_RULE_CONFIGURATION', $id,
            $rule->rule_name, null, "Deleted loan rule: {$rule->rule_name}");

        return response()->json(['success' => true, 'error' => null, 'data' => null]);
    }
}
