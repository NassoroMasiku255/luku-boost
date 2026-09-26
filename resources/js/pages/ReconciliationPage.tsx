import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "../lib/api";
import { Card, CardHeader, CardContent } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "../components/ui/table";

export function ReconciliationPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin-reconciliation", page],
    queryFn: () => api.get("/admin/reconciliation", { params: { page, size: 20 } }).then((r) => r.data.data),
  });

  const resolveMutation = useMutation({
    mutationFn: (id: string) =>
      api.patch(`/admin/reconciliation/${id}/resolve`, { status: "RESOLVED", notes: "Manually resolved" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-reconciliation"] }),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Reconciliation</h1>
        <p className="text-muted-foreground">Manage mismatched and unresolved transactions</p>
      </div>

      <Card>
        <CardHeader />
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">Loading...</div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Recon Ref</TableHead>
                    <TableHead>Internal Ref</TableHead>
                    <TableHead>Internal Status</TableHead>
                    <TableHead>Provider Status</TableHead>
                    <TableHead>Mismatch</TableHead>
                    <TableHead>Resolution</TableHead>
                    <TableHead>Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(data?.content || []).map((rec: Record<string, unknown>) => (
                    <TableRow key={rec.id as string}>
                      <TableCell className="font-mono text-xs">{rec.reconRef as string}</TableCell>
                      <TableCell className="font-mono text-xs">{rec.internalRef as string}</TableCell>
                      <TableCell>{rec.internalStatus as string}</TableCell>
                      <TableCell>{rec.providerStatus as string}</TableCell>
                      <TableCell>
                        <Badge variant="warning">{rec.mismatchType as string}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={rec.resolutionStatus === "RESOLVED" ? "success" : "secondary"}>
                          {rec.resolutionStatus as string}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {rec.resolutionStatus === "PENDING" && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => resolveMutation.mutate(rec.id as string)}
                            disabled={resolveMutation.isPending}
                          >
                            Resolve
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {(!data?.content || data.content.length === 0) && (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                        No reconciliation records
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>

              {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">Page {data.page + 1} of {data.totalPages}</p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage((p) => p - 1)}>Previous</Button>
                    <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>Next</Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
