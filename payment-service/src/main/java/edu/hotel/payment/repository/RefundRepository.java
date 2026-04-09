package edu.hotel.payment.repository;

import edu.hotel.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findAllByPaymentId(Long paymentId);
}
