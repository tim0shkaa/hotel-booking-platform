package edu.hotel.auth.entity;

import edu.hotel.auth.model.Action;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Setter
@Getter
@NoArgsConstructor
@ToString(exclude = {"user"})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    private String ipAddress;

    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
