package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
    private ScenarioResult pessimist;
    private ScenarioResult neutral;
    private ScenarioResult optimist;
}
