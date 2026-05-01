package com.finpred.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações centralizadas do API Gateway.
 * CORS é configurado via application.yml (spring.cloud.gateway.globalcors).
 */
@Configuration
public class GatewayConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String getJwtSecret() {
        return jwtSecret;
    }
}
