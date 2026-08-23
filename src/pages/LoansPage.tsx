import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import api from "../lib/api";
import { Card, CardHeader, CardContent } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "../components/ui/table";
import { Search } from "lucide-react";

function statusVariant(status: string) {
  switch (status) {
    case "ACTIVE":
    case "APPROVED":
      return "success" as const;
    case "COMPLETED":
      return "default" as const;
    case "DEFAULTED":
    case "FAILED":
      return "destructive" as const;
    case "PENDING":
      return "warning" as const;
    default:
      return "secondary" as const;
  }
}

export function LoansPage() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-loans", search, status, page],
    queryFn: () =>
      api
        .get("/admin/loans", { params: { ref: search || undefined, status: status || undefined, page, size: 20 } })
        .then((r) => r.data.data),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Loans</h1>
        <p className="text-muted-foreground">Manage and monitor all loans</p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by loan reference..."
                value={search}
                onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                className="pl-10"
              />
            </div>
            <select
              className="h-9 rounded-md border border-input px-3 text-sm bg-background"
              value={status}
              onChange={(e) => { setStatus(e.target.value); setPage(0); }}
            >
              <option value="">All Status</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="DEFAULTED">Defaulted</option>
              <option value="WRITTEN_OFF">Written Off</option>
            </select>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">Loading...</div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Loan Ref</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Meter</TableHead>
                    <TableHead>Principal</TableHead>
                    <TableHead>Outstanding</TableHead>
                    <TableHead>Due Date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.map((loan: Record<string, unknown>) => (
                    <TableRow key={loan.loanRef as string}>
                      <TableCell className="font-mono text-xs">{loan.loanRef as string}</TableCell>
                      <TableCell>
                        <Badge variant={statusVariant(loan.status as string)}>{loan.status as string}</Badge>
                      </TableCell>
                      <TableCell>{loan.meterNumber as string}</TableCell>
                      <TableCell>TZS {(loan.principalAmount as number)?.toLocaleString()}</TableCell>
                      <TableCell>TZS {(loan.outstandingAmount as number)?.toLocaleString()}</TableCell>
                      <TableCell>{loan.dueAt ? new Date(loan.dueAt as string).toLocaleDateString() : "-"}</TableCell>
                    </TableRow>
                  ))}
                  {(!data?.content || data.content.length === 0) && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                        No loans found
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>

              {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {data.page + 1} of {data.totalPages} ({data.totalElements} total)
                  </p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage((p) => p - 1)}>
                      Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
                      Next
                    </Button>
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
