package edu.hotel.payment.mapper;

import edu.hotel.payment.dto.payment.PaymentRequest;
import edu.hotel.payment.dto.payment.PaymentResponse;
import edu.hotel.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    Payment toEntity(PaymentRequest request);

    PaymentResponse toResponse(Payment payment);
}
