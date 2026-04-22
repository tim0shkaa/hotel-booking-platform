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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private RoomMapper roomMapper;

    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    void findRoomsByRoomTypeId_shouldReturnList_whenRoomTypeExists() {
        Long roomTypeId = 1L;
        Room room = new Room();
        RoomResponse response = new RoomResponse();

        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(new RoomType()));
        when(roomRepository.findByRoomTypeId(roomTypeId)).thenReturn(List.of(room));
        when(roomMapper.toResponse(room)).thenReturn(response);

        List<RoomResponse> result = roomService.findRoomsByRoomTypeId(roomTypeId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(response);
    }

    @Test
    void findRoomsByRoomTypeId_shouldThrowNotFoundException_whenRoomTypeNotFound() {
        when(roomTypeRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.findRoomsByRoomTypeId(99L))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository, never()).findByRoomTypeId(any());
    }

    @Test
    void create_shouldSetRoomTypeAndStatusAvailable() {
        Long roomTypeId = 1L;
        RoomRequest request = new RoomRequest();
        RoomType roomType = new RoomType();
        Room room = new Room();
        Room savedRoom = new Room();
        RoomResponse response = new RoomResponse();

        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(roomMapper.toEntity(request)).thenReturn(room);
        when(roomRepository.save(room)).thenReturn(savedRoom);
        when(roomMapper.toResponse(savedRoom)).thenReturn(response);

        RoomResponse result = roomService.create(roomTypeId, request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getRoomType()).isEqualTo(roomType);
        assertThat(captor.getValue().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void create_shouldThrowNotFoundException_whenRoomTypeNotFound() {
        when(roomTypeRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.create(99L, new RoomRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateStatus() {
        Long roomId = 1L;
        Room room = new Room();
        Room savedRoom = new Room();
        RoomResponse response = new RoomResponse();

        RoomStatusRequest request = new RoomStatusRequest();
        request.setStatus(RoomStatus.MAINTENANCE);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(savedRoom);
        when(roomMapper.toResponse(savedRoom)).thenReturn(response);

        RoomResponse result = roomService.update(roomId, request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    void update_shouldThrowNotFoundException_whenRoomNotFound() {
        when(roomRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(99L, new RoomStatusRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository, never()).save(any());
    }
}