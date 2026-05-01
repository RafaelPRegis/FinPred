package com.finpred.prediction.service;

import com.finpred.prediction.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class RestCoreServiceClient implements CoreServiceClient {

    private final RestTemplate restTemplate;
    
    // No Docker Compose, os serviços se comunicam pelo nome do container
    private final String coreServiceUrl = "http://core-service:8082/api/core/transactions";

    @Override
    public List<TransactionDTO> getUserTransactions(Long userId, String token) {
        log.info("Usando RestCoreServiceClient (Perfil PROD) para buscar transações do usuário {}", userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<List<TransactionDTO>> response = restTemplate.exchange(
                coreServiceUrl,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<TransactionDTO>>() {}
        );
        
        return response.getBody();
    }
}
