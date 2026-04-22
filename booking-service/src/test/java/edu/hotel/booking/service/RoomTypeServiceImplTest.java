package edu.hotel.booking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hotel.booking.dto.roomtype.RoomTypeRequest;
import edu.hotel.booking.dto.roomtype.RoomTypeResponse;
import edu.hotel.booking.dto.tariff.TariffRequest;
import edu.hotel.booking.dto.tariff.TariffResponse;
import edu.hotel.booking.entity.Hotel;
import edu.hotel.booking.entity.Room;
import edu.hotel.booking.entity.RoomType;
import edu.hotel.booking.entity.Tariff;
import edu.hotel.booking.mapper.RoomTypeMapper;
import edu.hotel.booking.mapper.TariffMapper;
import edu.hotel.booking.repository.HotelRepository;
import edu.hotel.booking.repository.RoomRepository;
import edu.hotel.booking.repository.RoomTypeRepository;
import edu.hotel.booking.repository.TariffRepository;
import edu.hotel.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private TariffRepository tariffRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomTypeMapper roomTypeMapper;
    @Mock private TariffMapper tariffMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private SetOperations<String, Object> setOperations;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    @Test
    void findAvailableRoomTypes_shouldReturnCachedResult_whenCacheHit() {
        Long hotelId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 1);
        LocalDate checkOut = LocalDate.of(2025, 6, 4);

        List<RoomTypeResponse> cached = List.of(new RoomTypeResponse());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cached);
        when(objectMapper.convertValue(eq(cached), any(TypeReference.class))).thenReturn(cached);

        List<RoomTypeResponse> result = roomTypeService.findAvailableRoomTypes(hotelId, checkIn, checkOut);

        assertThat(result).hasSize(1);
        verify(roomTypeRepository, never()).findWithTariffsByHotelId(any());
    }

    @Test
    void findAvailableRoomTypes_shouldLoadFromDbAndCache_whenCacheMiss() {
        Long hotelId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 1);
        LocalDate checkOut = LocalDate.of(2025, 6, 4);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        RoomTypeResponse response = new RoomTypeResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(roomTypeRepository.findWithTariffsByHotelId(hotelId)).thenReturn(List.of(roomType));
        when(roomRepository.findFirstAvailableRoom(eq(10L), eq(checkIn), eq(checkOut))).thenReturn(Optional.of(new Room()));
        when(roomTypeMapper.toResponse(roomType)).thenReturn(response);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        List<RoomTypeResponse> result = roomTypeService.findAvailableRoomTypes(hotelId, checkIn, checkOut);

        assertThat(result).hasSize(1);
        verify(valueOperations).set(anyString(), eq(result), any());
        verify(setOperations).add(anyString(), anyString());
    }

    @Test
    void findAvailableRoomTypes_shouldReturnEmptyList_whenNoRoomsAvailable() {
        Long hotelId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 1);
        LocalDate checkOut = LocalDate.of(2025, 6, 4);

        RoomType roomType = new RoomType();
        roomType.setId(10L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(roomTypeRepository.findWithTariffsByHotelId(hotelId)).thenReturn(List.of(roomType));
        when(roomRepository.findFirstAvailableRoom(eq(10L), eq(checkIn), eq(checkOut))).thenReturn(Optional.empty());
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        List<RoomTypeResponse> result = roomTypeService.findAvailableRoomTypes(hotelId, checkIn, checkOut);

        assertThat(result).isEmpty();
        verify(roomTypeMapper, never()).toResponse(any());
    }

    @Test
    void createRoomType_shouldSetHotelAndReturnResponse() {
        Long hotelId = 1L;
        RoomTypeRequest request = new RoomTypeRequest();
        Hotel hotel = new Hotel();
        RoomType roomType = new RoomType();
        RoomType savedRoomType = new RoomType();
        RoomTypeResponse response = new RoomTypeResponse();

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeMapper.toEntity(request)).thenReturn(roomType);
        when(roomTypeRepository.save(roomType)).thenReturn(savedRoomType);
        when(roomTypeMapper.toResponse(savedRoomType)).thenReturn(response);

        RoomTypeResponse result = roomTypeService.createRoomType(hotelId, request);

        assertThat(result).isEqualTo(response);
        assertThat(roomType.getHotel()).isEqualTo(hotel);
    }

    @Test
    void createRoomType_shouldThrowNotFoundException_whenHotelNotFound() {
        when(hotelRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomTypeService.createRoomType(99L, new RoomTypeRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    void createTariff_shouldSetRoomTypeAndReturnResponse() {
        Long roomTypeId = 1L;
        TariffRequest request = new TariffRequest();
        RoomType roomType = new RoomType();
        Tariff tariff = new Tariff();
        Tariff savedTariff = new Tariff();
        TariffResponse response = new TariffResponse();

        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(tariffMapper.toEntity(request)).thenReturn(tariff);
        when(tariffRepository.save(tariff)).thenReturn(savedTariff);
        when(tariffMapper.toResponse(savedTariff)).thenReturn(response);

        TariffResponse result = roomTypeService.createTariff(roomTypeId, request);

        assertThat(result).isEqualTo(response);
        assertThat(tariff.getRoomType()).isEqualTo(roomType);
    }

    @Test
    void createTariff_shouldThrowNotFoundException_whenRoomTypeNotFound() {
        when(roomTypeRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomTypeService.createTariff(99L, new TariffRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(tariffRepository, never()).save(any());
    }

    @Test
    void getActualTariffs_shouldReturnList_whenRoomTypeExists() {
        Long roomTypeId = 1L;
        Tariff tariff = new Tariff();
        TariffResponse response = new TariffResponse();

        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(new RoomType()));
        when(tariffRepository.findActualByRoomTypeId(eq(roomTypeId), any(LocalDate.class))).thenReturn(List.of(tariff));
        when(tariffMapper.toResponse(tariff)).thenReturn(response);

        List<TariffResponse> result = roomTypeService.getActualTariffs(roomTypeId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(response);
    }

    @Test
    void getActualTariffs_shouldThrowNotFoundException_whenRoomTypeNotFound() {
        when(roomTypeRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomTypeService.getActualTariffs(99L))
                .isInstanceOf(NotFoundException.class);

        verify(tariffRepository, never()).findActualByRoomTypeId(any(), any());
    }
}