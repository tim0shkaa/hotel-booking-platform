package edu.hotel.booking.controller;

import edu.hotel.booking.dto.room.RoomResponse;
import edu.hotel.booking.dto.room.RoomStatusRequest;
import edu.hotel.booking.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PutMapping("/rooms/{id}/status")
    public ResponseEntity<RoomResponse> update(
            @PathVariable("id") Long id, @Valid @RequestBody RoomStatusRequest request) {
        RoomResponse response = roomService.update(id, request);
        return ResponseEntity.ok(response);
    }
}
