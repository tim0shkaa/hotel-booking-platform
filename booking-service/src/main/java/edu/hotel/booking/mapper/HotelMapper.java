package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.hotel.HotelDetailResponse;
import edu.hotel.booking.dto.hotel.HotelRequest;
import edu.hotel.booking.dto.hotel.HotelSummaryResponse;
import edu.hotel.booking.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {RoomTypeMapper.class})
public interface HotelMapper{

    Hotel toEntity(HotelRequest request);

    HotelSummaryResponse toSummaryResponse(Hotel hotel);

    HotelDetailResponse toDetailResponse(Hotel hotel);

    void updateEntityFromRequest(HotelRequest request, @MappingTarget Hotel hotel);
}
