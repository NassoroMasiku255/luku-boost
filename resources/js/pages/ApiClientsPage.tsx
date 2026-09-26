import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import api from "../lib/api";
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "../components/ui/table";

interface ApiClient {
  id: string;
  clientName: string;
  clientId: string;
  description: string | null;
  allowedIps: string | null;
  rateLimitPerMinute: number;
  enabled: boolean;
  lastUsedAt: string | null;
  createdAt: string;
}

interface NewClientResponse {
  id: string;
  clientName: string;
  clientId: string;
  apiKey: string;
  description: string | null;
  allowedIps: string | null;
  rateLimitPerMinute: number;
  enabled: boolean;
  message: string;
}

export function ApiClientsPage() {
  const queryClient = useQueryClient();
  const [showAddForm, setShowAddForm] = useState(false);
  const [newClient, setNewClient] = useState({
    clientName: "", description: "", allowedIps: "", rateLimitPerMinute: 60,
  });
  const [createdCredentials, setCreatedCredentials] = useState<NewClientResponse | null>(null);
  const [regeneratedKey, setRegeneratedKey] = useState<{ clientId: string; apiKey: string } | null>(null);
  const [editId, setEditId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState({ clientName: "", description: "", allowedIps: "", rateLimitPerMinute: 60 });
  const [error, setError] = useState("");

  const { data: clients, isLoading } = useQuery<ApiClient[]>({
    queryKey: ["api-clients"],
    queryFn: () => api.get("/admin/api-clients").then((r) => r.data.data),
  });

  const createMutation = useMutation({
    mutationFn: (payload: typeof newClient) =>
      api.post("/admin/api-clients", payload).then((r) => r.data.data as NewClientResponse),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["api-clients"] });
      setCreatedCredentials(data);
      setShowAddForm(false);
      setNewClient({ clientName: "", description: "", allowedIps: "", rateLimitPerMinute: 60 });
      setError("");
    },
    onError: (err: any) => setError(err.response?.data?.message || "Failed to create client"),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, ...payload }: { id: string; clientName?: string; description?: string; allowedIps?: string; enabled?: boolean; rateLimitPerMinute?: number }) =>
      api.put(`/admin/api-clients/${id}`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["api-clients"] });
      setEditId(null);
    },
  });

  const regenerateMutation = useMutation({
    mutationFn: (id: string) =>
      api.post(`/admin/api-clients/${id}/regenerate-key`).then((r) => r.data.data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["api-clients"] });
      setRegeneratedKey(data);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/api-clients/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["api-clients"] }),
  });

  if (isLoading) return <div className="text-center py-8 text-muted-foreground">Loading...</div>;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">API Clients</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Manage API keys for mobile apps and third-party integrations accessing the Customer API.
        </p>
      </div>

      {/* Credentials Display — shown once after create or regenerate */}
      {createdCredentials && (
        <Card className="border-amber-500 bg-amber-50 dark:bg-amber-950/20">
          <CardHeader>
            <CardTitle className="text-amber-700 dark:text-amber-400">New API Client Created</CardTitle>
            <CardDescription className="text-amber-600 dark:text-amber-500">
              Save these credentials now. The API key will NOT be shown again.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div>
              <label className="text-xs font-medium text-muted-foreground block mb-1">Client ID</label>
              <code className="block bg-background border rounded px-3 py-2 text-sm font-mono select-all">
                {createdCredentials.clientId}
              </code>
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground block mb-1">API Key</label>
              <code className="block bg-background border rounded px-3 py-2 text-sm font-mono select-all break-all">
                {createdCredentials.apiKey}
              </code>
            </div>
            <Button size="sm" variant="outline" onClick={() => setCreatedCredentials(null)}>
              I have saved the credentials
            </Button>
          </CardContent>
        </Card>
      )}

      {regeneratedKey && (
        <Card className="border-amber-500 bg-amber-50 dark:bg-amber-950/20">
          <CardHeader>
            <CardTitle className="text-amber-700 dark:text-amber-400">API Key Regenerated</CardTitle>
            <CardDescription className="text-amber-600 dark:text-amber-500">
              The old key is now invalid. Save the new key — it will NOT be shown again.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div>
              <label className="text-xs font-medium text-muted-foreground block mb-1">Client ID</label>
              <code className="block bg-background border rounded px-3 py-2 text-sm font-mono select-all">
                {regeneratedKey.clientId}
              </code>
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground block mb-1">New API Key</label>
              <code className="block bg-background border rounded px-3 py-2 text-sm font-mono select-all break-all">
                {regeneratedKey.apiKey}
              </code>
            </div>
            <Button size="sm" variant="outline" onClick={() => setRegeneratedKey(null)}>
              I have saved the key
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Add Form */}
      <div className="flex justify-end">
        <Button onClick={() => { setShowAddForm(!showAddForm); setCreatedCredentials(null); setRegeneratedKey(null); }}>
          {showAddForm ? "Cancel" : "+ Create API Client"}
        </Button>
      </div>

      {showAddForm && (
        <Card>
          <CardHeader>
            <CardTitle>Create New API Client</CardTitle>
            <CardDescription>A Client ID and API Key will be generated automatically</CardDescription>
          </CardHeader>
          <CardContent>
            {error && <div className="mb-3 text-sm text-destructive bg-destructive/10 p-2 rounded">{error}</div>}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Client Name *</label>
                <Input
                  placeholder="e.g. LUKU Boost Mobile App"
                  value={newClient.clientName}
                  onChange={(e) => setNewClient({ ...newClient, clientName: e.target.value })}
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Rate Limit (req/min)</label>
                <Input
                  type="number"
                  value={newClient.rateLimitPerMinute}
                  onChange={(e) => setNewClient({ ...newClient, rateLimitPerMinute: parseInt(e.target.value) || 60 })}
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Allowed IPs (comma-separated, empty = all)</label>
                <Input
                  placeholder="e.g. 192.168.1.1,10.0.0.1 or leave empty"
                  value={newClient.allowedIps}
                  onChange={(e) => setNewClient({ ...newClient, allowedIps: e.target.value })}
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Description</label>
                <Input
                  placeholder="What is this client used for?"
                  value={newClient.description}
                  onChange={(e) => setNewClient({ ...newClient, description: e.target.value })}
                />
              </div>
            </div>
            <div className="mt-4 flex justify-end">
              <Button onClick={() => createMutation.mutate(newClient)} disabled={!newClient.clientName}>
                Create Client
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Clients Table */}
      <Card>
        <CardHeader>
          <CardTitle>Registered API Clients</CardTitle>
          <CardDescription>
            Each client uses X-Client-Id and X-Api-Key headers to authenticate with the Customer API.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {(clients || []).length === 0 ? (
            <div className="text-center py-8 text-muted-foreground text-sm">
              No API clients registered yet. Create one to get started.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Client Name</TableHead>
                  <TableHead>Client ID</TableHead>
                  <TableHead>Rate Limit</TableHead>
                  <TableHead>Allowed IPs</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Last Used</TableHead>
                  <TableHead className="w-[280px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(clients || []).map((client) => (
                  <TableRow key={client.id}>
                    <TableCell className="font-medium text-sm">
                      {editId === client.id ? (
                        <Input
                          value={editForm.clientName}
                          onChange={(e) => setEditForm({ ...editForm, clientName: e.target.value })}
                          className="h-7 text-xs"
                        />
                      ) : (
                        <>
                          {client.clientName}
                          {client.description && (
                            <p className="text-xs text-muted-foreground mt-0.5">{client.description}</p>
                          )}
                        </>
                      )}
                    </TableCell>
                    <TableCell>
                      <code className="text-xs font-mono bg-muted px-1.5 py-0.5 rounded select-all">
                        {client.clientId}
                      </code>
                    </TableCell>
                    <TableCell className="text-sm">
                      {editId === client.id ? (
                        <Input
                          type="number"
                          value={editForm.rateLimitPerMinute}
                          onChange={(e) => setEditForm({ ...editForm, rateLimitPerMinute: parseInt(e.target.value) || 60 })}
                          className="h-7 text-xs w-20"
                        />
                      ) : (
                        `${client.rateLimitPerMinute}/min`
                      )}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {editId === client.id ? (
                        <Input
                          value={editForm.allowedIps}
                          onChange={(e) => setEditForm({ ...editForm, allowedIps: e.target.value })}
                          className="h-7 text-xs"
                          placeholder="All IPs"
                        />
                      ) : (
                        client.allowedIps || "All"
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={client.enabled ? "success" : "destructive"}>
                        {client.enabled ? "Active" : "Disabled"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {client.lastUsedAt
                        ? new Date(client.lastUsedAt).toLocaleString()
                        : "Never"}
                    </TableCell>
                    <TableCell>
                      {editId === client.id ? (
                        <div className="flex gap-1 flex-wrap">
                          <Button
                            size="sm"
                            onClick={() => updateMutation.mutate({
                              id: client.id,
                              clientName: editForm.clientName,
                              description: editForm.description,
                              allowedIps: editForm.allowedIps,
                              rateLimitPerMinute: editForm.rateLimitPerMinute,
                            })}
                          >
                            Save
                          </Button>
                          <Button size="sm" variant="ghost" onClick={() => setEditId(null)}>Cancel</Button>
                        </div>
                      ) : (
                        <div className="flex gap-1 flex-wrap">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                              setEditId(client.id);
                              setEditForm({
                                clientName: client.clientName,
                                description: client.description || "",
                                allowedIps: client.allowedIps || "",
                                rateLimitPerMinute: client.rateLimitPerMinute,
                              });
                            }}
                          >
                            Edit
                          </Button>
                          <Button
                            size="sm"
                            variant={client.enabled ? "secondary" : "default"}
                            onClick={() => updateMutation.mutate({ id: client.id, enabled: !client.enabled })}
                          >
                            {client.enabled ? "Disable" : "Enable"}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                              if (confirm(`Regenerate API key for "${client.clientName}"? The old key will stop working immediately.`))
                                regenerateMutation.mutate(client.id);
                            }}
                          >
                            Regen Key
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => {
                              if (confirm(`Delete API client "${client.clientName}"? All requests using this client will fail.`))
                                deleteMutation.mutate(client.id);
                            }}
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
          )}
        </CardContent>
      </Card>
    </div>
  );
}
