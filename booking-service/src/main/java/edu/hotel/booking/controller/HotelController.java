package edu.hotel.booking.controller;

import edu.hotel.booking.dto.hotel.HotelDetailResponse;
import edu.hotel.booking.dto.hotel.HotelRequest;
import edu.hotel.booking.dto.hotel.HotelSummaryResponse;
import edu.hotel.booking.dto.roomtype.RoomTypeRequest;
import edu.hotel.booking.dto.roomtype.RoomTypeResponse;
import edu.hotel.booking.service.HotelService;
import edu.hotel.booking.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    private final RoomTypeService roomTypeService;

    @PostMapping()
    public ResponseEntity<HotelDetailResponse> create(@Valid @RequestBody HotelRequest request) {
        HotelDetailResponse response = hotelService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelDetailResponse> getHotelById(@PathVariable("id") Long id) {
        HotelDetailResponse response = hotelService.getHotelById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<HotelSummaryResponse>> getHotelsWithFilters(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer starRating,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(defaultValue = "false") boolean isAdmin,
            Pageable pageable) {
        return ResponseEntity.ok(
                hotelService.getHotelsWithFilters(city, starRating, amenities, isAdmin, pageable)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelDetailResponse> update(@PathVariable("id") Long id, @Valid @RequestBody HotelRequest request) {
        HotelDetailResponse response = hotelService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable("id") Long id) {
        hotelService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable("id") Long id) {
        hotelService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<RoomTypeResponse>> findAvailableRoomTypes(
            @PathVariable("id") Long hotelId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut
    ) {
        List<RoomTypeResponse> responses = roomTypeService.findAvailableRoomTypes(
                hotelId, checkIn, checkOut
        );
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/room-types")
    public ResponseEntity<RoomTypeResponse> createRoomType(
            @PathVariable("id") Long hotelId,
            @Valid @RequestBody RoomTypeRequest request) {
        RoomTypeResponse response = roomTypeService.createRoomType(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
