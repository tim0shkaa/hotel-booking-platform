package edu.hotel.booking.repository;

import edu.hotel.booking.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @EntityGraph(attributePaths = {"roomTypes"})
    Optional<Hotel> findWithRoomsTypesById(Long id);

    @Query(value = """
            SELECT * FROM hotels
            WHERE (CAST(:city AS VARCHAR) IS NULL OR city = CAST(:city AS VARCHAR))
            AND (CAST(:starRating AS INTEGER) IS NULL OR star_rating = CAST(:starRating AS INTEGER))
            AND (:amenities IS NULL OR amenities @> CAST(:amenities AS jsonb))
            """,
            countQuery = """
            SELECT COUNT(*) FROM hotels
            WHERE (CAST(:city AS VARCHAR) IS NULL OR city = CAST(:city AS VARCHAR))
            AND (CAST(:starRating AS INTEGER) IS NULL OR star_rating = CAST(:starRating AS INTEGER))
            AND (:amenities IS NULL OR amenities @> CAST(:amenities AS jsonb))
            """,
            nativeQuery = true)
    Page<Hotel> findAllWithFilters(
            @Param("city") String city,
            @Param("starRating") Integer starRating,
            @Param("amenities") String amenities,
            Pageable pageable
    );

    @Query(value = """
            SELECT * FROM hotels
            WHERE (CAST(:city AS VARCHAR) IS NULL OR city = CAST(:city AS VARCHAR))
            AND (CAST(:starRating AS INTEGER) IS NULL OR star_rating = CAST(:starRating AS INTEGER))
            AND (:amenities IS NULL OR amenities @> CAST(:amenities AS jsonb))
            """,
            countQuery = """
            SELECT COUNT(*) FROM hotels
            WHERE active = true
            AND (CAST(:city AS VARCHAR) IS NULL OR city = CAST(:city AS VARCHAR))
            AND (CAST(:starRating AS INTEGER) IS NULL OR star_rating = CAST(:starRating AS INTEGER))
            AND (:amenities IS NULL OR amenities @> CAST(:amenities AS jsonb))
            """,
            nativeQuery = true)
    Page<Hotel> findAllActiveWithFilters(
            @Param("city") String city,
            @Param("starRating") Integer starRating,
            @Param("amenities") String amenities,
            Pageable pageable
    );
}
