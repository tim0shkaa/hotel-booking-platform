package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.room.RoomRequest;
import edu.hotel.booking.dto.room.RoomResponse;
import edu.hotel.booking.dto.room.RoomStatusRequest;
import edu.hotel.booking.dto.room.RoomSummaryResponse;
import edu.hotel.booking.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")

public interface RoomMapper {

    Room toEntity(RoomRequest request);

    RoomResponse toResponse(Room room);

    void updateStatusFromRequest(RoomStatusRequest statusRequest, @MappingTarget Room room);

    @Mapping(source = "roomType.name", target = "roomTypeName")
    @Mapping(source = "roomType.hotel.name", target = "hotelName")
    @Mapping(source = "roomType.hotel.city", target = "hotelCity")
    RoomSummaryResponse toSummaryResponse(Room room);
}
