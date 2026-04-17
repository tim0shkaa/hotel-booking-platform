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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HotelSummaryResponse> getHotelsWithFilters(String city, Integer starRating, List<String> amenities, boolean isAdmin, Pageable pageable) {
        String amenitiesJson = null;
        if (amenities != null && !amenities.isEmpty()) {
            amenitiesJson = amenities.stream()
                    .collect(Collectors.joining("\",\"", "[\"", "\"]"));
        }
        Page<Hotel> hotels = isAdmin
                ? hotelRepository.findAllWithFilters(city, starRating, amenitiesJson, pageable)
                : hotelRepository.findAllActiveWithFilters(city, starRating, amenitiesJson, pageable);
        return hotels.map(hotelMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDetailResponse getHotelById(Long id) {
        Hotel hotel = hotelRepository.findWithRoomsTypesById(id)
                .orElseThrow(() -> new NotFoundException("Отель с id: " + id + " не найден"));

        List<RoomType> roomTypes = roomTypeRepository.findWithTariffsByHotelId(id);

        HotelDetailResponse response = hotelMapper.toDetailResponse(hotel);

        response.setRoomTypes(roomTypes.stream()
                .map(roomTypeMapper::toResponse)
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    @Transactional
    public HotelDetailResponse create(HotelRequest request) {
        Hotel hotel = hotelMapper.toEntity(request);
        hotel.setActive(false);
        hotel.setTotalReviews(0);
        Hotel savedHotel = hotelRepository.save(hotel);
        return hotelMapper.toDetailResponse(savedHotel);
    }

    @Override
    @Transactional
    public HotelDetailResponse update(Long id, HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Отель с id: " + id + " не найден"));
        hotelMapper.updateEntityFromRequest(request, hotel);
        Hotel updatedHotel = hotelRepository.save(hotel);
        return hotelMapper.toDetailResponse(updatedHotel);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Hotel hotel = hotelRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Отель с id: " + id + " не найден"));
        hotel.setActive(true);
        hotelRepository.save(hotel);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Hotel hotel = hotelRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Отель с id: " + id + " не найден"));
        hotel.setActive(false);
        hotelRepository.save(hotel);
    }
}
