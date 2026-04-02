package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.audit.BookingStatusHistoryResponse;
import edu.hotel.booking.entity.BookingStatusHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingStatusHistoryMapper {

    BookingStatusHistoryResponse toResponse(BookingStatusHistory history);
}
