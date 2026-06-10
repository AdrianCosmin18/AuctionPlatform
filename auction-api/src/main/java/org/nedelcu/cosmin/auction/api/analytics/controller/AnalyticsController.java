package org.nedelcu.cosmin.auction.api.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.analytics.dto.AnalyticsDashboardResponse;
import org.nedelcu.cosmin.auction.api.analytics.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public AnalyticsDashboardResponse getDashboard() {
        return analyticsService.getDashboard();
    }
}
