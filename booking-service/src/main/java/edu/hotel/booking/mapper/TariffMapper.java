package edu.hotel.booking.mapper;

import edu.hotel.booking.dto.tariff.TariffRequest;
import edu.hotel.booking.dto.tariff.TariffResponse;
import edu.hotel.booking.entity.Tariff;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface TariffMapper {

    Tariff toEntity(TariffRequest request);

    TariffResponse toResponse(Tariff tariff);
}
