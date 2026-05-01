package com.finpred.core.controller;

import com.finpred.core.dto.TaxSimulationRequest;
import com.finpred.core.dto.TaxSimulationResponse;
import com.finpred.core.service.TaxCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/taxes")
@RequiredArgsConstructor
public class TaxController {

    private final TaxCalculatorService taxCalculatorService;

    @PostMapping("/simulate")
    public ResponseEntity<TaxSimulationResponse> simulate(@Valid @RequestBody TaxSimulationRequest request) {
        return ResponseEntity.ok(taxCalculatorService.simulate(request));
    }
}
