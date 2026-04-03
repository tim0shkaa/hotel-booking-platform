package edu.hotel.booking.service;

import edu.hotel.booking.dto.roomtype.RoomTypeRequest;
import edu.hotel.booking.dto.roomtype.RoomTypeResponse;
import edu.hotel.booking.dto.tariff.TariffRequest;
import edu.hotel.booking.dto.tariff.TariffResponse;

import java.time.LocalDate;
import java.util.List;

public interface RoomTypeService {

    // GET /hotels/{id}/availability
    List<RoomTypeResponse> findAvailableRoomTypes(
            Long hotelId, LocalDate checkIn, LocalDate checkOut);

    // POST /hotels/{id}/room-types
    RoomTypeResponse createRoomType(Long hotelId, RoomTypeRequest request);

    // POST /room-types/{id}/tariffs
    TariffResponse createTariff(Long roomTypeId, TariffRequest request);

    // GET /room-types/{id}/tariffs
    List<TariffResponse> getActualTariffs(Long roomTypeId);
}
