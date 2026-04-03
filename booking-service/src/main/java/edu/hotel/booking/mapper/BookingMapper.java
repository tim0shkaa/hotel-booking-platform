package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.booking.BookingCreateRequest;
import edu.hotel.booking.dto.booking.BookingDetailResponse;
import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RoomMapper.class, GuestMapper.class, BookingStatusHistoryMapper.class})
public interface BookingMapper {

    Booking toEntity(BookingCreateRequest request);

    @Mapping(source = "room.roomType.id", target = "roomTypeId")
    @Mapping(source = "room.roomType.name", target = "roomTypeName")
    @Mapping(source = "room.roomType.hotel.name", target = "hotelName")
    BookingSummaryResponse toSummaryResponse(Booking booking);

    BookingDetailResponse toDetailResponse(Booking booking);
}
