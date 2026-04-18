package edu.hotel.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {

    private String eventId;

    private String eventType;

    private Long bookingId;

    private Long userId;

    private Long guestId;

    private Long hotelId;

    private Long roomTypeId;

    private Long tariffId;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private BigDecimal totalPrice;

    private String currency;

    private LocalDateTime occurredAt;
}
