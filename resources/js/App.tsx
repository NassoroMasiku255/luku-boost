import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "./context/AuthContext";
import { AdminLayout } from "./components/layout/AdminLayout";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { LoansPage } from "./pages/LoansPage";
import { CustomersPage } from "./pages/CustomersPage";
import { TransactionsPage } from "./pages/TransactionsPage";
import { ReconciliationPage } from "./pages/ReconciliationPage";
import { AuditLogsPage } from "./pages/AuditLogsPage";
import { AdminUsersPage } from "./pages/AdminUsersPage";
import { SettingsPage } from "./pages/SettingsPage";
import { ApiClientsPage } from "./pages/ApiClientsPage";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<AdminLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/loans" element={<LoansPage />} />
              <Route path="/customers" element={<CustomersPage />} />
              <Route path="/transactions" element={<TransactionsPage />} />
              <Route path="/reconciliation" element={<ReconciliationPage />} />
              <Route path="/audit-logs" element={<AuditLogsPage />} />
              <Route path="/users" element={<AdminUsersPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/api-clients" element={<ApiClientsPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
