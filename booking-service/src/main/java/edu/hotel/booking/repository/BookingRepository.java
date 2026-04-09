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

    @EntityGraph(attributePaths = {"room", "room.roomType", "room.roomType.hotel", "guest", "tariff"})
    Optional<Booking> findDetailById(Long id);

    @Query(value = """
            SELECT b.* FROM booking b
            JOIN room r ON r.id = b.room_id
            JOIN room_type rt ON rt.id = r.room_type_id
            WHERE (CAST(:hotelId AS BIGINT) IS NULL OR rt.hotel_id = CAST(:hotelId AS BIGINT))
            AND (CAST(:status AS VARCHAR) IS NULL OR b.status = CAST(:status AS VARCHAR))
            AND (CAST(:dateFrom AS DATE) IS NULL OR b.check_in >= CAST(:dateFrom AS DATE))
            AND (CAST(:dateTo AS DATE) IS NULL OR b.check_out <= CAST(:dateTo AS DATE))
            """,
            countQuery = """
            SELECT COUNT(*) FROM booking b
            JOIN room r ON r.id = b.room_id
            JOIN room_type rt ON rt.id = r.room_type_id
            WHERE (CAST(:hotelId AS BIGINT) IS NULL OR rt.hotel_id = CAST(:hotelId AS BIGINT))
            AND (CAST(:status AS VARCHAR) IS NULL OR b.status = CAST(:status AS VARCHAR))
            AND (CAST(:dateFrom AS DATE) IS NULL OR b.check_in >= CAST(:dateFrom AS DATE))
            AND (CAST(:dateTo AS DATE) IS NULL OR b.check_out <= CAST(:dateTo AS DATE))
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