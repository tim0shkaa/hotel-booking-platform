package edu.hotel.review.entity;

import edu.hotel.review.model.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "rating_aggregate")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class RatingAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(nullable = false)
    private Integer totalReviews;

    @Column(columnDefinition = "text")
    private String ratingDistribution;
}
