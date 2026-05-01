package com.finpred.prediction.controller;

import com.finpred.prediction.dto.SimulationRequest;
import com.finpred.prediction.dto.SimulationResponse;
import com.finpred.prediction.service.PredictionEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionEngineService predictionEngineService;

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> simulate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody SimulationRequest request) {
        
        SimulationResponse response = predictionEngineService.simulate(request);
        return ResponseEntity.ok(response);
    }
}
