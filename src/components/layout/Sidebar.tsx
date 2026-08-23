import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Zap,
  Settings,
  ScrollText,
  AlertTriangle,
  UserCog,
  LogOut,
  KeyRound,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard, permission: "LOAN_VIEW" },
  { to: "/loans", label: "Loans", icon: CreditCard, permission: "LOAN_VIEW" },
  { to: "/customers", label: "Customers", icon: Users, permission: "CUSTOMER_VIEW" },
  { to: "/transactions", label: "Transactions", icon: Zap, permission: "TRANSACTION_VIEW" },
  { to: "/reconciliation", label: "Reconciliation", icon: AlertTriangle, permission: "RECONCILIATION_VIEW" },
  { to: "/audit-logs", label: "Audit Logs", icon: ScrollText, permission: "AUDIT_VIEW" },
  { to: "/users", label: "Admin Users", icon: UserCog, permission: "USER_VIEW" },
  { to: "/api-clients", label: "API Clients", icon: KeyRound, permission: "CONFIG_VIEW" },
  { to: "/settings", label: "Settings", icon: Settings, permission: "CONFIG_VIEW" },
];

export function Sidebar() {
  const { user, logout, hasPermission } = useAuth();

  return (
    <aside className="flex flex-col w-64 min-h-screen bg-sidebar-background border-r border-sidebar-border">
      <div className="p-6 border-b border-sidebar-border">
        <h1 className="text-xl font-bold text-sidebar-primary">LUKU Boost</h1>
        <p className="text-xs text-sidebar-foreground mt-1">Admin Portal</p>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems
          .filter((item) => hasPermission(item.permission))
          .map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                  isActive
                    ? "bg-sidebar-accent text-sidebar-accent-foreground font-medium"
                    : "text-sidebar-foreground hover:bg-sidebar-accent/50"
                }`
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
      </nav>

      <div className="p-4 border-t border-sidebar-border">
        <div className="flex items-center gap-3 mb-3">
          <div className="h-8 w-8 rounded-full bg-sidebar-primary flex items-center justify-center text-sidebar-primary-foreground text-xs font-bold">
            {user?.fullName?.charAt(0) || "A"}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-sidebar-foreground truncate">{user?.fullName}</p>
            <p className="text-xs text-muted-foreground truncate">{user?.username}</p>
          </div>
        </div>
        <button
          onClick={logout}
          className="flex items-center gap-2 w-full px-3 py-2 text-sm text-destructive hover:bg-destructive/10 rounded-lg transition-colors"
        >
          <LogOut className="h-4 w-4" />
          Sign Out
        </button>
      </div>
    </aside>
  );
}
