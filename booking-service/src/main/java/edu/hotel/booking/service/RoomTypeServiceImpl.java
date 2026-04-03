package edu.hotel.booking.service;

import edu.hotel.booking.dto.roomtype.RoomTypeRequest;
import edu.hotel.booking.dto.roomtype.RoomTypeResponse;
import edu.hotel.booking.dto.tariff.TariffRequest;
import edu.hotel.booking.dto.tariff.TariffResponse;
import edu.hotel.booking.entity.Hotel;
import edu.hotel.booking.entity.RoomType;
import edu.hotel.booking.entity.Tariff;
import edu.hotel.booking.mapper.RoomTypeMapper;
import edu.hotel.booking.mapper.TariffMapper;
import edu.hotel.booking.repository.HotelRepository;
import edu.hotel.booking.repository.RoomRepository;
import edu.hotel.booking.repository.RoomTypeRepository;
import edu.hotel.booking.repository.TariffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;
    private final TariffRepository tariffRepository;

    private final RoomTypeMapper roomTypeMapper;
    private final TariffMapper tariffMapper;

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> findAvailableRoomTypes(
            Long hotelId, LocalDate checkIn, LocalDate checkOut) {

        List<RoomType> roomTypes = roomTypeRepository.findWithTariffsByHotelId(hotelId);

        return roomTypes.stream()
                .filter(roomType -> roomRepository
                        .findFirstAvailableRoom(roomType.getId(), checkIn, checkOut)
                        .isPresent())
                .map(roomTypeMapper::toResponse)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(Long id, RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отель с id " + id + " не найден"));

        RoomType roomType = roomTypeMapper.toEntity(request);
        roomType.setHotel(hotel);
        RoomType createRoomType = roomTypeRepository.save(roomType);
        return roomTypeMapper.toResponse(createRoomType);
    }

    @Override
    @Transactional
    public TariffResponse createTariff(Long id, TariffRequest request) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тип номера с id " + id + " не найден"));

        Tariff tariff = tariffMapper.toEntity(request);
        tariff.setRoomType(roomType);
        Tariff createTariff = tariffRepository.save(tariff);
        return tariffMapper.toResponse(createTariff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TariffResponse> getActualTariffs(Long roomTypeId) {
        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Тип номера с id: " + roomTypeId + " не найден"));
        List<Tariff> actualTariffs = tariffRepository
                .findActualByRoomTypeId(roomTypeId, LocalDate.now());
        return actualTariffs.stream()
                .map(tariffMapper::toResponse)
                .collect(Collectors.toList());
    }
}
