package edu.hotel.booking.service;

import edu.hotel.booking.dto.room.RoomRequest;
import edu.hotel.booking.dto.room.RoomResponse;
import edu.hotel.booking.dto.room.RoomStatusRequest;
import edu.hotel.booking.model.RoomStatus;

import java.util.List;

public interface RoomService {

    // GET /room-types/{id}/rooms
    List<RoomResponse> findRoomsByRoomTypeId(Long roomTypeId);

    // POST /room-types/{id}/rooms
    RoomResponse create(Long roomTypeId, RoomRequest request);

    // PUT /rooms/{id}/status
    RoomResponse update(Long id, RoomStatusRequest request);
}
