import { useAuth } from "../context/AuthContext";
import { useIdleTimeout } from "../hooks/useIdleTimeout";
import { useNavigate } from "react-router-dom";
import { useCallback } from "react";

const IDLE_TIME_MS = 15 * 60 * 1000;   // 15 minutes idle before warning
const WARNING_TIME_MS = 60 * 1000;      // 60 seconds to respond before auto-logout

export function IdleTimeoutWarning() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleIdle = useCallback(() => {
    logout();
    navigate("/login", { replace: true });
  }, [logout, navigate]);

  const { showWarning, remainingSeconds, stayActive } = useIdleTimeout({
    idleTimeMs: IDLE_TIME_MS,
    warningTimeMs: WARNING_TIME_MS,
    onIdle: handleIdle,
  });

  if (!isAuthenticated || !showWarning) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="bg-background border border-border rounded-xl shadow-2xl max-w-sm w-full mx-4 p-6 text-center">
        <div className="w-14 h-14 mx-auto mb-4 rounded-full bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
          <svg className="w-7 h-7 text-amber-600 dark:text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M12 3a9 9 0 100 18 9 9 0 000-18z" />
          </svg>
        </div>

        <h2 className="text-lg font-semibold mb-1">Session Expiring</h2>
        <p className="text-sm text-muted-foreground mb-4">
          Your session has been idle and will expire in
        </p>

        <div className="text-4xl font-bold text-amber-600 dark:text-amber-400 mb-4 tabular-nums">
          {remainingSeconds}s
        </div>

        <p className="text-xs text-muted-foreground mb-5">
          You will be logged out automatically. Click below to stay logged in.
        </p>

        <div className="flex gap-3">
          <button
            onClick={() => { logout(); navigate("/login", { replace: true }); }}
            className="flex-1 px-4 py-2 text-sm rounded-lg border border-border bg-background text-foreground hover:bg-muted transition-colors"
          >
            Log Out
          </button>
          <button
            onClick={stayActive}
            className="flex-1 px-4 py-2 text-sm rounded-lg bg-primary text-primary-foreground font-medium hover:bg-primary/90 transition-colors"
          >
            Stay Logged In
          </button>
        </div>
      </div>
    </div>
  );
}
