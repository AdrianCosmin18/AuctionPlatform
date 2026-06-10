package org.nedelcu.cosmin.auction.api.analytics.dto;

public record CategoryMetricResponse(
        String categoryCode,
        long count
) {
}
