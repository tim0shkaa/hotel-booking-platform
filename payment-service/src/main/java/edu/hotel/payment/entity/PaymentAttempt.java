package edu.hotel.payment.entity;

import edu.hotel.payment.model.AttemptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_attempt")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"payment"})
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    private String errorCode;

    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime attemptedAt;
}
