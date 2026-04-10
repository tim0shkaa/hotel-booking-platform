package edu.hotel.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEvent {

    private String eventId;

    private String eventType;

    private Long paymentId;

    private Long bookingId;

    private Long guestId;

    private BigDecimal amount;

    private String currency;

    private LocalDateTime occurredAt;
}
