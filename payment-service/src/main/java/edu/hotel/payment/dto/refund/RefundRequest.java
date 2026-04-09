package edu.hotel.payment.dto.refund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {

    @NotNull(message = "Нужно число")
    @Positive
    private BigDecimal amount;

    @NotBlank(message = "Причина не может быть пустой")
    private String reason;
}
