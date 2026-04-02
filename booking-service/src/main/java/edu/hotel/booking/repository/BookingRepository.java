package edu.hotel.booking.repository;

import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {


    Page<Booking> findByGuestId(Long guestId, Pageable pageable);

    @EntityGraph(attributePaths = {"statusHistory", "room", "room.roomType", "room.roomType.hotel", "guest", "tariff"})
    Optional<Booking> findDetailById(Long id);

    @Query(value = """
            SELECT b.* FROM booking b
            JOIN room r ON r.id = b.room_id
            JOIN room_type rt ON rt.id = r.room_type_id
            WHERE (:hotelId IS NULL OR rt.hotel_id = :hotelId)
            AND (:status IS NULL OR b.status = :status)
            AND (:dateFrom IS NULL OR b.check_in >= :dateFrom)
            AND (:dateTo IS NULL OR b.check_out <= :dateTo)
            """,
            countQuery = """
            SELECT COUNT(*) FROM booking b
            JOIN room r ON r.id = b.room_id
            JOIN room_type rt ON rt.id = r.room_type_id
            WHERE (:hotelId IS NULL OR rt.hotel_id = :hotelId)
            AND (:status IS NULL OR b.status = :status)
            AND (:dateFrom IS NULL OR b.check_in >= :dateFrom)
            AND (:dateTo IS NULL OR b.check_out <= :dateTo)
            """,
            nativeQuery = true)
    Page<Booking> findAllWithFilters(
            @Param("hotelId") Long hotelId,
            @Param("status") String status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );
}