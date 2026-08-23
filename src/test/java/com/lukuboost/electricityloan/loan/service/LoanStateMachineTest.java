package com.lukuboost.electricityloan.loan.service;

import com.lukuboost.electricityloan.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanStateMachineTest {

    private final LoanStateMachine stateMachine = new LoanStateMachine();

    @ParameterizedTest
    @CsvSource({
            "INITIATED, OTP_PENDING",
            "OTP_PENDING, OTP_VERIFIED",
            "OTP_VERIFIED, ELIGIBILITY_CHECKING",
            "ELIGIBILITY_CHECKING, ELIGIBLE",
            "ELIGIBILITY_CHECKING, NOT_ELIGIBLE",
            "ELIGIBILITY_CHECKING, FAILED",
            "ELIGIBLE, AMOUNT_SELECTED",
            "AMOUNT_SELECTED, PROCESSING",
            "PROCESSING, TOKEN_REQUESTED",
            "TOKEN_REQUESTED, TOKEN_RECEIVED",
            "TOKEN_REQUESTED, FAILED",
            "TOKEN_REQUESTED, UNKNOWN",
            "TOKEN_RECEIVED, COMPLETED",
    })
    void validTransitions_shouldNotThrow(String from, String to) {
        assertThatNoException().isThrownBy(() -> stateMachine.validateTransition(from, to));
    }

    @Test
    void invalidTransition_shouldThrow() {
        assertThatThrownBy(() -> stateMachine.validateTransition("INITIATED", "COMPLETED"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void invalidTransitionFromCompleted_shouldThrow() {
        assertThatThrownBy(() -> stateMachine.validateTransition("COMPLETED", "PROCESSING"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void isValidTransition_shouldReturnTrue() {
        assertThatNoException().isThrownBy(() ->
                stateMachine.validateTransition("ELIGIBLE", "AMOUNT_SELECTED"));
    }

    @Test
    void isValidTransition_shouldReturnFalseForInvalid() {
        assertThatThrownBy(() -> stateMachine.validateTransition("OTP_PENDING", "COMPLETED"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
