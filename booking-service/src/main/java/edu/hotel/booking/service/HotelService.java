package edu.hotel.booking.service;

import edu.hotel.booking.dto.hotel.HotelDetailResponse;
import edu.hotel.booking.dto.hotel.HotelRequest;
import edu.hotel.booking.dto.hotel.HotelSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HotelService {

    // GET /hotels — список с фильтрами
    Page<HotelSummaryResponse> getHotelsWithFilters(
            String city,
            Integer starRating,
            List<String> amenities,
            boolean isAdmin,
            Pageable pageable
    );

    // GET /hotels/{id}
    HotelDetailResponse getHotelById(Long id);

    // POST /hotels
    HotelDetailResponse create(HotelRequest request);

    // PUT /hotels/{id}
    HotelDetailResponse update(Long id, HotelRequest request);

    // POST /hotels/{id}/activate
    void activate(Long id);

    // POST /hotels/{id}/deactivate
    void deactivate(Long id);
}