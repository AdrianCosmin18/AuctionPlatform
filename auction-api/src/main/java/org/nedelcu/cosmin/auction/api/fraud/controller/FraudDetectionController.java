package org.nedelcu.cosmin.auction.api.fraud.controller;

import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudOverviewResponse;
import org.nedelcu.cosmin.auction.api.fraud.service.FraudDetectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/signals")
    public FraudOverviewResponse getSignals() {
        return fraudDetectionService.getSignals();
    }
}
