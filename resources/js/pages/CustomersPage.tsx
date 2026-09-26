import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import api from "../lib/api";
import { Card, CardHeader, CardContent } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "../components/ui/table";
import { Search } from "lucide-react";

export function CustomersPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-customers", search, page],
    queryFn: () =>
      api
        .get("/admin/customers", { params: { phone: search || undefined, page, size: 20 } })
        .then((r) => r.data.data),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Customers</h1>
        <p className="text-muted-foreground">View and manage customers</p>
      </div>

      <Card>
        <CardHeader>
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by phone number..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="pl-10"
            />
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
                    <TableHead>Name</TableHead>
                    <TableHead>Phone</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Total Loans</TableHead>
                    <TableHead>Joined</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.map((customer: Record<string, unknown>) => (
                    <TableRow key={customer.id as string}>
                      <TableCell>{(customer.firstName as string) || "N/A"} {customer.lastName as string}</TableCell>
                      <TableCell className="font-mono text-xs">{customer.phoneNumber as string}</TableCell>
                      <TableCell>
                        <Badge variant={customer.status === "ACTIVE" ? "success" : "secondary"}>
                          {customer.status as string}
                        </Badge>
                      </TableCell>
                      <TableCell>{(customer.totalLoans as number) || 0}</TableCell>
                      <TableCell>{customer.createdAt ? new Date(customer.createdAt as string).toLocaleDateString() : "-"}</TableCell>
                    </TableRow>
                  ))}
                  {(!data?.content || data.content.length === 0) && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                        No customers found
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>

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
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
