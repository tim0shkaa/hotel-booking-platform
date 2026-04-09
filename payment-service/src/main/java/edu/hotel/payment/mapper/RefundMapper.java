package edu.hotel.payment.mapper;

import edu.hotel.payment.dto.refund.RefundRequest;
import edu.hotel.payment.dto.refund.RefundResponse;
import edu.hotel.payment.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    @Mapping(source = "payment.id", target = "paymentId")
    RefundResponse toResponse(Refund refund);
}
