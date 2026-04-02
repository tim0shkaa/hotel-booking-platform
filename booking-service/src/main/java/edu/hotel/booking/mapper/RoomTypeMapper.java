package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.roomtype.RoomTypeRequest;
import edu.hotel.booking.dto.roomtype.RoomTypeResponse;
import edu.hotel.booking.entity.RoomType;
import edu.hotel.booking.entity.Tariff;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "Spring", uses = {TariffMapper.class})
public interface RoomTypeMapper {

    RoomType toEntity(RoomTypeRequest request);

    RoomTypeResponse toResponse(RoomType roomType);

    @AfterMapping
    default void calculateMinPrice(@MappingTarget RoomTypeResponse response, RoomType roomType) {
        if (roomType.getTariffs() != null && !roomType.getTariffs().isEmpty()) {
            BigDecimal minPrice = roomType.getTariffs().stream()
                    .map(Tariff::getPricePerNight)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            response.setMinPriceNight(minPrice);
        }
    }
}
