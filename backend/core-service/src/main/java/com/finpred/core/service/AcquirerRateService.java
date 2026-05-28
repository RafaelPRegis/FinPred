package com.finpred.core.service;

import com.finpred.core.dto.AcquirerRateRequest;
import com.finpred.core.dto.AcquirerRateResponse;
import com.finpred.core.model.AcquirerRate;
import com.finpred.core.repository.AcquirerRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcquirerRateService {

    private final AcquirerRateRepository acquirerRateRepository;

    public List<AcquirerRateResponse> findByUser(Long userId) {
        return acquirerRateRepository.findByUserId(userId).stream()
                .map(AcquirerRateResponse::fromEntity)
                .toList();
    }

    public List<AcquirerRateResponse> findActiveByUser(Long userId) {
        return acquirerRateRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(AcquirerRateResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AcquirerRateResponse create(Long userId, AcquirerRateRequest request) {
        AcquirerRate rate = AcquirerRate.builder()
                .userId(userId)
                .name(request.getName())
                .creditRate(request.getCreditRate())
                .creditRateInstallment2to6(request.getCreditRateInstallment2to6())
                .creditRateInstallment7to12(request.getCreditRateInstallment7to12())
                .debitRate(request.getDebitRate())
                .pixRate(request.getPixRate())
                .monthlyFee(request.getMonthlyFee())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        rate = acquirerRateRepository.save(rate);
        log.info("Adquirente criado: {} — userId={}", rate.getId(), userId);
        return AcquirerRateResponse.fromEntity(rate);
    }

    @Transactional
    public AcquirerRateResponse update(Long id, Long userId, AcquirerRateRequest request) {
        AcquirerRate rate = acquirerRateRepository.findById(id)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Adquirente não encontrado"));

        rate.setName(request.getName());
        rate.setCreditRate(request.getCreditRate());
        rate.setCreditRateInstallment2to6(request.getCreditRateInstallment2to6());
        rate.setCreditRateInstallment7to12(request.getCreditRateInstallment7to12());
        rate.setDebitRate(request.getDebitRate());
        rate.setPixRate(request.getPixRate());
        rate.setMonthlyFee(request.getMonthlyFee());
        if (request.getActive() != null) rate.setActive(request.getActive());

        rate = acquirerRateRepository.save(rate);
        return AcquirerRateResponse.fromEntity(rate);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        AcquirerRate rate = acquirerRateRepository.findById(id)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Adquirente não encontrado"));
        acquirerRateRepository.delete(rate);
    }
}
