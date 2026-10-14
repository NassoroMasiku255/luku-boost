package com.lukuboost.electricityloan.common.utility;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceGeneratorTest {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String TODAY = LocalDate.now().format(DATE_FMT);

    @Test
    void generateLoanReference_shouldHaveCorrectFormat() {
        String ref = ReferenceGenerator.generateLoanReference();
        assertThat(ref).startsWith("LOAN-" + TODAY + "-");
        assertThat(ref).hasSize("LOAN-".length() + 8 + 1 + 9);
    }

    @Test
    void generateApplicationReference_shouldHaveCorrectFormat() {
        String ref = ReferenceGenerator.generateApplicationReference();
        assertThat(ref).startsWith("APP-" + TODAY + "-");
    }

    @Test
    void generateTokenReference_shouldHaveCorrectFormat() {
        String ref = ReferenceGenerator.generateTokenReference();
        assertThat(ref).startsWith("TOKEN-" + TODAY + "-");
    }

    @Test
    void generateReconReference_shouldHaveCorrectFormat() {
        String ref = ReferenceGenerator.generateReconReference();
        assertThat(ref).startsWith("RECON-" + TODAY + "-");
    }

    @Test
    void generatedReferences_shouldBeUnique() {
        String ref1 = ReferenceGenerator.generateLoanReference();
        String ref2 = ReferenceGenerator.generateLoanReference();
        assertThat(ref1).isNotEqualTo(ref2);
    }

    @Test
    void generateCorrelationId_shouldBeUUID() {
        String id = ReferenceGenerator.generateCorrelationId();
        assertThat(id).hasSize(32); // Long without dashes
        assertThat(id).matches("[a-f0-9]{32}");
    }
}
