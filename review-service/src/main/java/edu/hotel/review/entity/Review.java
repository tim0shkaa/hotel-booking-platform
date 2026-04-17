package edu.hotel.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long guestId;

    @Column(nullable = false)
    private Long hotelId;

    @Column(nullable = false)
    private Long roomTypeId;

    @Column(nullable = false)
    private Integer overallRating;

    @Column(nullable = false)
    private Integer cleanlinessRating;

    @Column(nullable = false)
    private Integer serviceRating;

    @Column(nullable = false)
    private Integer locationRating;

    @Column(nullable = false)
    private Integer valueRating;

    @Column(columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
