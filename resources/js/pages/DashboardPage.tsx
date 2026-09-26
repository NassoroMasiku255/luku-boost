import { useQuery } from "@tanstack/react-query";
import api from "../lib/api";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import {
  Users,
  CreditCard,
  TrendingUp,
  AlertTriangle,
  Zap,
  Clock,
} from "lucide-react";

interface DashboardSummary {
  totalCustomers: number;
  activeCustomers: number;
  totalLoans: number;
  loansToday: number;
  loansThisMonth: number;
  totalAmountIssued: number;
  totalOutstanding: number;
  totalRepaid: number;
  failedTransactions: number;
  pendingTransactions: number;
  successfulElectricityPurchases: number;
  failedElectricityPurchases: number;
  otpRequestsToday: number;
  pendingReconciliations: number;
}

function formatCurrency(amount: number | null | undefined): string {
  if (amount == null) return "TZS 0";
  return `TZS ${amount.toLocaleString()}`;
}

export function DashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["dashboard-summary"],
    queryFn: () => api.get("/admin/dashboard/summary").then((r) => r.data.data as DashboardSummary),
    refetchInterval: 30000,
  });

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-muted-foreground">Loading dashboard...</div>;
  }

  if (isError || !data) {
    return <div className="flex items-center justify-center h-64 text-muted-foreground">Failed to load dashboard. Please refresh or log in again.</div>;
  }

  const stats = data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">Platform overview and real-time statistics</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Customers"
          value={stats.totalCustomers}
          subtitle={`${stats.activeCustomers} active`}
          icon={<Users className="h-4 w-4" />}
        />
        <StatCard
          title="Total Loans"
          value={stats.totalLoans}
          subtitle={`${stats.loansToday} today`}
          icon={<CreditCard className="h-4 w-4" />}
        />
        <StatCard
          title="Amount Issued"
          value={formatCurrency(stats.totalAmountIssued)}
          subtitle={`${formatCurrency(stats.totalOutstanding)} outstanding`}
          icon={<TrendingUp className="h-4 w-4" />}
        />
        <StatCard
          title="Total Repaid"
          value={formatCurrency(stats.totalRepaid)}
          subtitle={`${stats.loansThisMonth} loans this month`}
          icon={<TrendingUp className="h-4 w-4" />}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Electricity Purchases</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-sm">Successful</span>
                <Badge variant="success">{stats.successfulElectricityPurchases}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Failed</span>
                <Badge variant="destructive">{stats.failedElectricityPurchases}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Pending</span>
                <Badge variant="warning">{stats.pendingTransactions}</Badge>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Attention Required</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-sm">Failed Transactions</span>
                <Badge variant="destructive">{stats.failedTransactions}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Pending Reconciliation</span>
                <Badge variant="warning">{stats.pendingReconciliations}</Badge>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Today's Activity</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-sm">Loans Issued</span>
                <Badge>{stats.loansToday}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">OTP Requests</span>
                <Badge variant="secondary">{stats.otpRequestsToday}</Badge>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function StatCard({
  title,
  value,
  subtitle,
  icon,
}: {
  title: string;
  value: string | number;
  subtitle: string;
  icon: React.ReactNode;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        {icon}
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        <p className="text-xs text-muted-foreground">{subtitle}</p>
      </CardContent>
    </Card>
  );
}
