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
public class ReviewCreatedEvent {

    private String eventId;

    private String eventType;

    private Long reviewId;

    private Long hotelId;

    private Long roomTypeId;

    private Integer overallRating;

    private LocalDateTime occurredAt;
}
