package edu.hotel.booking.service;

import edu.hotel.booking.dto.room.RoomRequest;
import edu.hotel.booking.dto.room.RoomResponse;
import edu.hotel.booking.dto.room.RoomStatusRequest;
import edu.hotel.booking.entity.Room;
import edu.hotel.booking.entity.RoomType;
import edu.hotel.booking.mapper.RoomMapper;
import edu.hotel.booking.model.RoomStatus;
import edu.hotel.booking.repository.RoomRepository;
import edu.hotel.booking.repository.RoomTypeRepository;
import edu.hotel.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> findRoomsByRoomTypeId(Long roomTypeId) {
        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Тип номера с id: " + roomTypeId + " не найден"));

        List<Room> rooms = roomRepository.findByRoomTypeId(roomTypeId);

        return rooms.stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional()
    public RoomResponse create(Long roomTypeId, RoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Тип номера с id: " + roomTypeId + " не найден"));

        Room room = roomMapper.toEntity(request);
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);
        Room updateRoom = roomRepository.save(room);

        return roomMapper.toResponse(updateRoom);
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomStatusRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Комната с id: " + id + " не найдена"));
        room.setStatus(request.getStatus());
        Room updateRoom = roomRepository.save(room);
        return roomMapper.toResponse(updateRoom);
    }
}
