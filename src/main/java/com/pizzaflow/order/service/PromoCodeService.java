package com.pizzaflow.order.service;

import com.pizzaflow.common.exception.ConflictException;
import com.pizzaflow.common.exception.InvalidPromoCodeException;
import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.order.dto.CreatePromoCodeRequest;
import com.pizzaflow.order.dto.PromoCodeResponse;
import com.pizzaflow.order.dto.PromoValidationResponse;
import com.pizzaflow.order.entity.DiscountType;
import com.pizzaflow.order.entity.PromoCode;
import com.pizzaflow.order.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    public PromoCodeService(PromoCodeRepository promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    @Transactional(readOnly = true)
    public PromoValidationResponse validate(String code, BigDecimal orderAmount) {
        try {
            BigDecimal discount = calculateDiscount(code, orderAmount);
            BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);
            return new PromoValidationResponse(true, code.toUpperCase(), "Промокод успешно применен", discount, finalAmount);
        } catch (InvalidPromoCodeException ex) {
            return new PromoValidationResponse(false, code, ex.getMessage(), BigDecimal.ZERO, orderAmount);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        if (code == null || code.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new InvalidPromoCodeException("Промокод '" + code + "' не найден"));

        if (!promo.isActive()) {
            throw new InvalidPromoCodeException("Промокод '" + code + "' неактивен");
        }

        Instant now = Instant.now();
        if (now.isBefore(promo.getValidFrom()) || now.isAfter(promo.getValidTo())) {
            throw new InvalidPromoCodeException("Срок действия промокода '" + code + "' истёк");
        }

        if (promo.getUsageLimit() != null && promo.getTimesUsed() >= promo.getUsageLimit()) {
            throw new InvalidPromoCodeException("Лимит использований промокода '" + code + "' исчерпан");
        }

        if (orderAmount.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new InvalidPromoCodeException(String.format(
                    "Минимальная сумма заказа для применения промокода '%s' составляет %.2f руб. (текущая: %.2f руб.)",
                    promo.getCode(), promo.getMinOrderAmount(), orderAmount));
        }

        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promo.getMaxDiscountAmount() != null && discount.compareTo(promo.getMaxDiscountAmount()) > 0) {
                discount = promo.getMaxDiscountAmount();
            }
        } else {
            discount = promo.getDiscountValue().min(orderAmount);
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void incrementUsage(String code) {
        if (code == null || code.trim().isEmpty()) return;
        promoCodeRepository.findByCodeIgnoreCase(code.trim()).ifPresent(promo -> {
            promo.setTimesUsed(promo.getTimesUsed() + 1);
            promoCodeRepository.save(promo);
        });
    }

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> getAll() {
        return promoCodeRepository.findAll().stream()
                .map(PromoCodeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromoCodeResponse create(CreatePromoCodeRequest req) {
        if (promoCodeRepository.existsByCodeIgnoreCase(req.getCode())) {
            throw new ConflictException("Промокод с кодом '" + req.getCode() + "' уже существует");
        }
        PromoCode promo = new PromoCode(
                req.getCode().trim().toUpperCase(),
                req.getDiscountType(),
                req.getDiscountValue(),
                req.getMinOrderAmount(),
                req.getMaxDiscountAmount(),
                req.getUsageLimit(),
                req.getValidFrom(),
                req.getValidTo()
        );
        return PromoCodeResponse.fromEntity(promoCodeRepository.save(promo));
    }
}
