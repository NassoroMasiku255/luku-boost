import { useEffect, useRef, useCallback, useState } from "react";

interface UseIdleTimeoutOptions {
  idleTimeMs: number;       // Time before warning (default: 15 min)
  warningTimeMs: number;    // Time after warning before auto-logout (default: 60s)
  onIdle: () => void;       // Called when session expires
}

export function useIdleTimeout({ idleTimeMs, warningTimeMs, onIdle }: UseIdleTimeoutOptions) {
  const [showWarning, setShowWarning] = useState(false);
  const [remainingSeconds, setRemainingSeconds] = useState(0);

  const idleTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const warningTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const remainingRef = useRef(0);

  const clearAllTimers = useCallback(() => {
    if (idleTimerRef.current) clearTimeout(idleTimerRef.current);
    if (warningTimerRef.current) clearTimeout(warningTimerRef.current);
    if (countdownRef.current) clearInterval(countdownRef.current);
  }, []);

  const startWarningCountdown = useCallback(() => {
    const totalSeconds = Math.ceil(warningTimeMs / 1000);
    remainingRef.current = totalSeconds;
    setRemainingSeconds(totalSeconds);
    setShowWarning(true);

    countdownRef.current = setInterval(() => {
      remainingRef.current -= 1;
      setRemainingSeconds(remainingRef.current);
      if (remainingRef.current <= 0) {
        if (countdownRef.current) clearInterval(countdownRef.current);
      }
    }, 1000);

    warningTimerRef.current = setTimeout(() => {
      clearAllTimers();
      setShowWarning(false);
      onIdle();
    }, warningTimeMs);
  }, [warningTimeMs, onIdle, clearAllTimers]);

  const resetIdleTimer = useCallback(() => {
    clearAllTimers();
    setShowWarning(false);
    idleTimerRef.current = setTimeout(startWarningCountdown, idleTimeMs);
  }, [idleTimeMs, startWarningCountdown, clearAllTimers]);

  const stayActive = useCallback(() => {
    resetIdleTimer();
  }, [resetIdleTimer]);

  useEffect(() => {
    const events: (keyof WindowEventMap)[] = [
      "mousemove", "mousedown", "keydown", "scroll", "touchstart", "click",
    ];

    const handleActivity = () => {
      // Only reset if warning is NOT showing — activity during warning is ignored
      // (user must click "Stay Logged In" to dismiss warning)
      if (!showWarning) {
        resetIdleTimer();
      }
    };

    events.forEach((event) => window.addEventListener(event, handleActivity, { passive: true }));

    // Start the initial idle timer
    resetIdleTimer();

    return () => {
      events.forEach((event) => window.removeEventListener(event, handleActivity));
      clearAllTimers();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showWarning]);

  return { showWarning, remainingSeconds, stayActive };
}
