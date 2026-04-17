package edu.hotel.review.repository;

import edu.hotel.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository  extends JpaRepository<Review, Long> {

    boolean existsByBookingId(Long bookingId);

    @Query(value = """
        SELECT * FROM review
        WHERE hotel_id = :hotelId
        """,
            countQuery = """
        SELECT COUNT(*) FROM review
        WHERE hotel_id = :hotelId
        """,
            nativeQuery = true)
    Page<Review> findAllByHotelId(@Param("hotelId") Long hotelId,
                                  Pageable pageable);

    @Query(value = """
        SELECT * FROM review
        WHERE room_type_id = :roomTypeId
        """,
            countQuery = """
        SELECT COUNT(*) FROM review
        WHERE room_type_id = :roomTypeId
        """,
            nativeQuery = true)
    Page<Review> findAllByRoomTypeId(@Param("roomTypeId") Long roomTypeId,
                                     Pageable pageable);
}
