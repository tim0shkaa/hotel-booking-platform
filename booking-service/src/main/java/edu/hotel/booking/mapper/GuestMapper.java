package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.guest.GuestResponse;
import edu.hotel.booking.entity.Guest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GuestMapper {

    GuestResponse toResponse(Guest guest);
}
