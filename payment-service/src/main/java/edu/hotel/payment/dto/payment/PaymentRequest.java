package edu.hotel.payment.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Id бронирования обязательно")
    private Long bookingId;
}
