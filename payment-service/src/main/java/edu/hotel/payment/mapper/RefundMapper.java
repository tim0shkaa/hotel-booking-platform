package edu.hotel.payment.mapper;

import edu.hotel.payment.dto.refund.RefundRequest;
import edu.hotel.payment.dto.refund.RefundResponse;
import edu.hotel.payment.entity.Refund;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    Refund toEntity(RefundRequest request);

    RefundResponse toResponse(Refund refund);
}
