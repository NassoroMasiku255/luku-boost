import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import api from "../lib/api";
import { Card, CardHeader, CardContent } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "../components/ui/table";

type TabKey = "electricity" | "loan" | "failed";

export function TransactionsPage() {
  const [tab, setTab] = useState<TabKey>("electricity");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-transactions", tab, page],
    queryFn: () =>
      api.get(`/admin/transactions/${tab}`, { params: { page, size: 20 } }).then((r) => r.data.data),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Transactions</h1>
        <p className="text-muted-foreground">Monitor electricity purchases and loan transactions</p>
      </div>

      <div className="flex gap-2 border-b pb-2">
        {(["electricity", "loan", "failed"] as TabKey[]).map((t) => (
          <button
            key={t}
            onClick={() => { setTab(t); setPage(0); }}
            className={`px-4 py-2 text-sm rounded-t-lg transition-colors ${
              tab === t ? "bg-background border border-b-0 font-medium" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {t === "electricity" ? "Electricity" : t === "loan" ? "Loan" : "Failed"}
          </button>
        ))}
      </div>

      <Card>
        <CardHeader />
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">Loading...</div>
          ) : tab === "electricity" ? (
            <ElectricityTable data={data} />
          ) : tab === "loan" ? (
            <LoanTransactionTable data={data} />
          ) : (
            <FailedTable data={data} />
          )}

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-muted-foreground">
                Page {data.page + 1} of {data.totalPages}
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
        </CardContent>
      </Card>
    </div>
  );
}

function ElectricityTable({ data }: { data: Record<string, unknown> }) {
  const items = (data?.content || []) as Record<string, unknown>[];
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Reference</TableHead>
          <TableHead>Meter</TableHead>
          <TableHead>Amount</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Token</TableHead>
          <TableHead>Date</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((txn) => (
          <TableRow key={txn.internalRef as string}>
            <TableCell className="font-mono text-xs">{txn.internalRef as string}</TableCell>
            <TableCell>{txn.meterNumber as string}</TableCell>
            <TableCell>TZS {(txn.amount as number)?.toLocaleString()}</TableCell>
            <TableCell>
              <Badge variant={txn.requestStatus === "SUCCESS" ? "success" : txn.requestStatus === "FAILED" ? "destructive" : "warning"}>
                {txn.requestStatus as string}
              </Badge>
            </TableCell>
            <TableCell className="font-mono text-xs">{(txn.token as string) || "-"}</TableCell>
            <TableCell>{txn.createdAt ? new Date(txn.createdAt as string).toLocaleString() : "-"}</TableCell>
          </TableRow>
        ))}
        {items.length === 0 && (
          <TableRow><TableCell colSpan={6} className="text-center py-8 text-muted-foreground">No transactions found</TableCell></TableRow>
        )}
      </TableBody>
    </Table>
  );
}

function LoanTransactionTable({ data }: { data: Record<string, unknown> }) {
  const items = (data?.content || []) as Record<string, unknown>[];
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Reference</TableHead>
          <TableHead>Type</TableHead>
          <TableHead>Amount</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Date</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((txn) => (
          <TableRow key={txn.transactionRef as string}>
            <TableCell className="font-mono text-xs">{txn.transactionRef as string}</TableCell>
            <TableCell>{txn.type as string}</TableCell>
            <TableCell>TZS {(txn.amount as number)?.toLocaleString()}</TableCell>
            <TableCell>
              <Badge variant={txn.status === "COMPLETED" ? "success" : "warning"}>{txn.status as string}</Badge>
            </TableCell>
            <TableCell>{txn.createdAt ? new Date(txn.createdAt as string).toLocaleString() : "-"}</TableCell>
          </TableRow>
        ))}
        {items.length === 0 && (
          <TableRow><TableCell colSpan={5} className="text-center py-8 text-muted-foreground">No transactions found</TableCell></TableRow>
        )}
      </TableBody>
    </Table>
  );
}

function FailedTable({ data }: { data: Record<string, unknown> }) {
  const items = (data?.content || []) as Record<string, unknown>[];
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Original Ref</TableHead>
          <TableHead>Type</TableHead>
          <TableHead>Reason</TableHead>
          <TableHead>Retries</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Date</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((txn, i) => (
          <TableRow key={i}>
            <TableCell className="font-mono text-xs">{txn.originalRef as string}</TableCell>
            <TableCell>{txn.transactionType as string}</TableCell>
            <TableCell className="max-w-[200px] truncate">{txn.failureReason as string}</TableCell>
            <TableCell>{txn.retryCount as number}/{txn.maxRetries as number}</TableCell>
            <TableCell>
              <Badge variant={txn.status === "RESOLVED" ? "success" : "destructive"}>{txn.status as string}</Badge>
            </TableCell>
            <TableCell>{txn.createdAt ? new Date(txn.createdAt as string).toLocaleString() : "-"}</TableCell>
          </TableRow>
        ))}
        {items.length === 0 && (
          <TableRow><TableCell colSpan={6} className="text-center py-8 text-muted-foreground">No failed transactions</TableCell></TableRow>
        )}
      </TableBody>
    </Table>
  );
}
