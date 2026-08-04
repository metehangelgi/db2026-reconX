package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconSummaryCollectorTest {

    @Test
    void empty_returnsAllZeroCounts() {
        ReconSummary summary = ReconSummary.empty();

        assertThat(summary.total()).isZero();
        assertThat(summary.matched()).isZero();
        assertThat(summary.broken()).isZero();
    }

    @Test
    void serialAndParallelStreams_produceIdenticalSummary_over10kResults() {
        List<ReconResult> results = build10kResults();

        ReconSummary serial = results.stream().collect(new ReconSummaryCollector());
        ReconSummary parallel = results.parallelStream().collect(new ReconSummaryCollector());

        assertThat(parallel).isEqualTo(serial);
        assertThat(serial.total()).isEqualTo(10_000);
        assertThat(serial.matched()).isEqualTo(7_000);
        assertThat(serial.broken()).isEqualTo(3_000);
    }

    private List<ReconResult> build10kResults() {
        List<ReconResult> results = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            String ref = "REF-" + i;
            results.add(i % 10 < 7
                    ? ReconResult.matched(ref)
                    : ReconResult.breakResult(ref, "VALUE_MISMATCH", "diff"));
        }
        return results;
    }
}
