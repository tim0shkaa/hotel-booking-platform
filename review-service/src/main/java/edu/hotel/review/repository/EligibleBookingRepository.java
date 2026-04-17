package edu.hotel.review.repository;

import edu.hotel.review.entity.EligibleBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EligibleBookingRepository extends JpaRepository<EligibleBooking, Long> {

    Optional<EligibleBooking> findByBookingId(Long bookingId);
}
