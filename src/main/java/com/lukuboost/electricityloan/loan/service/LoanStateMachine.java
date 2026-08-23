package com.lukuboost.electricityloan.loan.service;

import com.lukuboost.electricityloan.common.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class LoanStateMachine {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry("INITIATED", Set.of("ELIGIBILITY_CHECKED", "FAILED", "CANCELLED", "EXPIRED")),
            Map.entry("ELIGIBILITY_CHECKED", Set.of("OTP_PENDING", "CANCELLED", "EXPIRED")),
            Map.entry("OTP_PENDING", Set.of("OTP_VERIFIED", "CANCELLED", "EXPIRED")),
            Map.entry("OTP_VERIFIED", Set.of("PROCESSING", "CANCELLED", "EXPIRED")),
            Map.entry("PROCESSING", Set.of("TOKEN_REQUESTED", "FAILED", "EXPIRED")),
            Map.entry("TOKEN_REQUESTED", Set.of("TOKEN_RECEIVED", "FAILED", "TIMEOUT", "UNKNOWN")),
            Map.entry("TOKEN_RECEIVED", Set.of("COMPLETED")),
            Map.entry("TIMEOUT", Set.of("TOKEN_RECEIVED", "FAILED", "UNKNOWN")),
            Map.entry("UNKNOWN", Set.of("TOKEN_RECEIVED", "FAILED", "TIMEOUT")),
            Map.entry("COMPLETED", Set.of("REVERSED")),
            Map.entry("FAILED", Set.of()),
            Map.entry("CANCELLED", Set.of()),
            Map.entry("EXPIRED", Set.of()),
            Map.entry("REVERSED", Set.of())
    );

    public void validateTransition(String currentStatus, String targetStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new InvalidStateTransitionException(currentStatus, targetStatus);
        }
    }

    public boolean canTransition(String currentStatus, String targetStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }
}
