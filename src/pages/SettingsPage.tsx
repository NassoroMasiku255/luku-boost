import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import api from "../lib/api";
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "../components/ui/table";

export function SettingsPage() {
  const [tab, setTab] = useState<"system" | "loan-rules">("system");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="text-muted-foreground">Manage system configurations and loan rules</p>
      </div>

      <div className="flex gap-2 border-b pb-2">
        <button
          onClick={() => setTab("system")}
          className={`px-4 py-2 text-sm rounded-t-lg ${tab === "system" ? "bg-background border border-b-0 font-medium" : "text-muted-foreground"}`}
        >
          System Config
        </button>
        <button
          onClick={() => setTab("loan-rules")}
          className={`px-4 py-2 text-sm rounded-t-lg ${tab === "loan-rules" ? "bg-background border border-b-0 font-medium" : "text-muted-foreground"}`}
        >
          Loan Rules
        </button>
      </div>

      {tab === "system" ? <SystemConfigTab /> : <LoanRulesTab />}
    </div>
  );
}

// ============================================================================
// System Configurations Tab
// ============================================================================

interface SystemConfig {
  id: string;
  configKey: string;
  configValue: string;
  category: string;
  description: string | null;
  valueType: string;
  isSensitive: boolean;
}

const SYSTEM_CATEGORIES = ["OTP", "LOAN", "PROVIDER", "RECONCILIATION", "GENERAL"];
const VALUE_TYPES = ["STRING", "INTEGER", "DECIMAL", "BOOLEAN"];

function SystemConfigTab() {
  const queryClient = useQueryClient();
  const [editId, setEditId] = useState<string | null>(null);
  const [editValue, setEditValue] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [showAddForm, setShowAddForm] = useState(false);
  const [newConfig, setNewConfig] = useState({
    configKey: "", configValue: "", category: "GENERAL", description: "", valueType: "STRING", isSensitive: false,
  });
  const [error, setError] = useState("");

  const { data, isLoading } = useQuery<SystemConfig[]>({
    queryKey: ["system-configs"],
    queryFn: () => api.get("/admin/configurations/system").then((r) => r.data.data),
  });

  const createMutation = useMutation({
    mutationFn: (payload: typeof newConfig) => api.post("/admin/configurations/system", payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["system-configs"] });
      setShowAddForm(false);
      setNewConfig({ configKey: "", configValue: "", category: "GENERAL", description: "", valueType: "STRING", isSensitive: false });
      setError("");
    },
    onError: (err: any) => setError(err.response?.data?.message || "Failed to create configuration"),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, configValue, description }: { id: string; configValue: string; description: string }) =>
      api.put(`/admin/configurations/system/${id}`, { configValue, description }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["system-configs"] });
      setEditId(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/configurations/system/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["system-configs"] }),
  });

  if (isLoading) return <div className="text-center py-8 text-muted-foreground">Loading...</div>;

  const grouped = (data || []).reduce<Record<string, SystemConfig[]>>((acc, c) => {
    const cat = c.category || "OTHER";
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(c);
    return acc;
  }, {});

  return (
    <div className="space-y-4">
      {/* Add New Config */}
      <div className="flex justify-end">
        <Button onClick={() => setShowAddForm(!showAddForm)}>
          {showAddForm ? "Cancel" : "+ Add Configuration"}
        </Button>
      </div>

      {showAddForm && (
        <Card>
          <CardHeader>
            <CardTitle>Add New System Configuration</CardTitle>
          </CardHeader>
          <CardContent>
            {error && <div className="mb-3 text-sm text-destructive bg-destructive/10 p-2 rounded">{error}</div>}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Config Key *</label>
                <Input
                  placeholder="e.g. loan.service.fee.percentage"
                  value={newConfig.configKey}
                  onChange={(e) => setNewConfig({ ...newConfig, configKey: e.target.value })}
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Value *</label>
                <Input
                  placeholder="e.g. 10"
                  value={newConfig.configValue}
                  onChange={(e) => setNewConfig({ ...newConfig, configValue: e.target.value })}
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Category</label>
                <select
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                  value={newConfig.category}
                  onChange={(e) => setNewConfig({ ...newConfig, category: e.target.value })}
                >
                  {SYSTEM_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Value Type</label>
                <select
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                  value={newConfig.valueType}
                  onChange={(e) => setNewConfig({ ...newConfig, valueType: e.target.value })}
                >
                  {VALUE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Description</label>
                <Input
                  placeholder="What does this configuration control?"
                  value={newConfig.description}
                  onChange={(e) => setNewConfig({ ...newConfig, description: e.target.value })}
                />
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="isSensitive"
                  checked={newConfig.isSensitive}
                  onChange={(e) => setNewConfig({ ...newConfig, isSensitive: e.target.checked })}
                  className="rounded"
                />
                <label htmlFor="isSensitive" className="text-sm">Sensitive value (hidden in UI)</label>
              </div>
            </div>
            <div className="mt-4 flex justify-end">
              <Button onClick={() => createMutation.mutate(newConfig)} disabled={!newConfig.configKey || !newConfig.configValue}>
                Create Configuration
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Grouped configs by category */}
      {Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b)).map(([category, configs]) => (
        <Card key={category}>
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2">
              <Badge variant="secondary">{category}</Badge>
              <span className="text-muted-foreground font-normal text-sm">({configs.length} items)</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Key</TableHead>
                  <TableHead>Value</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead className="w-[180px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {configs.map((config) => (
                  <TableRow key={config.id}>
                    <TableCell className="font-mono text-xs">{config.configKey}</TableCell>
                    <TableCell>
                      {editId === config.id ? (
                        <Input
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          className="h-7 text-xs"
                        />
                      ) : (
                        <span className="text-sm font-medium">{config.configValue}</span>
                      )}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground max-w-[200px]">
                      {editId === config.id ? (
                        <Input
                          value={editDescription}
                          onChange={(e) => setEditDescription(e.target.value)}
                          className="h-7 text-xs"
                          placeholder="Description"
                        />
                      ) : (
                        config.description || "-"
                      )}
                    </TableCell>
                    <TableCell className="text-xs">{config.valueType}</TableCell>
                    <TableCell>
                      {editId === config.id ? (
                        <div className="flex gap-1">
                          <Button
                            size="sm"
                            onClick={() => updateMutation.mutate({
                              id: config.id, configValue: editValue, description: editDescription,
                            })}
                          >
                            Save
                          </Button>
                          <Button size="sm" variant="ghost" onClick={() => setEditId(null)}>Cancel</Button>
                        </div>
                      ) : (
                        <div className="flex gap-1">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                              setEditId(config.id);
                              setEditValue(config.configValue);
                              setEditDescription(config.description || "");
                            }}
                          >
                            Edit
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => { if (confirm(`Delete "${config.configKey}"?`)) deleteMutation.mutate(config.id); }}
                          >
                            Delete
                          </Button>
                        </div>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

// ============================================================================
// Loan Rules Tab
// ============================================================================

interface LoanRule {
  id: string;
  ruleName: string;
  ruleType: string;
  ruleValue: string;
  priority: number;
  enabled: boolean;
  description: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

// Predefined rule templates — user selects from these, no JSON needed
const RULE_TEMPLATES = [
  {
    key: "SERVICE_FEE_PERCENTAGE",
    label: "Service Fee / Interest Rate (%)",
    type: "ELIGIBILITY",
    fields: [{ name: "percentage", label: "Percentage", inputType: "number" as const }],
    defaultDescription: "Service fee percentage charged on loan amount",
    priority: 0,
  },
  {
    key: "DEFAULT_ELIGIBILITY_PERCENTAGE",
    label: "Eligibility Percentage (%)",
    type: "ELIGIBILITY",
    fields: [{ name: "percentage", label: "Percentage of last purchase", inputType: "number" as const }],
    defaultDescription: "Eligible loan amount as percentage of last purchase",
    priority: 1,
  },
  {
    key: "MIN_LAST_PURCHASE_AMOUNT",
    label: "Minimum Last Purchase Amount",
    type: "ELIGIBILITY",
    fields: [{ name: "minAmount", label: "Minimum Amount (TZS)", inputType: "number" as const }],
    defaultDescription: "Minimum last purchase amount to qualify for loan",
    priority: 2,
  },
  {
    key: "MAX_DAYS_SINCE_PURCHASE",
    label: "Max Days Since Last Purchase",
    type: "ELIGIBILITY",
    fields: [{ name: "maxDays", label: "Maximum Days", inputType: "number" as const }],
    defaultDescription: "Maximum days since last electricity purchase",
    priority: 3,
  },
  {
    key: "OUTSTANDING_LOAN_CHECK",
    label: "Max Outstanding Loans",
    type: "ELIGIBILITY",
    fields: [{ name: "maxOutstanding", label: "Maximum Outstanding Loans", inputType: "number" as const }],
    defaultDescription: "Maximum number of outstanding loans allowed",
    priority: 4,
  },
  {
    key: "DAILY_LOAN_LIMIT",
    label: "Daily Loan Limit",
    type: "LIMIT",
    fields: [
      { name: "maxLoansPerDay", label: "Max Loans Per Day", inputType: "number" as const },
      { name: "maxAmountPerDay", label: "Max Amount Per Day (TZS)", inputType: "number" as const },
    ],
    defaultDescription: "Daily loan issuance limits",
    priority: 1,
  },
  {
    key: "MONTHLY_LOAN_LIMIT",
    label: "Monthly Loan Limit",
    type: "LIMIT",
    fields: [
      { name: "maxLoansPerMonth", label: "Max Loans Per Month", inputType: "number" as const },
      { name: "maxAmountPerMonth", label: "Max Amount Per Month (TZS)", inputType: "number" as const },
    ],
    defaultDescription: "Monthly loan issuance limits",
    priority: 2,
  },
];

function LoanRulesTab() {
  const queryClient = useQueryClient();
  const [editId, setEditId] = useState<string | null>(null);
  const [editValues, setEditValues] = useState<Record<string, string>>({});
  const [editDescription, setEditDescription] = useState("");
  const [showAddForm, setShowAddForm] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [addValues, setAddValues] = useState<Record<string, string>>({});
  const [addDescription, setAddDescription] = useState("");
  const [error, setError] = useState("");

  const { data, isLoading } = useQuery<LoanRule[]>({
    queryKey: ["loan-rules"],
    queryFn: () => api.get("/admin/configurations/loan-rules").then((r) => r.data.data),
  });

  const createMutation = useMutation({
    mutationFn: (payload: { ruleName: string; ruleType: string; ruleValue: string; priority: number; enabled: boolean; description: string }) =>
      api.post("/admin/configurations/loan-rules", payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["loan-rules"] });
      setShowAddForm(false);
      setSelectedTemplate("");
      setAddValues({});
      setAddDescription("");
      setError("");
    },
    onError: (err: any) => setError(err.response?.data?.message || "Failed to create rule"),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, ...payload }: { id: string; ruleValue?: string; description?: string; priority?: number; enabled?: boolean }) =>
      api.put(`/admin/configurations/loan-rules/${id}`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["loan-rules"] });
      setEditId(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/configurations/loan-rules/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["loan-rules"] }),
  });

  if (isLoading) return <div className="text-center py-8 text-muted-foreground">Loading...</div>;

  const existingRuleNames = (data || []).map((r) => r.ruleName);
  const availableTemplates = RULE_TEMPLATES.filter((t) => !existingRuleNames.includes(t.key));
  const currentTemplate = RULE_TEMPLATES.find((t) => t.key === selectedTemplate);

  const handleCreate = () => {
    if (!currentTemplate) return;
    const ruleValue = JSON.stringify(
      Object.fromEntries(currentTemplate.fields.map((f) => [f.name, Number(addValues[f.name]) || 0]))
    );
    createMutation.mutate({
      ruleName: currentTemplate.key,
      ruleType: currentTemplate.type,
      ruleValue,
      priority: currentTemplate.priority,
      enabled: true,
      description: addDescription || currentTemplate.defaultDescription,
    });
  };

  const handleStartEdit = (rule: LoanRule) => {
    setEditId(rule.id);
    setEditDescription(rule.description || "");
    try {
      const parsed = JSON.parse(rule.ruleValue);
      const values: Record<string, string> = {};
      Object.entries(parsed).forEach(([k, v]) => { values[k] = String(v); });
      setEditValues(values);
    } catch {
      setEditValues({});
    }
  };

  const handleSaveEdit = (rule: LoanRule) => {
    const template = RULE_TEMPLATES.find((t) => t.key === rule.ruleName);
    let ruleValue: string;
    if (template) {
      ruleValue = JSON.stringify(
        Object.fromEntries(template.fields.map((f) => [f.name, Number(editValues[f.name]) || 0]))
      );
    } else {
      ruleValue = JSON.stringify(
        Object.fromEntries(Object.entries(editValues).map(([k, v]) => [k, isNaN(Number(v)) ? v : Number(v)]))
      );
    }
    updateMutation.mutate({ id: rule.id, ruleValue, description: editDescription });
  };

  return (
    <div className="space-y-4">
      {/* Add New Rule */}
      <div className="flex justify-end">
        <Button onClick={() => setShowAddForm(!showAddForm)} disabled={availableTemplates.length === 0}>
          {showAddForm ? "Cancel" : "+ Add Loan Rule"}
        </Button>
      </div>

      {showAddForm && (
        <Card>
          <CardHeader>
            <CardTitle>Add New Loan Rule</CardTitle>
            <CardDescription>Select a rule type and set its value</CardDescription>
          </CardHeader>
          <CardContent>
            {error && <div className="mb-3 text-sm text-destructive bg-destructive/10 p-2 rounded">{error}</div>}
            <div className="space-y-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Select Rule Type *</label>
                <select
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                  value={selectedTemplate}
                  onChange={(e) => {
                    setSelectedTemplate(e.target.value);
                    setAddValues({});
                    const tpl = RULE_TEMPLATES.find((t) => t.key === e.target.value);
                    setAddDescription(tpl?.defaultDescription || "");
                  }}
                >
                  <option value="">-- Select a rule to add --</option>
                  {availableTemplates.map((t) => (
                    <option key={t.key} value={t.key}>{t.label}</option>
                  ))}
                </select>
              </div>

              {currentTemplate && (
                <>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {currentTemplate.fields.map((field) => (
                      <div key={field.name}>
                        <label className="text-xs font-medium text-muted-foreground mb-1 block">{field.label} *</label>
                        <Input
                          type={field.inputType}
                          placeholder={`Enter ${field.label.toLowerCase()}`}
                          value={addValues[field.name] || ""}
                          onChange={(e) => setAddValues({ ...addValues, [field.name]: e.target.value })}
                        />
                      </div>
                    ))}
                  </div>
                  <div>
                    <label className="text-xs font-medium text-muted-foreground mb-1 block">Description</label>
                    <Input
                      value={addDescription}
                      onChange={(e) => setAddDescription(e.target.value)}
                    />
                  </div>
                  <div className="flex justify-end">
                    <Button
                      onClick={handleCreate}
                      disabled={currentTemplate.fields.some((f) => !addValues[f.name])}
                    >
                      Create Rule
                    </Button>
                  </div>
                </>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Loan Rules Table */}
      <Card>
        <CardHeader>
          <CardTitle>Loan Rule Configurations</CardTitle>
          <CardDescription>
            These rules control loan eligibility, limits, and interest rates. Changes take effect immediately.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Rule</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Value</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-[220px]">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(data || [])
                .sort((a, b) => a.priority - b.priority)
                .map((rule) => {
                const template = RULE_TEMPLATES.find((t) => t.key === rule.ruleName);
                return (
                <TableRow key={rule.id}>
                  <TableCell className="font-medium text-sm">
                    {template?.label || rule.ruleName}
                  </TableCell>
                  <TableCell><Badge variant="secondary">{rule.ruleType}</Badge></TableCell>
                  <TableCell>
                    {editId === rule.id ? (
                      <div className="space-y-2">
                        {template ? (
                          template.fields.map((field) => (
                            <div key={field.name} className="flex items-center gap-2">
                              <span className="text-xs text-muted-foreground whitespace-nowrap">{field.label}:</span>
                              <Input
                                type={field.inputType}
                                value={editValues[field.name] || ""}
                                onChange={(e) => setEditValues({ ...editValues, [field.name]: e.target.value })}
                                className="h-7 text-xs w-24"
                              />
                            </div>
                          ))
                        ) : (
                          Object.entries(editValues).map(([k]) => (
                            <div key={k} className="flex items-center gap-2">
                              <span className="text-xs text-muted-foreground">{k}:</span>
                              <Input
                                value={editValues[k] || ""}
                                onChange={(e) => setEditValues({ ...editValues, [k]: e.target.value })}
                                className="h-7 text-xs w-24"
                              />
                            </div>
                          ))
                        )}
                      </div>
                    ) : (
                      <RuleValueDisplay value={rule.ruleValue} template={template} />
                    )}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground max-w-[180px]">
                    {editId === rule.id ? (
                      <Input
                        value={editDescription}
                        onChange={(e) => setEditDescription(e.target.value)}
                        className="h-7 text-xs"
                      />
                    ) : (
                      rule.description || "-"
                    )}
                  </TableCell>
                  <TableCell>
                    <Badge variant={rule.enabled ? "success" : "destructive"}>
                      {rule.enabled ? "Enabled" : "Disabled"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {editId === rule.id ? (
                      <div className="flex gap-1 flex-wrap">
                        <Button size="sm" onClick={() => handleSaveEdit(rule)}>Save</Button>
                        <Button size="sm" variant="ghost" onClick={() => setEditId(null)}>Cancel</Button>
                      </div>
                    ) : (
                      <div className="flex gap-1 flex-wrap">
                        <Button size="sm" variant="outline" onClick={() => handleStartEdit(rule)}>Edit</Button>
                        <Button
                          size="sm"
                          variant={rule.enabled ? "secondary" : "default"}
                          onClick={() => updateMutation.mutate({ id: rule.id, enabled: !rule.enabled })}
                        >
                          {rule.enabled ? "Disable" : "Enable"}
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => { if (confirm(`Delete rule "${template?.label || rule.ruleName}"?`)) deleteMutation.mutate(rule.id); }}
                        >
                          Delete
                        </Button>
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              )})}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

function RuleValueDisplay({ value, template }: { value: string; template?: typeof RULE_TEMPLATES[number] }) {
  try {
    const parsed = JSON.parse(value);
    return (
      <div className="text-xs space-y-0.5">
        {Object.entries(parsed).map(([k, v]) => {
          const field = template?.fields.find((f) => f.name === k);
          return (
            <div key={k} className="flex gap-1">
              <span className="text-muted-foreground">{field?.label || k}:</span>
              <span className="font-semibold">{String(v)}</span>
            </div>
          );
        })}
      </div>
    );
  } catch {
    return <span className="font-mono text-xs">{value}</span>;
  }
}
