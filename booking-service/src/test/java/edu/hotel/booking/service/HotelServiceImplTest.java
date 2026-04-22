package edu.hotel.booking.service;

import edu.hotel.booking.dto.hotel.HotelDetailResponse;
import edu.hotel.booking.dto.hotel.HotelRequest;
import edu.hotel.booking.dto.hotel.HotelSummaryResponse;
import edu.hotel.booking.entity.Hotel;
import edu.hotel.booking.entity.RoomType;
import edu.hotel.booking.mapper.HotelMapper;
import edu.hotel.booking.mapper.RoomTypeMapper;
import edu.hotel.booking.repository.HotelRepository;
import edu.hotel.booking.repository.RoomTypeRepository;
import edu.hotel.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private HotelMapper hotelMapper;
    @Mock private RoomTypeMapper roomTypeMapper;

    @InjectMocks
    private HotelServiceImpl hotelService;

    @Test
    void getHotelsWithFilters_shouldCallFindAllWithFilters_whenIsAdmin() {
        Pageable pageable = Pageable.unpaged();
        Hotel hotel = new Hotel();
        Page<Hotel> page = new PageImpl<>(List.of(hotel));
        HotelSummaryResponse summaryResponse = new HotelSummaryResponse();

        when(hotelRepository.findAllWithFilters(any(), any(), any(), eq(pageable))).thenReturn(page);
        when(hotelMapper.toSummaryResponse(hotel)).thenReturn(summaryResponse);

        Page<HotelSummaryResponse> result = hotelService.getHotelsWithFilters(null, null, null, true, pageable);

        assertThat(result).hasSize(1);
        verify(hotelRepository).findAllWithFilters(any(), any(), any(), eq(pageable));
        verify(hotelRepository, never()).findAllActiveWithFilters(any(), any(), any(), any());
    }

    @Test
    void getHotelsWithFilters_shouldCallFindAllActiveWithFilters_whenIsNotAdmin() {
        Pageable pageable = Pageable.unpaged();
        Page<Hotel> page = new PageImpl<>(List.of());

        when(hotelRepository.findAllActiveWithFilters(any(), any(), any(), eq(pageable))).thenReturn(page);

        Page<HotelSummaryResponse> result = hotelService.getHotelsWithFilters(null, null, null, false, pageable);

        assertThat(result).isEmpty();
        verify(hotelRepository).findAllActiveWithFilters(any(), any(), any(), eq(pageable));
        verify(hotelRepository, never()).findAllWithFilters(any(), any(), any(), any());
    }

    @Test
    void getHotelsWithFilters_shouldBuildAmenitiesJson_whenAmenitiesProvided() {
        Pageable pageable = Pageable.unpaged();
        Page<Hotel> page = new PageImpl<>(List.of());

        when(hotelRepository.findAllActiveWithFilters(any(), any(), any(), eq(pageable))).thenReturn(page);

        hotelService.getHotelsWithFilters(null, null, List.of("WiFi", "Pool"), false, pageable);

        verify(hotelRepository).findAllActiveWithFilters(isNull(), isNull(), eq("[\"WiFi\",\"Pool\"]"), eq(pageable));
    }

    @Test
    void getHotelsWithFilters_shouldPassNullAmenities_whenAmenitiesEmpty() {
        Pageable pageable = Pageable.unpaged();
        Page<Hotel> page = new PageImpl<>(List.of());

        when(hotelRepository.findAllActiveWithFilters(any(), any(), any(), eq(pageable))).thenReturn(page);

        hotelService.getHotelsWithFilters(null, null, List.of(), false, pageable);

        verify(hotelRepository).findAllActiveWithFilters(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getHotelById_shouldReturnDetailResponse_whenHotelExists() {
        Long hotelId = 1L;
        Hotel hotel = new Hotel();
        RoomType roomType = new RoomType();
        HotelDetailResponse response = new HotelDetailResponse();

        when(hotelRepository.findWithRoomsTypesById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findWithTariffsByHotelId(hotelId)).thenReturn(List.of(roomType));
        when(hotelMapper.toDetailResponse(hotel)).thenReturn(response);
        when(roomTypeMapper.toResponse(roomType)).thenReturn(any());

        HotelDetailResponse result = hotelService.getHotelById(hotelId);

        assertThat(result).isEqualTo(response);
        verify(roomTypeMapper).toResponse(roomType);
    }

    @Test
    void getHotelById_shouldThrowNotFoundException_whenHotelNotFound() {
        when(hotelRepository.findWithRoomsTypesById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.getHotelById(99L))
                .isInstanceOf(NotFoundException.class);

        verify(roomTypeRepository, never()).findWithTariffsByHotelId(any());
    }

    @Test
    void create_shouldSetActiveToFalseAndTotalReviewsToZero() {
        HotelRequest request = new HotelRequest();
        Hotel hotel = new Hotel();
        Hotel savedHotel = new Hotel();
        HotelDetailResponse response = new HotelDetailResponse();

        when(hotelMapper.toEntity(request)).thenReturn(hotel);
        when(hotelRepository.save(hotel)).thenReturn(savedHotel);
        when(hotelMapper.toDetailResponse(savedHotel)).thenReturn(response);

        HotelDetailResponse result = hotelService.create(request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getTotalReviews()).isZero();
    }

    @Test
    void update_shouldReturnUpdatedResponse_whenHotelExists() {
        Long hotelId = 1L;
        HotelRequest request = new HotelRequest();
        Hotel hotel = new Hotel();
        Hotel updatedHotel = new Hotel();
        HotelDetailResponse response = new HotelDetailResponse();

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(hotelRepository.save(hotel)).thenReturn(updatedHotel);
        when(hotelMapper.toDetailResponse(updatedHotel)).thenReturn(response);

        HotelDetailResponse result = hotelService.update(hotelId, request);

        assertThat(result).isEqualTo(response);
        verify(hotelMapper).updateEntityFromRequest(eq(request), eq(hotel));
    }

    @Test
    void update_shouldThrowNotFoundException_whenHotelNotFound() {
        when(hotelRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.update(99L, new HotelRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(hotelRepository, never()).save(any());
    }

    @Test
    void activate_shouldSetActiveToTrue_whenHotelExists() {
        Long hotelId = 1L;
        Hotel hotel = new Hotel();
        hotel.setActive(false);

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        hotelService.activate(hotelId);

        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void activate_shouldThrowNotFoundException_whenHotelNotFound() {
        when(hotelRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.activate(99L))
                .isInstanceOf(NotFoundException.class);

        verify(hotelRepository, never()).save(any());
    }

    @Test
    void deactivate_shouldSetActiveToFalse_whenHotelExists() {
        Long hotelId = 1L;
        Hotel hotel = new Hotel();
        hotel.setActive(true);

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        hotelService.deactivate(hotelId);

        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deactivate_shouldThrowNotFoundException_whenHotelNotFound() {
        when(hotelRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.deactivate(99L))
                .isInstanceOf(NotFoundException.class);

        verify(hotelRepository, never()).save(any());
    }
}