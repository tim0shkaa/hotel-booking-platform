package edu.hotel.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    private String eventId;

    private String eventType;

    private Long paymentId;

    private Long bookingId;

    private Long guestId;

    private String reason;

    private LocalDateTime occurredAt;
}
