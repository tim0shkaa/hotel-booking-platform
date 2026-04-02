package edu.hotel.booking.controller;

import edu.hotel.booking.dto.room.RoomRequest;
import edu.hotel.booking.dto.room.RoomResponse;
import edu.hotel.booking.dto.tariff.TariffRequest;
import edu.hotel.booking.dto.tariff.TariffResponse;
import edu.hotel.booking.entity.Tariff;
import edu.hotel.booking.service.RoomService;
import edu.hotel.booking.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomService roomService;

    private final RoomTypeService roomTypeService;

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomResponse>> findRoomsByRoomTypeId(
            @PathVariable("id") Long roomTypeId) {
        return ResponseEntity.ok(roomService.findRoomsByRoomTypeId(roomTypeId));
    }

    @PostMapping("/{id}/rooms")
    public ResponseEntity<RoomResponse> create(
            @PathVariable("id") Long roomTypeId, @Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.create(roomTypeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/tariffs")
    public ResponseEntity<TariffResponse> createTariff(
            @PathVariable("id") Long roomTypeId, @Valid @RequestBody TariffRequest request) {
        TariffResponse response = roomTypeService.createTariff(roomTypeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/tariffs")
    public ResponseEntity<List<TariffResponse>> getActualTariffs(@PathVariable("id") Long roomTypeId) {
        return ResponseEntity.ok(roomTypeService.getActualTariffs(roomTypeId));
    }
}
