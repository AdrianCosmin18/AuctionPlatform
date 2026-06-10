package org.nedelcu.cosmin.auction.api.fraud.dto;

import java.util.List;

public record FraudOverviewResponse(
        long totalSignals,
        long highSeveritySignals,
        long mediumSeveritySignals,
        long lowSeveritySignals,
        List<FraudSignalResponse> signals
) {
}
